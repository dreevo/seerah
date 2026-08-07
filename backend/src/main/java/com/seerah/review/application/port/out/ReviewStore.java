package com.seerah.review.application.port.out;

import com.seerah.shared.EntityType;

import java.util.UUID;

/** Outbound store for review actions and approvals. */
public interface ReviewStore {

    /** Upsert an app_user for the given scholar, returning its id (the FK for approvals). */
    UUID ensureScholar(String email, String displayName);

    /**
     * Insert (or refresh) an approval, capturing the current content hash in SQL
     * via {@code fn_content_hash} so it matches what the publish trigger computes.
     */
    void recordApproval(EntityType targetType, UUID targetId, int version, UUID scholarId, String note);

    /** Append a row to the immutable review_action log. */
    void recordAction(EntityType targetType, UUID targetId, int version, String decision,
                      String fromStatus, String toStatus, UUID actorId, String comment);

    int countApprovals(EntityType targetType, UUID targetId);
}
