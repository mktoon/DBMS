USE hospital_v1;
SET SESSION time_zone = '+00:00';
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- Call the three public procedures from an autocommit connection. They own
-- their transactions. Never call them inside another application transaction.
DELIMITER $$

-- Internal helper: caller must hold scheduling_guard FOR UPDATE.
CREATE PROCEDURE validate_appointment_slot(
    IN p_patient_id BIGINT UNSIGNED,
    IN p_physician_id BIGINT UNSIGNED,
    IN p_starts_at DATETIME,
    IN p_ends_at DATETIME,
    IN p_exclude_id BIGINT UNSIGNED
)
SQL SECURITY DEFINER
BEGIN
    DECLARE v_conflict BIGINT UNSIGNED DEFAULT NULL;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_conflict = NULL;

    IF p_starts_at IS NULL OR p_ends_at IS NULL OR p_ends_at <= p_starts_at THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Appointment end must follow its start';
    END IF;
    IF p_starts_at <= UTC_TIMESTAMP() THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Appointment must start in the future';
    END IF;

    -- A locking read sees committed changes after waiting for the guard,
    -- including at MySQL's default REPEATABLE READ isolation level.
    SELECT appointment_id INTO v_conflict
      FROM appointment
     WHERE status = 'scheduled'
       AND (patient_id = p_patient_id OR physician_id = p_physician_id)
       AND starts_at < p_ends_at AND ends_at > p_starts_at
       AND (p_exclude_id IS NULL OR appointment_id <> p_exclude_id)
     ORDER BY appointment_id LIMIT 1 FOR UPDATE;

    IF v_conflict IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Patient or physician already has an overlapping appointment';
    END IF;
END$$

CREATE PROCEDURE book_appointment(
    IN p_patient_id BIGINT UNSIGNED,
    IN p_physician_id BIGINT UNSIGNED,
    IN p_starts_at DATETIME,
    IN p_ends_at DATETIME,
    IN p_visit_reason VARCHAR(255)
)
SQL SECURITY DEFINER
BEGIN
    DECLARE v_guard TINYINT;
    DECLARE v_id BIGINT UNSIGNED;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
    START TRANSACTION;
    SELECT guard_id INTO v_guard FROM scheduling_guard WHERE guard_id = 1 FOR UPDATE;
    IF v_guard IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Scheduling guard is missing';
    END IF;
    CALL validate_appointment_slot(p_patient_id, p_physician_id, p_starts_at, p_ends_at, NULL);
    INSERT INTO appointment (patient_id, physician_id, starts_at, ends_at, visit_reason)
    VALUES (p_patient_id, p_physician_id, p_starts_at, p_ends_at, NULLIF(TRIM(p_visit_reason), ''));
    SET v_id = LAST_INSERT_ID();
    COMMIT;
    SELECT v_id AS appointment_id;
END$$

CREATE PROCEDURE reschedule_appointment(
    IN p_appointment_id BIGINT UNSIGNED,
    IN p_starts_at DATETIME,
    IN p_ends_at DATETIME
)
SQL SECURITY DEFINER
BEGIN
    DECLARE v_guard TINYINT;
    DECLARE v_patient_id BIGINT UNSIGNED DEFAULT NULL;
    DECLARE v_physician_id BIGINT UNSIGNED;
    DECLARE v_status VARCHAR(16);
    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
    START TRANSACTION;
    SELECT guard_id INTO v_guard FROM scheduling_guard WHERE guard_id = 1 FOR UPDATE;
    IF v_guard IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Scheduling guard is missing';
    END IF;
    SELECT patient_id, physician_id, status INTO v_patient_id, v_physician_id, v_status
    FROM appointment WHERE appointment_id = p_appointment_id FOR UPDATE;
    IF v_patient_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Appointment not found';
    END IF;
    IF v_status <> 'scheduled' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Only scheduled appointments can be rescheduled';
    END IF;
    CALL validate_appointment_slot(v_patient_id, v_physician_id, p_starts_at, p_ends_at, p_appointment_id);
    UPDATE appointment SET starts_at = p_starts_at, ends_at = p_ends_at
    WHERE appointment_id = p_appointment_id;
    COMMIT;
END$$

CREATE PROCEDURE set_appointment_status(
    IN p_appointment_id BIGINT UNSIGNED,
    IN p_status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin
)
SQL SECURITY DEFINER
BEGIN
    DECLARE v_guard TINYINT;
    DECLARE v_status VARCHAR(16) DEFAULT NULL;
    DECLARE v_starts_at DATETIME;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
    START TRANSACTION;
    SELECT guard_id INTO v_guard FROM scheduling_guard WHERE guard_id = 1 FOR UPDATE;
    IF v_guard IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Scheduling guard is missing';
    END IF;
    IF p_status IS NULL OR p_status NOT IN ('completed', 'cancelled') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'New status must be completed or cancelled';
    END IF;
    SELECT status, starts_at INTO v_status, v_starts_at
    FROM appointment WHERE appointment_id = p_appointment_id FOR UPDATE;
    IF v_status IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Appointment not found';
    END IF;
    IF v_status <> 'scheduled' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Completed and cancelled appointments are final';
    END IF;
    IF p_status = 'completed' AND v_starts_at > UTC_TIMESTAMP() THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A future appointment cannot be completed';
    END IF;
    UPDATE appointment SET status = p_status WHERE appointment_id = p_appointment_id;
    COMMIT;
END$$
DELIMITER ;
