package com.seerah.content.application.port.out;

import com.seerah.content.domain.Event;

/** Persist the {@link Event} aggregate (insert or update via optimistic locking, §26.1). */
public interface SaveEventPort {
    void save(Event event);
}
