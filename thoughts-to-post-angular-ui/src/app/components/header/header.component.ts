import { Component } from '@angular/core';

@Component({
    selector: 'app-header',
    standalone: true,
    template: `
    <header class="header">
      <div class="container">
        <div class="header-content">
          <div class="logo">
            <span class="logo-icon">✨</span>
            <span class="logo-text">Thoughts to Post</span>
          </div>
          <nav class="nav">
            <span class="nav-item active">Create</span>
            <span class="nav-item">History</span>
          </nav>
        </div>
      </div>
    </header>
  `,
    styles: [`
    .header {
      background: var(--bg-card);
      border-bottom: 1px solid var(--border-color);
      backdrop-filter: blur(20px);
      position: sticky;
      top: 0;
      z-index: 100;
    }

    .header-content {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 80px;
    }

    .logo {
      display: flex;
      align-items: center;
      gap: var(--spacing-sm);
      font-size: 1.5rem;
      font-weight: 700;
    }

    .logo-icon {
      font-size: 1.75rem;
    }

    .logo-text {
      background: var(--primary-gradient);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }

    .nav {
      display: flex;
      gap: var(--spacing-lg);
    }

    .nav-item {
      color: var(--text-secondary);
      font-weight: 500;
      cursor: pointer;
      transition: color 0.3s ease;
      position: relative;

      &:hover, &.active {
        color: var(--text-primary);
      }

      &.active::after {
        content: '';
        position: absolute;
        bottom: -8px;
        left: 0;
        right: 0;
        height: 2px;
        background: var(--primary-gradient);
        border-radius: 1px;
      }
    }
  `]
})
export class HeaderComponent { }
