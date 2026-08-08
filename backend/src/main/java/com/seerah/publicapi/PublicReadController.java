package com.seerah.publicapi;

import com.seerah.content.api.ChronicleReadPort;
import com.seerah.content.api.EventReadPort;
import com.seerah.content.api.RelatedEntity;
import com.seerah.content.api.LearningPathReadPort;
import com.seerah.content.api.LearningPathViews.PathDetail;
import com.seerah.content.api.LearningPathViews.PathSummary;
import com.seerah.content.api.RelationshipReadPort;
import com.seerah.people.api.PersonReadPort;
import com.seerah.places.api.PlaceReadPort;
import com.seerah.places.api.RouteReadPort;
import com.seerah.publicapi.PublicViews.MapPoint;
import com.seerah.publicapi.PublicViews.RouteLine;
import com.seerah.platform.error.NotFoundException;
import com.seerah.provenance.api.CitationDirectory;
import com.seerah.publicapi.PublicViews.EventDetail;
import com.seerah.publicapi.PublicViews.PersonDetail;
import com.seerah.publicapi.PublicViews.PersonEventItem;
import com.seerah.publicapi.PublicViews.PersonListItem;
import com.seerah.publicapi.PublicViews.RelatedEventItem;
import com.seerah.publicapi.PublicViews.RelatedPerson;
import com.seerah.publicapi.PublicViews.RelatedPlace;
import com.seerah.publicapi.PublicViews.RelatedVerse;
import com.seerah.publicapi.PublicViews.ChronicleItem;
import com.seerah.publicapi.PublicViews.SearchHit;
import com.seerah.publicapi.PublicViews.SourceItem;
import com.seerah.publicapi.PublicViews.TimelineItem;
import com.seerah.scripture.api.VerseReadPort;
import com.seerah.search.api.SearchPort;
import com.seerah.shared.EntityType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The read-only Backend-for-Frontend the reader app consumes. It assembles the
 * connected view of an event — its people, the verses revealed around it, where it
 * happened, the events before and after, and its sources — by composing the
 * published {@code api} ports of several modules. It owns no data (§8.6).
 */
@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:8080"})
public class PublicReadController {

    private final EventReadPort events;
    private final ChronicleReadPort chronicles;
    private final RelationshipReadPort relationships;
    private final PersonReadPort people;
    private final PlaceReadPort places;
    private final RouteReadPort routes;
    private final VerseReadPort verses;
    private final CitationDirectory citations;
    private final SearchPort search;
    private final LearningPathReadPort paths;
    private final HadithTexts hadith;
    private final UndatedEvents undated;

    public PublicReadController(EventReadPort events, ChronicleReadPort chronicles,
                                RelationshipReadPort relationships,
                                PersonReadPort people, PlaceReadPort places, RouteReadPort routes,
                                VerseReadPort verses, CitationDirectory citations,
                                SearchPort search,
                                LearningPathReadPort paths, HadithTexts hadith,
                                UndatedEvents undated) {
        this.events = events;
        this.chronicles = chronicles;
        this.relationships = relationships;
        this.people = people;
        this.places = places;
        this.routes = routes;
        this.verses = verses;
        this.citations = citations;
        this.search = search;
        this.paths = paths;
        this.hadith = hadith;
        this.undated = undated;
    }

    /** The library of chronicles the reader can choose between (the Seerah + prophets' stories). */
    @GetMapping("/chronicles")
    public List<ChronicleItem> chronicles() {
        return chronicles.published().stream()
                .map(c -> new ChronicleItem(c.slug(), c.title(), c.titleAr(), c.subtitle(),
                        c.blurb(), c.glyph(), c.kind(), c.ordinal(), c.eventCount()))
                .toList();
    }

    @GetMapping("/chronicles/{slug}")
    public ChronicleItem chronicle(@PathVariable String slug) {
        var c = chronicles.bySlug(slug)
                .orElseThrow(() -> new NotFoundException("chronicle.not_found", "No chronicle with slug " + slug));
        return new ChronicleItem(c.slug(), c.title(), c.titleAr(), c.subtitle(),
                c.blurb(), c.glyph(), c.kind(), c.ordinal(), c.eventCount());
    }

