package com.seerah.search.api;

import java.util.List;
import java.util.UUID;

/**
 * The Discovery contract. Search is served from Postgres text matching (ILIKE)
 * over the published corpus — enough for a small, fixed body of content. Returns
 * raw matches; the caller resolves them into display hits via the read ports.
 */
public interface SearchPort {

    List<SearchMatch> search(String query, int limit);

    record SearchMatch(String type, UUID id, String slug) { }
}
