import { Component, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { httpResource } from '@angular/common/http';

interface Verse { reference: string; surahNameEn: string; surahNameAr: string; }
interface Src { workTitle: string; grade: string | null; }
interface Beat {
  slug: string; title: string; summary: string | null;
  verse: Verse | null; source: Src | null;
}
interface Story { chronicle: string; prophet: string; glyph: string; blurb: string | null; beats: Beat[]; }

// A gallery of the Qur'an's narratives that stand outside the line of the prophets —
// each a card opening the cited event. Reuses the Story endpoint (title + summary +
// key āyah + primary source per event). No figural imagery.
@Component({
  selector: 'app-stories',
  imports: [RouterLink],
  template: `
    <section class="qs">
      <div class="qs-head">
        <div class="eyebrow">Signs among the nations</div>
        <h1>Stories of the <em>Qur’ān</em></h1>
        <p class="qs-lede">{{ data.value()?.blurb ||
          'Narratives the Qur’ān relates that stand outside the line of the prophets — each a sign, told strictly from the Qur’ān and, where the Sunnah relates the fuller account, from sound ḥadīth.' }}</p>
      </div>

      @if (data.isLoading()) { <p class="state">Opening the stories…</p> }
      @else {
        <div class="qs-grid">
          @for (b of beats(); track b.slug) {
            <a class="qs-card" [routerLink]="['/event', b.slug]">
              <span class="qs-orn" aria-hidden="true">◈</span>
              <h3>{{ b.title }}</h3>
              @if (b.summary) { <p class="qs-sum">{{ b.summary }}</p> }
              <div class="qs-foot">
                @if (b.verse; as v) { <span class="qs-ref">۝ {{ v.surahNameEn }} · {{ v.reference }}</span> }
                @if (b.source; as s) {
                  <span class="qs-src">{{ mark(s) }} {{ clean(s.workTitle) }}@if (s.grade) { <em> · {{ pretty(s.grade) }}</em> }</span>
                }
              </div>
            </a>
          }
        </div>
        <p class="qs-note">Every card is a real, cited event · Content drawn from the Qur’ān and sound ḥadīth · Peace be upon them all</p>
      }
    </section>
  `,
})
export class StoriesComponent {
  data = httpResource<Story>(() => '/api/public/chronicles/quran-stories/story');
  beats = computed(() => this.data.value()?.beats ?? []);

  mark(s: Src): string { return /qur/i.test(s.workTitle) ? '۝' : '⚑'; }
  clean(w: string): string { return w.replace(/^The Noble /, ''); }
  pretty(g: string): string { return g.charAt(0) + g.slice(1).toLowerCase(); }
}
