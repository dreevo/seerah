package com.seerah.ingestion.validation;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Authoring-time validation of a chronicle seed file — the governance that used
 * to run as a live pipeline, moved to where it belongs for a fixed corpus: a
 * check performed once, when the content is written, not on every read.
 *
 * <p>The rules encode the real, timeless editorial invariants:
 * <ul>
 *   <li>every event carries a supporting source and a locator (§13.2 — nothing is
 *       published uncited);</li>
 *   <li>a {@code SCHOLARS_DIFFER} event records at least two positions (§13.4);</li>
 *   <li>a Qur'anic event is {@code MUTAWATIR} (mass-transmitted revelation is not
 *       graded like an isnād);</li>
 *   <li>every referenced person / place / verse and every learning-path step
 *       resolves within the file, and verse refs are well formed.</li>
 * </ul>
 *
 * Pure and dependency-free, so it runs both at boot (the seeder fails fast on a
 * bad file) and as a plain unit test (no database).
 */
public final class SeedValidator {

    private static final Pattern VERSE = Pattern.compile("^\\d{1,3}:\\d{1,3}$");
    private static final Set<String> CERTAINTIES =
            Set.of("MUTAWATIR", "WELL_ATTESTED", "REPORTED", "WEAK", "SCHOLARS_DIFFER", "DISPUTED");

    private SeedValidator() { }

    /** @return a list of human-readable violations; empty means the file is valid. */
    public static List<String> validate(String name, JsonNode root) {
        List<String> problems = new ArrayList<>();

        Set<String> sourceSlugs = slugs(root, "sources");
        Set<String> personSlugs = slugs(root, "people");
        Set<String> placeSlugs = slugs(root, "places");
        Set<String> eventSlugs = new HashSet<>();

        JsonNode events = root.path("events");
        if (!events.isArray() || events.isEmpty()) {
            problems.add(name + ": has no events");
        }

        for (JsonNode e : events) {
            String slug = e.path("slug").asText("");
            String where = name + " · event '" + (slug.isEmpty() ? "(no slug)" : slug) + "'";
            if (slug.isEmpty()) problems.add(where + ": missing slug");
            else if (!eventSlugs.add(slug)) problems.add(where + ": duplicate slug");

            if (e.path("title").asText("").isBlank()) problems.add(where + ": missing title");
            if (e.path("summary").asText("").isBlank()) problems.add(where + ": missing summary");

            // §13.2 — no event without a supporting source + locator.
            String source = e.path("source").asText("");
            if (source.isBlank()) {
                problems.add(where + ": no supporting source (every event must be cited)");
            } else if (!sourceSlugs.isEmpty() && !sourceSlugs.contains(source)) {
                problems.add(where + ": cites source '" + source + "' not declared in sources[]");
            }
            if (e.path("locator").asText("").isBlank()) {
                problems.add(where + ": missing locator for its citation");
            }
            if (e.has("source2") && e.path("locator2").asText("").isBlank()) {
                problems.add(where + ": has source2 but no locator2");
            }

            // certainty must be known; Qur'an is mutawatir.
            String certainty = e.path("certainty").asText("");
            if (!CERTAINTIES.contains(certainty)) {
                problems.add(where + ": unknown certainty '" + certainty + "'");
            }
            if (isQuran(source) && !"MUTAWATIR".equals(certainty)) {
                problems.add(where + ": Qur'anic event should be MUTAWATIR, was '" + certainty + "'");
            }

            // §13.4 — SCHOLARS_DIFFER needs at least two recorded positions.
            if ("SCHOLARS_DIFFER".equals(certainty) && e.path("positions").size() < 2) {
                problems.add(where + ": SCHOLARS_DIFFER requires ≥ 2 positions, has "
                        + e.path("positions").size());
            }

            for (JsonNode v : e.path("verses")) {
                if (!VERSE.matcher(v.asText("")).matches()) {
                    problems.add(where + ": malformed verse ref '" + v.asText("") + "' (expected surah:ayah)");
                }
            }
            for (JsonNode p : e.path("people")) {
                if (!personSlugs.isEmpty() && !personSlugs.contains(p.asText())) {
                    problems.add(where + ": references person '" + p.asText() + "' not in people[]");
                }
            }
            if (e.hasNonNull("place") && !placeSlugs.isEmpty() && !placeSlugs.contains(e.path("place").asText())) {
                problems.add(where + ": references place '" + e.path("place").asText() + "' not in places[]");
            }
        }

        // learning-path steps must resolve to real events.
        for (JsonNode path : root.path("paths")) {
            String ps = path.path("slug").asText("(path)");
            for (JsonNode step : path.path("steps")) {
                if (!eventSlugs.contains(step.asText())) {
                    problems.add(name + " · path '" + ps + "': step '" + step.asText() + "' is not an event in this file");
                }
            }
        }

        return problems;
    }

    private static boolean isQuran(String sourceSlug) {
        return sourceSlug != null && sourceSlug.toLowerCase().contains("quran");
    }

    private static Set<String> slugs(JsonNode root, String field) {
        Set<String> out = new HashSet<>();
        for (JsonNode n : root.path(field)) {
            String s = n.path("slug").asText("");
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }
}
