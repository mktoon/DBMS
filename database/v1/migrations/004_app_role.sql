USE hospital_v1;
-- No password or login account is created here. Assign this role to a dedicated
-- application user. Do not add broader schema grants to that user.
CREATE ROLE 'hospital_v1_app';
GRANT SELECT ON hospital_v1.department TO 'hospital_v1_app';
GRANT SELECT ON hospital_v1.physician TO 'hospital_v1_app';
GRANT SELECT ON hospital_v1.patient TO 'hospital_v1_app';
GRANT INSERT (full_name, date_of_birth, phone, address, primary_physician_id)
    ON hospital_v1.patient TO 'hospital_v1_app';
GRANT UPDATE (full_name, date_of_birth, phone, address, primary_physician_id)
    ON hospital_v1.patient TO 'hospital_v1_app';
GRANT SELECT ON hospital_v1.appointment TO 'hospital_v1_app';
GRANT SELECT ON hospital_v1.v_physician_directory TO 'hospital_v1_app';
GRANT SELECT ON hospital_v1.v_patient_directory TO 'hospital_v1_app';
GRANT SELECT ON hospital_v1.v_appointment_details TO 'hospital_v1_app';
GRANT SELECT ON hospital_v1.v_daily_appointment_totals TO 'hospital_v1_app';
GRANT EXECUTE ON PROCEDURE hospital_v1.book_appointment TO 'hospital_v1_app';
GRANT EXECUTE ON PROCEDURE hospital_v1.reschedule_appointment TO 'hospital_v1_app';
GRANT EXECUTE ON PROCEDURE hospital_v1.set_appointment_status TO 'hospital_v1_app';
-- The application cannot mutate appointments directly, delete history,
-- call the internal validation helper, or modify scheduling_guard.
