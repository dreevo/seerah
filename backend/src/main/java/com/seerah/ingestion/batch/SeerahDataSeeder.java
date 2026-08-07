package com.seerah.ingestion.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seerah.content.api.LearningPathRegistrar;
import com.seerah.content.application.port.in.ApproveEventUseCase;
import com.seerah.content.application.port.in.CreateEventUseCase;
import com.seerah.content.application.port.in.LinkEntitiesUseCase;
import com.seerah.content.application.port.in.PublishEventUseCase;
import com.seerah.content.application.port.in.SetEventTextUseCase;
import com.seerah.content.application.port.in.SubmitEventUseCase;
import com.seerah.media.api.MediaRegistrar;
import com.seerah.media.domain.MediaKind;
import com.seerah.people.application.port.in.CreatePersonUseCase;
import com.seerah.people.application.port.in.PersonLifecycleUseCases;
import com.seerah.people.domain.PersonRole;
import com.seerah.places.api.GeoPoint;
import com.seerah.places.api.PlaceRegistrar;
import com.seerah.places.api.RouteRegistrar;
import com.seerah.provenance.api.CitationRegistrar;
import com.seerah.provenance.api.CitationRegistrar.AddCitation;
import com.seerah.provenance.api.CitationRegistrar.AddScholarlyPosition;
import com.seerah.provenance.api.CitationRegistrar.RegisterSource;
import com.seerah.review.api.ReviewRegistrar;
import com.seerah.scripture.api.VerseReadPort;
import com.seerah.scripture.api.VerseRegistrar;
import com.seerah.scripture.domain.RevelationPlace;
import com.seerah.shared.Certainty;
import com.seerah.shared.CitationRole;
import com.seerah.shared.EntityType;
import com.seerah.shared.HadithGrade;
import com.seerah.shared.RelationshipType;
import com.seerah.shared.SourceTier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ingests the vetted Phase-1 chronology from {@code resources/seed/chronology.json}
 * and loads it through the real use-cases — create → cite → scholar sign-off →
 * publish — so every seeded item passes the same invariants (§13.2, §13.4, §13.6)
 * that guard the live editorial flow. This is the ingestion module's job: batch
 * import from a vetted file (§ ingestion / batch, §12.7). Enabled with
 * {@code seerah.seed=true}.
 */
@Component
@ConditionalOnProperty(name = "seerah.seed", havingValue = "true")
public class SeerahDataSeeder implements ApplicationRunner {

    private static final String SCHOLAR_EMAIL = "board@seerah.platform";
    private static final String SCHOLAR_NAME = "Scholarly Advisory Board";

    private final CreateEventUseCase createEvent;
    private final SetEventTextUseCase setText;
    private final SubmitEventUseCase submitEvent;
    private final ApproveEventUseCase approveEvent;
    private final PublishEventUseCase publishEvent;
    private final LinkEntitiesUseCase link;
    private final CreatePersonUseCase createPerson;
    private final PersonLifecycleUseCases personLifecycle;
    private final CitationRegistrar citations;
    private final VerseRegistrar scripture;
    private final VerseReadPort verseRead;
    private final PlaceRegistrar placesReg;
    private final ReviewRegistrar review;
    private final RouteRegistrar routesReg;
    private final LearningPathRegistrar pathsReg;
    private final MediaRegistrar mediaReg;
    private final IngestionLog ingestionLog;
    private final ObjectMapper json;
    private final com.seerah.content.api.ChronicleReadPort chronicles;

    /** Sources are shared across chronicle files (e.g. Sahih al-Bukhari), so a
     *  slug registered by one file is reused by the next rather than re-inserted. */
    private final Map<String, UUID> sourceRegistry = new LinkedHashMap<>();
    /** People are likewise shared across files — a prophet (e.g. Dāwūd, Sulaymān)
     *  can appear in more than one chronicle, so a slug is created once and reused. */
    private final Map<String, UUID> personRegistry = new LinkedHashMap<>();

