package com.seerah.shared;

/**
 * Provenance strength of a work (§12.2 {@code source_tier}). Order drives
 * citation-strength comparison: {@code PRIMARY} outranks {@code CLASSICAL},
 * and {@code TERTIARY} is never sufficient alone.
 */
public enum SourceTier {
    PRIMARY, CLASSICAL, SECONDARY, TERTIARY
}
