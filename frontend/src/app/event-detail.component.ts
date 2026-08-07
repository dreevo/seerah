import { Component, DestroyRef, ElementRef, computed, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { httpResource } from '@angular/common/http';
import { EventDetail } from './models';
import { EventMapComponent } from './event-map.component';
import { IconComponent } from './icon.component';

interface SectionRef { id: string; label: string; icon: string; }

@Component({
  selector: 'app-event-detail',
  imports: [RouterLink, EventMapComponent, IconComponent],
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
            <section id="why" data-sec="why" class="pane"><h2><app-icon name="explore" [size]="18" />Why It Happened</h2><p>{{ e.why }}</p></section>
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

          @if (e.people.length) {
            <section id="people" data-sec="people" class="pane">
              <h2><app-icon name="companions" [size]="18" />Companions Involved</h2>
              <div class="grid">
                @for (p of e.people; track p.id) {
                  <div class="mini">
                    <span class="mrole" [attr.title]="pretty(p.role)"><app-icon [name]="personIcon(p.role, p.relation)" [size]="16" /></span>
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

          @if (e.media.length) {
            <section id="illustrations" data-sec="illustrations" class="pane">
              <h2><app-icon name="source" [size]="18" />Illustrations</h2>
              <div class="media-grid">
                @for (m of e.media; track m.attribution) {
                  <div class="media-card">
                    <div class="mk-glyph">{{ glyph(m.kind) }}</div>
                    <div class="mk-body">
                      <div class="mk-kind">{{ pretty(m.kind) }}</div>
                      @if (m.caption) { <div class="mk-cap">{{ m.caption }}</div> }
                      <div class="mk-attr">{{ m.attribution }} · {{ m.licence }}</div>
                    </div>
                  </div>
                }
              </div>
              <p class="note">Visual language of geography, architecture, and the written word — never a depiction of any person.</p>
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
    if (e.people.length) s.push({ id: 'people', label: 'Companions', icon: 'companions' });
    if (e.places.length) s.push({ id: 'geography', label: 'Geography', icon: 'waypoint' });
    if (e.media.length) s.push({ id: 'illustrations', label: 'Illustrations', icon: 'source' });
    if (e.relatedEvents.length) s.push({ id: 'timeline', label: 'Timeline', icon: 'timeline' });
    if (e.sources.length) s.push({ id: 'sources', label: 'Sources', icon: 'source' });
    return s;
  });

  /** What the event is grounded in, read from its real citations — shown as vivid chips. */
  grounding = computed(() => {
    const e = this.event.value();
    if (!e) return [] as { key: string; text: string; icon: string; cls: string; title: string }[];
    let quran = false, sahih = false, hadith = false, classical = false;
    for (const s of e.sources) {
      if (/qur'?an|quran/i.test(s.workTitle)) quran = true;
      else if (s.grade === 'SAHIH') sahih = true;
      else if (s.grade) hadith = true;
      else classical = true;
    }
    const out: { key: string; text: string; icon: string; cls: string; title: string }[] = [];
    if (quran) out.push({ key: 'quran', text: 'Qur’ān', icon: 'verse', cls: 'g-quran', title: 'Grounded in the Qur’an — mass-transmitted revelation' });
    if (sahih) out.push({ key: 'sahih', text: 'Ṣaḥīḥ ḥadīth', icon: 'source', cls: 'g-sahih', title: 'Supported by a sound (ṣaḥīḥ) narration' });
    if (hadith) out.push({ key: 'hadith', text: 'Ḥadīth', icon: 'source', cls: 'g-hadith', title: 'Supported by a graded ḥadīth narration' });
    if (classical) out.push({ key: 'classical', text: 'Classical source', icon: 'source', cls: 'g-classical', title: 'Drawn from a classical scholarly work' });
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
  personIcon(role: string, relation: string): string { return IconComponent.personIcon(role, relation); }

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

  glyph(kind: string): string {
    switch (kind) {
      case 'MAP': return '🗺';
      case 'DIAGRAM': return '◈';
      case 'CALLIGRAPHY': return '﷽';
      case 'MANUSCRIPT_SCAN': return '📜';
      case 'AUDIO': return '♪';
      default: return '❖';
    }
  }
}
