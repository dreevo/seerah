import { Component, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { IconComponent } from './icon.component';
import { ChronicleService } from './chronicle.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, IconComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  chronicle = inject(ChronicleService);
}
