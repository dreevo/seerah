package com.seerah.search.application.port.out;

import java.util.List;
import java.util.UUID;

/** Outbound read model for search — the store that knows how to match text. */
public interface SearchIndex {

    record Row(UUID id, String slug) { }

    List<Row> matchingEvents(String pattern, int limit);

    List<Row> matchingPeople(String pattern, int limit);
}
