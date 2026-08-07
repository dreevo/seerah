package com.seerah.search.application.port.out;

import java.util.UUID;

/**
 * The write side of the search index, used by the projection relay to keep the
 * engine in step with the published corpus (§17). The Postgres engine needs no
 * writer (it searches the tables directly); the OpenSearch engine does.
 */
public interface SearchIndexWriter {

    void ensureIndex();

    /** Index (or replace) one document. {@code type} is "EVENT" or "PERSON". */
    void index(String type, UUID id, String slug, String text);

    /** Make recently indexed documents searchable now (after a batch, or in tests). */
    void refresh();
}
