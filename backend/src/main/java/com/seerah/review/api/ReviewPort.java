package com.seerah.review.api;

import com.seerah.shared.EntityType;

import java.util.UUID;

/** Read side of the review context — how many scholars have signed off on a target. */
public interface ReviewPort {

    int approvalCount(EntityType targetType, UUID targetId);
}