    public SeerahDataSeeder(CreateEventUseCase createEvent, SetEventTextUseCase setText,
                            SubmitEventUseCase submitEvent, ApproveEventUseCase approveEvent,
                            PublishEventUseCase publishEvent, LinkEntitiesUseCase link,
                            CreatePersonUseCase createPerson, PersonLifecycleUseCases personLifecycle,
                            CitationRegistrar citations, VerseRegistrar scripture, VerseReadPort verseRead,
                            PlaceRegistrar placesReg, ReviewRegistrar review, RouteRegistrar routesReg,
                            LearningPathRegistrar pathsReg, MediaRegistrar mediaReg,
                            IngestionLog ingestionLog, ObjectMapper json,
                            com.seerah.content.api.ChronicleReadPort chronicles) {
        this.chronicles = chronicles;
        this.createEvent = createEvent;
        this.setText = setText;
        this.submitEvent = submitEvent;
        this.approveEvent = approveEvent;
        this.publishEvent = publishEvent;
        this.link = link;
        this.createPerson = createPerson;
        this.personLifecycle = personLifecycle;
        this.citations = citations;
        this.scripture = scripture;
        this.verseRead = verseRead;
        this.placesReg = placesReg;
        this.review = review;
        this.routesReg = routesReg;
        this.pathsReg = pathsReg;
        this.mediaReg = mediaReg;
        this.ingestionLog = ingestionLog;
        this.json = json;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        loadQuranReference();

        // Idempotency: if the editorial corpus is already present, do nothing.
        // Without this, restarting a seeded app would re-insert and crash on the
        // first duplicate slug — so a re-run must be a safe no-op.
        if (ingestionLog.hasRows("source")) {
            return;
        }

        // The platform is a library of chronicles. Ingest each vetted file into
        // its chronicle; every item still passes create → cite → sign-off → publish.
        UUID seerah = chronicles.idBySlug("seerah")
                .orElseThrow(() -> new IllegalStateException("seerah chronicle missing — check V10 migration"));
        UUID yusuf = chronicles.idBySlug("yusuf")
                .orElseThrow(() -> new IllegalStateException("yusuf chronicle missing — check V10 migration"));
        UUID musa = chronicles.idBySlug("musa")
                .orElseThrow(() -> new IllegalStateException("musa chronicle missing — check V11 migration"));
        UUID ibrahim = chronicles.idBySlug("ibrahim")
                .orElseThrow(() -> new IllegalStateException("ibrahim chronicle missing — check V12 migration"));
        UUID nuh = chronicles.idBySlug("nuh")
                .orElseThrow(() -> new IllegalStateException("nuh chronicle missing — check V12 migration"));
        UUID isa = chronicles.idBySlug("isa")
                .orElseThrow(() -> new IllegalStateException("isa chronicle missing — check V13 migration"));
        UUID adam = chronicles.idBySlug("adam")
                .orElseThrow(() -> new IllegalStateException("adam chronicle missing — check V14 migration"));
        UUID dawud = chronicles.idBySlug("dawud")
                .orElseThrow(() -> new IllegalStateException("dawud chronicle missing — check V15 migration"));
        UUID sulayman = chronicles.idBySlug("sulayman")
                .orElseThrow(() -> new IllegalStateException("sulayman chronicle missing — check V15 migration"));
        UUID yunus = chronicles.idBySlug("yunus")
                .orElseThrow(() -> new IllegalStateException("yunus chronicle missing — check V16 migration"));
        UUID hud = chronicles.idBySlug("hud")
                .orElseThrow(() -> new IllegalStateException("hud chronicle missing — check V16 migration"));
        UUID salih = chronicles.idBySlug("salih")
                .orElseThrow(() -> new IllegalStateException("salih chronicle missing — check V16 migration"));
        UUID lut = chronicles.idBySlug("lut")
                .orElseThrow(() -> new IllegalStateException("lut chronicle missing — check V17 migration"));
        UUID shuayb = chronicles.idBySlug("shuayb")
                .orElseThrow(() -> new IllegalStateException("shuayb chronicle missing — check V17 migration"));
        UUID ayyub = chronicles.idBySlug("ayyub")
                .orElseThrow(() -> new IllegalStateException("ayyub chronicle missing — check V17 migration"));
        UUID zakariyya = chronicles.idBySlug("zakariyya")
                .orElseThrow(() -> new IllegalStateException("zakariyya chronicle missing — check V18 migration"));
        UUID yahya = chronicles.idBySlug("yahya")
                .orElseThrow(() -> new IllegalStateException("yahya chronicle missing — check V18 migration"));
        UUID idris = chronicles.idBySlug("idris")
                .orElseThrow(() -> new IllegalStateException("idris chronicle missing — check V19 migration"));
        UUID ilyas = chronicles.idBySlug("ilyas")
                .orElseThrow(() -> new IllegalStateException("ilyas chronicle missing — check V19 migration"));
        UUID alyasa = chronicles.idBySlug("alyasa")
                .orElseThrow(() -> new IllegalStateException("alyasa chronicle missing — check V19 migration"));
        UUID dhulkifl = chronicles.idBySlug("dhulkifl")
                .orElseThrow(() -> new IllegalStateException("dhulkifl chronicle missing — check V19 migration"));

        ingest(adam, "seed/chronicle-adam.json", "editorial-adam");
        ingest(idris, "seed/chronicle-idris.json", "editorial-idris");
        ingest(nuh, "seed/chronicle-nuh.json", "editorial-nuh");
        ingest(hud, "seed/chronicle-hud.json", "editorial-hud");
        ingest(salih, "seed/chronicle-salih.json", "editorial-salih");
        ingest(ibrahim, "seed/chronicle-ibrahim.json", "editorial-ibrahim");
        ingest(lut, "seed/chronicle-lut.json", "editorial-lut");
        ingest(shuayb, "seed/chronicle-shuayb.json", "editorial-shuayb");
        ingest(ayyub, "seed/chronicle-ayyub.json", "editorial-ayyub");
        ingest(yusuf, "seed/chronicle-yusuf.json", "editorial-yusuf");
        ingest(yunus, "seed/chronicle-yunus.json", "editorial-yunus");
        ingest(musa, "seed/chronicle-musa.json", "editorial-musa");
        ingest(dawud, "seed/chronicle-dawud.json", "editorial-dawud");
        ingest(sulayman, "seed/chronicle-sulayman.json", "editorial-sulayman");
        ingest(zakariyya, "seed/chronicle-zakariyya.json", "editorial-zakariyya");
        ingest(yahya, "seed/chronicle-yahya.json", "editorial-yahya");
        ingest(ilyas, "seed/chronicle-ilyas.json", "editorial-ilyas");
        ingest(alyasa, "seed/chronicle-alyasa.json", "editorial-alyasa");
        ingest(dhulkifl, "seed/chronicle-dhulkifl.json", "editorial-dhulkifl");
        ingest(isa, "seed/chronicle-isa.json", "editorial-isa");
        ingest(seerah, "seed/chronology.json", "editorial-chronology");
    }

