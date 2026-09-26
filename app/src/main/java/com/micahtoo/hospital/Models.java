package com.micahtoo.hospital;

import java.time.*;
import java.util.List;

public final class Models {
    private Models() { }
    public record Patient(long id, String name, LocalDate dateOfBirth, String phone, String address,
                          Long primaryPhysicianId, String primaryPhysicianName) { }
    public record Physician(long id, String name, String position, long departmentId, String departmentName) { }
    public record Department(long id, String name) { }
    public record Appointment(long id, long patientId, String patientName, long physicianId, String physicianName,
                              String departmentName, LocalDateTime startsAt, LocalDateTime endsAt, String visitReason, String status) { }
    public record ReportRow(String department, String physician, long scheduled, long completed, long cancelled, long total) { }
    public record Overview(long patients, long physicians, long scheduled, long completed, long cancelled) { }
    public record Page<T>(List<T> items, int number, boolean hasNext) { }
}
