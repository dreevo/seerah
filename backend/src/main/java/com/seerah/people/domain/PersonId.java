package com.seerah.people.domain;

import java.util.UUID;

/** Typed identity for the {@link Person} aggregate. */
public record PersonId(UUID value) {

    public PersonId {
        if (value == null) throw new IllegalArgumentException("PersonId value must not be null");
    }

    public static PersonId newId() { return new PersonId(UUID.randomUUID()); }

    public static PersonId of(UUID value) { return new PersonId(value); }

    @Override
    public String toString() { return value.toString(); }
}
