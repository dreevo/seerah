package com.seerah.content.application.port.out;

import com.seerah.shared.EntityType;

import java.util.Optional;
import java.util.UUID;

/**
 * A generic reader/writer of localisable text in the {@code translation} table,
 * for content entities beyond events (e.g. learning-path titles and blurbs).
 * Keeps §11.2 intact — no human-readable string lives on an aggregate row.
 */
public interface ContentTextPort {

    void putText(EntityType type, UUID id, String field, String locale, String value);

    Optional<String> text(EntityType type, UUID id, String field, String locale);
}
