package com.seerah.content.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Read and write the localisable strings of an event (title, summary, …), which
 * live in the {@code translation} table rather than on the aggregate (§11.2).
 */
public interface EventTranslationPort {

    void putValue(UUID eventId, String fieldName, String locale, String value);

    Optional<String> value(UUID eventId, String fieldName, String locale);
}
