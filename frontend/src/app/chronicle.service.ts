import { computed, inject, Injectable } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { httpResource } from '@angular/common/http';
import { filter, map } from 'rxjs';
import { ChronicleItem } from './models';

/**
 * Tracks which chronicle the reader is currently inside by watching the URL
 * (`/c/:slug/...`). The header nav uses this to scope its links, so no component
 * has to push state up. The list of chronicles is fetched once and shared.
 */
@Injectable({ providedIn: 'root' })
export class ChronicleService {
  private router = inject(Router);

  private url = toSignal(
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd), map(() => this.router.url)),
    { initialValue: this.router.url },
  );

  /** The current chronicle slug from the URL, or null on the gateway / global pages. */
  slug = computed<string | null>(() => {
    const m = this.url().match(/^\/c\/([^/?#]+)/);
    return m ? decodeURIComponent(m[1]) : null;
  });

  all = httpResource<ChronicleItem[]>(() => '/api/public/chronicles', { defaultValue: [] });

  current = computed<ChronicleItem | null>(
    () => this.all.value().find((c: ChronicleItem) => c.slug === this.slug()) ?? null,
  );
}
