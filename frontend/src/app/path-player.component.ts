import { Component, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { httpResource } from '@angular/common/http';
import { PathDetail } from './models';

@Component({
  selector: 'app-path-player',
  imports: [RouterLink],
  template: `
    <a class="back" [routerLink]="['/c', chronicle(), 'explore']">← All journeys</a>

    @if (path.isLoading()) {
      <p class="state">Loading…</p>
    } @else if (path.error()) {
      <p class="state err">That journey was not found.</p>
    } @else if (path.value(); as p) {
      <header class="player-head">
        <div class="eyebrow">Guided Journey</div>
        <h1>{{ p.title }}</h1>
        @if (p.blurb) { <p class="sub">{{ p.blurb }}</p> }
      </header>

      <div class="progress"><i [style.width.%]="pct(p.steps.length)"></i></div>

      <div class="step-dots">
        @for (s of p.steps; track s.ordinal; let i = $index) {
          <button [class.on]="i === step()" [class.done]="i < step()" (click)="step.set(i)"
                  [attr.aria-label]="'Step ' + (i + 1)"><i></i></button>
        }
      </div>

      @if (p.steps[step()]; as s) {
        <div class="stepcard">
          <div class="sn">Step {{ step() + 1 }} of {{ p.steps.length }}</div>
          <div class="stt">{{ s.eventTitle }}</div>
          @if (s.prompt) { <p class="sp">{{ s.prompt }}</p> }
          <a class="open" [routerLink]="['/event', s.eventSlug]">Open this event →</a>
        </div>

        <div class="pnav">
          <button class="action-btn" [disabled]="step() === 0" (click)="prev()">← Previous</button>
          @if (step() < p.steps.length - 1) {
            <button class="action-btn primary" (click)="next()">Next step →</button>
          } @else {
            <a class="action-btn primary" [routerLink]="['/c', chronicle(), 'explore']">Finish ✓</a>
          }
        </div>
      }
    }
  `,
})
export class PathPlayerComponent {
  chronicle = input.required<string>();
  slug = input.required<string>();
  step = signal(0);

  path = httpResource<PathDetail>(() => `/api/public/paths/${this.slug()}?locale=en`);

  pct(total: number): number {
    return total <= 1 ? 100 : ((this.step() + 1) / total) * 100;
  }

  next() { this.step.update((s) => s + 1); }
  prev() { this.step.update((s) => Math.max(0, s - 1)); }
}
