package com.seerah.search.adapter.out.embedding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seerah.search.application.port.out.SearchIndex;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * Semantic search over the fixed corpus. At startup (after seeding) every
 * published event and person is embedded once into a unit vector and held in
 * memory; a query is embedded the same way and events and people are ranked
 * <em>together</em> by cosine similarity — so the single most relevant thing wins,
 * whether it is an event or a person, and matches are by meaning, not keywords.
 * The corpus is tiny, so a brute-force scan is instant and needs no vector DB.
 */
@Component
public class SemanticSearchAdapter implements SearchIndex {

    /** Below this cosine, a candidate is treated as unrelated (keeps junk out). */
    private static final float MIN_SIMILARITY = 0.15f;

    private final EmbeddingModel model;
    private final JdbcTemplate jdbc;

    private final AtomicReference<List<Entry>> events = new AtomicReference<>(List.of());
    private final AtomicReference<List<Entry>> people = new AtomicReference<>(List.of());
    private Map<String, String> descriptors = Map.of();

    private record Entry(String type, UUID id, String slug, float[] vec) { }

    public SemanticSearchAdapter(EmbeddingModel model, JdbcTemplate jdbc) {
        this.model = model;
        this.jdbc = jdbc;
    }

    /** Build the index once the app is up and the seed has run. */
    @EventListener(ApplicationReadyEvent.class)
    public void buildIndex() {
        descriptors = loadDescriptors();
        events.set(load("EVENT", """
            SELECT e.id, e.slug,
                   (SELECT string_agg(t.value, ' ') FROM translation t
                    WHERE t.entity_type = 'EVENT' AND t.entity_id = e.id
                      AND t.field_name IN ('title', 'summary')) AS text
            FROM event e WHERE e.status = 'PUBLISHED'
            """));
        // A person is embedded from their names PLUS the two events where they first
        // appear (title + summary) — their "introduction". That gives relational
        // context without the dilution of their whole event history, so a query like
        // "brother of Musa" finds Harun via the event that names him as Musa's brother.
        people.set(load("PERSON", """
            SELECT p.id, p.slug, concat_ws(' ',
                     (SELECT string_agg(a.alias, ' ') FROM person_alias a WHERE a.person_id = p.id),
                     (SELECT string_agg(intro.txt, ' ') FROM (
                        SELECT concat_ws(' ',
                                 (SELECT value FROM translation WHERE entity_type='EVENT' AND entity_id=e.id AND field_name='title'),
                                 (SELECT value FROM translation WHERE entity_type='EVENT' AND entity_id=e.id AND field_name='summary')) AS txt
                        FROM relationship r JOIN event e ON e.id = r.subject_id
                        WHERE r.subject_type='EVENT' AND r.rel_type='PARTICIPATED_IN'
                          AND r.object_type='PERSON' AND r.object_id = p.id
                        ORDER BY e.sort_key ASC, e.greg_start ASC NULLS LAST
                        LIMIT 2) intro)
                   ) AS text
            FROM person p WHERE p.status = 'PUBLISHED'
            """));
    }

    private List<Entry> load(String type, String sql) {
        return jdbc.query(sql, (rs, i) -> {
            UUID id = rs.getObject(1, UUID.class);
            String slug = rs.getString(2);
            String base = rs.getString(3);
            String text = base == null || base.isBlank() ? slug : base;
            // A curated identity/kinship descriptor (search-only) leads the person's
            // text so relational queries resolve: "brother of Musa" -> Harun.
            String desc = descriptors.get(slug);
            if (desc != null) text = desc + ". " + desc + ". " + text;
            return new Entry(type, id, slug, model.embed(text));
        });
    }

    private Map<String, String> loadDescriptors() {
        try (var in = new ClassPathResource("search/person-descriptors.json").getInputStream()) {
            Map<String, String> m = new ObjectMapper().readValue(in, new TypeReference<>() { });
            m.remove("_note");
            return m;
        } catch (Exception e) {
            return Map.of();
        }
    }

    @Override
    public List<Hit> search(String query, int limit) {
        List<Entry> ev = events.get(), pe = people.get();
        if (ev.isEmpty() && pe.isEmpty()) return List.of();
        float[] q = model.embed(query);
        return Stream.concat(ev.stream(), pe.stream())
                .map(e -> new Hit(e.type(), e.id(), e.slug(), EmbeddingModel.cosine(q, e.vec())))
                .filter(h -> h.score() >= MIN_SIMILARITY)
                .sorted((a, b) -> Float.compare(b.score(), a.score()))
                .limit(limit)
                .toList();
    }
}
