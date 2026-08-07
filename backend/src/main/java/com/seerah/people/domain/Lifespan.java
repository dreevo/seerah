package com.seerah.people.domain;

/**
 * A person's lifespan, in either or both calendars (§12.4). All fields optional:
 * many figures have a known death year but an unknown birth year, and vice versa.
 * Validates only the constraint the database also enforces — death not before birth.
 */
public record Lifespan(Integer birthYearCe, Integer deathYearCe,
                       Integer birthYearAh, Integer deathYearAh) {

    public Lifespan {
        if (birthYearCe != null && deathYearCe != null && deathYearCe < birthYearCe) {
            throw new IllegalArgumentException("death year must not be before birth year");
        }
    }

    public static Lifespan unknown() {
        return new Lifespan(null, null, null, null);
    }
}
