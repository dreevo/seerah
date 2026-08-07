package com.seerah.content.application.port.in;

import java.util.UUID;

/** Move an event from DRAFT (or CHANGES_REQUESTED) into IN_REVIEW. */
public interface SubmitEventUseCase {
    void submit(UUID eventId);
}
