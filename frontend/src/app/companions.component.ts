import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { httpResource } from '@angular/common/http';
import { PersonListItem } from './models';
import { IconComponent } from './icon.component';

@Component({
  selector: 'app-companions',
  imports: [RouterLink, IconComponent],
  template: `
    <section class="hero">
      <div class="eyebrow">The People of the Chronicle</div>
      <h1>The <em>People</em></h1>
      <p class="sub">Every person named across this chronology, and the events their role shaped.
        Each profile is itself cited and published.</p>
      <div class="divider"><span class="l"></span><span class="star">۞</span><span class="r"></span></div>
    </section>

    @if (people.isLoading()) {
      <p class="state">Loading…</p>
    } @else if (people.error()) {
      <p class="state err">The people service is unavailable.</p>
    } @else {
      <div class="comp-grid">
        @for (p of people.value(); track p.id) {
          <a class="comp-card" [routerLink]="['/person', p.slug]">
            <span class="crole" [attr.title]="pretty(p.role)"><app-icon [name]="roleIcon(p)" [size]="17" /></span>
            @if (p.nameArabic) { <div class="car" dir="rtl">{{ p.nameArabic }}</div> }
            <div class="cn">{{ p.name }}</div>
            <div class="cr">{{ pretty(p.role) }}@if (p.deathYearAh) { · d. {{ p.deathYearAh }} AH }</div>
          </a>
        }
      </div>
    }
  `,
})
export class CompanionsComponent {
  chronicle = input.required<string>();
  people = httpResource<PersonListItem[]>(
    () => `/api/public/people?locale=en&chronicle=${encodeURIComponent(this.chronicle())}`,
    { defaultValue: [] },
  );

  roleIcon(p: PersonListItem): string {
    return IconComponent.personIcon(p.role);
  }

  pretty(v: string): string {
    return v.charAt(0) + v.slice(1).toLowerCase();
  }
}
