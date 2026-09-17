import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header>
      <h1>Caseware engagement updates</h1>
      <p>Template versions and the changes waiting for each engagement.</p>
    </header>

    <main>
      <router-outlet />
    </main>
  `,
  styles: [],
})
export class AppComponent {}
