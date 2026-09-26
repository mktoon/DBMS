package com.micahtoo.hospital;

import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;
import static com.micahtoo.hospital.Models.*;

@Repository
public class ClinicRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private static final int PAGE_SIZE = 50;
    private static final RowMapper<Patient> PATIENT = (r,n) -> new Patient(r.getLong("patient_id"),r.getString("full_name"),
            r.getObject("date_of_birth",LocalDate.class),r.getString("phone"),r.getString("address"),
            r.getObject("primary_physician_id",Long.class),r.getString("primary_physician_name"));
    private static final RowMapper<Physician> PHYSICIAN = (r,n) -> new Physician(r.getLong("physician_id"),r.getString("full_name"),
            r.getString("position"),r.getLong("department_id"),r.getString("department_name"));
    private static final RowMapper<Appointment> APPOINTMENT = (r,n) -> new Appointment(r.getLong("appointment_id"),r.getLong("patient_id"),
            r.getString("patient_name"),r.getLong("physician_id"),r.getString("physician_name"),r.getString("department_name"),
            r.getObject("starts_at",LocalDateTime.class),r.getObject("ends_at",LocalDateTime.class),r.getString("visit_reason"),r.getString("status"));
    public ClinicRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }
    public List<Department> departments() {
        return jdbc.query("SELECT department_id,name FROM department ORDER BY name",Map.of(),(r,n)->new Department(r.getLong(1),r.getString(2)));
    }
    public List<Physician> physicians(Long department) {
        return jdbc.query("SELECT * FROM v_physician_directory WHERE (:department IS NULL OR department_id=:department) ORDER BY department_name,full_name,physician_id",
                new MapSqlParameterSource("department",department),PHYSICIAN);
    }
    public boolean physicianExists(Long id) {
        return id != null && jdbc.queryForObject("SELECT COUNT(*) FROM physician WHERE physician_id=:id",Map.of("id",id),Long.class)>0;
    }
    public Page<Patient> patients(String query, int page) {
        validatePage(page);
        String q = query == null ? "" : query.strip();
        if (q.length()>100) throw new FormProblem("Keep the patient search to 100 characters.");
        String pattern="%"+q.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        var rows=jdbc.query("SELECT * FROM v_patient_directory WHERE full_name LIKE :pattern ESCAPE '!' OR CAST(patient_id AS CHAR)=:q ORDER BY full_name,patient_id LIMIT 51 OFFSET :offset",
                Map.of("pattern",pattern,"q",q,"offset",page*PAGE_SIZE),PATIENT);
        return page(rows,page);
    }
    public Patient patient(long id) {
        return one(jdbc.query("SELECT * FROM v_patient_directory WHERE patient_id=:id",Map.of("id",id),PATIENT));
    }
    public long savePatient(Long id, Forms.PatientForm f) {
        var p=new MapSqlParameterSource().addValue("name",f.getFullName()).addValue("dob",f.getDateOfBirth())
                .addValue("phone",f.getPhone()).addValue("address",f.getAddress()).addValue("physician",f.getPrimaryPhysicianId());
        if(id == null) {
            var key=new GeneratedKeyHolder();
            jdbc.update("INSERT INTO patient(full_name,date_of_birth,phone,address,primary_physician_id) VALUES(:name,:dob,:phone,:address,:physician)",p,key,new String[]{"patient_id"});
            return Objects.requireNonNull(key.getKey()).longValue();
        }
        patient(id);
        jdbc.update("UPDATE patient SET full_name=:name,date_of_birth=:dob,phone=:phone,address=:address,primary_physician_id=:physician WHERE patient_id=:id",p.addValue("id",id));
        return id;
    }
    public Appointment appointment(long id) {
        return one(jdbc.query("SELECT * FROM v_appointment_details WHERE appointment_id=:id",Map.of("id",id),APPOINTMENT));
    }
    public List<Appointment> patientHistory(long id) {
        return jdbc.query("SELECT * FROM v_appointment_details WHERE patient_id=:id ORDER BY starts_at DESC,appointment_id DESC LIMIT 50",Map.of("id",id),APPOINTMENT);
    }
    public Page<Appointment> appointments(LocalDateTime from, LocalDateTime until, String status, Long physician, int page) {
        validatePage(page);
        if (!Set.of("all","scheduled","completed","cancelled").contains(status)) throw new FormProblem("Choose a valid appointment status.");
        var p=new MapSqlParameterSource().addValue("from",from).addValue("until",until).addValue("status",status)
                .addValue("physician",physician).addValue("offset",page*PAGE_SIZE);
        var rows=jdbc.query("SELECT * FROM v_appointment_details WHERE starts_at>=:from AND starts_at<:until AND (:status='all' OR status=:status) AND (:physician IS NULL OR physician_id=:physician) ORDER BY starts_at,appointment_id LIMIT 51 OFFSET :offset",p,APPOINTMENT);
        return page(rows,page);
    }
    public List<ReportRow> report(LocalDateTime from, LocalDateTime until) {
        return jdbc.query("SELECT department_name,physician_name,SUM(status='scheduled') scheduled,SUM(status='completed') completed,SUM(status='cancelled') cancelled,COUNT(*) total FROM v_appointment_details WHERE starts_at>=:from AND starts_at<:until GROUP BY department_id,department_name,physician_id,physician_name ORDER BY department_name,physician_name",
                Map.of("from",from,"until",until),(r,n)->new ReportRow(r.getString(1),r.getString(2),r.getLong(3),r.getLong(4),r.getLong(5),r.getLong(6)));
    }
    public Overview overview(LocalDateTime from, LocalDateTime until) {
        var totals=report(from,until);
        return new Overview(jdbc.queryForObject("SELECT COUNT(*) FROM patient",Map.of(),Long.class),
                jdbc.queryForObject("SELECT COUNT(*) FROM physician",Map.of(),Long.class),
                totals.stream().mapToLong(ReportRow::scheduled).sum(),totals.stream().mapToLong(ReportRow::completed).sum(),totals.stream().mapToLong(ReportRow::cancelled).sum());
    }
    private static <T> T one(List<T> rows) {
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return rows.get(0);
    }
    private static void validatePage(int page) { if(page<0 || page>100000) throw new FormProblem("Choose a valid page."); }
    private static <T> Page<T> page(List<T> rows,int number) { return new Page<>(rows.subList(0,Math.min(PAGE_SIZE,rows.size())),number,rows.size()>PAGE_SIZE); }
}
