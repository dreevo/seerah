package com.seerah.review.api;

import com.seerah.shared.EntityType;

import java.util.UUID;

/**
 * Write side of the review context — a scholar signing off on a specific version
 * of a target. The sign-off fingerprints the exact content the scholar saw
 * (§13.6), so a later edit invalidates it and the item must be re-approved
 * before it can be published.
 */
public interface ReviewRegistrar {

    /**
     * Record a scholar's approval of {@code targetId} at {@code version}. Captures
     * a content hash of the target as it stands now; publishing later checks it.
     *
     * @return the approval id
     */
    UUID approve(EntityType targetType, UUID targetId, int version,
                 String scholarEmail, String scholarName, String note);
}
