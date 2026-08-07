package com.seerah.content.domain;

/**
 * How precisely an event is dated (§10.1.1, §12.2 {@code date_precision}). Six
 * meaningful levels plus {@code PERIOD_ONLY} and {@code UNDATED}; the latter
 * exists for Phase 3 prophet narratives that have no chronology and are anchored
 * to a period instead.
 */
public enum DatePrecision {
    EXACT_DAY, MONTH, SEASON, YEAR, YEAR_RANGE, DECADE, PERIOD_ONLY, UNDATED
}