    @GetMapping("/timeline")
    public List<TimelineItem> timeline(@RequestParam(defaultValue = "en") String locale,
                                       @RequestParam(required = false) String chronicle) {
        return events.publishedTimeline(locale, chronicle).stream()
                .map(e -> new TimelineItem(e.id(), e.slug(), e.title(),
                        e.hijriYear(), e.gregYear(), e.certainty(), e.major(),
                        undated.isUndated(e.slug())))
                .toList();
    }

    @GetMapping("/events/{slug}")
    public EventDetail event(@PathVariable String slug, @RequestParam(defaultValue = "en") String locale) {
        var d = events.findDetailBySlug(slug, locale)
                .orElseThrow(() -> new NotFoundException("event.not_found", "No published event with slug " + slug));

        List<RelatedPerson> peopleOut = new ArrayList<>();
        List<RelatedVerse> versesOut = new ArrayList<>();
        List<RelatedPlace> placesOut = new ArrayList<>();
        List<RelatedEventItem> eventsOut = new ArrayList<>();

        for (RelatedEntity edge : relationships.neighboursOf(EntityType.EVENT, d.id())) {
            switch (edge.objectType()) {
                case PERSON -> people.findById(edge.objectId(), locale).ifPresent(p ->
                        peopleOut.add(new RelatedPerson(p.id(), p.slug(), p.name(), p.nameArabic(),
                                p.role(), edge.relType().name())));
                case VERSE -> verses.findById(edge.objectId(), locale).ifPresent(v ->
                        versesOut.add(new RelatedVerse(v.reference(), v.surahNameEn(), v.surahNameAr(),
                                v.textUthmani(), v.translationText(), v.translator(), edge.relType().name())));
                case PLACE -> places.findById(edge.objectId(), locale).ifPresent(pl ->
                        placesOut.add(new RelatedPlace(pl.id(), pl.slug(), pl.name(), pl.modernName(),
                                pl.latitude(), pl.longitude(), pl.approximate(), edge.relType().name())));
                case EVENT -> events.findById(edge.objectId(), locale).ifPresent(e ->
                        eventsOut.add(new RelatedEventItem(e.id(), e.slug(), e.title(), edge.relType().name())));
                default -> { /* other entity types not surfaced yet */ }
            }
        }

        List<SourceItem> sources = citations.citationsFor(EntityType.EVENT, d.id()).stream()
                .map(c -> {
                    HadithTexts.Entry t = hadith.lookup(c.workTitle(), c.locator()); // literal text + isnād
                    return new SourceItem(c.workTitle(), c.tier(), c.locator(),
                            t != null ? t.en() : c.quote(), t != null ? t.ar() : null,
                            t != null ? t.chain() : null, c.grade());
                })
                .toList();

        List<RouteLine> routeLines = routes.routesForEvent(d.id()).stream()
                .map(r -> new RouteLine(r.slug(), r.conjectural(), r.distanceKm(),
                        r.points().stream().map(p -> new MapPoint(p.lat(), p.lng())).toList()))
                .toList();

        return new EventDetail(d.id(), d.slug(), d.title(), d.summary(), d.why(), d.certainty(),
                d.hijriYear(), d.gregYear(), d.major(), d.chronicleSlug(), d.chronicleTitle(),
                peopleOut, versesOut, placesOut, routeLines, eventsOut, sources);
    }

    @GetMapping("/people")
    public List<PersonListItem> companions(@RequestParam(defaultValue = "en") String locale,
                                           @RequestParam(required = false) String chronicle) {
        Set<UUID> inChronicle = chronicle == null ? null : peopleIdsIn(chronicle, locale);
        return people.publishedList(locale).stream()
                .filter(p -> inChronicle == null || inChronicle.contains(p.id()))
                .map(p -> new PersonListItem(p.id(), p.slug(), p.name(), p.nameArabic(), p.role(), p.deathYearAh()))
                .toList();
    }

