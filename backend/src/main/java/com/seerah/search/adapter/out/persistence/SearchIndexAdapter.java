package com.seerah.search.adapter.out.persistence;

import com.seerah.search.application.port.out.SearchIndex;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Postgres-backed search. Matches the query against the published corpus: event
 * titles and summaries (in {@code translation}) and person names (in
 * {@code person_alias}), case-insensitive {@code ILIKE}. This is the search
 * engine — the corpus is small and fixed, so a live ILIKE over the content tables
 * needs no separate index, projection, or second engine.
 */
@Component
public class SearchIndexAdapter implements SearchIndex {

    @PersistenceContext
    private EntityManager em;

    private static String like(String term) {
        return "%" + term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    private static final String EVENTS_SQL = """
        SELECT e.id, e.slug FROM event e
        WHERE e.status = 'PUBLISHED'
          AND EXISTS (
            SELECT 1 FROM translation t
            WHERE t.entity_type = 'EVENT' AND t.entity_id = e.id
              AND t.field_name IN ('title', 'summary')
              AND t.value ILIKE :pattern)
        ORDER BY e.greg_start NULLS LAST, e.slug
        LIMIT :lim
        """;

    private static final String PEOPLE_SQL = """
        SELECT p.id, p.slug FROM person p
        WHERE p.status = 'PUBLISHED'
          AND EXISTS (
            SELECT 1 FROM person_alias a
            WHERE a.person_id = p.id AND a.alias ILIKE :pattern)
        ORDER BY p.slug
        LIMIT :lim
        """;

    @Override
    public List<Row> matchingEvents(String term, int limit) {
        return run(EVENTS_SQL, like(term), limit);
    }

    @Override
    public List<Row> matchingPeople(String term, int limit) {
        return run(PEOPLE_SQL, like(term), limit);
    }

    @SuppressWarnings("unchecked")
    private List<Row> run(String sql, String pattern, int limit) {
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("pattern", pattern)
                .setParameter("lim", limit)
                .getResultList();
        // slug is citext → pgjdbc returns a PGobject, so normalise via toString().
        return rows.stream()
                .map(r -> new Row(
                        r[0] instanceof UUID u ? u : UUID.fromString(r[0].toString()),
                        r[1].toString()))
                .toList();
    }
}
