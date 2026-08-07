/* ============================================================================
   SEERAH — Interactive Chronology of the Life of the Prophet Muhammad ﷺ
   APPLICATION LOGIC
   ----------------------------------------------------------------------------
   A single-page, backend-free application. The "read path depends on as little
   as possible" — all content is the reviewed data layer in data.js.
   ========================================================================== */
(function () {
  'use strict';

  const { ERAS, CAT, CAT_COLOR, EVENTS, PATHS, GLOSSARY, SEARCH_SUGGESTIONS } = window.SEERAH;
  const $  = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));
  const esc = (s) => String(s).replace(/[&<>"']/g, (c) =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

  /* Index events by id and remember chronological order. */
  const BY_ID = {};
  EVENTS.forEach((e, i) => { e._i = i; BY_ID[e.id] = e; });

  /* ---------- state ---------- */
  let Z = 0;                       // zoom level
  let ERA = 'all';                 // era filter
  let CAL = 'ce';                  // calendar: 'ce' Gregorian | 'ah' Hijri
  let ACTIVE_PATH = null;          // { path, step }
  const ZW = [1500, 2600, 4400];
  const ZL = ['Era View', 'Decade View', 'Year View'];
  const Y0 = 565, Y1 = 634;

  /* ---------- small helpers ---------- */
  function dateLabel(e) {
    if (CAL === 'ah') return e.hij === '—' ? 'Before Hijrah' : e.hij;
    return e.y + ' CE';
  }
  function fullDate(e) {
    const ah = e.hij === '—' ? 'Before Hijrah' : e.hij;
    return CAL === 'ah' ? `${ah} · ${e.y} CE` : `${e.y} CE${e.hij !== '—' ? ' · ' + e.hij : ''}`;
  }
  const STAR = '<svg class="mark" viewBox="0 0 100 100" aria-hidden="true"><path d="M50 0 L61 28 L89 17 L78 45 L100 50 L78 55 L89 83 L61 72 L50 100 L39 72 L11 83 L22 55 L0 50 L22 45 L11 17 L39 28 Z" fill="#C8A44B"/></svg>';

  function toast(msg) {
    const t = $('#toast');
    t.textContent = msg; t.classList.add('on');
    clearTimeout(toast._t); toast._t = setTimeout(() => t.classList.remove('on'), 2200);
  }

  /* =========================================================================
     ROUTING
     ========================================================================= */
  const VIEWS = ['timeline', 'explore', 'companions', 'library'];
  function go(view, opts = {}) {
    $$('.view').forEach((x) => x.classList.remove('on'));
    const el = $('#view-' + view);
    if (el) el.classList.add('on');
    VIEWS.forEach((n) => {
      const b = $('#nav-' + n);
      if (b) b.classList.toggle('on', n === view);
    });
    $('#nav').classList.remove('open');
    if (!opts.keepScroll) window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  /* =========================================================================
     TIMELINE
     ========================================================================= */
  function visibleEvents() {
    return EVENTS.filter((e) => {
      if (ERA === 'all') return true;
      const er = ERAS[ERA];
      return e.y >= er.a && e.y < er.b;
    });
  }

  function buildTimeline() {
    const tl = $('#tl');
    const W = ZW[Z];
    tl.style.width = W + 'px';
    tl.innerHTML = '';
    const px = (y) => ((y - Y0) / (Y1 - Y0)) * W;

    // era bands
    ERAS.forEach((e, i) => {
      if (ERA !== 'all' && ERA !== i) return;
      const d = document.createElement('div');
      d.className = 'era-band ' + e.c;
      d.style.left = px(e.a) + 'px';
      d.style.width = (px(e.b) - px(e.a)) + 'px';
      d.textContent = e.n;
      tl.appendChild(d);
    });

    // axis
    const ax = document.createElement('div');
    ax.className = 'axis'; ax.style.width = W + 'px';
    tl.appendChild(ax);

    // ticks
    const step = Z === 0 ? 10 : Z === 1 ? 5 : 2;
    for (let y = 570; y <= 632; y += step) {
      const t = document.createElement('div');
      t.className = 'tick'; t.style.left = px(y) + 'px';
      const yr = CAL === 'ah' ? approxHijri(y) : y + ' CE';
      t.innerHTML = '<span>' + yr + '</span>';
      tl.appendChild(t);
    }

    // nodes
    const vis = visibleEvents();
    if (!vis.length) {
      const m = document.createElement('div');
      m.className = 'tl-empty'; m.textContent = 'No events fall within this period.';
      m.style.cssText += 'position:absolute;top:150px;width:100%';
      tl.appendChild(m);
      return;
    }
    let up = true;
    vis.forEach((e, i) => {
      const n = document.createElement('button');
      n.className = 'node ' + (up ? 'up' : 'dn');
      n.setAttribute('aria-label', e.t + ', ' + fullDate(e));
      const x = px(e.y);
      n.style.left = (x - 78) + 'px';
      const h = up ? (i % 4 === 0 ? 128 : 88) : (i % 4 === 1 ? 128 : 88);
      if (up) n.style.top = (236 - h - 58) + 'px';
      else n.style.top = (238 + h) + 'px';

      const keytag = e.key ? '<span class="keytag">Pivotal</span>' : '';
      const card =
        '<div class="card">' + keytag +
        '<div class="yr">' + esc(fullDate(e)) + '</div>' +
        '<div class="ttl">' + esc(e.t) + '</div>' +
        '<span class="cat ' + e.cat + '">' + CAT[e.cat] + '</span></div>';
      n.innerHTML = (up ? '' : STAR) + card + (up ? STAR : '');

      const st = document.createElement('div');
      st.className = 'stem'; st.style.height = h + 'px';
      st.style[up ? 'top' : 'bottom'] = '100%';
      n.appendChild(st);
      n.addEventListener('click', () => openEvent(e.id));
      tl.appendChild(n);
      up = !up;
    });
  }

  /* Rough CE→AH label for axis ticks (approximate; Hijra = 622 CE). */
  function approxHijri(ceYear) {
    if (ceYear < 622) return ceYear + ' CE';
    const ah = Math.round((ceYear - 622) * (33 / 32)) + 1;
    return ah + ' AH';
  }

  function zoom(d) {
    Z = Math.max(0, Math.min(2, Z + d));
    $('#zlabel').textContent = ZL[Z];
    $('#zoom-out').disabled = Z === 0;
    $('#zoom-in').disabled = Z === 2;
    buildTimeline();
  }
  function setEra(e, btn) {
    ERA = e;
    $$('#era-seg button').forEach((b) => b.classList.remove('on'));
    btn.classList.add('on');
    $('#era-intro').textContent = e === 'all'
      ? 'Three periods, one continuous story. Zoom in, or click any event to open it.'
      : ERAS[e].blurb;
    buildTimeline();
  }
  function setCal(c, btn) {
    CAL = c;
    $$('#cal-sw button').forEach((b) => b.classList.remove('on'));
    btn.classList.add('on');
    buildTimeline();
    // if a detail is open, re-render it
    if ($('#view-detail').classList.contains('on') && openEvent._cur) openEvent(openEvent._cur, true);
  }

  /* =========================================================================
     EVENT DETAIL
     ========================================================================= */
  function openEvent(id, keepView) {
    const e = BY_ID[id];
    if (!e) return;
    openEvent._cur = id;

    // chronological neighbours
    const prev = e._i > 0 ? EVENTS[e._i - 1] : null;
    const next = e._i < EVENTS.length - 1 ? EVENTS[e._i + 1] : null;

    const verses = e.verses.length ? e.verses.map((v) => `
      <div class="verse">
        <div class="v-ar">${v.ar}</div>
        <div class="v-tr">“${esc(v.tr)}”</div>
        <div class="v-cite">Surah ${esc(v.s)} · ${esc(v.n)}</div>
        <div class="v-note">${esc(v.note)}</div>
      </div>`).join('')
      : `<p class="empty">No revelation is directly linked to this event in the sources reviewed. Related verses from the surrounding period are collected in the Library.</p>`;

    const ppl = e.people.map((p) => {
      const ci = COMPANIONS.findIndex((c) => c.n === p.n);
      return `<button class="mini" onclick="SeerahApp.openCompanionIndex(${ci})">
        <div class="ar">${p.ar}</div><div class="n">${esc(p.n)}</div><div class="r">${esc(p.r)}</div>
      </button>`;
    }).join('');

    const les = e.lessons.map((l, i) =>
      `<div class="lesson"><div class="num">${String(i + 1).padStart(2, '0')}</div>
        <div><div class="lt">${esc(l.t)}</div><p>${esc(l.x)}</p></div></div>`).join('');

    const srcs = e.srcs.map((s) =>
      `<div class="src"><div class="w">${esc(s[0])}<em>${esc(s[1])}</em></div>
        <span class="t ${s[2]}">Tier ${s[2].toUpperCase()}</span></div>`).join('');

    const pts = e.places.map((p) =>
      `<g><circle cx="${p.x}" cy="${p.y}" r="1.5" fill="#C8A44B"/>
        <circle cx="${p.x}" cy="${p.y}" r="3.4" fill="none" stroke="#C8A44B" stroke-width=".4" opacity=".55"/>
        <text x="${p.x + 5}" y="${p.y + 1.4}" fill="#E3CE93" font-size="3.1" font-family="Inter">${esc(p.n)}</text></g>`).join('');
    const rts = e.routes.map((r) =>
      `<polyline points="${r.map((p) => p.join(',')).join(' ')}" fill="none" stroke="#C8A44B" stroke-width=".7" stroke-dasharray="2 1.6" opacity=".8"/>`).join('');

    const relCard = (r, label) => r
      ? `<button class="mini" onclick="SeerahApp.openEvent('${r.id}')">
          <div class="k">${label}</div><div class="n">${esc(r.t)}</div>
          <div class="r">${esc(fullDate(r))} — ${CAT[r.cat]}</div></button>` : '';
    const rel = [relCard(prev, 'What came before'), relCard(next, 'What came after')].filter(Boolean).join('')
      || '<p class="empty">This event sits at the edge of the current chronology.</p>';

    // path player bar (only when inside a guided journey)
    const player = ACTIVE_PATH ? renderPlayerBar(e) : '';

    const certLabel = e.cert === 'confirmed'
      ? '◆ Well-attested — multiple primary sources agree'
      : '◆ Scholars differ on details of dating or number';

    $('#detail').innerHTML = `
    ${player}
    <button class="back" onclick="SeerahApp.go('${ACTIVE_PATH ? 'explore' : 'timeline'}')">← ${ACTIVE_PATH ? 'Leave this journey' : 'Back to Timeline'}</button>
    <div class="d-head">
      <span class="cat ${e.cat}">${CAT[e.cat]}</span>
      <h2>${esc(e.t)}</h2>
      <div class="d-ar">${e.ar}</div>
      <div class="d-meta">
        <div><b>Gregorian</b>${e.y} CE</div>
        <div><b>Hijri</b>${e.hij === '—' ? 'Before Hijrah' : e.hij}</div>
        <div><b>Location</b>${esc(e.loc)}</div>
      </div>
      <div class="d-head-foot">
        <span class="certainty ${e.cert}">${certLabel}</span>
        <button class="action-btn" onclick="SeerahApp.share('${e.id}')">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 12v7a1 1 0 001 1h14a1 1 0 001-1v-7M16 6l-4-4-4 4M12 2v13"/></svg>Share</button>
        <button class="action-btn" onclick="window.print()">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 9V2h12v7M6 18H4a2 2 0 01-2-2v-4a2 2 0 012-2h16a2 2 0 012 2v4a2 2 0 01-2 2h-2M6 14h12v8H6z"/></svg>Print</button>
      </div>
    </div>
    <div class="d-body">
      <div class="tabs" role="tablist">
        <button class="on" onclick="SeerahApp.tab(0,this)"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M4 5h16M4 12h16M4 19h10"/></svg>Summary</button>
        <button onclick="SeerahApp.tab(1,this)"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>Before &amp; After</button>
        <button onclick="SeerahApp.tab(2,this)"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M4 5v14l8-3 8 3V5l-8 3z"/></svg>Quran Context</button>
        <button onclick="SeerahApp.tab(3,this)"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><circle cx="9" cy="8" r="3.2"/><path d="M3 20c0-3.4 2.7-5.5 6-5.5s6 2.1 6 5.5M17 11.5a3 3 0 100-6"/></svg>Companions</button>
        <button onclick="SeerahApp.tab(4,this)"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M9 4L3 6.5v14L9 18l6 2.5 6-2.5v-14L15 6.5z"/><path d="M9 4v14M15 6.5v14"/></svg>Geography</button>
        <button onclick="SeerahApp.tab(5,this)"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M12 3l2.5 5.5L20 9.5l-4 4 1 6-5-2.8L7 19.5l1-6-4-4 5.5-1z"/></svg>Lessons</button>
        <button onclick="SeerahApp.tab(6,this)"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M5 4h11l3 3v13H5z"/><path d="M9 10h7M9 14h7"/></svg>Sources</button>
      </div>
      <div>
        <div class="panel on">
          <div class="pane"><h3>What Happened</h3><p>${esc(e.sum)}</p></div>
          <div class="pane"><h3>Why It Happened</h3><p>${esc(e.why)}</p></div>
        </div>
        <div class="panel"><div class="pane"><h3>Timeline Context</h3>
          <p>Events in the Seerah are rarely isolated. This is what immediately surrounds this moment in the chronology.</p>
          <div class="chrono-nav">${rel}</div></div></div>
        <div class="panel"><div class="pane"><h3>Revelation Around This Event</h3>${verses}</div></div>
        <div class="panel"><div class="pane"><h3>Companions Involved</h3>
          <p>Those whose role in this event is documented in the sources. Open any name for their full profile.</p>
          <div class="grid2">${ppl || '<p class="empty">No companion is individually named for this event.</p>'}</div></div></div>
        <div class="panel"><div class="pane"><h3>Geographic Context</h3>
          <p>Locations and routes associated with this event. Terrain and distance frequently explain decisions that look puzzling in a text-only account.</p>
          <div class="map-box">
            <svg viewBox="0 0 100 100" style="width:100%;height:auto;display:block" role="img" aria-label="Stylised map of ${esc(e.t)}">
              <defs><pattern id="g" width="8" height="8" patternUnits="userSpaceOnUse"><path d="M8 0H0v8" fill="none" stroke="#C8A44B" stroke-width=".18" opacity=".22"/></pattern></defs>
              <rect width="100" height="100" fill="url(#g)"/>
              <path d="M18 14 Q30 30 24 48 Q20 66 30 84 L70 88 Q78 66 74 46 Q70 26 78 12 Z" fill="#1B3A52" opacity=".5" stroke="#C8A44B" stroke-width=".3" stroke-opacity=".3"/>
              ${rts}${pts}
            </svg>
            <div class="map-legend">
              <div><i style="background:#C8A44B"></i>Route travelled</div>
              <div><i style="background:#C8A44B;width:8px;height:8px;border-radius:50%"></i>Location</div>
              <div style="color:#5F6D7B">Stylised — not to scale · no figural imagery</div>
            </div>
          </div></div></div>
        <div class="panel"><div class="pane"><h3>Lessons Drawn</h3>${les}</div></div>
        <div class="panel"><div class="pane"><h3>Sources &amp; Citations</h3>
          <p>Every claim on this page traces to the works below. Tier A denotes classical primary sources and authenticated hadith; Tier B denotes respected modern scholarly syntheses.</p>
          ${srcs}</div></div>
      </div>
    </div>`;

    if (!keepView) { go('detail'); window.scrollTo({ top: 0, behavior: 'smooth' }); }
    location.hash = 'event/' + e.id;
  }

  function tab(i, btn) {
    $$('.tabs button').forEach((b) => b.classList.remove('on'));
    btn.classList.add('on');
    $$('.panel').forEach((p, j) => p.classList.toggle('on', j === i));
  }

  function share(id) {
    const url = location.origin + location.pathname + '#event/' + id;
    const done = () => toast('Link copied to clipboard');
    if (navigator.clipboard) navigator.clipboard.writeText(url).then(done, () => prompt('Copy this link:', url));
    else prompt('Copy this link:', url);
  }

  /* =========================================================================
     GUIDED PATH PLAYER
     ========================================================================= */
  function renderPlayerBar(e) {
    const p = ACTIVE_PATH.path;
    const idx = p.ids.indexOf(e.id);
    ACTIVE_PATH.step = idx;
    const pct = ((idx + 1) / p.ids.length) * 100;
    const dots = p.ids.map((id, i) => {
      const ev = BY_ID[id];
      const cls = i === idx ? 'on' : (i < idx ? 'done' : '');
      return `<button class="${cls}" onclick="SeerahApp.pathGoto(${i})"><i></i><span>${esc(ev.t)}</span></button>`;
    }).join('');
    const prevBtn = idx > 0 ? `<button class="action-btn" onclick="SeerahApp.pathGoto(${idx - 1})">← Previous</button>` : '';
    const nextBtn = idx < p.ids.length - 1
      ? `<button class="action-btn" onclick="SeerahApp.pathGoto(${idx + 1})">Next step →</button>`
      : `<button class="action-btn" onclick="SeerahApp.go('explore')">Finish journey ✓</button>`;
    return `
      <div class="player-bar">
        <div class="pinfo">Guided journey · Step ${idx + 1} of ${p.ids.length}<b>${esc(p.t)}</b></div>
        <div class="pnav">${prevBtn}${nextBtn}</div>
      </div>
      <div class="progress-track"><i style="width:${pct}%"></i></div>
      <div class="step-dots">${dots}</div>`;
  }
  function startPath(id) {
    const p = PATHS.find((x) => x.id === id);
    if (!p) return;
    ACTIVE_PATH = { path: p, step: 0 };
    openEvent(p.ids[0]);
  }
  function pathGoto(i) {
    if (!ACTIVE_PATH) return;
    const id = ACTIVE_PATH.path.ids[i];
    if (id) openEvent(id);
  }

  /* =========================================================================
     EXPLORE
     ========================================================================= */
  function renderExplore() {
    $('#paths').innerHTML = PATHS.map((p) => {
      const dots = p.ids.slice(0, 8).map(() => '<i></i>').join('');
      return `<button class="path-card" onclick="SeerahApp.startPath('${p.id}')">
        <span class="badge">${esc(p.badge)}</span>
        <div class="pt">${esc(p.t)}</div>
        <div class="pd">${esc(p.d)}</div>
        <div class="pmeta">${p.ids.length} events<span class="dots">${dots}</span></div>
      </button>`;
    }).join('');
  }

  /* =========================================================================
     LIBRARY
     ========================================================================= */
  let LIB_FILTER = 'all';
  function renderLibrary() {
    const rows = EVENTS
      .filter((e) => LIB_FILTER === 'all' || e.cat === LIB_FILTER)
      .map((e) =>
        `<button class="lib-row" onclick="SeerahApp.openEvent('${e.id}')">
          <span class="when">${esc(fullDate(e))}</span>
          <span class="what">${esc(e.t)}<small>${esc(e.loc)}</small></span>
          <span class="cat ${e.cat}" style="font-size:8.5px">${CAT[e.cat]}</span>
        </button>`).join('');
    $('#libList').innerHTML = rows || '<p class="empty" style="padding:20px">No events in this category.</p>';
  }
  function setLibFilter(cat, btn) {
    LIB_FILTER = cat;
    $$('#lib-seg button').forEach((b) => b.classList.remove('on'));
    btn.classList.add('on');
    renderLibrary();
  }

  /* Glossary (rendered once into the library page). */
  function renderGlossary() {
    $('#glossList').innerHTML = GLOSSARY.map((g) =>
      `<div class="gloss-item"><div class="gt"><b>${esc(g.term)}</b><span class="ga">${g.ar}</span></div>
        <p>${esc(g.def)}</p></div>`).join('');
  }

  /* =========================================================================
     COMPANIONS  (derived from the people named across events)
     ========================================================================= */
  let COMPANIONS = [];
  function buildCompanions() {
    const map = new Map();
    EVENTS.forEach((e) => e.people.forEach((p) => {
      if (!map.has(p.n)) map.set(p.n, { n: p.n, ar: p.ar, roles: [], events: [] });
      const c = map.get(p.n);
      c.roles.push(p.r);
      c.events.push(e.id);
    }));
    COMPANIONS = Array.from(map.values())
      .sort((a, b) => b.events.length - a.events.length || a.n.localeCompare(b.n));
  }
  function renderCompanions() {
    $('#compGrid').innerHTML = COMPANIONS.map((c, i) =>
      `<button class="comp-card" onclick="SeerahApp.openCompanionIndex(${i})">
        <div class="car">${c.ar}</div>
        <div class="cn">${esc(c.n)}</div>
        <div class="cr">${esc(c.roles[0])}</div>
        <div class="cev">Appears in ${c.events.length} event${c.events.length > 1 ? 's' : ''}</div>
      </button>`).join('');
  }
  function openCompanionIndex(i) { showCompanion(COMPANIONS[i]); }
  function openCompanion(name) {
    const c = COMPANIONS.find((x) => x.n === name);
    if (c) showCompanion(c);
  }
  function showCompanion(c) {
    const evs = c.events.map((id, k) => {
      const e = BY_ID[id];
      return `<button class="mini" onclick="SeerahApp.openEvent('${e.id}')">
        <div class="k">${esc(fullDate(e))} · ${CAT[e.cat]}</div>
        <div class="n">${esc(e.t)}</div>
        <div class="r">${esc(c.roles[k])}</div></button>`;
    }).join('');
    $('#compDetail').innerHTML = `
      <button class="back" onclick="SeerahApp.go('companions')">← Back to Companions</button>
      <div class="d-head">
        <span class="cat life">Companion</span>
        <h2>${esc(c.n)}</h2>
        <div class="d-ar">${c.ar}</div>
        <div class="d-meta"><div><b>Appears in</b>${c.events.length} documented event${c.events.length > 1 ? 's' : ''} of the Seerah</div></div>
      </div>
      <div class="pane"><h3>Role Across the Chronology</h3>
        <p>This profile is assembled from the events in which ${esc(c.n)} is individually named in the sources. Fuller companion biographies are a later phase of the platform.</p>
        <div class="grid2">${evs}</div>
      </div>`;
    go('comp-detail');
  }

  /* =========================================================================
     SEARCH
     ========================================================================= */
  function openSearch() {
    $('#overlay').classList.add('on');
    setTimeout(() => $('#sin').focus(), 60);
  }
  function closeSearch() {
    $('#overlay').classList.remove('on');
    $('#sin').value = ''; runSearch('');
  }
  const ARROW = '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M5 12h14M13 6l6 6-6 6"/></svg>';
  function runSearch(q) {
    const L = $('#hintList'), lab = $('#hintLabel');
    if (!q.trim()) {
      lab.textContent = 'Try asking';
      L.innerHTML = SEARCH_SUGGESTIONS.map((s) =>
        `<button class="hint" onclick="SeerahApp.searchFill(this)">${ARROW}${esc(s)}</button>`).join('');
      return;
    }
    const t = q.toLowerCase();
    const r = EVENTS.filter((e) =>
      (e.t + e.sum + e.why + e.loc + e.ar + e.cat + e.people.map((p) => p.n).join(' ')).toLowerCase().includes(t));
    lab.textContent = r.length
      ? r.length + ' result' + (r.length > 1 ? 's' : '') + ' — grounded in cited sources'
      : 'No match in the reviewed corpus';
    L.innerHTML = r.length
      ? r.map((e) =>
          `<button class="hint" onclick="SeerahApp.pick('${e.id}')">${ARROW}
            <span><b style="font-weight:600">${esc(e.t)}</b>
            <span style="color:#7C8794;font-size:12px"> · ${esc(fullDate(e))} · ${CAT[e.cat]}</span></span></button>`).join('')
      : `<div class="hint" style="cursor:default;color:#7C8794">The assistant answers only from scholar-reviewed content. Nothing in the corpus matches this query.</div>`;
  }
  function searchFill(btn) { const v = btn.textContent.trim(); $('#sin').value = v; runSearch(v); }
  function pick(id) { closeSearch(); ACTIVE_PATH = null; openEvent(id); }

  /* =========================================================================
     HASH ROUTING (deep links + back button)
     ========================================================================= */
  function handleHash() {
    const h = location.hash.replace(/^#/, '');
    if (h.startsWith('event/')) {
      const id = h.slice(6);
      if (BY_ID[id]) { openEvent(id); return; }
    }
    if (VIEWS.includes(h)) go(h);
  }

  /* =========================================================================
     INIT
     ========================================================================= */
  function init() {
    // era intro default
    $('#era-intro').textContent = 'Three periods, one continuous story. Zoom in, or click any event to open it.';
    buildTimeline();
    renderExplore();
    renderLibrary();
    renderGlossary();
    buildCompanions();
    renderCompanions();
    runSearch('');

    // keyboard shortcuts
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') { closeSearch(); return; }
      const typing = /^(INPUT|TEXTAREA)$/.test(document.activeElement.tagName);
      if (!typing && (e.key === '/' )) { e.preventDefault(); openSearch(); }
    });

    // deep link on load
    if (location.hash) handleHash();
    window.addEventListener('hashchange', () => {
      // only auto-open events (avoid loops from our own go())
      if (location.hash.startsWith('#event/')) handleHash();
    });
  }

  /* public surface (referenced by inline handlers) */
  window.SeerahApp = {
    go, zoom, setEra, setCal, openEvent, tab, share,
    startPath, pathGoto, renderExplore,
    setLibFilter, openCompanion, openCompanionIndex,
    openSearch, closeSearch, runSearch, searchFill, pick
  };

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
  else init();
})();