    /** Ingest one vetted chronicle file into the given chronicle. */
    private void ingest(UUID chronicleId, String resource, String runName) throws IOException {
        byte[] raw;
        try (InputStream in = new ClassPathResource(resource).getInputStream()) {
            raw = in.readAllBytes();
        }
        JsonNode root = json.readTree(raw);
        UUID runId = ingestionLog.startRun(runName, sha256(raw));
        int[] skipped = {0};

        Map<String, UUID> sources = new LinkedHashMap<>();
        for (JsonNode s : root.path("sources")) {
            String sslug = s.get("slug").asText();
            UUID sid = sourceRegistry.computeIfAbsent(sslug, k -> citations.registerSource(new RegisterSource(
                    sslug, s.get("workTitle").asText(), s.get("author").asText(),
                    SourceTier.valueOf(s.get("tier").asText()), false)));
            sources.put(sslug, sid);
        }
        // The source each person's biographical notice cites (declared per file).
        String personSourceSlug = root.path("personSource").asText("");
        if (personSourceSlug.isEmpty() && !sources.isEmpty()) personSourceSlug = sources.keySet().iterator().next();
        UUID biographies = sources.getOrDefault(personSourceSlug, sources.values().stream().findFirst().orElse(null));

        Map<String, UUID> people = new LinkedHashMap<>();
        String bioSlug = personSourceSlug;
        for (JsonNode p : root.path("people")) {
            String pslug = p.get("slug").asText();
            UUID pid = personRegistry.computeIfAbsent(pslug, k -> publishPerson(p, bioSlug, biographies));
            people.put(pslug, pid);
        }

        Map<String, UUID> places = new LinkedHashMap<>();
        for (JsonNode pl : root.path("places")) {
            places.put(pl.get("slug").asText(), placesReg.upsertPlace(new PlaceRegistrar.Command(
                    pl.get("slug").asText(), pl.get("name").asText(), pl.path("ar").asText(null),
                    pl.path("modern").asText(null), pl.get("lat").asDouble(), pl.get("lng").asDouble(),
                    pl.path("approx").asBoolean(false))));
        }

        Map<String, UUID> events = new LinkedHashMap<>();
        List<String> order = new ArrayList<>();
        int row = 0;
        for (JsonNode e : root.path("events")) {
            row++;
            String slug = e.get("slug").asText();
            UUID id = publishEventNode(e, sources, chronicleId, row);
            events.put(slug, id);
            order.add(slug);

            for (JsonNode ps : e.path("people")) {
                UUID pid = people.get(ps.asText());
                if (pid == null) { skip(runId, row, "unknown person: " + ps.asText(), e, skipped); continue; }
                relate(EntityType.EVENT, id, RelationshipType.PARTICIPATED_IN, EntityType.PERSON, pid, 0.9);
            }
            for (JsonNode vr : e.path("verses")) {
                UUID vid = resolveVerse(vr.asText());
                if (vid == null) { skip(runId, row, "verse not in corpus: " + vr.asText(), e, skipped); continue; }
                relate(EntityType.EVENT, id, RelationshipType.REVEALED_ABOUT, EntityType.VERSE, vid, 0.95);
            }
            if (e.hasNonNull("place")) {
                UUID plid = places.get(e.get("place").asText());
                if (plid == null) skip(runId, row, "unknown place: " + e.get("place").asText(), e, skipped);
                else relate(EntityType.EVENT, id, RelationshipType.OCCURRED_AT, EntityType.PLACE, plid, 1.0);
            }
            int ord = 0;
            for (JsonNode m : e.path("media")) {
                UUID mid = mediaReg.registerAsset(new MediaRegistrar.RegisterAsset(
                        m.get("s3Key").asText(), MediaKind.valueOf(m.get("kind").asText()), "image/svg+xml",
                        4096L, "CC-BY-4.0", m.get("attribution").asText(), null));
                mediaReg.linkToEvent(mid, id, ord++, m.path("caption").asText(null));
            }
        }

        // Chain the events in chronological (file) order.
        for (int i = 1; i < order.size(); i++) {
            UUID earlier = events.get(order.get(i - 1));
            UUID later = events.get(order.get(i));
            relate(EntityType.EVENT, earlier, RelationshipType.PRECEDED, EntityType.EVENT, later, 1.0);
            relate(EntityType.EVENT, later, RelationshipType.FOLLOWED, EntityType.EVENT, earlier, 1.0);
        }

        for (JsonNode r : root.path("routes")) {
            List<GeoPoint> pts = new ArrayList<>();
            for (JsonNode pt : r.path("points")) {
                pts.add(new GeoPoint(pt.get(0).asDouble(), pt.get(1).asDouble()));
            }
            routesReg.upsertRoute(r.get("slug").asText(), events.get(r.get("event").asText()),
                    r.get("conjectural").asBoolean(), pts);
        }

        for (JsonNode p : root.path("paths")) {
            UUID pid = pathsReg.createPath(p.get("slug").asText(), p.get("title").asText(),
                    p.path("blurb").asText(null), "GENERAL", p.path("est").asInt(0));
            int step = 1;
            for (JsonNode s : p.path("steps")) {
                pathsReg.addEventStep(pid, step++, events.get(s.asText()), null);
            }
        }

        ingestionLog.finish(runId, order.size(), order.size(), skipped[0], "COMPLETED");
    }

