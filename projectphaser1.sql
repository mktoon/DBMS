CREATE DATABASE Hospitalmanagement;
CREATE TABLE physician(
        physicianID int NOT NULL,
        name varchar(40) Not NULL,
        `position` varchar(40),
        SSN int,
        PRIMARY KEY (physicianID)
);
CREATE TABLE department(
        deptID int,
        name varchar(40),
        headID int,
        FOREIGN KEY(headID) REFERENCES physician(physicianID)        
);
CREATE TABLE affiliatedwith(
        physicianID int,
        departmentID int,
        FOREIGN KEY (physicianID) REFERENCES physician(physicianID),
        FOREIGN KEY (departmentID) REFERENCES department(deptID)
);
CREATE TABLE `procedure`(
        procID int,
        name varchar(40),
        cost real
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
        foreign key (primaryphysicianID) references physician(physicianID)
);
CREATE TABLE nurse(
        nurseID int,
        name varchar(40),
        `position` varchar(40),
        SSN int
);
CREATE TABLE medication(
         medID int,
         name varchar(40)
);
CREATE TABLE prescribes(
        physicianID int,
        patientID int,
        medicationID int,
        prescribedDate date,
        dose varchar(40),
        FOREIGN KEY (physicianID) REFERENCES physician(physicianID),
        FOREIGN KEY (patientID) REFERENCES patient(patientID)
);
CREATE TABLE room(
        roomID int,
        roomType varchar(40)        
);
CREATE TABLE stay(
        stayID int,
        patientID int,
        roomID int,
        startDate date,
        endDate date,
        FOREIGN KEY (patientID) REFERENCES patient(patientID),
        FOREIGN KEY (roomID) REFERENCES room(roomID)
);
CREATE TABLE undergoes(
        patientID int,
        procedureID int,
        stayID int,
        processingDate date,
        physicianID int,
        nurseID int,
        FOREIGN KEY (patientID) REFERENCES patient(patientID),
        FOREIGN KEY (procID) REFERENCES `procedure`(patientID),
        FOREIGN KEY (stayID) REFERENCES stay(stayID),
        FOREIGN KEY (physicianID) REFERENCES phycisian(physicianID),
        FOREIGN KEY (nurseID) REFERENCES nurse(nurseID)
        
);
CREATE TABLE on_call_nurse(
        nurseID int,
        startDate date,
        endDate date,
        FOREIGN KEY (nurseID) REFERENCES nurse(nurseID)
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
        FOREIGN KEY (physicianID) REFERENCES physician(physicianID)        
);
INSERT INTO EMPLOYEE VALUES (0001, 'Dave', 'Sales');