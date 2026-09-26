"""Integration tests: a fresh, disposable MySQL 8.4 server is required.

Creates hospital_v1 and a test login; never drops an existing database.
The Python client is test tooling. The planned application remains Java.
"""
import os
from pathlib import Path
import secrets
import threading
import unittest
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone

import pymysql

ROOT = Path(__file__).resolve().parents[1]
TEST_PASSWORD = secrets.token_hex(24)


def connection(user="root", database="hospital_v1"):
    return pymysql.connect(
        host=os.getenv("MYSQL_HOST", "127.0.0.1"),
        port=int(os.getenv("MYSQL_PORT", "3306")),
        user=user,
        password=os.environ["MYSQL_ROOT_PASSWORD"] if user == "root" else TEST_PASSWORD,
        database=database,
        autocommit=True,
        charset="utf8mb4",
        init_command="SET SESSION time_zone = '+00:00'",
        connect_timeout=10,
        read_timeout=20,
        write_timeout=20,
    )


def sql_script(conn, path):
    """Load this project's SQL, including its DELIMITER directives."""
    delimiter = ";"
    buffer = []
    with conn.cursor() as cursor:
        for line in path.read_text().splitlines():
            stripped = line.strip()
            if not stripped or stripped.startswith("--"):
                continue
            if stripped.upper().startswith("DELIMITER "):
                if buffer:
                    raise ValueError(f"Unterminated SQL before DELIMITER in {path}")
                delimiter = stripped.split()[1]
                continue
            buffer.append(line)
            if stripped.endswith(delimiter):
                statement = "\n".join(buffer).rstrip()[:-len(delimiter)]
                cursor.execute(statement)
                while cursor.nextset():
                    pass
                buffer.clear()
        if buffer:
            raise ValueError(f"Unterminated SQL in {path}")


def run(conn, sql, args=None):
    with conn.cursor() as cursor:
        cursor.execute(sql, args)
        rows = cursor.fetchall()
        while cursor.nextset():
            pass
        return rows


def insert(conn, sql, args):
    with conn.cursor() as cursor:
        cursor.execute(sql, args)
        return cursor.lastrowid


def book(conn, patient, physician, start, end):
    return run(conn, "CALL book_appointment(%s,%s,%s,%s,%s)",
               (patient, physician, start, end, "Synthetic test visit"))[0][0]


class DatabaseTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        with connection(database=None) as admin:
            # CREATE DATABASE fails if it exists. Tests do not reset user data.
            for path in sorted((ROOT / "migrations").glob("*.sql")):
                sql_script(admin, path)
            sql_script(admin, ROOT / "seed.sql")
            run(admin, "CREATE USER 'v1_test_app'@'%%' IDENTIFIED BY %s", (TEST_PASSWORD,))
            run(admin, "GRANT 'hospital_v1_app' TO 'v1_test_app'@'%'")
            run(admin, "SET DEFAULT ROLE 'hospital_v1_app' TO 'v1_test_app'@'%'")

    def setUp(self):
        self.admin = connection()
        self.app = connection("v1_test_app")
        self.addCleanup(self.admin.close)
        self.addCleanup(self.app.close)
        self.department = insert(self.admin, "INSERT INTO department(name) VALUES (%s)",
                                 ("Test " + secrets.token_hex(6),))
        self.physician = self.new_physician()
        self.patient = self.new_patient()
        # Use tomorrow's clock time and round away subsecond differences.
        self.start = (datetime.now(timezone.utc) + timedelta(days=2)).replace(
            tzinfo=None, microsecond=0)
        self.end = self.start + timedelta(minutes=30)

    def new_physician(self):
        return insert(self.admin,
                      "INSERT INTO physician(full_name,department_id,position) VALUES (%s,%s,%s)",
                      ("Synthetic Physician", self.department, "Physician"))

    def new_patient(self):
        return insert(self.app, "INSERT INTO patient(full_name,date_of_birth) VALUES (%s,%s)",
                      ("Synthetic Patient", "1990-01-01"))

    def assert_db_error(self, code, action, contains=None):
        with self.assertRaises(pymysql.MySQLError) as cm:
            action()
        self.assertEqual(cm.exception.args[0], code)
        if contains:
            self.assertIn(contains, cm.exception.args[1])

    def test_patient_registration_and_edit(self):
        other = self.new_patient()
        self.assertNotEqual(other, self.patient)
        run(self.app, "UPDATE patient SET phone=%s, primary_physician_id=%s WHERE patient_id=%s",
            ("555-0199", self.physician, self.patient))
        row = run(self.app, "SELECT phone,primary_physician_id FROM patient WHERE patient_id=%s",
                  (self.patient,))[0]
        self.assertEqual(row, ("555-0199", self.physician))

    def test_patient_validation(self):
        self.assert_db_error(3819, lambda: run(self.app,
            "INSERT INTO patient(full_name,date_of_birth) VALUES ('  ','1990-01-01')"))
        self.assert_db_error(1644, lambda: run(self.app,
            "INSERT INTO patient(full_name,date_of_birth) VALUES ('Test',UTC_DATE()+INTERVAL 1 DAY)"))
        self.assert_db_error(1644, lambda: run(self.app,
            "UPDATE patient SET date_of_birth=UTC_DATE()+INTERVAL 1 DAY WHERE patient_id=%s",
            (self.patient,)))
        self.assert_db_error(1452, lambda: run(self.app,
            "UPDATE patient SET primary_physician_id=999999999 WHERE patient_id=%s",
            (self.patient,)))

    def test_direct_constraints(self):
        self.assert_db_error(3819, lambda: run(self.admin,
            "INSERT INTO appointment(patient_id,physician_id,starts_at,ends_at) VALUES (%s,%s,%s,%s)",
            (self.patient, self.physician, self.end, self.start)))
        self.assert_db_error(3819, lambda: run(self.admin,
            "INSERT INTO appointment(patient_id,physician_id,starts_at,ends_at,status) "
            "VALUES (%s,%s,%s,%s,'unknown')", (self.patient, self.physician, self.start, self.end)))

    def test_book_and_reconnect(self):
        appt = book(self.app, self.patient, self.physician, self.start, self.end)
        with connection("v1_test_app") as another:
            row = run(another, "SELECT patient_id,physician_id,status FROM appointment WHERE appointment_id=%s",
                      (appt,))[0]
        self.assertEqual(row, (self.patient, self.physician, "scheduled"))

    def test_missing_references(self):
        self.assert_db_error(1452, lambda: book(
            self.app, 999999999, self.physician, self.start, self.end))
        self.assert_db_error(1452, lambda: book(
            self.app, self.patient, 999999999, self.start, self.end))
        self.assertEqual(run(self.admin,
            "SELECT COUNT(*) FROM appointment WHERE patient_id=%s", (self.patient,))[0][0], 0)

    def test_invalid_intervals_and_past_bookings(self):
        for start, end in [(self.end, self.start), (self.start, self.start), (None, self.end),
                           (self.start - timedelta(days=3), self.end - timedelta(days=3))]:
            with self.subTest(start=start, end=end):
                self.assert_db_error(1644, lambda: book(
                    self.app, self.patient, self.physician, start, end))

    def test_physician_overlap_including_enclosing_interval(self):
        book(self.app, self.patient, self.physician, self.start, self.end)
        other_patient = self.new_patient()
        for start, end in [
            (self.start, self.end),
            (self.start - timedelta(minutes=10), self.start + timedelta(minutes=10)),
            (self.end - timedelta(minutes=10), self.end + timedelta(minutes=10)),
            (self.start - timedelta(minutes=10), self.end + timedelta(minutes=10)),
            (self.start + timedelta(minutes=5), self.end - timedelta(minutes=5)),
        ]:
            with self.subTest(start=start, end=end):
                self.assert_db_error(1644, lambda: book(
                    self.app, other_patient, self.physician, start, end), "overlapping")

    def test_patient_overlap_across_physicians(self):
        book(self.app, self.patient, self.physician, self.start, self.end)
        other_physician = self.new_physician()
        self.assert_db_error(1644, lambda: book(
            self.app, self.patient, other_physician, self.start, self.end), "overlapping")

    def test_adjacent_and_independent_appointments(self):
        book(self.app, self.patient, self.physician, self.start, self.end)
        book(self.app, self.patient, self.physician, self.end, self.end + timedelta(minutes=30))
        book(self.app, self.new_patient(), self.new_physician(), self.start, self.end)

    def test_cancel_retains_history_and_releases_slot(self):
        appt = book(self.app, self.patient, self.physician, self.start, self.end)
        run(self.app, "CALL set_appointment_status(%s,'cancelled')", (appt,))
        replacement = book(self.app, self.patient, self.physician, self.start, self.end)
        self.assertNotEqual(appt, replacement)
        self.assertEqual(run(self.app, "SELECT status FROM appointment WHERE appointment_id=%s",
                             (appt,))[0][0], "cancelled")
        self.assert_db_error(1451, lambda: run(self.admin,
            "DELETE FROM patient WHERE patient_id=%s", (self.patient,)))
        self.assert_db_error(1644, lambda: run(self.app,
            "CALL reschedule_appointment(%s,%s,%s)", (appt, self.start, self.end)))
        self.assert_db_error(1644, lambda: run(self.app,
            "CALL set_appointment_status(%s,'completed')", (appt,)))

    def test_reschedule_and_rollback_on_conflict(self):
        appt = book(self.app, self.patient, self.physician, self.start, self.end)
        start2, end2 = self.start + timedelta(hours=2), self.end + timedelta(hours=2)
        run(self.app, "CALL reschedule_appointment(%s,%s,%s)", (appt, start2, end2))
        book(self.app, self.patient, self.physician, self.start, self.end)
        self.assert_db_error(1644, lambda: run(self.app,
            "CALL reschedule_appointment(%s,%s,%s)", (appt, self.start, self.end)), "overlapping")
        row = run(self.app, "SELECT starts_at,ends_at FROM appointment WHERE appointment_id=%s",
                  (appt,))[0]
        self.assertEqual(row, (start2, end2))
        # Excludes the appointment itself from conflict detection.
        run(self.app, "CALL reschedule_appointment(%s,%s,%s)", (appt, start2, end2))

    def test_terminal_status_rules(self):
        appt = book(self.app, self.patient, self.physician, self.start, self.end)
        self.assert_db_error(1644, lambda: run(self.app,
            "CALL set_appointment_status(%s,'completed')", (appt,)), "future")
        for status in ["scheduled", "unknown", "COMPLETED", None]:
            self.assert_db_error(1644, lambda: run(self.app,
                "CALL set_appointment_status(%s,%s)", (appt, status)))
        # Fixture representing an appointment that has started.
        run(self.admin, "UPDATE appointment SET starts_at=UTC_TIMESTAMP()-INTERVAL 1 HOUR,"
            "ends_at=UTC_TIMESTAMP()-INTERVAL 30 MINUTE WHERE appointment_id=%s", (appt,))
        run(self.app, "CALL set_appointment_status(%s,'completed')", (appt,))
        self.assert_db_error(1644, lambda: run(self.app,
            "CALL set_appointment_status(%s,'cancelled')", (appt,)), "final")

    def test_missing_appointments(self):
        self.assert_db_error(1644, lambda: run(self.app,
            "CALL reschedule_appointment(999999999,%s,%s)", (self.start, self.end)), "not found")
        self.assert_db_error(1644, lambda: run(self.app,
            "CALL set_appointment_status(999999999,'cancelled')"), "not found")

    def test_application_cannot_bypass_procedures(self):
        for sql in [
            "INSERT INTO appointment(patient_id,physician_id,starts_at,ends_at) "
            "VALUES (1,1,'2099-01-01 09:00:00','2099-01-01 10:00:00')",
            "UPDATE appointment SET status='cancelled'",
            "DELETE FROM appointment",
            "UPDATE scheduling_guard SET guard_id=1",
            "DELETE FROM patient",
        ]:
            with self.subTest(sql=sql):
                self.assert_db_error(1142, lambda: run(self.app, sql))
        self.assert_db_error(1370, lambda: run(self.app,
            "CALL validate_appointment_slot(1,1,'2099-01-01 09:00:00','2099-01-01 10:00:00',NULL)"))

    def race(self, actions):
        barrier = threading.Barrier(len(actions))

        def worker(action):
            with connection("v1_test_app") as conn:
                barrier.wait(timeout=10)
                try:
                    action(conn)
                    return "booked"
                except pymysql.MySQLError as exc:
                    return exc.args[0]

        with ThreadPoolExecutor(max_workers=len(actions)) as pool:
            results = list(pool.map(worker, actions))
        self.assertCountEqual(results, ["booked", 1644])

    def test_concurrent_physician_bookings(self):
        other = self.new_patient()
        self.race([
            lambda c: book(c, self.patient, self.physician, self.start, self.end),
            lambda c: book(c, other, self.physician, self.start, self.end),
        ])
        self.assertEqual(run(self.admin,
            "SELECT COUNT(*) FROM appointment WHERE physician_id=%s", (self.physician,))[0][0], 1)

    def test_concurrent_patient_bookings(self):
        other = self.new_physician()
        self.race([
            lambda c: book(c, self.patient, self.physician, self.start, self.end),
            lambda c: book(c, self.patient, other, self.start, self.end),
        ])

    def test_reschedule_racing_with_booking(self):
        appt = book(self.app, self.patient, self.physician,
                    self.start + timedelta(hours=3), self.end + timedelta(hours=3))
        other = self.new_patient()
        self.race([
            lambda c: run(c, "CALL reschedule_appointment(%s,%s,%s)", (appt, self.start, self.end)),
            lambda c: book(c, other, self.physician, self.start, self.end),
        ])

    def test_directory_reports_and_seed(self):
        # Views must include patients without a primary physician.
        self.assertEqual(run(self.app,
            "SELECT primary_physician_name FROM v_patient_directory WHERE patient_id=%s",
            (self.patient,))[0][0], None)
        row = run(self.app, "SELECT department_id FROM v_physician_directory WHERE physician_id=%s",
                  (self.physician,))[0]
        self.assertEqual(row[0], self.department)
        appt = book(self.app, self.patient, self.physician, self.start, self.end)
        totals = run(self.app, "SELECT status,appointment_count FROM v_daily_appointment_totals "
                     "WHERE physician_id=%s", (self.physician,))
        self.assertEqual(totals, (("scheduled", 1),))
        run(self.app, "CALL set_appointment_status(%s,'cancelled')", (appt,))
        self.assertEqual(run(self.app, "SELECT status,appointment_count FROM v_daily_appointment_totals "
                             "WHERE physician_id=%s", (self.physician,)), (("cancelled", 1),))
        self.assertEqual(run(self.admin, "SELECT COUNT(*) FROM patient WHERE full_name IN "
                             "('Taylor Example','Casey Sample','Riley Demo')")[0][0], 3)
        self.assertEqual(run(self.admin, "SELECT status,COUNT(*) FROM appointment WHERE visit_reason "
            "LIKE 'Fictional%' GROUP BY status ORDER BY status"),
            (("cancelled", 1), ("completed", 1), ("scheduled", 2)))
        sql_script(self.app, ROOT / "reports.sql")


if __name__ == "__main__":
    unittest.main(verbosity=2)
