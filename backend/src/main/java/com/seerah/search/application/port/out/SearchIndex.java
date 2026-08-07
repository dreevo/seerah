package com.seerah.search.application.port.out;

import java.util.List;
import java.util.UUID;

/** Outbound read model for search — ranks the corpus by relevance to a query. */
public interface SearchIndex {

    /** A scored match; {@code type} is EVENT or PERSON, {@code score} is the similarity. */
    record Hit(String type, UUID id, String slug, float score) { }

    /** Events and people ranked together, most relevant first. */
    List<Hit> search(String query, int limit);
}
