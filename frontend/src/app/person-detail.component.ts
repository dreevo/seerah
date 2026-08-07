import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { httpResource } from '@angular/common/http';
import { PersonDetail } from './models';

@Component({
  selector: 'app-person-detail',
  imports: [RouterLink],
  template: `
    <a class="back" routerLink="/">← Back to the library</a>

    @if (person.isLoading()) {
      <p class="state">Loading…</p>
    } @else if (person.error()) {
      <p class="state err">No published profile with that name was found.</p>
    } @else if (person.value(); as p) {
      <header class="d-head">
        <span class="cert c-REPORTED">Companion</span>
        <h1>{{ p.name }}</h1>
        @if (p.nameArabic) { <div class="d-ar" dir="rtl">{{ p.nameArabic }}</div> }
        <div class="meta">
          <span><b>Role</b>{{ pretty(p.role) }}</span>
          @if (p.deathYearAh) { <span><b>Died</b>{{ p.deathYearAh }} AH</span> }
        </div>
      </header>

      <section class="pane">
        <h2>Role Across the Chronology</h2>
        @if (p.events.length) {
          <div class="grid">
            @for (ev of p.events; track ev.slug) {
              <a class="mini link" [routerLink]="['/event', ev.slug]">
                <div class="k">{{ pretty(ev.relation) }}</div>
                <div class="n">{{ ev.title }}</div>
              </a>
            }
          </div>
        } @else {
          <p>No event individually names this person yet.</p>
        }
      </section>
    }
  `,
})
export class PersonDetailComponent {
  slug = input.required<string>();

  person = httpResource<PersonDetail>(() => `/api/public/people/${this.slug()}?locale=en`);

  pretty(v: string): string {
    return v.split('_').map((w) => w.charAt(0) + w.slice(1).toLowerCase()).join(' ');
  }
}
