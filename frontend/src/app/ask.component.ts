import { Component, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { httpResource } from '@angular/common/http';

interface Cite { index: number; workTitle: string; tier: string; locator: string; }
interface Passage { eventSlug: string; eventTitle: string; text: string; confidence: string; markers: number[]; }
interface Answer { answered: boolean; message: string; passages: Passage[]; sources: Cite[]; }

@Component({
  selector: 'app-ask',
  imports: [RouterLink],
  template: `
    <section class="hero" style="padding-bottom:18px">
      <div class="eyebrow">Grounded Assistant</div>
      <h1>Ask this <em>Chronicle</em></h1>
      <p class="sub">This assistant has no knowledge of its own. It answers only from this chronicle's
        published, scholar-reviewed material, and cites every sentence — where the corpus is silent, it says so.</p>
    </section>

    <div class="search-box">
      <input
        class="search-in"
        [value]="q()"
        (keydown.enter)="submit()"
        (input)="draft.set($any($event.target).value)"
        placeholder="e.g. “What happened at Badr?” or “Tell me about the Hijrah”"
      />
      <div class="ask-hint">Press Enter to ask · answers are extractive and cited, never generated</div>
    </div>

    @if (q().trim().length >= 2) {
      @if (answer.isLoading()) {
        <p class="state">Retrieving from the corpus…</p>
      } @else if (answer.value(); as a) {
        @if (!a.answered) {
          <div class="refusal">{{ a.message }}</div>
        } @else {
          <div class="answer">
            @for (p of a.passages; track p.eventSlug) {
              <div class="passage">
                <p>{{ p.text }}
                  @for (m of p.markers; track m) { <sup class="mk">[S{{ m }}]</sup> }
                </p>
                <div class="pfoot">
                  <a [routerLink]="['/event', p.eventSlug]">{{ p.eventTitle }}</a>
                  <span class="conf">{{ p.confidence }}</span>
                </div>
              </div>
            }
            <div class="sourcelist">
              <div class="lb">Sources</div>
              @for (s of a.sources; track s.index) {
                <div class="s"><b>[S{{ s.index }}]</b> {{ s.workTitle }} <em>· {{ s.locator }}</em>
                  <span class="tier" [class]="'t-' + s.tier">{{ s.tier.charAt(0) + s.tier.slice(1).toLowerCase() }}</span>
                </div>
              }
            </div>
          </div>
        }
      }
    } @else {
      <p class="state">Ask a question about an event or a person in this chronicle.</p>
    }
  `,
})
export class AskComponent {
  chronicle = input.required<string>();
  draft = signal('');
  q = signal('');

  submit() { this.q.set(this.draft()); }

  answer = httpResource<Answer>(
    () => {
      const term = this.q().trim();
      return term.length >= 2
        ? `/api/public/ask?q=${encodeURIComponent(term)}&chronicle=${encodeURIComponent(this.chronicle())}`
        : undefined;
    },
  );
}
