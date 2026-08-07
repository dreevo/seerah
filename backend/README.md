# Seerah Platform — Backend (Phase 1)

The real platform described in the **Technical Design Record**, begun for real:
a **Java 21 · Spring Boot 3 · PostgreSQL** modular monolith with a hexagonal
interior and boundaries enforced by tests.

> Built so far: the **foundation** plus five real modules — **`content`** (Event
> + relationships), **`people`** (Person), **`scripture`** (Qur'anic verses),
> **`provenance`** (sources/citations), and a **`publicapi`** read BFF — plus an
> **`ingestion`** seeder that loads real, connected Seerah data through the proper
> use-cases. The connected chronology is live end-to-end and covered by tests.
> The remaining modules exist as package charters ("drawn now, while drawing them
> is free" — §22.1).

---

## The one principle this slice proves

> **Nothing reaches the public without a citation.** (§13.2)
> **Where scholars differ, the disagreement must be on record.** (§13.4)
> **And nothing publishes carrying an approval that was never given to it.** (§13.6)

The first two are enforced in the `Event` aggregate at publish time; the third —
the **stale-approval check**, "the most important rule in the platform and the
least obvious" — is a **database trigger**. Publishing recomputes a SHA-256
`content_hash` over the event's substantive content + translations + citations and
refuses unless a scholar `approval` with a matching hash exists. Because the hash
is computed in SQL, even a direct `UPDATE` can't sneak past it. All three are
covered by tests, including one that approves an event, silently edits it, and
watches the publish get rejected as `STALE_APPROVAL` until it is re-approved.

## Architecture (as built)

```
com.seerah
├── SeerahApplication            @SpringBootApplication, nothing else
├── platform/                    cross-cutting; every module may import
│   ├── error/                   DomainException hierarchy → RFC 9457 ProblemDetail (§27)
│   └── outbox/                  transactional outbox (§25.5)
├── shared/                      the (tiny) shared kernel: cross-context enums (§6.8.1)
│
├── content/                     ← the fully-built module (the "Catalog" context)
│   ├── api/                     EventReadPort, EventSummaryView       (published contract)
│   ├── domain/                  Event aggregate, HistoricalDate, Slug … (framework-free)
│   ├── application/             use-case ports + services (the transaction boundary)
│   └── adapter/                 in/web (REST) · out/persistence (JPA) · out/provenance
│
├── provenance/                  ← slim but real (a core subdomain, §5.1.1)
│   ├── api/                     CitationDirectory (read), CitationRegistrar (write)
│   ├── application/             CitationService + out-port
│   └── adapter/                 in/web · out/persistence
│
└── people places review search assistant ingestion identity media   (charters only)
```

Since the first increment this has grown to a **connected graph**:

```
content     Event aggregate + narrative order + the polymorphic relationship edge (§12.5),
            with forward (neighboursOf) and reverse (referencesTo) traversal; and learning paths —
            curated, ordered "Guided Journeys" through the events (§12.8)
people      Person aggregate + aliases; same DRAFT→…→PUBLISHED workflow, same citation gate
places      Reference geography (§12.4): places (lat/long + a DB-generated PostGIS point) and routes
            (PostGIS linestrings — the Hijrah path etc., with a measured distance, conjectural by default)
scripture   Surah / Verse / translation reference data (verbatim Uthmani, never transformed §12.7)
provenance  Source / Citation / CitationLink / ScholarlyPosition; the citation-required authority
review      Scholarly sign-off + the stale-approval check (§13.6): review_action log, approval rows
            fingerprinted by a SQL content_hash, and a DB trigger that refuses to publish anything
            whose content changed after it was approved
identity    The editorial gate (§4.2, §6.5): Spring Security role rules — public GET open, writes need
            an EDITOR, /api/review/** needs a SCHOLAR. HTTP Basic + in-memory dev accounts now;
            Keycloak/JWT (§ security phase) slots in behind the same rules
media       Map / manuscript / diagram / calligraphy / audio assets (§12.9), attached to events. The
            media_kind enum has NO value for a depiction of a person (§6.5) — the type system cannot
            name the prohibited thing; attribution is NOT NULL, so an unattributed asset can't be stored
search      Discovery — two engines behind one SearchPort, chosen by search.engine (§17, §20):
            Postgres ILIKE (default), or OpenSearch fed by the transactional-outbox relay
            (SearchProjectionRelay). Debezium connector config for the CDC swap is in
            resources/debezium/. Fuller Arabic analysis (§18) is a mapping change in the adapter.
assistant   §5.7 — retrieves and never asserts. Extractive, grounded Q&A: it can only return text
            that already passed review and is cited, with [S#] markers; when the corpus is silent it
            returns the exact fixed refusal. No LLM, no persistence — the guardrails are structural
publicapi   read-only BFF composing the above via their api ports: connected event detail
            (people · verses · places · routes · neighbouring events · sources · approvals),
            companions, search, and the grounded assistant
ingestion   file-driven seeder with a full audit trail (§12.7, §12.10):
            • REFERENCE — bulk-loads the entire Qur'an (114 surahs, 6,236 verses; Tanzil Uthmani +
              Saheeh Intl translation) via one batched JDBC pass, guarded/idempotent, checksummed.
            • EDITORIAL — reads resources/seed/chronology.json (22 pivotal events, Year of the Elephant
              → the Passing of the Prophet ﷺ, with 28 companions, places, routes, illustrations, guided
              journeys) and loads it through the real use-cases (create → cite → scholar sign-off →
              publish), resolving verse links against the corpus and recording an ingestion_run.
            Anything it refuses is captured in skip_audit — silent data loss is worse than a failure.
            Enable with seerah.seed=true.
```

**The dependency rule** (§23.1) runs inward: `adapter → application → domain`.
Cross-module calls go **only** through a module's `api` package. Both — plus the
privacy of every module's adapters — are enforced by the 10 rules in
[`ArchitectureTest`](src/test/java/com/seerah/ArchitectureTest.java).

Referential integrity for the polymorphic `relationship` edge (whose target can be
any entity type) is enforced by a **Postgres constraint trigger** that raises
SQLSTATE 23503 — so Spring maps it to a `DataIntegrityViolationException` exactly
as a real foreign key would (§12.5).

## Running the tests

Requires **Docker** (for the Testcontainers Postgres) and **JDK 21** (auto-detected
by the Gradle toolchain).

```bash
cd backend
./gradlew test          # domain unit tests + ArchUnit boundaries + full integration slice
```

- `content/domain/EventPublishTest` — pure-domain invariants, no infrastructure.
- `ArchitectureTest` — the module boundaries as executable rules.
- `ContentPublishIntegrationTest` — the whole slice against a real Postgres:
  publish-rejected-until-cited, scholars-differ-needs-two-positions, and the
  outbox event on successful publish.

## Running the app

```bash
# a local Postgres with PostGIS (or point DB_URL/DB_USER/DB_PASSWORD at your own)
docker run -d --name seerah-pg -e POSTGRES_USER=seerah -e POSTGRES_PASSWORD=seerah \
  -e POSTGRES_DB=seerah -p 5432:5432 postgis/postgis:16-3.4

./gradlew bootRun \
  --args='--seerah.seed=true'      # Flyway applies V1-V7, then the seeder loads a live chronology
```

Then the reader endpoints are ready for the Angular app:

```bash
curl -s localhost:8080/api/public/timeline | jq '.[].title'
curl -s localhost:8080/api/public/events/the-battle-of-badr \
  | jq '{title, people:[.people[].name], verse:.verses[0].reference, place:.places[0].name, sources:[.sources[].workTitle]}'
curl -s localhost:8080/api/public/people | jq '.[].name'
curl -s localhost:8080/api/public/people/hamza | jq '{name, events:[.events[].title]}'
curl -s 'localhost:8080/api/public/search?q=amnesty' | jq '.[] | {type, title}'
curl -s 'localhost:8080/api/public/ask?q=what%20happened%20at%20Badr' \
  | jq '{answered, passages:[.passages[].eventTitle], sources:[.sources[].workTitle]}'
curl -s 'localhost:8080/api/public/ask?q=recipe%20for%20biryani' | jq '.message'
curl -s localhost:8080/api/public/paths | jq '.[] | {title, stepCount}'
curl -s localhost:8080/api/public/paths/start-here | jq '{title, steps:[.steps[].eventTitle]}'
```

### A full walk-through with `curl`

```bash
# Writes need an editor; scholarly sign-off needs a scholar (dev accounts below).
ED='-u editor:editor-dev'   # override via SEERAH_EDITOR_PASSWORD
SC='-u scholar:scholar-dev' # override via SEERAH_SCHOLAR_PASSWORD

# 1. create a draft event (editor)
EID=$(curl -s $ED -XPOST localhost:8080/api/events -H 'content-type: application/json' -d '{
  "slug":"battle-of-badr","title":"The Battle of Badr","hijriYear":2,"gregorianYear":624,
  "certainty":"WELL_ATTESTED","major":true,"sortKey":0}' | sed 's/.*"id":"\([^"]*\)".*/\1/')

# 2. move through the review workflow (editor)
curl -s $ED -XPOST localhost:8080/api/events/$EID/submit
curl -s $ED -XPOST localhost:8080/api/events/$EID/approve

# 3. try to publish WITHOUT a citation → 422 event.publish.requires_citation
curl -s $ED -XPOST localhost:8080/api/events/$EID/publish

# 4. register a source and cite the event (editor)
SID=$(curl -s $ED -XPOST localhost:8080/api/sources -H 'content-type: application/json' -d '{
  "slug":"bukhari","workTitle":"Sahih al-Bukhari","author":"al-Bukhari","tier":"PRIMARY","quotable":true}' \
  | sed 's/.*"id":"\([^"]*\)".*/\1/')
curl -s $ED -XPOST localhost:8080/api/citations -H 'content-type: application/json' -d "{
  \"sourceId\":\"$SID\",\"locator\":\"Maghazi 3951\",\"locatorKind\":\"HADITH\",
  \"grade\":\"SAHIH\",\"targetType\":\"EVENT\",\"targetId\":\"$EID\",\"role\":\"SUPPORTS\"}"

# 5. a scholar signs off on the final content (§13.6)
curl -s $SC -XPOST localhost:8080/api/review/events/approve -H 'content-type: application/json' -d "{
  \"targetId\":\"$EID\",\"version\":1,\"scholarEmail\":\"board@seerah.test\",\"scholarName\":\"Board\"}"

# 6. publish → 204, and it now appears on the timeline
curl -s $ED -XPOST localhost:8080/api/events/$EID/publish
curl -s localhost:8080/api/public/timeline
```

## The search phase (§17): flipping to OpenSearch

Search runs on Postgres by default. To run it on OpenSearch instead — the same
`SearchPort`, so nothing above the adapter changes:

```bash
docker run -d --name seerah-os -p 9200:9200 \
  -e discovery.type=single-node -e DISABLE_SECURITY_PLUGIN=true \
  opensearchproject/opensearch:2.17.1

./gradlew bootRun --args='--seerah.seed=true \
  --search.engine=opensearch --search.opensearch.uri=http://localhost:9200'
```

On publish, the **outbox relay** (`SearchProjectionRelay`, a Phase-1 polling job)
projects each event/person into the index; `GET /api/public/search?q=` then answers
from OpenSearch. In the CDC phase the poller is retired and **Debezium** tails the
same outbox table — the connector config is in
[`resources/debezium/seerah-outbox-connector.json`](src/main/resources/debezium/seerah-outbox-connector.json),
and the outbox contract does not change.

## Faithfulness notes & what's next

- The Postgres schema in `db/migration` is ported directly from §12 of the record
  (native enums, the `event` CHECK constraints, `translation` as a table not JSONB,
  the outbox). Titles/summaries live in `translation`, never on `event` (§11.2).
- Deferred exactly as the record defers them (§5.9 — every deferral has a named
  removal trigger): **OpenSearch/Debezium** (search phase), **Neo4j** (Phase 5
  graph canvas), **Keycloak** security, the AI **assistant**. The outbox table is
  already here so the search phase can simply attach Debezium to it.
- Next increments: the `people` and `places` aggregates, the `review` dossier with
  multi-reviewer sign-off and the stale-approval check (§13.6), the read-model
  projections (§13), and the Angular 17 reader (needs Node ≥ 18; the Phase-1
  static reader in the repo root is the design reference for it).
```
