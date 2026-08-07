package com.seerah.shared;

/**
 * Editorial workflow state (§12.2 {@code content_status}). Declaration order is
 * significant — the review queue sorts by it, so it must match the Postgres
 * enum exactly.
 */
public enum ContentStatus {
    DRAFT, IN_REVIEW, CHANGES_REQUESTED, APPROVED, PUBLISHED, RETIRED
}
