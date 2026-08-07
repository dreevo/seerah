package com.seerah.content.domain;

/**
 * Which calendar a date is expressed in (§12.2 {@code calendar_system}). The
 * platform stores both Hijri and Gregorian and never converts on read (§10.1.2):
 * conversion is lossy and contested, so a stored value is always what a source
 * actually said.
 */
public enum CalendarSystem {
    HIJRI, GREGORIAN, JULIAN, RELATIVE
}
