package com.seerah.content.application.port.in;

import java.util.UUID;

/**
 * Publish an approved event. The provenance invariants (§13.2, §13.4) are
 * checked here and in the aggregate; a violation aborts the transaction.
 */
public interface PublishEventUseCase {
    void publish(UUID eventId);
}
