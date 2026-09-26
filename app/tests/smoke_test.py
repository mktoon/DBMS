"""Exercise the packaged Java app through real HTTP against fresh MySQL.

Requires a disposable MySQL server: initialization refuses an existing hospital_v1.
Only fixture/setup code uses root. The Java process uses the restricted app role.
"""
import http.cookiejar
from html.parser import HTMLParser
import importlib.util
import os
from pathlib import Path
import secrets
import socket
import subprocess
import sys
import time
import tempfile
import unittest
from datetime import datetime, timedelta, timezone
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlparse
from urllib.request import build_opener, HTTPCookieProcessor, ProxyHandler
from zoneinfo import ZoneInfo

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("database_tests", ROOT / "database/v1/tests/test_database.py")
db = importlib.util.module_from_spec(spec)
spec.loader.exec_module(db)
APP_PASSWORD = secrets.token_urlsafe(24)
STAFF_PASSWORD = secrets.token_urlsafe(24)
BASE = ""
ZONE = ZoneInfo("America/Los_Angeles")


class Inputs(HTMLParser):
    def __init__(self, text):
        super().__init__()
        self.values = {}
        self.feed(text)

    def handle_starttag(self, tag, attrs):
        a = dict(attrs)
        if tag == "input" and "name" in a:
            self.values[a["name"]] = a.get("value", "")


class Browser:
    def __init__(self):
        self.opener = build_opener(ProxyHandler({}), HTTPCookieProcessor(http.cookiejar.CookieJar()))

    def request(self, path, data=None):
        try:
            response = self.opener.open(BASE + path, None if data is None else urlencode(data).encode(), timeout=15)
        except HTTPError as e:
            response = e
        with response:
            return response.status, response.read().decode(), urlparse(response.url).path

    def post(self, path, data, form_path=None):
        _, page, _ = self.request(form_path or path)
        token = Inputs(page).values.get("_csrf")
        if not token:
            raise AssertionError(f"Missing CSRF token on {form_path or path}")
        return self.request(path, {**data, "_csrf": token})

    def login(self):
        return self.post("/login", {"username": "test_staff", "password": STAFF_PASSWORD})


