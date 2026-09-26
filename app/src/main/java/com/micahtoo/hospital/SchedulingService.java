package com.micahtoo.hospital;

import java.sql.*;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;

@Service
public class SchedulingService {
    private final DataSource dataSource;
    private final ClinicTime time;
    private final ClinicRepository repository;
    private static final Set<String> BUSINESS_MESSAGES=Set.of(
        "Appointment end must follow its start", "Appointment must start in the future",
        "Patient or physician already has an overlapping appointment", "Appointment not found",
        "Only scheduled appointments can be rescheduled", "New status must be completed or cancelled",
        "Completed and cancelled appointments are final", "A future appointment cannot be completed");
    public SchedulingService(DataSource dataSource, ClinicTime time, ClinicRepository repository) {
        this.dataSource=dataSource; this.time=time; this.repository=repository;
    }
    public long book(Forms.AppointmentForm form) {
        repository.patient(form.getPatientId());
        if(!repository.physicianExists(form.getPhysicianId())) throw new FormProblem("Choose a physician from the directory.");
        var start=time.toUtc(form.getStartsAt()); var end=time.toUtc(form.getEndsAt());
        validate(start,end);
        return call("{call book_appointment(?,?,?,?,?)}",s->{s.setLong(1,form.getPatientId());s.setLong(2,form.getPhysicianId());s.setObject(3,start);s.setObject(4,end);s.setString(5,form.getVisitReason());});
    }
    public void reschedule(long id, Forms.AppointmentForm form) {
        var start=time.toUtc(form.getStartsAt());var end=time.toUtc(form.getEndsAt());validate(start,end);
        call("{call reschedule_appointment(?,?,?)}",s->{s.setLong(1,id);s.setObject(2,start);s.setObject(3,end);});
    }
    public void status(long id,String status) {
        if(!Set.of("cancelled","completed").contains(status)) throw new FormProblem("Choose cancel or complete.");
        call("{call set_appointment_status(?,?)}",s->{s.setLong(1,id);s.setString(2,status);});
    }
    private void validate(java.time.LocalDateTime start,java.time.LocalDateTime end) {
        if(!end.isAfter(start)) throw new FormProblem("The end time must be after the start time.");
        if(!start.isAfter(time.nowUtc())) throw new FormProblem("Choose a start time in the future.");
        if(end.getYear()>9999) throw new FormProblem("Choose an end date before year 10000.");
    }
    // Deliberately no @Transactional: these MySQL procedures own their transactions.
    private long call(String sql, Binder binder) {
        try(Connection connection=dataSource.getConnection()) {
            if(!connection.getAutoCommit()) throw new IllegalStateException("Scheduling requires an autocommit connection");
            try(CallableStatement statement=connection.prepareCall(sql)) {
                binder.bind(statement);
                boolean result=statement.execute();long id=0;
                while(true) {
                    if(result) { try(ResultSet rows=statement.getResultSet()) { while(rows.next()) id=rows.getLong(1); } }
                    else if(statement.getUpdateCount()==-1) break;
                    result=statement.getMoreResults(Statement.CLOSE_CURRENT_RESULT);
                }
                return id;
            }
        } catch(SQLException e) {
            if("45000".equals(e.getSQLState()) && BUSINESS_MESSAGES.contains(e.getMessage())) throw new FormProblem(e.getMessage()+".");
            if(e.getErrorCode()==1452) throw new FormProblem("The selected patient or physician is no longer available.");
            throw new DataAccessResourceFailureException("Scheduling could not be saved",e);
        }
    }
    @FunctionalInterface private interface Binder { void bind(CallableStatement statement) throws SQLException; }
}
