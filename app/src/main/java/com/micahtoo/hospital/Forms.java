package com.micahtoo.hospital;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

public final class Forms {
    private Forms() { }
    public static class PatientForm {
        @NotBlank(message="Enter the patient's full name.") @Size(max=150) private String fullName;
        @NotNull(message="Enter a date of birth.") @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate dateOfBirth;
        @Size(max=32) private String phone;
        @Size(max=255) private String address;
        @Positive private Long primaryPhysicianId;
        public String getFullName() { return fullName; } public void setFullName(String v) { fullName = clean(v); }
        public LocalDate getDateOfBirth() { return dateOfBirth; } public void setDateOfBirth(LocalDate v) { dateOfBirth=v; }
        public String getPhone() { return phone; } public void setPhone(String v) { phone=clean(v); }
        public String getAddress() { return address; } public void setAddress(String v) { address=clean(v); }
        public Long getPrimaryPhysicianId() { return primaryPhysicianId; } public void setPrimaryPhysicianId(Long v) { primaryPhysicianId=v; }
    }
    public static class AppointmentForm {
        @NotNull(message="Choose a patient.") @Positive private Long patientId;
        @NotNull(message="Choose a physician.") @Positive private Long physicianId;
        @NotNull(message="Enter a start time.") @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) private LocalDateTime startsAt;
        @NotNull(message="Enter an end time.") @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) private LocalDateTime endsAt;
        @Size(max=255) private String visitReason;
        public Long getPatientId() { return patientId; } public void setPatientId(Long v) { patientId=v; }
        public Long getPhysicianId() { return physicianId; } public void setPhysicianId(Long v) { physicianId=v; }
        public LocalDateTime getStartsAt() { return startsAt; } public void setStartsAt(LocalDateTime v) { startsAt=v; }
        public LocalDateTime getEndsAt() { return endsAt; } public void setEndsAt(LocalDateTime v) { endsAt=v; }
        public String getVisitReason() { return visitReason; } public void setVisitReason(String v) { visitReason=clean(v); }
    }
    static String clean(String value) { return value == null || value.isBlank() ? null : value.strip(); }
}