class WebTests(unittest.TestCase):
    def setUp(self):
        self.browser = Browser()
        self.assertEqual(self.browser.login()[2], "/")
        self.admin = db.connection()
        self.addCleanup(self.admin.close)
        self.patient = db.insert(self.admin, "INSERT INTO patient(full_name,date_of_birth) VALUES (%s,'1990-01-01')",
                                 ("Web Patient " + secrets.token_hex(3),))
        department = db.insert(self.admin, "INSERT INTO department(name) VALUES (%s)", ("Web " + secrets.token_hex(4),))
        self.physician = db.insert(self.admin, "INSERT INTO physician(full_name,department_id,position) VALUES (%s,%s,'Physician')",
                                  ("Web Physician", department))
        self.start = (datetime.now(ZONE) + timedelta(days=3)).replace(hour=11, minute=0, second=0, microsecond=0)

    def fields(self, patient=None, physician=None, start=None):
        start = start or self.start
        return dict(patientId=patient or self.patient, physicianId=physician or self.physician,
                    startsAt=start.strftime("%Y-%m-%dT%H:%M"), endsAt=(start+timedelta(minutes=30)).strftime("%Y-%m-%dT%H:%M"),
                    visitReason="Fictional check-in")

    def book(self, **kwargs):
        fields = self.fields(**kwargs)
        status, page, path = self.browser.post("/appointments", fields, f"/appointments/new?patientId={fields['patientId']}")
        self.assertEqual(status, 200)
        self.assertRegex(path, r"^/appointments/\d+$", page[:400])
        return int(path.rsplit("/", 1)[1])

    def test_01_login_logout_and_csrf(self):
        guest = Browser()
        for path in ("/", "/patients", "/physicians", "/appointments", "/reports", f"/patients/{self.patient}"):
            self.assertEqual(guest.request(path)[2], "/login")
        self.assertIn("not recognized", guest.post("/login", {"username":"test_staff","password":"wrong"})[1])
        self.assertEqual(self.browser.request("/patients", {"fullName":"Blocked","dateOfBirth":"1990-01-01"})[0], 403)
        self.assertEqual(self.browser.post("/logout", {}, "/")[2], "/login")
        self.assertEqual(self.browser.request("/patients")[2], "/login")

    def test_02_all_screens_render(self):
        for path in ("/", "/patients", "/patients/new", f"/patients/{self.patient}", f"/patients/{self.patient}/edit",
                     "/physicians", "/appointments", f"/appointments/new?patientId={self.patient}", "/reports"):
            with self.subTest(path=path):
                status, page, actual = self.browser.request(path)
                self.assertEqual(status, 200, path)
                self.assertEqual(actual, urlparse(path).path)
                self.assertIn("CareDesk", page)

    def test_03_register_search_edit_and_escape(self):
        name = "Synthetic <script>alert(1)</script>"
        fields = dict(fullName=name, dateOfBirth="1995-06-15", phone="555-0142", address="Sample address", primaryPhysicianId="")
        status, page, path = self.browser.post("/patients", fields, "/patients/new")
        self.assertEqual(status, 200); self.assertRegex(path, r"^/patients/\d+$")
        self.assertIn("&lt;script&gt;", page); self.assertNotIn("<script>alert(1)</script>", page)
        patient = int(path.rsplit("/",1)[1])
        self.assertIn(f"Patient #{patient}", self.browser.request("/patients?"+urlencode({"q":str(patient)}))[1])
        fields.update(fullName="Updated Fictional Patient", phone="555-0166", primaryPhysicianId=self.physician)
        self.assertEqual(self.browser.post(path, fields, path+"/edit")[2], path)
        self.assertEqual(db.run(self.admin,"SELECT full_name,phone,primary_physician_id FROM patient WHERE patient_id=%s",(patient,))[0],
                         ("Updated Fictional Patient","555-0166",self.physician))
        self.assertEqual(self.browser.request(path+"/edit")[0],200)

    def test_04_invalid_patient_is_not_saved(self):
        before = db.run(self.admin,"SELECT COUNT(*) FROM patient")[0][0]
        status, page, path = self.browser.post("/patients",dict(fullName=" ",dateOfBirth="2999-01-01",primaryPhysicianId="99999999"),"/patients/new")
        self.assertEqual(status,200);self.assertEqual(path,"/patients");self.assertIn("Please check",page)
        self.assertEqual(db.run(self.admin,"SELECT COUNT(*) FROM patient")[0][0],before)
        self.assertIn('value="2999-01-01"',page)

    def test_05_booking_persists_utc_and_rejects_overlap(self):
        appointment = self.book()
        utc = self.start.astimezone(timezone.utc).replace(tzinfo=None)
        self.assertEqual(db.run(self.admin,"SELECT starts_at FROM appointment WHERE appointment_id=%s",(appointment,))[0][0],utc)
        status, page, path = self.browser.post("/appointments",self.fields(),f"/appointments/new?patientId={self.patient}")
        self.assertEqual(status,200);self.assertEqual(path,"/appointments");self.assertIn("overlapping appointment",page)
        self.assertEqual(db.run(self.admin,"SELECT COUNT(*) FROM appointment WHERE patient_id=%s",(self.patient,))[0][0],1)

    def test_06_cancel_retains_history_and_releases_slot(self):
        appointment = self.book();path=f"/appointments/{appointment}"
        status,page,_ = self.browser.post(path+"/status",{"status":"cancelled"},path)
        self.assertEqual(status,200);self.assertIn("Appointment cancelled",page)
        new = self.book();self.assertNotEqual(new,appointment)
        self.assertEqual(db.run(self.admin,"SELECT status FROM appointment WHERE appointment_id=%s",(appointment,))[0][0],"cancelled")
        self.assertIn("cancelled",self.browser.request(f"/patients/{self.patient}")[1])

    def test_07_reschedule_conflict_rolls_back_and_valid_move_succeeds(self):
        first=self.book();second=self.book(start=self.start+timedelta(hours=1))
        path=f"/appointments/{first}/reschedule"
        status,page,actual=self.browser.post(path,self.fields(start=self.start+timedelta(hours=1)))
        self.assertEqual(status,200);self.assertEqual(actual,path);self.assertIn("overlapping appointment",page)
        self.assertEqual(db.run(self.admin,"SELECT starts_at FROM appointment WHERE appointment_id=%s",(first,))[0][0],self.start.astimezone(timezone.utc).replace(tzinfo=None))
        self.assertEqual(self.browser.post(path,self.fields(start=self.start+timedelta(hours=2)))[2],f"/appointments/{first}")
        self.assertEqual(self.browser.request(f"/appointments/{second}")[0],200)

    def test_08_completion_rules(self):
        future=self.book();path=f"/appointments/{future}"
        self.assertIn("future appointment cannot be completed",self.browser.post(path+"/status",{"status":"completed"},path)[1])
        past=datetime.now(timezone.utc).replace(tzinfo=None,microsecond=0)-timedelta(hours=2)
        appointment=db.insert(self.admin,"INSERT INTO appointment(patient_id,physician_id,starts_at,ends_at) VALUES (%s,%s,%s,%s)",
                              (self.patient,self.physician,past,past+timedelta(minutes=30)))
        path=f"/appointments/{appointment}"
        self.assertIn("Mark completed",self.browser.request(path)[1])
        self.assertIn("Appointment completed",self.browser.post(path+"/status",{"status":"completed"},path)[1])
        self.assertIn("final",self.browser.post(path+"/status",{"status":"cancelled"},path)[1])

    def test_09_past_and_inverted_times_are_rejected(self):
        fields=self.fields(start=self.start-timedelta(days=10))
        self.assertIn("future",self.browser.post("/appointments",fields,f"/appointments/new?patientId={self.patient}")[1])
        fields=self.fields();fields["endsAt"]=fields["startsAt"]
        self.assertIn("end time must be after",self.browser.post("/appointments",fields,f"/appointments/new?patientId={self.patient}")[1])

    def test_10_filters_and_local_report_boundaries(self):
        # A late local visit belongs to the selected day even when UTC is the next day.
        appointment=self.book(start=self.start.replace(hour=23))
        local=self.start.date().isoformat()
        page=self.browser.request("/appointments?"+urlencode(dict(date=local,physician=self.physician,status="scheduled")))[1]
        self.assertIn(f"/appointments/{appointment}",page)
        page=self.browser.request("/reports?"+urlencode(dict(**{"from":local},to=local)))[1]
        self.assertIn("Web Physician",page)
        self.assertNotIn("Web Physician",self.browser.request("/reports?from=1000-01-01&to=1000-01-01")[1])
        self.assertEqual(self.browser.request("/reports?from=2026-02-01&to=2026-01-01")[0],400)

    def test_11_bad_ids_and_malformed_filters(self):
        for path in ("/patients/99999999","/appointments/99999999"):
            self.assertEqual(self.browser.request(path)[0],404)
        for path in ("/appointments?date=invalid","/patients?page=-1","/appointments?status=invalid"):
            self.assertEqual(self.browser.request(path)[0],400)

    def test_12_restricted_account_cannot_write_appointments(self):
        import pymysql
        conn=pymysql.connect(host=os.getenv("MYSQL_HOST","127.0.0.1"),port=int(os.getenv("MYSQL_PORT","3306")),
                             user="app_smoke",password=APP_PASSWORD,database="hospital_v1",autocommit=True)
        with conn:
            self.assertIn("hospital_v1_app",db.run(conn,"SELECT CURRENT_ROLE()")[0][0])
            with self.assertRaises(pymysql.MySQLError) as error:
                db.run(conn,"DELETE FROM appointment WHERE appointment_id=0")
            self.assertEqual(error.exception.args[0],1142)

    def test_13_existing_database_provisioning_and_password_rotation(self):
        # Capture the shell script's stdin for mysql, then execute that exact SQL
        # on the disposable server. No real password or generated SQL is logged.
        import pymysql
        with tempfile.TemporaryDirectory() as directory:
            shim=Path(directory)/"mysql"
            shim.write_text("#!/bin/sh\ncat\n")
            shim.chmod(0o700)
            for password in ("Synthetic-'quote\\slash$-123", "Rotated-test-"+secrets.token_hex(12)):
                env={**os.environ,"PATH":directory+os.pathsep+os.environ["PATH"],
                     "MYSQL_ROOT_PASSWORD":"unused-by-capture","MYSQL_APP_PASSWORD":password}
                output=subprocess.run(["sh",str(ROOT/"database/v1/provision-app.sh")],env=env,capture_output=True,text=True,check=True).stdout
                sql=Path(directory)/"provision.sql"
                sql.write_text(output.removesuffix("Application database account is ready.\n"))
                db.sql_script(self.admin,sql)
                with pymysql.connect(host=os.getenv("MYSQL_HOST","127.0.0.1"),port=int(os.getenv("MYSQL_PORT","3306")),
                                     user="hospital_app",password=password,database="hospital_v1",autocommit=True) as app:
                    self.assertIn("hospital_v1_app",db.run(app,"SELECT CURRENT_ROLE()")[0][0])
                    self.assertGreater(db.run(app,"SELECT COUNT(*) FROM patient")[0][0],0)
                    with self.assertRaises(pymysql.MySQLError): db.run(app,"DELETE FROM appointment WHERE appointment_id=0")


