import { Component, computed, input, signal } from '@angular/core';
import { SourceItem } from './models';

const PAGE_CHARS = 640;   // above this a single language's text becomes pageable

/** One ḥadīth in full — literal Arabic + English (each paginated on its own when long),
 *  followed by its isnād drawn as a chain-tree in the app's own idiom. */
@Component({
  selector: 'app-hadith-card',
  template: `
    <div class="h-wrap">
    <figure class="hadith">
      <div class="h-ar" dir="rtl">{{ arPages()[arPage()] }}</div>
      @if (arPages().length > 1) {
        <div class="h-pager ar">
          <button class="hp-btn" (click)="turn(arPage, arPages(), -1)" [disabled]="arPage() === 0" aria-label="Previous (Arabic)">‹</button>
          <div class="hp-dots">
            @for (d of dots(arPages()); track d) {
              <button class="hp-dot" [class.on]="d === arPage()" (click)="arPage.set(d)" [attr.aria-label]="'Arabic part ' + (d + 1)"></button>
            }
          </div>
          <span class="hp-count">{{ arPage() + 1 }} / {{ arPages().length }} · عربى</span>
          <button class="hp-btn" (click)="turn(arPage, arPages(), 1)" [disabled]="arPage() === arPages().length - 1" aria-label="Next (Arabic)">›</button>
        </div>
      }

      <div class="h-en">“{{ enPages()[enPage()] }}”</div>
      @if (enPages().length > 1) {
        <div class="h-pager">
          <button class="hp-btn" (click)="turn(enPage, enPages(), -1)" [disabled]="enPage() === 0" aria-label="Previous (English)">‹</button>
          <div class="hp-dots">
            @for (d of dots(enPages()); track d) {
              <button class="hp-dot" [class.on]="d === enPage()" (click)="enPage.set(d)" [attr.aria-label]="'English part ' + (d + 1)"></button>
            }
          </div>
          <span class="hp-count">{{ enPage() + 1 }} / {{ enPages().length }} · English</span>
          <button class="hp-btn" (click)="turn(enPage, enPages(), 1)" [disabled]="enPage() === enPages().length - 1" aria-label="Next (English)">›</button>
        </div>
      }

      <figcaption class="h-cite">
        @if (source().grade) { <span class="grade g-{{ source().grade }}">{{ gradeLabel(source().grade!) }}</span> }
        <span class="h-src">{{ source().workTitle }}</span>
        @if (source().locator) { <span class="h-ref">{{ ref() }}</span> }
        @if (narrator(); as n) { <span class="h-nar">Narrated by {{ n }}</span> }
      </figcaption>
    </figure>

    @if (displayChain().length) {
      <figure class="isnad-tree" aria-label="Chain of narration">
        <figcaption class="it-head"><span class="ar">سِلْسِلَةُ الإِسْنَاد</span><span class="en">Chain of narration — up to the Prophet ﷺ</span></figcaption>
        <ol class="it-spine">
          <li class="it-node prophet" [style.--i]="0">
            <span class="it-dot"></span>
            <div class="it-name"><span class="ar">النَّبِيُّ ﷺ</span><span class="en">Prophet Muḥammad</span></div>
          </li>
          @for (n of displayChain(); track $index) {
            <li class="it-node" [style.--i]="$index + 1">
              <span class="it-dot"></span>
              <div class="it-name"><span class="ar" dir="rtl">{{ n }}</span></div>
            </li>
          }
          <li class="it-node rec" [style.--i]="displayChain().length + 1">
            <span class="it-dot"></span>
            <div class="it-name"><span class="en">Recorded in {{ source().workTitle }}</span><span class="ref">{{ ref() }}</span></div>
          </li>
        </ol>
      </figure>
    }
    </div>
  `,
})
export class HadithCardComponent {
  source = input.required<SourceItem>();
  arPage = signal(0);
  enPage = signal(0);

  arPages = computed(() => this.split(this.source().quoteAr ?? ''));
  enPages = computed(() => this.split(this.matn(this.source().quote)));

  /** Narrators top-down: the Companion (just under the Prophet) → the collector's teacher. */
  displayChain = computed(() => (this.source().chain ?? []).slice().reverse());

  ref = computed(() => {
    const m = /no\.\s*[\d\-]+/.exec(this.source().locator ?? '');
    return m ? m[0] : (this.source().locator ?? '');
  });

  dots(pages: string[]): number[] { return pages.map((_, i) => i); }
  turn(page: ReturnType<typeof signal<number>>, pages: string[], d: number) {
    page.update((p) => Math.max(0, Math.min(pages.length - 1, p + d)));
  }

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

  /** The report itself — the "Narrated X:" / "X reported:" attribution stripped. */
  matn(en: string | null): string {
    if (!en) return '';
    const i = en.indexOf(':');
    if (i > 0 && i < 100 && /narrat|report|said|saying/i.test(en.slice(0, i))) {
      return en.slice(i + 1).trim();
    }
    return en.trim();
  }

  /** Split one language's text into coherent pages at sentence boundaries (each ≤ ~PAGE_CHARS). */
  private split(text: string): string[] {
    if (text.length <= PAGE_CHARS) return [text];
    // Arabic periods are wrapped in directional marks (‏.‏), so allow those after the punctuation.
    const parts = text.split(/(?<=[.؟!؛،])[‎‏]*\s+/).filter((s) => s.trim());
    const pages: string[] = [];
    let buf = '';
    for (const p of parts) {
      if (buf && buf.length + p.length > PAGE_CHARS) { pages.push(buf); buf = p; }
      else buf = buf ? buf + ' ' + p : p;
    }
    if (buf) pages.push(buf);
    return pages.length ? pages : [text];
  }
}
