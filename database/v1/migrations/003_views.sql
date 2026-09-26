USE hospital_v1;

CREATE SQL SECURITY DEFINER VIEW v_physician_directory AS
SELECT p.physician_id, p.full_name, p.position, d.department_id, d.name AS department_name
FROM physician p JOIN department d ON d.department_id = p.department_id;

CREATE SQL SECURITY DEFINER VIEW v_patient_directory AS
SELECT p.patient_id, p.full_name, p.date_of_birth, p.phone, p.address,
       p.primary_physician_id, ph.full_name AS primary_physician_name
FROM patient p LEFT JOIN physician ph ON ph.physician_id = p.primary_physician_id;

CREATE SQL SECURITY DEFINER VIEW v_appointment_details AS
SELECT a.appointment_id, a.patient_id, pt.full_name AS patient_name,
       a.physician_id, ph.full_name AS physician_name,
       d.department_id, d.name AS department_name,
       a.starts_at, a.ends_at, a.visit_reason, a.status
FROM appointment a
JOIN patient pt ON pt.patient_id = a.patient_id
JOIN physician ph ON ph.physician_id = a.physician_id
JOIN department d ON d.department_id = ph.department_id;

CREATE SQL SECURITY DEFINER VIEW v_daily_appointment_totals AS
SELECT DATE(starts_at) AS appointment_date_utc, department_id, department_name,
       physician_id, physician_name, status, COUNT(*) AS appointment_count
FROM v_appointment_details
GROUP BY DATE(starts_at), department_id, department_name, physician_id, physician_name, status;
