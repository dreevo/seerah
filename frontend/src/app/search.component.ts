import { Component, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { httpResource } from '@angular/common/http';

interface SearchHit {
  type: string;
  slug: string;
  title: string;
  subtitle: string;
  arabic: string | null;
}

@Component({
  selector: 'app-search',
  imports: [RouterLink],
  template: `
    <section class="hero" style="padding-bottom:18px">
      <div class="eyebrow">Search this Chronicle</div>
      <h1>Find an <em>event</em> or <em>person</em></h1>
    </section>

    <div class="search-box">
      <input
        class="search-in"
        [value]="q()"
        (input)="q.set($any($event.target).value)"
        placeholder="Try “Badr”, “Hamza”, “amnesty”, “Hijrah”…"
        autofocus
      />
    </div>

    @if (q().trim().length < 2) {
      <p class="state">Type at least two letters. Answers come only from published, cited content.</p>
    } @else if (results.isLoading()) {
      <p class="state">Searching…</p>
    } @else if (results.value()!.length === 0) {
      <p class="state">Nothing in the reviewed corpus matches “{{ q() }}”.</p>
    } @else {
      <div class="results">
        @for (h of results.value(); track h.type + h.slug) {
          <a class="hit" [routerLink]="h.type === 'EVENT' ? ['/event', h.slug] : ['/person', h.slug]">
            <span class="badge" [class.p]="h.type === 'PERSON'">{{ h.type === 'EVENT' ? 'Event' : 'Person' }}</span>
            <span class="ht">
              <span class="hn">{{ h.title }}</span>
              <span class="hs">{{ h.subtitle }}</span>
            </span>
            @if (h.arabic) { <span class="ha" dir="rtl">{{ h.arabic }}</span> }
          </a>
        }
      </div>
    }
  `,
})
export class SearchComponent {
  chronicle = input.required<string>();
  q = signal('');

  results = httpResource<SearchHit[]>(
    () => {
      const term = this.q().trim();
      return term.length >= 2
        ? `/api/public/search?q=${encodeURIComponent(term)}&chronicle=${encodeURIComponent(this.chronicle())}`
        : undefined;
    },
    { defaultValue: [] },
  );
}
