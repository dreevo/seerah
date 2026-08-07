import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { httpResource } from '@angular/common/http';
import { PathSummary } from './models';

@Component({
  selector: 'app-explore',
  imports: [RouterLink],
  template: `
    <section class="hero">
      <div class="eyebrow">Guided Journeys</div>
      <h1>Learning <em>Paths</em></h1>
      <p class="sub">Curated sequences for those who want structure rather than free exploration —
        each walks you through connected events, one step at a time.</p>
      <div class="divider"><span class="l"></span><span class="star">۞</span><span class="r"></span></div>
    </section>

    @if (paths.isLoading()) {
      <p class="state">Loading the journeys…</p>
    } @else if (paths.value()!.length === 0) {
      <p class="state">No guided journeys for this chronicle yet.</p>
    } @else {
      <div class="path-grid">
        @for (p of paths.value(); track p.slug) {
          <a class="path-card" [routerLink]="['/c', chronicle(), 'path', p.slug]">
            <div class="badge">{{ p.stepCount }} steps@if (p.estMinutes) { · ~{{ p.estMinutes }} min }</div>
            <div class="pt">{{ p.title }}</div>
            @if (p.blurb) { <div class="pd">{{ p.blurb }}</div> }
            <div class="pgo">Begin the journey →</div>
          </a>
        }
      </div>
    }
  `,
})
export class ExploreComponent {
  chronicle = input.required<string>();
  paths = httpResource<PathSummary[]>(
    () => `/api/public/paths?locale=en&chronicle=${encodeURIComponent(this.chronicle())}`,
    { defaultValue: [] },
  );
}
