package com.seerah.review.application;

import com.seerah.review.api.ReviewPort;
import com.seerah.review.api.ReviewRegistrar;
import com.seerah.review.application.port.out.ReviewStore;
import com.seerah.shared.EntityType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * The scholarly-review application service. Recording an approval both appends to
 * the immutable {@code review_action} log and writes an {@code approval} row whose
 * content hash pins the exact content reviewed (§13.6).
 */
@Service
@Transactional
public class ReviewService implements ReviewRegistrar, ReviewPort {

    private final ReviewStore store;

    public ReviewService(ReviewStore store) {
        this.store = store;
    }

    @Override
    public UUID approve(EntityType targetType, UUID targetId, int version,
                        String scholarEmail, String scholarName, String note) {
        UUID scholarId = store.ensureScholar(scholarEmail, scholarName);
        store.recordApproval(targetType, targetId, version, scholarId, note);
        store.recordAction(targetType, targetId, version, "APPROVED", "IN_REVIEW", "APPROVED", scholarId, note);
        return scholarId;
    }

    @Override
    @Transactional(readOnly = true)
    public int approvalCount(EntityType targetType, UUID targetId) {
        return store.countApprovals(targetType, targetId);
    }
}
