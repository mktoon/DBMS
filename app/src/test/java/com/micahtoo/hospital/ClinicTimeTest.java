package com.micahtoo.hospital;

import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClinicTimeTest {
    private final ClinicTime time=new ClinicTime("America/Los_Angeles",Clock.fixed(Instant.parse("2026-09-27T02:00:00Z"),ZoneOffset.UTC));
    @Test void localDayDiffersFromUtcDay() { assertEquals(LocalDate.parse("2026-09-26"),time.today());assertEquals(LocalDate.parse("2026-09-27"),time.todayUtc()); }
    @Test void normalTimeRoundTrips() { var local=LocalDateTime.parse("2026-09-26T11:30");assertEquals(LocalDateTime.parse("2026-09-26T18:30"),time.toUtc(local));assertEquals(local,time.toLocal(time.toUtc(local))); }
    @Test void springGapIsRejected() { assertThrows(FormProblem.class,()->time.toUtc(LocalDateTime.parse("2027-03-14T02:30"))); }
    @Test void autumnAmbiguityIsRejected() { assertThrows(FormProblem.class,()->time.toUtc(LocalDateTime.parse("2026-11-01T01:30"))); }
    @Test void localDayBoundsHandleDst() { assertEquals(23,Duration.between(time.dayStartUtc(LocalDate.parse("2027-03-14")),time.dayStartUtc(LocalDate.parse("2027-03-15"))).toHours()); }
    @Test void reportRangeRejectsReversedOrTooLong() { assertThrows(FormProblem.class,()->time.validateRange(LocalDate.parse("2026-02-02"),LocalDate.parse("2026-02-01")));assertThrows(FormProblem.class,()->time.validateRange(LocalDate.parse("2025-01-01"),LocalDate.parse("2026-01-02"))); }
}
