package com.seerah.assistant.application;

import com.seerah.assistant.api.AssistantPort;
import com.seerah.content.api.EventDetailView;
import com.seerah.content.api.EventReadPort;
import com.seerah.content.api.RelatedEntity;
import com.seerah.content.api.RelationshipReadPort;
import com.seerah.provenance.api.CitationDirectory;
import com.seerah.search.api.SearchPort;
import com.seerah.shared.EntityType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A retrieval-grounded assistant that composes an answer <em>only</em> from
 * published, cited content, obeying the platform's absolute rules structurally
 * rather than by prompt:
 * <ul>
 *   <li>rule 1 — every passage carries the [S#] markers of the sources supporting it;</li>
 *   <li>rule 2 — when nothing in the corpus matches, it returns the fixed refusal and stops;</li>
 *   <li>rule 6 — it emits no Arabic script (only English summaries, work titles, and references);</li>
 *   <li>rule 9 — it carries forward each event's certainty as a confidence label;</li>
 *   <li>rule 12 — it returns the recorded summary only, never a lesson, opinion, or ruling.</li>
 * </ul>
 * Because it can only ever return text that already passed scholarly review and is
 * cited, it cannot fabricate. It has no store of its own.
 */
@Service
@Transactional(readOnly = true)
public class AssistantService implements AssistantPort {

    private static final int MAX_PASSAGES = 4;

    private final SearchPort search;
    private final EventReadPort events;
    private final RelationshipReadPort relationships;
    private final CitationDirectory citations;

    public AssistantService(SearchPort search, EventReadPort events,
                            RelationshipReadPort relationships, CitationDirectory citations) {
        this.search = search;
        this.events = events;
        this.relationships = relationships;
        this.citations = citations;
    }

    @Override
    public Answer ask(String question) {
        if (question == null || question.isBlank()) {
            return new Answer(false, REFUSAL, List.of(), List.of());
        }

        // Retrieve candidate events by keyword: direct event matches, plus events
        // that name a matched person. The question is tokenised so a natural-language
        // query ("what happened at Badr?") reaches the corpus term by term.
        Set<UUID> eventIds = new LinkedHashSet<>();
        for (String term : keywords(question)) {
            for (SearchPort.SearchMatch m : search.search(term, 8)) {
                if ("EVENT".equals(m.type())) {
                    eventIds.add(m.id());
                } else if ("PERSON".equals(m.type())) {
                    for (RelatedEntity edge : relationships.referencesTo(EntityType.PERSON, m.id())) {
                        if (edge.objectType() == EntityType.EVENT) {
                            eventIds.add(edge.objectId());
                        }
                    }
                }
            }
        }

        Map<String, Integer> sourceIndex = new LinkedHashMap<>();
        List<Cite> sources = new ArrayList<>();
        List<Passage> passages = new ArrayList<>();

        for (UUID eventId : eventIds) {
            if (passages.size() >= MAX_PASSAGES) break;
            EventDetailView e = events.findDetailById(eventId, "en").orElse(null);
            if (e == null || e.summary() == null || e.summary().isBlank()) continue;

            List<CitationDirectory.CitationView> cites = citations.citationsFor(EntityType.EVENT, eventId);
            if (cites.isEmpty()) continue; // rule 1: never emit an unsourced sentence

            List<Integer> markers = new ArrayList<>();
            for (CitationDirectory.CitationView c : cites) {
                String key = c.workTitle() + "|" + c.locator();
                int idx = sourceIndex.computeIfAbsent(key, k -> {
                    int n = sources.size() + 1;
                    sources.add(new Cite(n, c.workTitle(), c.tier(), c.locator()));
                    return n;
                });
                if (!markers.contains(idx)) markers.add(idx);
            }
            passages.add(new Passage(e.slug(), e.title(), e.summary(), confidence(e.certainty()), markers));
        }

        if (passages.isEmpty()) {
            return new Answer(false, REFUSAL, List.of(), List.of());
        }
        return new Answer(true, "", passages, sources);
    }

    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "for", "that", "this", "with", "from", "about", "what", "when",
            "where", "who", "why", "how", "did", "does", "was", "were", "are", "has", "had",
            "tell", "please", "happened", "during", "into", "over", "his", "her", "them",
            "they", "you", "your", "between", "after", "before");

    /** Split a question into significant search terms (≥3 letters, non-stopword). */
    private static List<String> keywords(String question) {
        List<String> terms = new ArrayList<>();
        for (String raw : question.toLowerCase().split("[^\\p{L}]+")) {
            if (raw.length() >= 3 && !STOPWORDS.contains(raw) && !terms.contains(raw)) {
                terms.add(raw);
            }
            if (terms.size() >= 6) break;
        }
        return terms;
    }

    /** Rule 9 — carry the source confidence forward in words. */
    private static String confidence(String certainty) {
        return switch (certainty) {
            case "MUTAWATIR" -> "mass-transmitted";
            case "WELL_ATTESTED" -> "well-attested";
            case "REPORTED" -> "a single report";
            case "WEAK" -> "a weak report";
            case "SCHOLARS_DIFFER" -> "scholars differ";
            case "DISPUTED" -> "disputed";
            default -> "reported";
        };
    }
}
