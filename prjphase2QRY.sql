use project;/*
SELECT P.patientID, P.SSN AS patient_ssn, P.name AS patient_name, P.address AS patient_address, 
       P.Dob AS patient_dob, P.phone AS patient_phone, P.insuranceNumber AS patient_insuranceNumber, 
       P.primaryphysicianID AS patient_primaryPhysID, 
       D.physicianID, D.name AS physician_name, D.position AS physician_position, D.SSN AS physician_ssn, 
       A.appID AS appointmentID
FROM appointment A, patient P, physician D, affiliatedwith AW, department DT
WHERE A.patientID = P.patientID and A.physicianID = D.physicianID and D.physicianID = AW.physicianID and AW.departmentID = DT.deptID and DT.name = 'psychiatry'
ORDER BY A.appID;

*/
SELECT r.roomID, p1.name AS patient1, s1.startDate AS startDate1, s1.endDate AS endDate1, p2.name AS patient2, s2.startDate AS startDate2, s2.endDate AS endDate2
FROM stay s1
JOIN stay s2 ON s1.roomID = s2.roomID AND s1.startDate = s2.startDate AND s1.patientID < s2.patientID
JOIN room r ON s1.roomID = r.roomID AND r.roomType = 'Double'
JOIN patient p1 ON s1.patientID = p1.patientID
JOIN patient p2 ON s2.patientID = p2.patientID
WHERE s1.startDate <= '2019-01-15' AND s1.endDate >= '2022-12-15' AND s2.startDate <= '2019-01-15' AND s2.endDate >= '2022-12-15';

/*
prj 6
SELECT N.nurseID, N.name, N.position, N.ssn, O.startDate, O.endDate
FROM nurse N, on_call_nurse O
WHERE N.nurseID = O.nurseID and  '2022-05-03' BETWEEN O.startDate AND O.endDate;
*/
/*
prj 5
SELECT P.name AS patient_name, D.name AS physician_name, R.prescribedDate
FROM patient P, prescribes R, physician D, medication M
WHERE M.medID = R.medicationID and D.physicianID = R.physicianID and P.patientID = R.patientID and  M.name = 'Med B';
*/
/*
prj 4
SELECT *
FROM patient P
WHERE P.primaryphysicianID IN (SELECT D.headID
								FROM department D
								WHERE D.name = 'psychiatry'
)*/



/*
prj 3
SELECT P.patientID, P.SSN, P.name, P.address, P.Dob, P.phone, P.insuranceNumber, P.primaryphysicianID
FROM patient P, undergoes U,`procedure` R
WHERE P.patientID = U.patientID and U.procedureID = R.procID and R.cost > 500.00;
*/



/*
prj 2
SELECT P.name AS patientName, D.name AS physicianName, N.name AS nurseName, A.startDate, A.endDate, PR1.name AS primaryPhysicianName
FROM appointment A, patient P, physician D, nurse N, physician  AS PR1
WHERE A.patientID = P.patientID and A.physicianID = D.physicianID and A.nurseID = N.nurseID
 and P.primaryphysicianID = PR1.physicianID and A.physicianID <> P.primaryphysicianID;
*/
/*
proj 1;
SELECT P.physicianID, P.name, P.position, P.SSN
FROM physician P,undergoes U, `procedure` R
WHERE P.physicianID = U.physicianID and U.procedureID = R.procID and R.name = 'Proc A';
*/