def main():
    global BASE
    with db.connection(database=None) as admin:
        for path in sorted((ROOT/"database/v1/migrations").glob("*.sql")):
            db.sql_script(admin,path)
        db.sql_script(admin,ROOT/"database/v1/seed.sql")
        db.run(admin,"CREATE USER 'app_smoke'@'%%' IDENTIFIED BY %s",(APP_PASSWORD,))
        db.run(admin,"GRANT 'hospital_v1_app' TO 'app_smoke'@'%'")
        db.run(admin,"SET DEFAULT ROLE 'hospital_v1_app' TO 'app_smoke'@'%'")
    with socket.socket() as sock:
        sock.bind(("127.0.0.1",0));port=sock.getsockname()[1]
    BASE=f"http://127.0.0.1:{port}"
    env={**os.environ,"PORT":str(port),"APP_BIND_ADDRESS":"127.0.0.1",
         "DB_HOST":os.getenv("MYSQL_HOST","127.0.0.1"),"DB_PORT":os.getenv("MYSQL_PORT","3306"),
         "DB_USERNAME":"app_smoke","MYSQL_APP_PASSWORD":APP_PASSWORD,
         "HOSPITAL_USERNAME":"test_staff","HOSPITAL_PASSWORD":STAFF_PASSWORD,"HOSPITAL_TIME_ZONE":ZONE.key}
    log_path=ROOT/"app/target/smoke-server.log"
    with log_path.open("w") as log:
        process=subprocess.Popen(["java","-jar",str(ROOT/"app/target/hospital-app-0.1.0.jar")],env=env,stdout=log,stderr=subprocess.STDOUT)
        try:
            deadline=time.monotonic()+60
            while time.monotonic()<deadline:
                if process.poll() is not None: raise RuntimeError("App exited; see app/target/smoke-server.log")
                try:
                    if Browser().request("/login")[0]==200: break
                except (URLError,ConnectionError): pass
                time.sleep(0.25)
            else: raise RuntimeError("App did not become ready; see app/target/smoke-server.log")
            result=unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(WebTests))
            # Optional local browser inspection; no credentials are written to disk.
            if result.wasSuccessful() and os.getenv("BROWSER_CHECK_SCRIPT"):
                subprocess.run(["node",os.environ["BROWSER_CHECK_SCRIPT"]],env={**os.environ,"SMOKE_BASE":BASE,"SMOKE_PASSWORD":STAFF_PASSWORD},check=True)
            return 0 if result.wasSuccessful() else 1
        finally:
            process.terminate()
            try: process.wait(timeout=15)
            except subprocess.TimeoutExpired: process.kill();process.wait()


if __name__=="__main__":
    sys.exit(main())