    /** Event ids belonging to a chronicle (used to scope people/search to it). */
    private Set<UUID> eventIdsIn(String chronicle, String locale) {
        Set<UUID> ids = new HashSet<>();
        for (var e : events.publishedTimeline(locale, chronicle)) ids.add(e.id());
        return ids;
    }

    /** Person ids that appear in a chronicle's published events. */
    private Set<UUID> peopleIdsIn(String chronicle, String locale) {
        Set<UUID> personIds = new HashSet<>();
        for (UUID eventId : eventIdsIn(chronicle, locale)) {
            for (RelatedEntity edge : relationships.neighboursOf(EntityType.EVENT, eventId)) {
                if (edge.objectType() == EntityType.PERSON) personIds.add(edge.objectId());
            }
        }
        return personIds;
    }

    @GetMapping("/people/{slug}")
    public PersonDetail person(@PathVariable String slug, @RequestParam(defaultValue = "en") String locale) {
        var p = people.findBySlug(slug, locale)
                .orElseThrow(() -> new NotFoundException("person.not_found", "No published person with slug " + slug));

        // The events that name this person are the edges pointing at them.
        List<PersonEventItem> in = new ArrayList<>();
        for (RelatedEntity edge : relationships.referencesTo(EntityType.PERSON, p.id())) {
            if (edge.objectType() == EntityType.EVENT) {
                events.findById(edge.objectId(), locale).ifPresent(e ->
                        in.add(new PersonEventItem(e.slug(), e.title(), edge.relType().name())));
            }
        }
        return new PersonDetail(p.id(), p.slug(), p.name(), p.nameArabic(), p.role(), p.deathYearAh(), in);
    }

    @GetMapping("/search")
    public List<SearchHit> search(@RequestParam(defaultValue = "") String q,
                                  @RequestParam(defaultValue = "en") String locale,
                                  @RequestParam(required = false) String chronicle) {
        Set<UUID> eventScope = chronicle == null ? null : eventIdsIn(chronicle, locale);
        Set<UUID> personScope = chronicle == null ? null : peopleIdsIn(chronicle, locale);
        List<SearchHit> hits = new ArrayList<>();
        for (SearchPort.SearchMatch m : search.search(q, 30)) {
            if ("EVENT".equals(m.type())) {
                if (eventScope != null && !eventScope.contains(m.id())) continue;
                events.findById(m.id(), locale).ifPresent(e -> hits.add(new SearchHit(
                        "EVENT", e.slug(), e.title(),
                        (e.gregYear() != null ? e.gregYear() + " CE · " : "") + "Event", null)));
            } else {
                if (personScope != null && !personScope.contains(m.id())) continue;
                people.findById(m.id(), locale).ifPresent(p -> hits.add(new SearchHit(
                        "PERSON", p.slug(), p.name(), pretty(p.role()), p.nameArabic())));
            }
        }
        return hits;
    }

    @GetMapping("/paths")
    public List<PathSummary> paths(@RequestParam(defaultValue = "en") String locale,
                                   @RequestParam(required = false) String chronicle) {
        List<PathSummary> all = paths.publishedPaths(locale);
        if (chronicle == null) return all;
        // A path belongs to a chronicle when its steps walk that chronicle's events.
        Set<String> slugs = new HashSet<>();
        for (var e : events.publishedTimeline(locale, chronicle)) slugs.add(e.slug());
        return all.stream()
                .filter(s -> paths.pathBySlug(s.slug(), locale)
                        .map(d -> d.steps().stream().anyMatch(st -> slugs.contains(st.eventSlug())))
                        .orElse(false))
                .toList();
    }

    @GetMapping("/paths/{slug}")
    public PathDetail path(@PathVariable String slug, @RequestParam(defaultValue = "en") String locale) {
        return paths.pathBySlug(slug, locale)
                .orElseThrow(() -> new NotFoundException("path.not_found", "No learning path with slug " + slug));
    }

    private static String pretty(String v) {
        return v == null ? "" : v.charAt(0) + v.substring(1).toLowerCase();
    }
}
