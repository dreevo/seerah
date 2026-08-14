import { Component, DestroyRef, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

/** One beat of a nation's story — a real event, linked. `sig` is its signature doom. */
interface Cell { slug: string; t: string; sig?: string; }
interface Nation { ar: string; name: string; to: string; chronicle: string; cells: Cell[]; }

// The Qur'an tells the warner-prophets with one recurring arc. These five beats are
// the shared spine; the cells below are the *actual* events of each nation — so you
// watch five peoples rhyme through the same pattern, yet each meet a doom of its own.
const BEATS = [
  { t: 'The call', s: '“Worship Allah — you have no deity but Him.”' },
  { t: 'Their way', s: 'What they clung to and would not leave' },
  { t: 'Denial & defiance', s: 'They reject, mock, or demand the doom' },
  { t: 'The reckoning', s: 'Each nation meets a punishment of its own' },
  { t: 'Delivered', s: '“We saved him and those who believed, by mercy from Us.”' },
];

const NATIONS: Nation[] = [
  { ar: 'نُوح', name: 'Nūḥ', to: 'his people', chronicle: 'nuh', cells: [
    { slug: 'nuh-sent-to-his-people', t: 'Sent to His People' },
    { slug: 'the-people-cling-to-idols', t: 'Clinging to the Idols' },
    { slug: 'nuh-prays-against-them', t: 'The Prayer of Nūḥ' },
    { slug: 'the-flood-and-the-pairs', t: 'The Flood', sig: 'The Flood' },
    { slug: 'the-ark-rests-on-judi', t: 'The Ark Rests on Jūdī' },
  ] },
  { ar: 'هُود', name: 'Hūd', to: 'ʿĀd', chronicle: 'hud', cells: [
    { slug: 'hud-sent-to-ad', t: 'Sent to ʿĀd' },
    { slug: 'hud-the-pride-of-ad', t: 'The Pride of ʿĀd' },
    { slug: 'hud-warns-and-they-defy', t: 'The Warning & the Defiance' },
    { slug: 'hud-the-barren-wind', t: 'The Barren Wind', sig: 'A furious wind' },
    { slug: 'hud-the-believers-saved', t: 'The Believers Saved' },
  ] },
  { ar: 'صَالِح', name: 'Ṣāliḥ', to: 'Thamūd', chronicle: 'salih', cells: [
    { slug: 'salih-sent-to-thamud', t: 'Sent to Thamūd' },
    { slug: 'salih-the-she-camel', t: 'The She-Camel — a Sign' },
    { slug: 'salih-the-camel-hamstrung', t: 'They Hamstrung Her' },
    { slug: 'salih-the-blast', t: 'The Blast', sig: 'The Blast (Ṣayḥa)' },
    { slug: 'salih-the-believers-saved', t: 'The Believers Saved' },
  ] },
  { ar: 'لُوط', name: 'Lūṭ', to: 'his people', chronicle: 'lut', cells: [
    { slug: 'lut-sent-to-his-people', t: 'Sent to His People' },
    { slug: 'lut-they-reject-him', t: 'The Answer of His People' },
    { slug: 'lut-the-guests', t: 'The Guests & the Assault' },
    { slug: 'lut-the-destruction', t: 'The Overturning', sig: 'Overturned · rained with stones' },
    { slug: 'lut-saved-his-wife-perished', t: 'Saved, Except His Wife' },
  ] },
  { ar: 'شُعَيْب', name: 'Shuʿayb', to: 'Madyan', chronicle: 'shuayb', cells: [
    { slug: 'shuayb-sent-to-madyan', t: 'Sent to Madyan' },
    { slug: 'shuayb-the-cheating', t: 'The Cheating of the Scales' },
    { slug: 'shuayb-they-mock-and-threaten', t: 'The Mockery & the Threat' },
    { slug: 'shuayb-the-punishment', t: 'The Earthquake', sig: 'The earthquake / the cloud' },
    { slug: 'shuayb-and-believers-saved', t: 'The Believers Saved' },
  ] },
  { ar: 'مُوسَىٰ', name: 'Mūsā', to: 'Pharaoh & his people', chronicle: 'musa', cells: [
    { slug: 'musa-before-pharaoh', t: 'Sent to Pharaoh' },
    { slug: 'musa-and-the-magicians', t: 'The Sorcerers Summoned' },
    { slug: 'musa-the-signs-on-egypt', t: 'The Signs on Egypt' },
    { slug: 'musa-drowning-of-pharaoh', t: 'The Drowning', sig: 'Drowned in the sea' },
    { slug: 'musa-parting-of-the-sea', t: 'Israel Brought Across' },
  ] },
];

@Component({
  selector: 'app-messengers-way',
  imports: [RouterLink],
  template: `
    <section class="mw">
      <div class="mw-head">
        <div class="eyebrow">A pattern across the Qur’ān</div>
        <h1>The <em>Way</em> of the Messengers</h1>
        <p class="mw-lede">To the people of Nūḥ, then ʿĀd, Thamūd, the people of Lūṭ and Madyan, and Pharaoh’s Egypt —
          the Qur’ān tells one recurring arc: a messenger sent, a people who cling to their way,
          denial and defiance, a reckoning, and the believers delivered. Watch six nations move through
          the <b>same five beats</b> — each meeting a doom of its own.</p>
      </div>

      <div class="mw-controls">
        <button class="mw-btn" (click)="prev()" aria-label="Previous beat">‹</button>
        <button class="mw-btn play" (click)="toggle()">{{ playing() ? '❚❚ Pause' : '▶ Play' }}</button>
        <button class="mw-btn" (click)="next()" aria-label="Next beat">›</button>
        <div class="mw-beatlabel">
          <span class="mw-bt"><b>{{ beat() + 1 }}.</b> {{ beats[beat()].t }}</span>
          <span class="mw-bs">{{ beats[beat()].s }}</span>
        </div>
      </div>

      <div class="mw-scroll">
        <div class="mw-grid">
          <div class="mw-corner"></div>
          @for (b of beats; track b.t; let ci = $index) {
            <div class="mw-beat" [class.active]="ci === beat()" [class.done]="ci < beat()" (click)="beat.set(ci); stop()">
              <span class="mw-bnum">{{ ci + 1 }}</span><span>{{ b.t }}</span>
            </div>
          }
          @for (n of nations; track n.chronicle) {
            <a class="mw-nation" [routerLink]="['/c', n.chronicle]" [title]="'Open the ' + n.name + ' chronicle'">
              <span class="ar">{{ n.ar }}</span>
              <span class="nm">{{ n.name }}</span>
              <span class="to">to {{ n.to }}</span>
            </a>
            @for (c of n.cells; track c.slug; let ci = $index) {
              <button class="mw-cell" [class.on]="ci <= beat()" [class.active]="ci === beat()"
                      (click)="open(c.slug)" [disabled]="ci > beat()"
                      [attr.aria-hidden]="ci > beat() ? true : null">
                @if (ci <= beat()) {
                  <span class="mw-ct">{{ c.t }}</span>
                  @if (c.sig) { <span class="mw-sig">{{ c.sig }}</span> }
                }
              </button>
            }
          }
        </div>
      </div>
      <p class="mw-foot">Every cell is a real, cited event — click to open it. Sequence and wording follow the Qur’ān.</p>
      <p class="mw-note">The Qur’ān also names peoples whose story it does not detail — the <b>People of ar-Rass</b> and
        <b>Tubbaʿ</b> (25:38, 50:12–14) — so they cannot be drawn here without inventing what the source withholds.
        And within Pharaoh’s Egypt stands <b>Qārūn</b>, a man of Mūsā’s own people whom the earth swallowed for his
        arrogance (28:76–82) — <a routerLink="/event/musa-qarun-and-his-treasure">read his account</a>.</p>
    </section>
  `,
})
export class MessengersWayComponent {
  private router = inject(Router);
  readonly beats = BEATS;
  readonly nations = NATIONS;

  beat = signal(0);
  playing = signal(true);
  private timer = 0;

  constructor() {
    this.run();
    inject(DestroyRef).onDestroy(() => clearTimeout(this.timer));
  }

  private run() {
    clearTimeout(this.timer);
    if (!this.playing()) return;
    const atEnd = this.beat() >= this.beats.length - 1;
    this.timer = window.setTimeout(() => {
      this.beat.set(atEnd ? 0 : this.beat() + 1);
      this.run();
    }, atEnd ? 3200 : 2600);   // pause on the full pattern before looping
  }

  toggle() { this.playing.update((p) => !p); this.run(); }
  stop() { this.playing.set(false); clearTimeout(this.timer); }
  next() { this.stop(); this.beat.set(Math.min(this.beats.length - 1, this.beat() + 1)); }
  prev() { this.stop(); this.beat.set(Math.max(0, this.beat() - 1)); }
  open(slug: string) { this.router.navigate(['/event', slug]); }
}
