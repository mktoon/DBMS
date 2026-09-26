package com.micahtoo.hospital;

import java.time.*;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClinicTime {
    private final ZoneId zone;
    private final Clock clock;
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm XXX");
    public ClinicTime(@Value("${hospital.time-zone}") String zone, Clock clock) {
        this.zone = ZoneId.of(zone);
        this.clock = clock;
    }
    public String zoneName() { return zone.getId(); }
    public LocalDate today() { return LocalDate.now(clock.withZone(zone)); }
    public LocalDate todayUtc() { return LocalDate.now(clock.withZone(ZoneOffset.UTC)); }
    public LocalDateTime nowUtc() { return LocalDateTime.now(clock.withZone(ZoneOffset.UTC)); }
    public LocalDateTime dayStartUtc(LocalDate date) {
        return LocalDateTime.ofInstant(date.atStartOfDay(zone).toInstant(), ZoneOffset.UTC);
    }
    public LocalDateTime toUtc(LocalDateTime local) {
        if (local == null) throw new FormProblem("Enter both appointment times.");
        var offsets = zone.getRules().getValidOffsets(local);
        if (offsets.size() != 1) {
            throw new FormProblem("That local time is skipped or repeated by a daylight-saving change. Choose a time outside that hour.");
        }
        return LocalDateTime.ofInstant(local.toInstant(offsets.get(0)), ZoneOffset.UTC);
    }
    public LocalDateTime toLocal(LocalDateTime utc) { return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(zone).toLocalDateTime(); }
    public String display(LocalDateTime utc) { return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(zone).format(DISPLAY); }
    public boolean canComplete(LocalDateTime utc) { return !utc.isAfter(nowUtc()); }
    public void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)
                || java.time.temporal.ChronoUnit.DAYS.between(from, to) > 365
                || from.getYear() < 1000 || to.getYear() > 9998) {
            throw new FormProblem("Choose a valid date range of at most 366 days.");
        }
    }
}
