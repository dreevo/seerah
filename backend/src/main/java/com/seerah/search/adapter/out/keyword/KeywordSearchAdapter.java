package com.seerah.search.adapter.out.keyword;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seerah.search.application.port.out.SearchIndex;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The low-footprint search used when the on-device embedding model is turned off
 * ({@code seerah.search.semantic=false}) — e.g. on a 1 GB free-tier box. Ranks the
 * fixed corpus by keyword overlap over event titles/summaries and person aliases
 * (plus the curated identity descriptors), served straight from Postgres. No model
 * and ~no memory; matches are by words, not meaning. The corpus is tiny, so scoring
 * every published event and person per query is instant.
 */
@Component
@ConditionalOnProperty(name = "seerah.search.semantic", havingValue = "false")
public class KeywordSearchAdapter implements SearchIndex {

    private final JdbcTemplate jdbc;
    private final Map<String, String> descriptors;

    public KeywordSearchAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.descriptors = loadDescriptors();
    }

    @Override
    public List<Hit> search(String query, int limit) {
        String[] terms = query.toLowerCase(Locale.ROOT).split("\\s+");
        List<Hit> hits = new ArrayList<>();

        jdbc.query("""
            SELECT e.id, e.slug,
                   (SELECT value FROM translation WHERE entity_type='EVENT' AND entity_id=e.id AND field_name='title')   AS title,
                   (SELECT value FROM translation WHERE entity_type='EVENT' AND entity_id=e.id AND field_name='summary') AS summary
            FROM event e WHERE e.status='PUBLISHED'
            """, rs -> {
            String title = n(rs.getString("title"));
            float s = score(terms, title, title + " " + n(rs.getString("summary")));
            if (s > 0) hits.add(new Hit("EVENT", rs.getObject("id", UUID.class), rs.getString("slug"), s));
        });

        jdbc.query("""
            SELECT p.id, p.slug,
                   (SELECT string_agg(a.alias, ' ') FROM person_alias a WHERE a.person_id=p.id) AS aliases
            FROM person p WHERE p.status='PUBLISHED'
            """, rs -> {
            String slug = rs.getString("slug");
            String aliases = n(rs.getString("aliases"));
            float s = score(terms, aliases, aliases + " " + descriptors.getOrDefault(slug, ""));
            if (s > 0) hits.add(new Hit("PERSON", rs.getObject("id", UUID.class), slug, s));
        });

        hits.sort((a, b) -> Float.compare(b.score(), a.score()));
        return hits.size() > limit ? new ArrayList<>(hits.subList(0, limit)) : hits;
    }

    /** +2 for a term in the primary field (title / aliases), +1 anywhere in the text. */
    private static float score(String[] terms, String primary, String full) {
        String p = primary.toLowerCase(Locale.ROOT);
        String f = full.toLowerCase(Locale.ROOT);
        float s = 0;
        for (String t : terms) {
            if (t.isBlank()) continue;
            if (p.contains(t)) s += 2;
            else if (f.contains(t)) s += 1;
        }
        return s;
    }

    private static String n(String v) {
        return v == null ? "" : v;
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
}
