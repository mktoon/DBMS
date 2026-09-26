-- Baseline for a NEW MySQL 8.4 database. Run once; fails if it already exists.
-- The original coursework HospitalDB is not migrated by this script.
SET SESSION time_zone = '+00:00';
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';
CREATE DATABASE hospital_v1 CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE hospital_v1;

CREATE TABLE department (
    department_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uq_department_name UNIQUE (name),
    CONSTRAINT ck_department_name CHECK (CHAR_LENGTH(TRIM(name)) > 0)
) ENGINE=InnoDB;

CREATE TABLE physician (
    physician_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    department_id BIGINT UNSIGNED NOT NULL,
    position VARCHAR(80) NOT NULL,
    CONSTRAINT fk_physician_department FOREIGN KEY (department_id)
        REFERENCES department (department_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_physician_name CHECK (CHAR_LENGTH(TRIM(full_name)) > 0),
    CONSTRAINT ck_physician_position CHECK (CHAR_LENGTH(TRIM(position)) > 0),
    INDEX ix_physician_department_name (department_id, full_name)
) ENGINE=InnoDB;

CREATE TABLE patient (
    patient_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    date_of_birth DATE NOT NULL,
    phone VARCHAR(32) NULL,
    address VARCHAR(255) NULL,
    primary_physician_id BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ck_patient_name CHECK (CHAR_LENGTH(TRIM(full_name)) > 0),
    CONSTRAINT ck_patient_birth_date CHECK (date_of_birth >= '1000-01-01'),
    CONSTRAINT ck_patient_phone CHECK (phone IS NULL OR CHAR_LENGTH(TRIM(phone)) > 0),
    CONSTRAINT fk_patient_primary_physician FOREIGN KEY (primary_physician_id)
        REFERENCES physician (physician_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_patient_name (full_name)
) ENGINE=InnoDB;

CREATE TABLE appointment (
    appointment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT UNSIGNED NOT NULL,
    physician_id BIGINT UNSIGNED NOT NULL,
    starts_at DATETIME NOT NULL COMMENT 'UTC; convert local input before writing',
    ends_at DATETIME NOT NULL COMMENT 'UTC; exclusive end of appointment interval',
    visit_reason VARCHAR(255) NULL,
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'scheduled',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ck_appointment_interval CHECK (ends_at > starts_at),
    CONSTRAINT ck_appointment_status CHECK (status IN ('scheduled', 'completed', 'cancelled')),
    CONSTRAINT fk_appointment_patient FOREIGN KEY (patient_id)
        REFERENCES patient (patient_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_appointment_physician FOREIGN KEY (physician_id)
        REFERENCES physician (physician_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_appointment_physician_slot (physician_id, status, starts_at, ends_at),
    INDEX ix_appointment_patient_slot (patient_id, status, starts_at, ends_at),
    INDEX ix_appointment_schedule (starts_at, status)
) ENGINE=InnoDB;

-- A single row serializes short scheduling transactions in this small demo.
CREATE TABLE scheduling_guard (
    guard_id TINYINT NOT NULL PRIMARY KEY,
    CONSTRAINT ck_single_scheduling_guard CHECK (guard_id = 1)
) ENGINE=InnoDB;
INSERT INTO scheduling_guard (guard_id) VALUES (1);

DELIMITER $$
CREATE TRIGGER patient_birth_date_insert BEFORE INSERT ON patient
FOR EACH ROW
BEGIN
    IF NEW.date_of_birth > UTC_DATE() THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Date of birth cannot be in the future';
    END IF;
END$$

CREATE TRIGGER patient_birth_date_update BEFORE UPDATE ON patient
FOR EACH ROW
BEGIN
    IF NEW.date_of_birth > UTC_DATE() THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Date of birth cannot be in the future';
    END IF;
END$$
DELIMITER ;
