package com.seerah.content.application.port.out;

import com.seerah.content.domain.Event;
import com.seerah.content.domain.EventId;

import java.util.Optional;

/** Load the {@link Event} aggregate for command handling. */
public interface LoadEventPort {
    Optional<Event> load(EventId id);
}
