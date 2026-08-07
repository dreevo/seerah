package com.seerah.people.application.port.in;

import java.util.UUID;

/**
 * The review-workflow transitions for a person, grouped as one contract for
 * brevity (the person workflow has no per-step divergence worth separate ports
 * at this stage).
 */
public interface PersonLifecycleUseCases {

    void submit(UUID personId);

    void approve(UUID personId);

    /** Publish an approved person profile; rejected unless it is cited (§13.2). */
    void publish(UUID personId);
}
