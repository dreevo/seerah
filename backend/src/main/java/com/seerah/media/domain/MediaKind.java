package com.seerah.media.domain;

/**
 * The kinds of media the platform accepts (§12.2 {@code media_kind}). The absence
 * of any value for a depiction of a person is deliberate and load-bearing: the
 * type system cannot name the one thing the platform must never publish (§6.5).
 * The visual language is geography, architecture, and the written word.
 */
public enum MediaKind {
    MAP, MANUSCRIPT_SCAN, PHOTOGRAPH, DIAGRAM, AUDIO, CALLIGRAPHY
    // No PORTRAIT. No ILLUSTRATION_OF_PERSON. The omission is the enforcement.
}
