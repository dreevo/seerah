package com.seerah.shared;

/**
 * The shared kernel (§6.8.1). These few enums are the only vocabulary shared
 * verbatim across contexts — deliberately kept tiny. {@code EntityType} lets
 * provenance point a citation at anything without depending on the target
 * module's domain.
 *
 * <p>Values and order mirror the {@code entity_type} Postgres enum (§12.2).
 */
public enum EntityType {
    EVENT, PERSON, PLACE, ROUTE, PERIOD, RELATIONSHIP, LESSON,
    THEME, VERSE, HADITH, LEARNING_PATH, TAFSIR_EXCERPT
}
