import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
    selector: 'app-header',
    standalone: true,
    imports: [RouterLink, RouterLinkActive],
    template: `
    <header class="header">
      <div class="container">
        <div class="header-content">
          <div class="logo" routerLink="/" style="cursor: pointer;">
            <span class="logo-icon">✨</span>
            <span class="logo-text">Thoughts to Post</span>
          </div>
          <nav class="nav">
            <a class="nav-item" routerLink="/thoughts/create" routerLinkActive="active">Create</a>
            <a class="nav-item" routerLink="/posts/pending" routerLinkActive="active">Pending</a>
            <a class="nav-item" routerLink="/posts/history" routerLinkActive="active">History</a>
            <div class="nav-dropdown">
              <span class="nav-item">Admin ▾</span>
              <div class="dropdown-content">
                <a routerLink="/admin/categories" routerLinkActive="active">Categories</a>
                <a routerLink="/admin/platform-prompts" routerLinkActive="active">Platform Prompts</a>
              </div>
            </div>
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
      align-items: center;
      gap: var(--spacing-lg);
    }

    .nav-dropdown {
      position: relative;
      display: inline-block;

      &:hover .dropdown-content {
        display: block;
      }
    }

    .dropdown-content {
      display: none;
      position: absolute;
      right: 0;
      background-color: var(--bg-card);
      min-width: 200px;
      box-shadow: var(--shadow-lg);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      z-index: 1;
      padding: var(--spacing-xs) 0;

      a {
        color: var(--text-secondary);
        padding: var(--spacing-sm) var(--spacing-lg);
        text-decoration: none;
        display: block;
        transition: all 0.3s ease;

        &:hover, &.active {
          background: rgba(255, 255, 255, 0.05);
          color: var(--text-primary);
        }
      }
    }

    .nav-item {
      color: var(--text-secondary);
      font-weight: 500;
      cursor: pointer;
      transition: color 0.3s ease;
      position: relative;
      text-decoration: none;

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
