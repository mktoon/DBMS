-- Optional, fictional demo records. Apply once after migrations.
USE hospital_v1;
SET SESSION time_zone = '+00:00';
START TRANSACTION;
INSERT INTO department (name) VALUES ('General Medicine'), ('Cardiology');
SET @general = LAST_INSERT_ID();
SET @cardiology = @general + 1;

INSERT INTO physician (full_name, department_id, position) VALUES
    ('Dr. Avery Example', @general, 'Physician'),
    ('Dr. Morgan Sample', @general, 'Physician'),
    ('Dr. Jordan Demo', @cardiology, 'Cardiologist');
SET @avery = LAST_INSERT_ID();
SET @morgan = @avery + 1;
SET @jordan = @avery + 2;

INSERT INTO patient (full_name, date_of_birth, phone, address, primary_physician_id) VALUES
    ('Taylor Example', '1990-04-12', '555-0101', '100 Example Way', @avery),
    ('Casey Sample', '1985-09-18', '555-0102', '200 Example Way', @morgan),
    ('Riley Demo', '2000-02-29', NULL, NULL, @jordan);
SET @taylor = LAST_INSERT_ID();
SET @casey = @taylor + 1;
SET @riley = @taylor + 2;

-- Administrative fixture insert for a historical completed visit.
INSERT INTO appointment (patient_id, physician_id, starts_at, ends_at, visit_reason, status)
VALUES (@taylor, @avery, UTC_DATE() - INTERVAL 1 DAY + INTERVAL 9 HOUR,
        UTC_DATE() - INTERVAL 1 DAY + INTERVAL 9 HOUR + INTERVAL 30 MINUTE,
        'Fictional follow-up visit', 'completed');
COMMIT;

SET @tomorrow = UTC_DATE() + INTERVAL 1 DAY;
CALL book_appointment(@taylor, @avery, @tomorrow + INTERVAL 9 HOUR,
    @tomorrow + INTERVAL 9 HOUR + INTERVAL 30 MINUTE, 'Fictional routine visit');
CALL book_appointment(@casey, @morgan, @tomorrow + INTERVAL 9 HOUR,
    @tomorrow + INTERVAL 10 HOUR, 'Fictional consultation');
CALL book_appointment(@riley, @jordan, @tomorrow + INTERVAL 11 HOUR,
    @tomorrow + INTERVAL 11 HOUR + INTERVAL 30 MINUTE, 'Fictional cancelled visit');
SET @cancelled_id = LAST_INSERT_ID();
CALL set_appointment_status(@cancelled_id, 'cancelled');
