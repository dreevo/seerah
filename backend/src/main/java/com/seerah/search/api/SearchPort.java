package com.seerah.search.api;

import java.util.List;
import java.util.UUID;

/**
 * The Discovery contract. Phase 1 serves search from Postgres text matching over
 * the published corpus (§17 — the OpenSearch/Debezium pipeline and proper Arabic
 * analysis of §18 are the search-phase work, with this port as the seam). Returns
 * raw matches; the caller resolves them into display hits via the read ports.
 */
public interface SearchPort {

    List<SearchMatch> search(String query, int limit);

    record SearchMatch(String type, UUID id, String slug) { }
}
