# DBMS
Database management Systems
This is a repository towork on my DBMS class project. Required to be a mySQL code. Here is a more clearer description of the DBMS project i am trying to work on it
this is part 1 of the project.

Scenario:
"Las Palmas Medical Center" is a new hospital in Las Cruces, NM. They have hired you as a database administrator to create and manage their database. Assume that "Las Palmas Medical Center" has decided to have the following relational schema for their database:

Physician (physicianID: integer, name: varchar(40), position: varchar(40), ssn: integer)
Department (deptID: integer, name: varchar(40), headID: integer)
Foreign key: headID references Physician(physicianID)

AffiliatedWith (physicianID: integer, departmentID: integer)
Foreign key: physicianID references Physician(physicianID)
Foreign key: departmentID references Department(deptID)

Procedure (procID: integer, name: varchar(40), cost: real)

Patient (patientID: integer, ssn: integer, name: varchar(40), address: varchar(100), dob: date, phone: varchar(16), insuranceNumber: integer, primaryPhysID: integer)
Foreign key: primaryPhysID references Physician(physicianID)

Nurse (nurseID: integer, name: varchar(40), position: varchar(40), ssn: integer)

Medication (medID: integer, name: varchar(40))

Prescribes (physicianID: integer, patientID: integer, medicationID: integer, prescribedDate: date, dose: varchar(40))
Foreign key: physicianID references Physician(physicianID)
Foreign key: patientID references Patient(patientID)
Foreign key: medicationID references Medication(medID

Room (roomID: integer, roomType: varchar(40))

Stay (stayID: integer, patientID: integer, roomID: integer, startDate: date, endDate: date)
Foreign key: patientID references Patient(patientID)
Foreign key: roomID references Room(roomID)

Undergoes (patientID: integer, procedureID: integer, stayID: integer, procDate: date, physicianID: integer, nurseID: integer)
Foreign key: patientID references Patient(patientID)
Foreign key: procedureID references Procedure(procID)
Foreign key: stayID references Stay(stayID)
Foreign key: physicianID references Physician(physicianID)
Foreign key: nurseID references Nurse(nurseID)

OnCall (nurseID: integer, startDate: date, endDate: date)
Foreign key: nurseID references Nurse(nurseID)

Appointment (appID: integer, patientID: integer, nurseID: integer, physicianID: integer, startDateTime: datetime, endDateTime: datetime)
Foreign key: patientID references Patient(patientID)
Foreign key: nurseID references Nurse(nurseID)
Foreign key: physicianID references Physician(physicianID)
 

Note: The primary keys are shown with underlines.

Following are the value constrains for the tables:

Physicians can only have positions "Intern", "Surgeon", "Senior", "Chief of Medicine", "Resident", and "Psychiatrist".
Departments name can only be "General Medicine", "Surgery", and "Psychiatry".
Nurses can only have positions "Head Nurse" and "Nurse".
Rooms type can only be "Single" and "Double".
 

Sample Data (please don't use in your submission!):

Physician (1, "John Doe", "Surgeon", 11111111)
Department (1, "Surgery", 1)
AffiliatedWith (1, 1)
Procedure (1, "Proc A", 1500.0)
Patient (1, 100001, "John Smith", "12 Foo Drive", "1980-01-01", "575-123-1234", 123454, 1)
Nurse (1, "Jane Doe", "Head Nurse", 333333)
Medication (1, "Med A")
Prescribes (1, 1, 1, "2022-01-15", "5/day")
Room (1, "Single")
Stay (1, 1, 1, "2022-01-07", "2022-01-20")
Undergoes (1, 1, 1, "2022-01-10", 1, 1)
OnCall (1, "2022-01-01", "2022-02-01")
Appointment (1, 1, 1, 1, "2021-12-05 14:00", "2021-12-05 15:00")
Phase 1: Create a database using MySQL
Objectives:

Write DDL statements to create tables and constraints.
Write DML statements to insert data into a database.
Run SQL statements from a script file that includes a batch of SQL statements.
