CREATE DATABASE HospitalDB;
CREATE TABLE physician(
        physicianID int NOT NULL,
        name varchar(40) NOT NULL,
        position varchar(40) NOT NULL CHECK(position IN('Intern','Surgeon','Senior','Chief Of Medicine','Resident','Psychiatrist')),
        SSN int,
        PRIMARY KEY (physicianID)
);

CREATE TABLE department(
        deptID int,
        name varchar(40) NOT NULL CHECK (name IN ('General Medicine', 'Surgery', 'Psychiatry')),
        headID int NOT NULL, 
        FOREIGN KEY(headID) REFERENCES physician(physicianID),
        PRIMARY KEY(deptID)
);

CREATE TABLE affiliatedwith(
        physicianID int,
        departmentID int,
        FOREIGN KEY (physicianID) REFERENCES physician(physicianID),
        FOREIGN KEY (departmentID) REFERENCES department(deptID),
        PRIMARY KEY(physicianID, departmentID)
);

CREATE TABLE `procedure`(
        procID int,
        name varchar(40),
        cost real,
        PRIMARY KEY(procID)
);

CREATE TABLE patient(
        patientID int,
        SSN int,
        name varchar(40),
        address varchar(40),
        Dob date,
        phone varchar(16),
        insuranceNumber int,
        primaryphysicianID int,
        FOREIGN KEY (primaryphysicianID) REFERENCES physician(physicianID),
        PRIMARY KEY(patientID)
);

CREATE TABLE nurse(
        nurseID int,
        name varchar(40),
        position varchar(40) NOT NULL CHECK(position IN('Head Nurse', 'Nurse')),
        SSN int,
        PRIMARY KEY(nurseID)
);

CREATE TABLE medication(
         medID int NOT NULL PRIMARY KEY,
         name varchar(40)
);

CREATE TABLE prescribes(
        physicianID int,
        patientID int,
        medicationID int,
        prescribedDate date,
        dose varchar(40),
        FOREIGN KEY (physicianID) REFERENCES physician(physicianID),
        FOREIGN KEY (patientID) REFERENCES patient(patientID),
        FOREIGN KEY (medicationID) REFERENCES medication(medID),
        PRIMARY KEY(physicianID, patientID, medicationID)
);

CREATE TABLE room(
        roomID int,
        roomType varchar(40) NOT NULL CHECK (roomType IN ('Single', 'Double')),
        PRIMARY KEY(roomID)
);

CREATE TABLE stay(
        stayID int,
        patientID int,
        roomID int,
        startDate date,
        endDate date,
        FOREIGN KEY (patientID) REFERENCES patient(patientID),
	    FOREIGN KEY (roomID) REFERENCES room(roomID),
        PRIMARY KEY(stayID)
);

CREATE TABLE undergoes(
        patientID int,
        procedureID int,
        stayID int,
        processingDate date,
        physicianID int,
        nurseID int,
        FOREIGN KEY (patientID) REFERENCES patient(patientID),
        FOREIGN KEY (procedureID) REFERENCES `procedure`(procID),
        FOREIGN KEY (stayID) REFERENCES stay(stayID),
        FOREIGN KEY (physicianID) REFERENCES physician(physicianID),
        FOREIGN KEY (nurseID) REFERENCES nurse(nurseID),
        PRIMARY KEY(patientID, procedureID, stayID, physicianID, nurseID)
);

CREATE TABLE on_call_nurse(
        nurseID int,
        startDate date,
        endDate date,
        FOREIGN KEY (nurseID) REFERENCES nurse(nurseID),
        PRIMARY KEY(nurseID)
);

CREATE TABLE appointment(
        appID int,
        patientID int,
        nurseID int,
        physicianID int,
        startDate datetime,
        endDate datetime,
        FOREIGN KEY (patientID) REFERENCES patient(patientID),
        FOREIGN KEY (nurseID) REFERENCES nurse(nurseID),
        FOREIGN KEY (physicianID) REFERENCES physician(physicianID),
        PRIMARY KEY(appID)
);
