package com.seerah.content.domain;

import java.util.UUID;

/**
 * Typed identity for the {@link Event} aggregate. A wrapper, not a bare UUID, so
 * that "an event id" and "a place id" can never be silently interchanged.
 */
public record EventId(UUID value) {

    public EventId {
        if (value == null) {
            throw new IllegalArgumentException("EventId value must not be null");
        }
    }

    public static EventId newId() {
        return new EventId(UUID.randomUUID());
    }

    public static EventId of(UUID value) {
        return new EventId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
