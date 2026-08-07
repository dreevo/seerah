package com.seerah.search.application;

import com.seerah.search.api.SearchPort;
import com.seerah.search.application.port.out.SearchIndex;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Answers a query only from published content (§5.7 — the corpus, not a model, is
 * the source of truth). Events lead the results, then people.
 */
@Service
@Transactional(readOnly = true)
public class SearchService implements SearchPort {

    private final SearchIndex index;

    public SearchService(SearchIndex index) {
        this.index = index;
    }

    @Override
    public List<SearchMatch> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String term = query.strip();
        List<SearchMatch> hits = new ArrayList<>();
        index.matchingEvents(term, limit).forEach(r -> hits.add(new SearchMatch("EVENT", r.id(), r.slug())));
        index.matchingPeople(term, limit).forEach(r -> hits.add(new SearchMatch("PERSON", r.id(), r.slug())));
        return hits.size() > limit ? hits.subList(0, limit) : hits;
    }
}
