package com.seerah.content.application.port.in;

import java.util.UUID;

/** Set a localisable text field of an event (summary, why, …) in a given locale. */
public interface SetEventTextUseCase {
    void setText(UUID eventId, String field, String locale, String value);
}
