# Seerah — Reader (Angular 21)

The public-facing reader for the Seerah platform: an interactive, connected
chronology of the life of the Prophet Muhammad ﷺ. It consumes the backend's
read-only BFF (`/api/public/**`) and renders it in the platform's night-sky &
gold, calligraphic visual language — geometry and the written word only, no
figural imagery.

- **Angular 21**, standalone components, `httpResource`-based data access.
- Routes: a **zoomable, pannable timeline** (`/`) — era bands, Era/Decade/Year zoom,
  period filters, and alternating event nodes you scroll across; an **event detail**
  (`/event/:slug`) — summary,
  the verses revealed around the event (verbatim Uthmani + a named translation),
  the companions who took part, a **stylised geography map with journey routes**
  (dashed when reconstructed), an **illustrations** panel (maps/diagrams/calligraphy,
  never a person), the events before and after, and the cited sources —
  **guided journeys** (`/explore` + a step-by-step player at `/path/:slug`),
  a **companions index** (`/companions`) and a
  **companion profile** (`/person/:slug`) showing the events that name them, a live
  **search** (`/search`), and a grounded **assistant** (`/ask`) that answers only
  from cited content and shows its sources — or says the corpus doesn't cover it.

## Prerequisites

- **Node ≥ 20** (this repo used Node 22 via nvm — Angular 21 needs ≥ 20.19).
  ```bash
  nvm use 22   # or: nvm install 22
  ```
- The **backend running with seed data** on `:8080` (see `../backend/README.md`):
  ```bash
  cd ../backend && ./gradlew bootRun --args='--seerah.seed=true'
  ```

## Run

```bash
npm install          # first time
npm start            # ng serve on http://localhost:4200, proxying /api → :8080
```

The dev proxy (`proxy.conf.json`, wired into `angular.json`) forwards `/api`
calls to the backend, so the same relative URLs work in `ng serve` and when the
built bundle is served same-origin.

## Build

```bash
npm run build        # → dist/seerah-web
```

## Where things are

```
src/app/
├── app.ts / app.html          shell: header, brand, footer, <router-outlet>
├── app.routes.ts              '' → timeline, 'event/:slug' → detail
├── seerah-api.ts              the one HTTP client for /api/public
├── models.ts                  response interfaces (mirror PublicViews.java)
├── timeline.component.ts      the zoomable/pannable timeline canvas (era bands, zoom, nodes)
├── explore.component.ts       guided journeys (learning paths) index
├── path-player.component.ts   step-by-step journey player
├── event-detail.component.ts  the connected event view (incl. the stylised map + routes)
├── companions.component.ts    the companions index
├── person-detail.component.ts a companion profile + the events that name them
├── search.component.ts        live search over events + companions
└── ask.component.ts           the grounded assistant (cited answers or the refusal)
src/styles.scss                the global navy/gold theme
```

> The static single-file prototype in the repo root (`../index.html`) remains the
> visual design reference this app realises against real, cited API data.
