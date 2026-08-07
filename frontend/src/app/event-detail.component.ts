import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { httpResource } from '@angular/common/http';
import { EventDetail } from './models';
import { EventMapComponent } from './event-map.component';
import { IconComponent } from './icon.component';

@Component({
  selector: 'app-event-detail',
  imports: [RouterLink, EventMapComponent, IconComponent],
  template: `
    @if (event.value(); as ev) {
      <a class="back" [routerLink]="ev.chronicleSlug ? ['/c', ev.chronicleSlug] : ['/']">← Back to {{ ev.chronicleTitle || 'the timeline' }}</a>
    } @else {
      <a class="back" routerLink="/">← Back</a>
    }

    @if (event.isLoading()) {
      <p class="state">Loading…</p>
    } @else if (event.error()) {
      <p class="state err">No published event with that name was found.</p>
    } @else if (event.value(); as e) {
      <header class="d-head">
        <span class="cert" [class]="'c-' + e.certainty"><app-icon [name]="certIcon(e.certainty)" [size]="13" />{{ label(e.certainty) }}</span>
        <h1>{{ e.title }}</h1>
        <div class="meta">
          @if (e.gregYear != null) {
            <span><b>Gregorian</b>{{ e.gregYear }} CE</span>
            <span><b>Hijri</b>{{ e.hijriYear && e.hijriYear > 0 ? e.hijriYear + ' AH' : 'Before Hijrah' }}</span>
          } @else if (e.chronicleTitle) {
            <span><b>Chronicle</b>{{ e.chronicleTitle }}</span>
          }
        </div>
        @if (e.approvals > 0) {
          <div class="reviewed" title="Published only after scholarly sign-off on this exact content (§13.6)">
            <app-icon name="reviewed" [size]="14" /> Reviewed &amp; approved · signed off by {{ e.approvals }} {{ e.approvals === 1 ? 'scholar' : 'scholars' }}
          </div>
        }
      </header>

      @if (e.summary) {
        <section class="pane"><h2><app-icon name="info" [size]="17" />What Happened</h2><p>{{ e.summary }}</p></section>
      }
      @if (e.why) {
        <section class="pane"><h2><app-icon name="explore" [size]="17" />Why It Happened</h2><p>{{ e.why }}</p></section>
      }

      @if (e.verses.length) {
        <section class="pane">
          <h2><app-icon name="verse" [size]="17" />Revelation Around This Event</h2>
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
        <section class="pane">
          <h2><app-icon name="companions" [size]="17" />Companions Involved</h2>
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
        <section class="pane">
          <h2><app-icon name="waypoint" [size]="17" />Geographic Context</h2>
          <app-event-map [places]="e.places" [routes]="e.routes" />
        </section>
      }

      @if (e.media.length) {
        <section class="pane">
          <h2><app-icon name="source" [size]="17" />Illustrations</h2>
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
        <section class="pane">
          <h2><app-icon name="timeline" [size]="17" />Timeline Context</h2>
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
        <section class="pane">
          <h2><app-icon name="source" [size]="17" />Sources &amp; Citations</h2>
          @for (s of e.sources; track s.workTitle + s.locator) {
            <div class="src">
              <span class="w"><app-icon [name]="tierIcon(s.tier)" [size]="15" />{{ s.workTitle }}<em>{{ s.locator }}</em></span>
              <span class="src-tags">
                @if (s.grade) { <span class="grade" [title]="'Hadith grading'">{{ gradeLabel(s.grade) }}</span> }
                <span class="tier" [class]="'t-' + s.tier">{{ pretty(s.tier) }}</span>
              </span>
            </div>
          }
          <p class="note">Every claim on this page traces to the works above.</p>
        </section>
      }
    }
  `,
})
export class EventDetailComponent {
  slug = input.required<string>();

  event = httpResource<EventDetail>(
    () => `/api/public/events/${this.slug()}?locale=en`,
  );

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

  label(c: string): string {
    switch (c) {
      case 'MUTAWATIR': return 'Mass-transmitted — no serious dispute';
      case 'WELL_ATTESTED': return 'Well-attested — multiple sound chains';
      case 'SCHOLARS_DIFFER': return 'Scholars differ on details';
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
