import { Component, computed, input, signal } from '@angular/core';
import { SourceItem } from './models';

const PAGE_CHARS = 620;   // above this, the ḥadīth becomes pageable rather than one long block

/** One ḥadīth shown in full — literal Arabic + English, paginated when long, with its isnād. */
@Component({
  selector: 'app-hadith-card',
  template: `
    <figure class="hadith">
      <div class="h-body">
        <div class="h-main">
          <div class="h-ar" dir="rtl">{{ arPages()[page()] }}</div>
          <div class="h-en">“{{ enPages()[page()] }}”</div>

          @if (pageCount() > 1) {
            <div class="h-pager">
              <button class="hp-btn" (click)="prev()" [disabled]="page() === 0" aria-label="Previous part">‹</button>
              <div class="hp-dots">
                @for (d of dots(); track d) {
                  <button class="hp-dot" [class.on]="d === page()" (click)="page.set(d)" [attr.aria-label]="'Part ' + (d + 1)"></button>
                }
              </div>
              <span class="hp-count">{{ page() + 1 }} / {{ pageCount() }}</span>
              <button class="hp-btn" (click)="next()" [disabled]="page() === pageCount() - 1" aria-label="Next part">›</button>
            </div>
          }
        </div>

        @if (displayChain().length) {
          <aside class="isnad" aria-label="Chain of narration">
            <div class="isnad-h">Chain of narration <span>isnād</span></div>
            <ol class="isnad-list">
              <li class="isn prophet"><span class="dot"></span><span class="nm">Prophet Muḥammad <b>ﷺ</b></span></li>
              @for (n of displayChain(); track $index) {
                <li class="isn"><span class="dot"></span><span class="nm" dir="rtl">{{ n }}</span></li>
              }
              <li class="isn collector"><span class="dot"></span><span class="nm">Recorded in {{ source().workTitle }}</span></li>
            </ol>
          </aside>
        }
      </div>

      <figcaption class="h-cite">
        @if (source().grade) { <span class="grade g-{{ source().grade }}">{{ gradeLabel(source().grade!) }}</span> }
        <span class="h-src">{{ source().workTitle }}</span>
        @if (source().locator) { <span class="h-ref">{{ ref() }}</span> }
        @if (narrator(); as n) { <span class="h-nar">Narrated by {{ n }}</span> }
      </figcaption>
      @if (!displayChain().length) {
        <p class="note h-isnadnote">The full chain of narration (isnād) precedes the report in the Arabic above.</p>
      }
    </figure>
  `,
})
export class HadithCardComponent {
  source = input.required<SourceItem>();
  page = signal(0);

  private enPagesAll = computed(() => this.paginate(this.matn(this.source().quote), this.source().quoteAr ?? ''));
  enPages = computed(() => this.enPagesAll().en);
  arPages = computed(() => this.enPagesAll().ar);
  pageCount = computed(() => this.enPages().length);
  dots = computed(() => Array.from({ length: this.pageCount() }, (_, i) => i));

  /** Narrators top-down from just under the Prophet (the Companion) to the collector's teacher. */
  displayChain = computed(() => (this.source().chain ?? []).slice().reverse());

  ref = computed(() => {
    const m = /no\.\s*[\d\-]+/.exec(this.source().locator ?? '');
    return m ? m[0] : (this.source().locator ?? '');
  });

  prev() { this.page.update((p) => Math.max(0, p - 1)); }
  next() { this.page.update((p) => Math.min(this.pageCount() - 1, p + 1)); }

  gradeLabel(g: string): string {
    switch (g) {
      case 'SAHIH': return 'Ṣaḥīḥ';
      case 'HASAN': return 'Ḥasan';
      case 'DAIF': return 'Ḍaʿīf';
      case 'MAWDU': return 'Mawḍūʿ';
      default: return g.charAt(0) + g.slice(1).toLowerCase();
    }
  }

  narrator(): string | null {
    const en = this.source().quote;
    if (!en) return null;
    let m = en.match(/^Narrated ([^:]{2,60}?):/);
    if (m) return m[1].replace(/[`]/g, '').trim();
    m = en.match(/^([A-Z][\w' .`-]{2,40}?) reported/);
    if (m) return m[1].replace(/[`]/g, '').trim();
    return null;
  }

  /** The report itself — the "Narrated X:" / "X reported:" attribution stripped so the words stand out. */
  matn(en: string | null): string {
    if (!en) return '';
    const i = en.indexOf(':');
    if (i > 0 && i < 100 && /narrat|report|said|saying/i.test(en.slice(0, i))) {
      return en.slice(i + 1).trim();
    }
    return en.trim();
  }

  /** Split a long ḥadīth into aligned Arabic/English pages at sentence boundaries. */
  private paginate(en: string, ar: string): { en: string[]; ar: string[] } {
    if (en.length <= PAGE_CHARS && ar.length <= PAGE_CHARS) return { en: [en], ar: [ar] };
    const n = Math.max(1, Math.ceil(Math.max(en.length, ar.length) / PAGE_CHARS));
    return { en: this.chunk(en, n), ar: this.chunk(ar, n) };
  }

  /** Distribute a text's sentences into n buckets of roughly-equal length. */
  private chunk(text: string, n: number): string[] {
    // split at sentence ends AND Arabic commas; Arabic periods are wrapped in
    // directional marks (‏.‏), so allow those between the punctuation and the space.
    const parts = text.split(/(?<=[.؟!؛،])[‎‏]*\s+/).filter((s) => s.trim());
    if (parts.length <= 1) return [text];
    const target = text.length / n;
    const out: string[] = [];
    let buf = '';
    for (const p of parts) {
      buf = buf ? buf + ' ' + p : p;
      if (buf.length >= target && out.length < n - 1) { out.push(buf); buf = ''; }
    }
    if (buf) out.push(buf);
    while (out.length < n) out.push('');
    return out;
  }
}
