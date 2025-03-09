# Hospital Management System

## Overview

This project is a **Hospital Management System** designed to manage various hospital operations, including physician and patient records, appointments, procedures, and medications. The system also tracks room assignments, nurse shifts, and medical procedures that patients undergo during their stay.

The system is built using a relational database with several tables that manage the relationships between physicians, patients, departments, procedures, nurses, and hospital stays.

## Features

- **Physician Management**: Track information about physicians, including their department and position.
- **Patient Management**: Record patient details such as name, contact info, primary physician, and more.
- **Appointment Scheduling**: Schedule and manage appointments between patients and physicians.
- **Medication and Prescription Management**: Assign medications to patients prescribed by physicians.
- **Room Assignment**: Manage patient room assignments during their stay.
- **Procedure Management**: Track medical procedures performed on patients.
- **Nurse Scheduling**: Manage nurse shifts and on-call schedules.

## Database Schema

### Tables

1. **Physician**
   - `physicianID`: Unique identifier for each physician.
   - `name`: Name of the physician.
   - `position`: Job title or position (e.g., Intern, Senior, etc.).
   - `ssn`: Social Security Number.

2. **Department**
   - `deptID`: Unique department ID.
   - `name`: Department name (e.g., Surgery, Psychiatry, etc.).
   - `headID`: Physician ID for the department head.

3. **AffiliatedWith**
   - `physicianID`: Foreign key from `Physician`.
   - `departmentID`: Foreign key from `Department`.

4. **Procedure**
   - `procID`: Unique procedure ID.
   - `name`: Name of the procedure (e.g., Proc A, Proc B).
   - `cost`: Cost of the procedure.

5. **Patient**
   - `patientID`: Unique identifier for each patient.
   - `ssn`: Patient's Social Security Number.
   - `name`: Patient's full name.
   - `address`: Patient's address.
   - `dob`: Date of Birth.
   - `phone`: Phone number.
   - `insuranceNumber`: Insurance number.
   - `primaryphysicianID`: Physician ID for the patient's primary care physician.

6. **Nurse**
   - `nurseID`: Unique nurse ID.
   - `name`: Name of the nurse.
   - `position`: Nurse's job position.
   - `ssn`: Social Security Number.

7. **Medication**
   - `medID`: Unique medication ID.
   - `name`: Medication name (e.g., Med D, Med B).

8. **Prescribes**
   - `physicianID`: Foreign key from `Physician`.
   - `patientID`: Foreign key from `Patient`.
   - `medicationID`: Foreign key from `Medication`.
   - `prescribedDate`: Date when the medication was prescribed.
   - `dose`: Daily dose prescribed.

9. **Room**
   - `roomID`: Room ID (e.g., 123, 124).
   - `roomType`: Type of room (e.g., Single, Double).

10. **Stay**
    - `stayID`: Unique ID for each patient stay.
    - `patientID`: Foreign key from `Patient`.
    - `roomID`: Foreign key from `Room`.
    - `startDate`: Start date of the stay.
    - `endDate`: End date of the stay.

11. **Undergoes**
    - `patientID`: Foreign key from `Patient`.
    - `procedureID`: Foreign key from `Procedure`.
    - `stayID`: Foreign key from `Stay`.
    - `processingDate`: Date when the procedure was performed.
    - `physicianID`: Foreign key from `Physician`.
    - `nurseID`: Foreign key from `Nurse`.

12. **On_call_nurse**
    - `nurseID`: Foreign key from `Nurse`.
    - `startDate`: Start date of the on-call shift.
    - `endDate`: End date of the on-call shift.

13. **Appointment**
    - `appID`: Unique appointment ID.
    - `patientID`: Foreign key from `Patient`.
    - `nurseID`: Foreign key from `Nurse`.
    - `physicianID`: Foreign key from `Physician`.
    - `startDate`: Appointment start time.
    - `endDate`: Appointment end time.

## Setup

### Prerequisites

1. **Database**: This project uses MySQL (or compatible databases). Make sure to install MySQL server and set up the database.
2. **SQL Scripts**: Ensure you have the SQL script containing the `CREATE` table statements and the sample `INSERT` statements provided in the project.

### Installation

1. Clone this repository to your local machine:

```bash
git clone https://github.com/yourusername/hospital-management-system.git
```

2. Open the SQL script in a MySQL-compatible environment and execute it to create the tables and insert sample data.

```sql
-- Create the database and tables
CREATE DATABASE hospitalDB;
USE hospitalDB;

-- Paste the schema and insert queries here
```

3. Verify the data was inserted successfully by running simple `SELECT` queries for each table, such as:

```sql
SELECT * FROM Physician;
SELECT * FROM Patient;
SELECT * FROM Department;
```

## Usage

Once the database is set up, you can use it to:

- Manage hospital records, including physicians, nurses, patients, and more.
- Track patient stays, appointments, prescriptions, and procedures.
- View and update physician and nurse details.
- Schedule patient procedures and appointments.

## Contributing

1. Fork this repository.
2. Create a new branch (`git checkout -b feature-name`).
3. Make your changes.
4. Commit your changes (`git commit -am 'Add new feature'`).
5. Push to the branch (`git push origin feature-name`).
6. Create a pull request.

## Micah Too
## Project