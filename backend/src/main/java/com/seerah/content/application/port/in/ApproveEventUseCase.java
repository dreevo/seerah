package com.seerah.content.application.port.in;

import java.util.UUID;

/** Scholarly-review approval: move an event from IN_REVIEW to APPROVED. */
public interface ApproveEventUseCase {
    void approve(UUID eventId);
}
