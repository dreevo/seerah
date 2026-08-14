package com.seerah.publicapi;

import com.seerah.content.api.ChronicleReadPort;
import com.seerah.content.api.EventReadPort;
import com.seerah.content.api.RelatedEntity;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final HadithTexts hadith;
    private final UndatedEvents undated;

    public PublicReadController(EventReadPort events, ChronicleReadPort chronicles,
                                RelationshipReadPort relationships,
                                PersonReadPort people, PlaceReadPort places, RouteReadPort routes,
                                VerseReadPort verses, CitationDirectory citations,
                                SearchPort search,
                                HadithTexts hadith,
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

        List<SourceItem> sources = new ArrayList<>(citations.citationsFor(EntityType.EVENT, d.id()).stream()
                .map(c -> {
                    HadithTexts.Entry t = hadith.lookup(c.workTitle(), c.locator()); // literal text + isnād
                    return new SourceItem(c.workTitle(), c.tier(), c.locator(),
                            t != null ? t.en() : c.quote(), t != null ? t.ar() : null,
                            t != null ? t.chain() : null, c.grade());
                })
                .toList());

        // An event that has verses revealed around it is, by that, grounded in the Qur'an —
        // ensure the Qur'an is credited in the sources even when both citation slots hold ḥadīth.
        boolean citesQuran = sources.stream()
                .anyMatch(s -> s.workTitle() != null && s.workTitle().toLowerCase().contains("qur"));
        if (!versesOut.isEmpty() && !citesQuran) {
            String locator = versesOut.stream()
                    .map(v -> "Surah " + v.surahNameEn() + " " + v.reference())
                    .distinct()
                    .collect(java.util.stream.Collectors.joining(" · "));
            sources.add(0, new SourceItem("The Noble Qur'an", "PRIMARY", locator, null, null, null, null));
        }

        List<RouteLine> routeLines = routes.routesForEvent(d.id()).stream()
                .map(r -> new RouteLine(r.slug(), r.conjectural(), r.distanceKm(),
                        r.points().stream().map(p -> new MapPoint(p.lat(), p.lng())).toList()))
                .toList();

        return new EventDetail(d.id(), d.slug(), d.title(), d.summary(), d.why(), d.certainty(),
                d.hijriYear(), d.gregYear(), d.major(), d.chronicleSlug(), d.chronicleTitle(),
                peopleOut, versesOut, placesOut, routeLines, eventsOut, sources);
    }

    /** Every located place across all chronicles — the map's context layer. */
    @GetMapping("/places")
    public List<PublicViews.MapPlace> places(@RequestParam(defaultValue = "en") String locale) {
        return places.publishedList(locale).stream()
                .filter(p -> p.latitude() != null && p.longitude() != null)
                .map(p -> new PublicViews.MapPlace(p.slug(), p.name(), p.modernName(),
                        p.latitude(), p.longitude(), p.approximate()))
                .toList();
    }

    /** Which sūrahs each prophet appears in, and how many āyāt — the Qur'an constellation. */
    @GetMapping("/quran-map")
    public List<PublicViews.ProphetSurahs> quranMap(@RequestParam(defaultValue = "en") String locale) {
        List<PublicViews.ProphetSurahs> out = new ArrayList<>();
        for (var c : chronicles.published()) {
            if ("COLLECTION".equals(c.kind())) continue;   // the constellation is of prophets, not the Qur'an-stories set
            Map<Integer, int[]> counts = new HashMap<>();
            Map<Integer, String[]> names = new HashMap<>();
            for (var e : events.publishedTimeline(locale, c.slug())) {
                for (RelatedEntity edge : relationships.neighboursOf(EntityType.EVENT, e.id())) {
                    if (edge.objectType() != EntityType.VERSE) continue;
                    verses.findById(edge.objectId(), locale).ifPresent(v -> {
                        counts.computeIfAbsent(v.surahNumber(), k -> new int[1])[0]++;
                        names.putIfAbsent(v.surahNumber(), new String[] { v.surahNameEn(), v.surahNameAr() });
                    });
                }
            }
            if (counts.isEmpty()) continue;
            int total = counts.values().stream().mapToInt(a -> a[0]).sum();
            List<PublicViews.SurahRef> surahs = counts.entrySet().stream()
                    .map(en -> new PublicViews.SurahRef(en.getKey(),
                            names.get(en.getKey())[0], names.get(en.getKey())[1], en.getValue()[0]))
                    .sorted(Comparator.comparingInt(PublicViews.SurahRef::n))
                    .toList();
            out.add(new PublicViews.ProphetSurahs(c.slug(), cleanTitle(c.title()), c.glyph(), total, surahs));
        }
        return out;
    }

    private static String cleanTitle(String t) {
        return t == null ? "" : t.replaceFirst("^The Story of Prophet |^The Life of the Prophet |^The Story of ", "");
    }

    /**
     * A whole chronicle as an ordered, cinematic walkthrough — each event a "beat"
     * carrying its summary, the one āyah revealed around it, where it happened, and
     * its primary source. This is Story Mode: the timeline told scene by scene.
     */
    @GetMapping("/chronicles/{slug}/story")
    public PublicViews.Story story(@PathVariable String slug, @RequestParam(defaultValue = "en") String locale) {
        var c = chronicles.bySlug(slug)
                .orElseThrow(() -> new NotFoundException("chronicle.not_found", "No chronicle with slug " + slug));

        List<PublicViews.StoryBeat> beats = new ArrayList<>();
        for (var t : events.publishedTimeline(locale, slug)) {
            var d = events.findDetailBySlug(t.slug(), locale).orElse(null);
            if (d == null) continue;

            RelatedVerse verse = null;
            RelatedPlace place = null;
            for (RelatedEntity edge : relationships.neighboursOf(EntityType.EVENT, d.id())) {
                if (edge.objectType() == EntityType.VERSE && verse == null) {
                    var v = verses.findById(edge.objectId(), locale).orElse(null);
                    if (v != null) {
                        verse = new RelatedVerse(v.reference(), v.surahNameEn(), v.surahNameAr(),
                                v.textUthmani(), v.translationText(), v.translator(), edge.relType().name());
                    }
                } else if (edge.objectType() == EntityType.PLACE && place == null) {
                    var pl = places.findById(edge.objectId(), locale).orElse(null);
                    if (pl != null && pl.latitude() != null && pl.longitude() != null) {
                        place = new RelatedPlace(pl.id(), pl.slug(), pl.name(), pl.modernName(),
                                pl.latitude(), pl.longitude(), pl.approximate(), edge.relType().name());
                    }
                }
            }

            // The strongest citation: a PRIMARY-tier source if any, else the first.
            SourceItem source = citations.citationsFor(EntityType.EVENT, d.id()).stream()
                    .min(Comparator.comparingInt(cit -> "PRIMARY".equalsIgnoreCase(cit.tier()) ? 0 : 1))
                    .map(cit -> {
                        HadithTexts.Entry txt = hadith.lookup(cit.workTitle(), cit.locator());
                        return new SourceItem(cit.workTitle(), cit.tier(), cit.locator(),
                                txt != null ? txt.en() : cit.quote(), txt != null ? txt.ar() : null,
                                txt != null ? txt.chain() : null, cit.grade());
                    })
                    .orElse(null);
            // An event grounded in the Qur'an but citing only ḥadīth still credits the Qur'an.
            if (source == null && verse != null) {
                source = new SourceItem("The Noble Qur'an", "PRIMARY",
                        "Surah " + verse.surahNameEn() + " " + verse.reference(), null, null, null, null);
            }

            beats.add(new PublicViews.StoryBeat(d.slug(), d.title(), d.summary(), d.why(),
                    d.hijriYear(), d.gregYear(), d.major(), undated.isUndated(d.slug()),
                    verse, place, source));
        }
        return new PublicViews.Story(c.slug(), cleanTitle(c.title()), c.glyph(), c.titleAr(), c.blurb(), beats);
    }

    /**
     * The isnād behind the corpus: every ḥadīth we cite, its full chain, the Companion
     * who anchors it, and the events it grounds — the raw material for the network that
     * shows how these narrations reach us from the Prophet ﷺ through named Companions.
     */
    @GetMapping("/isnad")
    public List<PublicViews.IsnadReport> isnad(@RequestParam(defaultValue = "en") String locale) {
        // key "collection:number" → the report we are assembling for that ḥadīth
        Map<String, List<PublicViews.IsnadEventRef>> eventsByKey = new LinkedHashMap<>();
        Map<String, HadithTexts.Entry> entryByKey = new LinkedHashMap<>();
        Map<String, String> gradeByKey = new HashMap<>();

        for (var c : chronicles.published()) {
            String prophet = cleanTitle(c.title());
            for (var e : events.publishedTimeline(locale, c.slug())) {
                for (var cit : citations.citationsFor(EntityType.EVENT, e.id())) {
                    String key = hadith.key(cit.workTitle(), cit.locator());
                    if (key == null) continue;
                    HadithTexts.Entry entry = hadith.lookup(cit.workTitle(), cit.locator());
                    if (entry == null || entry.chain() == null || entry.chain().isEmpty()) continue;
                    entryByKey.putIfAbsent(key, entry);
                    if (cit.grade() != null) gradeByKey.putIfAbsent(key, cit.grade());
                    eventsByKey.computeIfAbsent(key, k -> new ArrayList<>())
                            .add(new PublicViews.IsnadEventRef(e.slug(), e.title(), c.slug(), prophet, c.glyph()));
                }
            }
        }

        List<PublicViews.IsnadReport> out = new ArrayList<>();
        for (var en : entryByKey.entrySet()) {
            String[] parts = en.getKey().split(":", 2);
            var chain = en.getValue().chain();
            Narrators.Companion comp = Narrators.of(chain.get(chain.size() - 1));
            out.add(new PublicViews.IsnadReport(parts[0], collectorName(parts[0]), parts[1],
                    gradeByKey.get(en.getKey()), comp.ar(), comp.en(), chain, eventsByKey.get(en.getKey())));
        }
        return out;
    }

    private static String collectorName(String coll) {
        return switch (coll) {
            case "bukhari" -> "Ṣaḥīḥ al-Bukhārī";
            case "muslim" -> "Ṣaḥīḥ Muslim";
            case "tirmidhi" -> "Jāmiʿ al-Tirmidhī";
            default -> coll;
        };
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

    private static String pretty(String v) {
        return v == null ? "" : v.charAt(0) + v.substring(1).toLowerCase();
    }
}
