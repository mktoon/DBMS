USE hospital_v1;
SET SESSION time_zone = '+00:00';

-- Dashboard. Counts use UTC dates for this V1 database milestone.
SELECT (SELECT COUNT(*) FROM patient) AS patient_count,
       (SELECT COUNT(*) FROM physician) AS physician_count,
       (SELECT COUNT(*) FROM appointment
         WHERE starts_at >= UTC_DATE() AND starts_at < UTC_DATE() + INTERVAL 1 DAY
           AND status = 'scheduled') AS scheduled_today;

-- Change these UTC boundaries to inspect another period; end is exclusive.
SET @range_start = UTC_DATE();
SET @range_end = UTC_DATE() + INTERVAL 7 DAY;
SELECT appointment_id, patient_name, physician_name, department_name,
       starts_at, ends_at, visit_reason, status
FROM v_appointment_details
WHERE starts_at >= @range_start AND starts_at < @range_end
ORDER BY starts_at, appointment_id;

SELECT department_name, physician_name, status, COUNT(*) AS appointment_count
FROM v_appointment_details
WHERE starts_at >= @range_start AND starts_at < @range_end
GROUP BY department_id, department_name, physician_id, physician_name, status
ORDER BY department_name, physician_name, status;