    /** Bulk-load the full Qur'an (114 surahs, 6,236 verses) once, with an audit run (§12.7, §12.10). */
    private void loadQuranReference() throws IOException {
        if (scripture.isReferenceLoaded()) {
            return;
        }
        byte[] raw;
        try (InputStream in = new ClassPathResource("seed/quran/quran.json").getInputStream()) {
            raw = in.readAllBytes();
        }
        JsonNode q = json.readTree(raw);
        UUID runId = ingestionLog.startRun("quran-reference", sha256(raw));

        List<VerseRegistrar.SurahRow> surahs = new ArrayList<>();
        for (JsonNode s : q.path("surahs")) {
            surahs.add(new VerseRegistrar.SurahRow(s.get("n").asInt(), s.get("nameAr").asText(),
                    s.get("translit").asText(), RevelationPlace.valueOf(s.get("place").asText()), s.get("ayat").asInt()));
        }
        List<VerseRegistrar.VerseRow> verses = new ArrayList<>();
        for (JsonNode v : q.path("verses")) {
            verses.add(new VerseRegistrar.VerseRow(v.get("s").asInt(), v.get("a").asInt(),
                    v.get("ar").asText(), v.get("en").asText()));
        }
        scripture.loadReference(q.path("translator").asText("Saheeh International"), surahs, verses);
        ingestionLog.finish(runId, verses.size(), verses.size(), 0, "COMPLETED");
    }

    private UUID resolveVerse(String ref) {
        String[] p = ref.split(":");
        return verseRead.findByRef(Integer.parseInt(p[0]), Integer.parseInt(p[1]), "en")
                .map(v -> v.id()).orElse(null);
    }

