import { Component, DestroyRef, ElementRef, computed, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { httpResource } from '@angular/common/http';
import { EventDetail } from './models';
import { EventMapComponent } from './event-map.component';
import { IconComponent } from './icon.component';
import { HadithCardComponent } from './hadith-card.component';

interface SectionRef { id: string; label: string; icon: string; }

@Component({
  selector: 'app-event-detail',
  imports: [RouterLink, EventMapComponent, IconComponent, HadithCardComponent],
  template: `
    @if (event.isLoading()) {
      <a class="back" routerLink="/">← Back</a>
      <p class="state">Loading…</p>
    } @else if (event.error()) {
      <a class="back" routerLink="/">← Back</a>
      <p class="state err">No published event with that name was found.</p>
    } @else if (event.value(); as e) {
      <div class="detail-shell">
        <!-- left section navigation — sticky, scrollspy-highlighted -->
        <aside class="secnav-wrap">
          <nav class="secnav" aria-label="Sections of this page">
            <div class="secnav-h">On this page</div>
            <ul>
              @for (s of sections(); track s.id) {
                <li>
                  <a [class.on]="active() === s.id" tabindex="0"
                     (click)="go(s.id)" (keydown.enter)="go(s.id)">
                    <span class="sn-dot"></span>
                    <app-icon [name]="s.icon" [size]="15" />
                    <span class="sn-label">{{ s.label }}</span>
                  </a>
                </li>
              }
            </ul>
          </nav>
        </aside>

        <div class="detail-main">
          <a class="back" [routerLink]="e.chronicleSlug ? ['/c', e.chronicleSlug] : ['/']">← Back to {{ e.chronicleTitle || 'the timeline' }}</a>

          <header class="d-head">
            <h1>{{ e.title }}</h1>
            <div class="meta">
              @if (e.gregYear != null) {
                <span><b>Gregorian</b>{{ e.gregYear }} CE</span>
                <span><b>Hijri</b>{{ e.hijriYear && e.hijriYear > 0 ? e.hijriYear + ' AH' : 'Before Hijrah' }}</span>
              } @else if (e.chronicleTitle) {
                <span><b>Chronicle</b>{{ e.chronicleTitle }}</span>
              }
            </div>
            <!-- authenticity band: what grounds this event, made plain and vivid -->
            <div class="authbar">
              <span class="auth-chip cert c-{{ e.certainty }}" [title]="label(e.certainty)">
                <app-icon [name]="certIcon(e.certainty)" [size]="14" />{{ shortLabel(e.certainty) }}
              </span>
              @for (g of grounding(); track g.key) {
                <span class="auth-chip {{ g.cls }}" [title]="g.title">
                  <app-icon [name]="g.icon" [size]="14" />{{ g.text }}
                </span>
              }
            </div>
          </header>

          @if (e.summary) {
            <section id="what" data-sec="what" class="pane"><h2><app-icon name="info" [size]="18" />What Happened</h2><p>{{ e.summary }}</p></section>
          }
          @if (e.why) {
            <section id="why" data-sec="why" class="pane insight">
              <div class="insight-tag">The Lesson</div>
              <h2><app-icon name="explore" [size]="18" />Why It Happened</h2>
              <p>{{ e.why }}</p>
            </section>
          }

          @if (e.verses.length) {
            <section id="revelation" data-sec="revelation" class="pane">
              <h2><app-icon name="verse" [size]="18" />Revelation Around This Event</h2>
              @for (v of e.verses; track v.reference) {
                <div class="verse">
                  <div class="v-ar" dir="rtl">{{ v.textUthmani }}</div>
                  @if (v.translation) { <div class="v-tr">“{{ v.translation }}”</div> }
                  <div class="v-cite">Surah {{ v.surahNameEn }} · {{ v.reference }}@if (v.translator) { — {{ v.translator }}}</div>
                </div>
              }
            </section>
          }

          @if (hadithSources().length) {
            <section id="hadith" data-sec="hadith" class="pane">
              <h2><app-icon name="source" [size]="18" />The Prophetic Narration</h2>
              @for (s of hadithSources(); track s.locator) {
                <app-hadith-card [source]="s" />
              }
              <p class="note">The words of each ḥadīth exactly as recorded in the collection. Long reports turn the pages ‹ › ; where the chain of narration could be verified up to a Companion, it is drawn beside the text.</p>
            </section>
          }

          @if (e.people.length) {
            <section id="people" data-sec="people" class="pane">
              <h2><app-icon name="companions" [size]="18" />Companions Involved</h2>
              <div class="grid">
                @for (p of e.people; track p.id) {
                  <div class="mini">
                    @if (p.nameArabic) { <div class="ar" dir="rtl">{{ p.nameArabic }}</div> }
                    <div class="n">{{ p.name }}</div>
                    <div class="r">{{ pretty(p.relation) }} · {{ pretty(p.role) }}</div>
                  </div>
                }
              </div>
            </section>
          }

          @if (e.places.length) {
            <section id="geography" data-sec="geography" class="pane">
              <h2><app-icon name="waypoint" [size]="18" />Geographic Context</h2>
              <app-event-map [places]="e.places" [routes]="e.routes" />
            </section>
          }


          @if (e.relatedEvents.length) {
            <section id="timeline" data-sec="timeline" class="pane">
              <h2><app-icon name="timeline" [size]="18" />Timeline Context</h2>
              <div class="grid">
                @for (r of e.relatedEvents; track r.id) {
                  <a class="mini link" [routerLink]="['/event', r.slug]">
                    <div class="k">{{ pretty(r.relation) }}</div>
                    <div class="n">{{ r.title }}</div>
                  </a>
                }
              </div>
            </section>
          }

          @if (e.sources.length) {
            <section id="sources" data-sec="sources" class="pane">
              <h2><app-icon name="source" [size]="18" />Sources &amp; Citations</h2>
              @for (s of e.sources; track s.workTitle + s.locator) {
                <div class="src">
                  <span class="w"><app-icon [name]="tierIcon(s.tier)" [size]="15" />{{ s.workTitle }}<em>{{ s.locator }}</em></span>
                  <span class="src-tags">
                    @if (s.grade) { <span class="grade g-{{ s.grade }}" title="Hadith authentication grade">{{ gradeLabel(s.grade) }}</span> }
                    <span class="tier t-{{ s.tier }}">{{ pretty(s.tier) }}</span>
                  </span>
                </div>
              }
              <p class="note">Every claim on this page traces to the works above.</p>
            </section>
          }
        </div>
      </div>
    }
  `,
})
export class EventDetailComponent {
  slug = input.required<string>();
  private host = inject<ElementRef<HTMLElement>>(ElementRef);

  event = httpResource<EventDetail>(
    () => `/api/public/events/${this.slug()}?locale=en`,
  );

  active = signal<string>('');
  private observer?: IntersectionObserver;

  /** The sections actually present for this event — drives the nav and scrollspy. */
  sections = computed<SectionRef[]>(() => {
    const e = this.event.value();
    if (!e) return [];
    const s: SectionRef[] = [];
    if (e.summary) s.push({ id: 'what', label: 'What Happened', icon: 'info' });
    if (e.why) s.push({ id: 'why', label: 'Why It Happened', icon: 'explore' });
    if (e.verses.length) s.push({ id: 'revelation', label: 'Revelation', icon: 'verse' });
    if (e.sources.some((x) => x.quoteAr)) s.push({ id: 'hadith', label: 'Ḥadīth', icon: 'source' });
    if (e.people.length) s.push({ id: 'people', label: 'Companions', icon: 'companions' });
    if (e.places.length) s.push({ id: 'geography', label: 'Geography', icon: 'waypoint' });
    if (e.relatedEvents.length) s.push({ id: 'timeline', label: 'Timeline', icon: 'timeline' });
    if (e.sources.length) s.push({ id: 'sources', label: 'Sources', icon: 'source' });
    return s;
  });

  /** The cited ḥadīth that carry their full literal text (Arabic isnād + matn + English). */
  hadithSources = computed(() => (this.event.value()?.sources ?? []).filter((s) => !!s.quoteAr));

  /** The narrator named at the head of the report, if the English begins with one. */
  narrator(en: string | null): string | null {
    if (!en) return null;
    let m = en.match(/^Narrated ([^:]{2,60}?):/);
    if (m) return m[1].replace(/[`]/g, '').trim();
    m = en.match(/^([A-Z][\w' .`-]{2,40}?) reported/);
    if (m) return m[1].replace(/[`]/g, '').trim();
    return null;
  }

  /** The report itself — the isnād/attribution prefix stripped so the words stand out. */
  matn(en: string | null): string {
    if (!en) return '';
    const i = en.indexOf(':');
    if (i > 0 && i < 100 && /narrat|report|said|saying/i.test(en.slice(0, i))) {
      return en.slice(i + 1).trim();
    }
    return en.trim();
  }

  /** What the event is grounded in, read from its real citations — shown as vivid chips. */
  grounding = computed(() => {
    const e = this.event.value();
    if (!e) return [] as { key: string; text: string; icon: string; cls: string; title: string }[];
    const isQuran = (t: string) => /qur'?an|quran/i.test(t);
    const isHadith = (t: string) => /bukhari|muslim|tirmidhi|nasa|abu[ -]?dawud|ibn[ -]?majah|muwatta|musnad|sunan/i.test(t);
    let quran = false, sahih = false, hadith = false, classical = false;
    for (const s of e.sources) {
      if (isQuran(s.workTitle)) quran = true;
      else if (isHadith(s.workTitle) && s.grade === 'SAHIH') sahih = true;
      else if (isHadith(s.workTitle)) hadith = true;
      else classical = true;
    }
    const out: { key: string; text: string; icon: string; cls: string; title: string }[] = [];
    if (quran) out.push({ key: 'quran', text: 'Qur’ān', icon: 'verse', cls: 'g-quran', title: 'Grounded in the Qur’an — mass-transmitted revelation' });
    if (sahih) out.push({ key: 'sahih', text: 'Ṣaḥīḥ ḥadīth', icon: 'source', cls: 'g-sahih', title: 'Supported by a sound (ṣaḥīḥ) narration from al-Bukhārī or Muslim' });
    if (hadith) out.push({ key: 'hadith', text: 'Ḥadīth', icon: 'source', cls: 'g-hadith', title: 'Supported by a narration from a Sunan collection' });
    if (classical) out.push({ key: 'classical', text: 'Classical source', icon: 'source', cls: 'g-classical', title: 'Drawn from a classical scholarly work (sīra)' });
    return out;
  });

  constructor() {
    // Re-bind the scrollspy whenever the set of sections changes (after the DOM paints).
    effect(() => {
      this.sections();
      setTimeout(() => this.bindSpy(), 40);
    });
    inject(DestroyRef).onDestroy(() => this.observer?.disconnect());
  }

  private bindSpy(): void {
    this.observer?.disconnect();
    const els = this.host.nativeElement.querySelectorAll<HTMLElement>('[data-sec]');
    if (!els.length) return;
    this.observer = new IntersectionObserver((entries) => {
      const seen = entries.filter((en) => en.isIntersecting)
        .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);
      if (seen.length) this.active.set(seen[0].target.getAttribute('data-sec') ?? '');
    }, { rootMargin: '-16% 0px -72% 0px', threshold: 0 });
    els.forEach((el) => this.observer!.observe(el));
    if (!this.active()) this.active.set(els[0].getAttribute('data-sec') ?? '');
  }

  go(id: string): void {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    this.active.set(id);
  }

  gradeLabel(g: string): string {
    switch (g) {
      case 'SAHIH': return 'Ṣaḥīḥ';
      case 'HASAN': return 'Ḥasan';
      case 'DAIF': return 'Ḍaʿīf';
      case 'MAWDU': return 'Mawḍūʿ';
      default: return g.charAt(0) + g.slice(1).toLowerCase();
    }
  }
  certIcon(c: string): string { return IconComponent.certaintyIcon(c); }
  tierIcon(t: string): string { return IconComponent.tierIcon(t); }

  /** Full descriptive label (used as the certainty chip's tooltip). */
  label(c: string): string {
    switch (c) {
      case 'MUTAWATIR': return 'Mass-transmitted — no serious dispute';
      case 'WELL_ATTESTED': return 'Well-attested — multiple sound chains';
      case 'SCHOLARS_DIFFER': return 'Scholars differ on the details';
      default: return this.pretty(c);
    }
  }

  /** Short chip label. */
  shortLabel(c: string): string {
    switch (c) {
      case 'MUTAWATIR': return 'Mutawātir';
      case 'WELL_ATTESTED': return 'Well-attested';
      case 'SCHOLARS_DIFFER': return 'Scholars differ';
      default: return this.pretty(c);
    }
  }

  pretty(v: string): string {
    return v.split('_').map((w) => w.charAt(0) + w.slice(1).toLowerCase()).join(' ');
  }
}
