package com.seerah.content.domain;

import java.time.LocalDate;

/**
 * A historical date value object (§10.1). Encodes the reality that Seerah dates
 * are frequently imprecise or contested: it holds an explicit {@link DatePrecision},
 * may carry a Hijri date, a Gregorian range, or both, and never converts between
 * calendars (conversion is lossy and disputed, §10.1.2).
 *
 * <p>The factory enforces the same invariants the database CHECK constraints do
 * (§12.3): anything not {@code UNDATED} must actually carry a date.
 */
public record HistoricalDate(
        CalendarSystem calendar,
        Integer hijriYear,
        Integer hijriMonth,
        Integer hijriDay,
        LocalDate gregStart,
        LocalDate gregEnd,
        DatePrecision precision,
        String note) {

    public HistoricalDate {
        if (calendar == null) throw new IllegalArgumentException("calendar is required");
        if (precision == null) throw new IllegalArgumentException("precision is required");

        if (hijriMonth != null && (hijriMonth < 1 || hijriMonth > 12)) {
            throw new IllegalArgumentException("hijriMonth must be between 1 and 12");
        }
        if (hijriDay != null && (hijriDay < 1 || hijriDay > 30)) {
            throw new IllegalArgumentException("hijriDay must be between 1 and 30");
        }
        if (gregStart != null && gregEnd != null && gregEnd.isBefore(gregStart)) {
            throw new IllegalArgumentException("gregEnd must not be before gregStart");
        }
        // ck_event_dated_has_date: dated events must carry a date somewhere.
        if (precision != DatePrecision.UNDATED && hijriYear == null && gregStart == null) {
            throw new IllegalArgumentException(
                "a non-UNDATED date must carry a Hijri year or a Gregorian start date");
        }
    }

    /** Convenience factory for a Hijri year with optional Gregorian anchor. */
    public static HistoricalDate hijriYear(int hijriYear, Integer gregorianYear) {
        LocalDate greg = gregorianYear == null ? null : LocalDate.of(gregorianYear, 1, 1);
        return new HistoricalDate(CalendarSystem.HIJRI, hijriYear, null, null,
                greg, greg, DatePrecision.YEAR, null);
    }

    public boolean isDated() {
        return precision != DatePrecision.UNDATED;
    }
}
