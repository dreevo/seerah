# Seerah — Interactive Chronology of the Life of the Prophet Muhammad ﷺ

> *Not a collection of separate stories — a connected chronology.*

An interactive platform that teaches the Seerah as a **connected graph**: events
linked to the Qur'anic verses revealed around them, the companions who took part,
the geography of where they happened, and the lessons they carry — every fact
**cited**, tagged with a **certainty**, and **signed off by a scholar** before it
is published.

This repo is the full implementation of the *Technical Design Record*: a
**Java 21 · Spring Boot 3 · PostgreSQL/PostGIS** backend and an **Angular 21**
reader, loaded with the **entire Qur'an (6,236 verses)** and the **22 pivotal
events** of the Prophet's ﷺ life.

---

## What's in the repo

```
seerah/
├── backend/            Java 21 / Spring Boot 3 — the platform (10 modules, hexagonal)
├── frontend/           Angular 21 — the public reader
├── index.html + assets/  the original single-file prototype (design reference)
├── docker-compose.yml  bring the backend + database up with one command
└── *.docx              the Blueprint (product) and Technical Design Record (engineering)
```

The backend and frontend each have their own README with the detail; this file is
the map and the quick-start.

---

## Quick start

You need **Docker**. The frontend also needs **Node ≥ 20** (use `nvm use 22`).

### 1. Start the backend + database

```bash
docker compose up --build          # PostGIS + the API, seeded, on http://localhost:8080
```

First boot runs the Flyway migrations, bulk-loads the full Qur'an, and ingests the
chronology through the real editorial pipeline. When it's up:

```bash
curl -s localhost:8080/api/public/timeline | jq '.[].title'
curl -s localhost:8080/api/public/events/the-battle-of-badr \
  | jq '{title, people:[.people[].name], verse:.verses[0].reference, sources:[.sources[].workTitle]}'
curl -s 'localhost:8080/api/public/ask?q=what%20happened%20at%20Badr' | jq '{answered, sources:[.sources[].workTitle]}'
```

### 2. Start the reader

```bash
cd frontend
nvm use 22 && npm install
npm start                          # http://localhost:4200, proxies /api → :8080
```

Open **http://localhost:4200** — the zoomable timeline, guided journeys, connected
event detail (verses · companions · map + routes · illustrations · sources ·
"reviewed" badge), companions, search, and the grounded assistant.

> **Prefer no Docker?** Run Postgres yourself (or `docker compose up db`) and start
> the backend with `cd backend && ./gradlew bootRun --args='--seerah.seed=true'`.
> Turn on the OpenSearch search engine with `docker compose --profile search up`
> and `SEARCH_ENGINE=opensearch`.

---

## How it works as a whole

### The request path (reading)

```
Angular reader ──HTTP──▶  publicapi (BFF)  ──▶ composes each module's `api` port:
  GET /api/public/timeline                      content        → the chronology
  GET /api/public/events/{slug}                 content        → summary, dates, certainty
                                                relationships  → people · verses · places · neighbours
                                                scripture      → the verse text (from the 6,236-verse corpus)
                                                places         → the map points + routes
                                                media          → illustrations (never a person)
                                                provenance     → the sources
                                                review         → "reviewed by N scholars"
  GET /api/public/search?q= · /ask?q=           search · assistant
```

`publicapi` owns no data — it's a read-only Backend-for-Frontend that assembles the
connected view from the published `api` ports of the ten modules. The reader is a
thin Angular app over those endpoints.

### The editorial path (writing) — where the guarantees live

Content does not appear because someone typed it; it passes three gates, enforced
in the aggregate **and** in the database:

```
draft ─submit▶ in-review ─approve▶ approved ──publish──▶ PUBLISHED
   every claim cited (§13.2) ┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈▶ can't publish uncited
   scholars-differ ⇒ ≥2 positions (§13.4) ┈┈┈┈▶ can't publish without them
   a scholar signs off on the exact content ┈┈▶ can't publish stale (§13.6, DB trigger
                                                 re-hashes the content at publish time)
```

Writing is **role-gated** (Spring Security): editors create and edit; only a
**scholar** may sign off; the public read path is open to everyone.

### The data

Two kinds, by design:

- **Reference data** — the **whole Qur'an** (114 surahs, 6,236 verses; Tanzil
  Uthmani + Saheeh International), bulk-loaded once from a checksummed file. Not
  editorial; no review workflow.
- **Editorial data** — `backend/src/main/resources/seed/chronology.json`: 22 events,
  29 companions, places, routes, illustrations, guided journeys — hand-authored,
  sourced, and loaded through the pipeline above. **Extending the chronology is
  editing that JSON — no code.** Every import leaves an `ingestion_run`, and every
  refused row is captured in `skip_audit` (silent data loss is worse than a failure).

### Deferred exactly as the record defers it

The outbox is written on every publish; the **search phase** (§17) projects it into
OpenSearch behind the same `SearchPort` (flip `search.engine=opensearch`), and the
CDC swap to **Debezium** is a connector config, not new code. Real S3 media
binaries, the fuller Arabic analyzers (§18), and — above all — **content authored
and reviewed by qualified scholars** are the operational/editorial road ahead.

---

## Verify it

```bash
cd backend && ./gradlew test        # 41 tests: domain invariants, ArchUnit boundaries,
                                    # + Testcontainers integration (Postgres, OpenSearch)
cd frontend && npm run build        # the reader compiles
```

---

*This is an educational and engineering artifact, not a fatwa or a substitute for
scholarly review. Where scholars differ, the platform shows the disagreement rather
than choosing a side; nothing reaches a reader without a citation and a scholar's
sign-off.*
