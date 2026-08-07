package com.seerah.search.application;

import com.seerah.search.api.SearchPort;
import com.seerah.search.application.port.out.SearchIndex;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Answers a query only from published content (§5.7 — the corpus, not a model, is
 * the source of truth). Events and people are ranked together by semantic
 * relevance, most relevant first.
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
        return index.search(query.strip(), limit).stream()
                .map(h -> new SearchMatch(h.type(), h.id(), h.slug()))
                .toList();
    }
}