    private void skip(UUID runId, int row, String reason, JsonNode payload, int[] counter) {
        ingestionLog.skip(runId, row, reason, payload.toString());
        counter[0]++;
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private UUID publishPerson(JsonNode p, String sourceSlug, UUID sourceId) {
        String slug = p.get("slug").asText();
        UUID id = createPerson.create(new CreatePersonUseCase.Command(
                slug, p.get("name").asText(), p.path("ar").asText(null),
                PersonRole.valueOf(p.get("role").asText()),
                intOrNull(p, "birthCe"), intOrNull(p, "deathCe"), null,
                p.has("deathCe") ? hijriFor(p.get("deathCe").asInt()) : null, null));
        personLifecycle.submit(id);
        personLifecycle.approve(id);
        cite(sourceSlug, sourceId, "biographical notice — " + slug, EntityType.PERSON, id, CitationRole.SUPPORTS);
        personLifecycle.publish(id);
        return id;
    }

    private UUID publishEventNode(JsonNode e, Map<String, UUID> sources, UUID chronicleId, int sortKey) {
        String slug = e.get("slug").asText();
        // greg year is optional: ancient prophetic chronologies (e.g. Yūsuf) have
        // no attested Gregorian year, so they order by sequence alone.
        Integer greg = e.hasNonNull("greg") ? e.get("greg").asInt() : null;
        int hijri = greg == null ? 0 : hijriFor(greg);
        UUID id = createEvent.create(new CreateEventUseCase.Command(
                slug, chronicleId, "en", e.get("title").asText(), hijri, greg,
                Certainty.valueOf(e.get("certainty").asText()), e.path("major").asBoolean(false), sortKey));
        setText.setText(id, "summary", "en", e.get("summary").asText());
        if (e.hasNonNull("why")) setText.setText(id, "why", "en", e.get("why").asText());
        submitEvent.submit(id);
        approveEvent.approve(id);

        String s1 = e.get("source").asText();
        cite(s1, sources.get(s1), e.get("locator").asText(), EntityType.EVENT, id, CitationRole.SUPPORTS);
        if (e.hasNonNull("source2")) {
            String s2 = e.get("source2").asText();
            cite(s2, sources.get(s2), e.get("locator2").asText(), EntityType.EVENT, id, CitationRole.CONTEXTUALISES);
        }
        int ord = 0;
        for (JsonNode pos : e.path("positions")) {
            citations.addScholarlyPosition(new AddScholarlyPosition(EntityType.EVENT, id,
                    pos.get("key").asText(), pos.get("heldBy").asText(), pos.get("summary").asText(), null, ord++));
        }

        review.approve(EntityType.EVENT, id, 1, SCHOLAR_EMAIL, SCHOLAR_NAME, "seed sign-off");
        publishEvent.publish(id);
        return id;
    }

    private void cite(String sourceSlug, UUID sourceId, String locator,
                      EntityType targetType, UUID targetId, CitationRole role) {
        citations.addCitation(new AddCitation(sourceId, locator, locatorKindFor(sourceSlug), null,
                gradeFor(sourceSlug), targetType, targetId, role, null));
    }

    /**
     * A hadith grade applies only to hadith. The Qur'an is mass-transmitted
     * revelation (its certainty is carried by the event's MUTAWATIR tag, not an
     * isnād grade), and the classical sīra is biography, not a graded hadith — so
     * both are left ungraded. Only the authenticated collections carry SAHIH.
     */
    private static HadithGrade gradeFor(String slug) {
        String s = slug == null ? "" : slug.toLowerCase();
        if (s.contains("bukhari") || s.contains("muslim") || s.contains("tirmidhi")
                || s.contains("nasai") || s.contains("abu-dawud") || s.contains("ibn-majah")
                || s.contains("muwatta") || s.contains("ahmad") || s.contains("hadith")) {
            return HadithGrade.SAHIH;
        }
        return null;
    }

    private static String locatorKindFor(String slug) {
        String s = slug == null ? "" : slug.toLowerCase();
        if (s.contains("quran")) return "AYAH";
        if (gradeFor(slug) != null) return "HADITH";
        return "SECTION";
    }

    private void relate(EntityType st, UUID sid, RelationshipType rel, EntityType ot, UUID oid, double weight) {
        if (sid == null || oid == null) return;
        link.link(new LinkEntitiesUseCase.Command(st, sid, rel, ot, oid, weight, null));
    }

    private static Integer intOrNull(JsonNode n, String field) {
        return n.hasNonNull(field) ? n.get(field).asInt() : null;
    }

    /** Rough CE→AH used only for seed display; the platform stores what sources state. */
    private static int hijriFor(int gregYear) {
        return gregYear < 622 ? 0 : Math.max(1, Math.round((gregYear - 622) * (33f / 32f)) + 1);
    }
}
