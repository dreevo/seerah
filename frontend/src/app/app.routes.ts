import { Routes } from '@angular/router';
import { GatewayComponent } from './gateway.component';
import { TimelineComponent } from './timeline.component';
import { EventDetailComponent } from './event-detail.component';
import { CompanionsComponent } from './companions.component';
import { PersonDetailComponent } from './person-detail.component';
import { SearchComponent } from './search.component';
import { AskComponent } from './ask.component';
import { ExploreComponent } from './explore.component';
import { PathPlayerComponent } from './path-player.component';

export const routes: Routes = [
  { path: '', component: GatewayComponent, title: 'The Prophetic Library — Chronicles' },

  // Everything below is scoped to a chosen chronicle (:chronicle route param,
  // bound to each component's `chronicle` input via withComponentInputBinding).
  { path: 'c/:chronicle', component: TimelineComponent, title: 'Chronicle — Timeline' },
  { path: 'c/:chronicle/explore', component: ExploreComponent, title: 'Chronicle — Guided Journeys' },
  { path: 'c/:chronicle/path/:slug', component: PathPlayerComponent, title: 'Chronicle — Journey' },
  { path: 'c/:chronicle/companions', component: CompanionsComponent, title: 'Chronicle — People' },
  { path: 'c/:chronicle/search', component: SearchComponent, title: 'Chronicle — Search' },
  { path: 'c/:chronicle/ask', component: AskComponent, title: 'Chronicle — Ask' },

  // Global detail pages (an event/person carries its own chronicle context).
  { path: 'event/:slug', component: EventDetailComponent, title: 'Chronicle — Event' },
  { path: 'person/:slug', component: PersonDetailComponent, title: 'Chronicle — Person' },

  { path: '**', redirectTo: '' },
];
