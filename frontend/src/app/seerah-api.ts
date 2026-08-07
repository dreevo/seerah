import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EventDetail, TimelineItem } from './models';

/**
 * The one client for the Seerah read API. Relative URLs, so the same build works
 * behind the dev proxy (ng serve → :8080) and when served same-origin.
 */
@Injectable({ providedIn: 'root' })
export class SeerahApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/public';

  timeline(locale = 'en'): Observable<TimelineItem[]> {
    return this.http.get<TimelineItem[]>(`${this.base}/timeline`, { params: { locale } });
  }

  event(slug: string, locale = 'en'): Observable<EventDetail> {
    return this.http.get<EventDetail>(`${this.base}/events/${slug}`, { params: { locale } });
  }
}
