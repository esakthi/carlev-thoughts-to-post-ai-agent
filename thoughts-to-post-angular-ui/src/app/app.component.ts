import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './components/header/header.component';

@Component({
    selector: 'app-root',
    standalone: true,
    imports: [RouterOutlet, HeaderComponent],
    template: `
    <app-header />
    <main class="main-content">
      <router-outlet />
    </main>
  `,
    styles: [`
    .main-content {
      min-height: calc(100vh - 80px);
      padding: var(--spacing-xl) 0;
    }
  `]
})
export class AppComponent {
    title = 'Thoughts to Post';
}
