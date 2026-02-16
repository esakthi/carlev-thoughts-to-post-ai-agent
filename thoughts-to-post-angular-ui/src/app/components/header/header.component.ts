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
            <a class="nav-item" routerLink="/dashboard" routerLinkActive="active">Dashboard</a>
            <div class="nav-dropdown">
              <span class="nav-item dropdown-toggle">Thoughts</span>
              <div class="dropdown-menu">
                <a routerLink="/thoughts/collection">Collection</a>
                <a routerLink="/thoughts/create">Create</a>
              </div>
            </div>
            <div class="nav-dropdown">
              <span class="nav-item dropdown-toggle">Posts</span>
              <div class="dropdown-menu">
                <a routerLink="/posts/pending">Pending</a>
                <a routerLink="/posts/history">History</a>
              </div>
            </div>
            <a class="nav-item" routerLink="/admin/categories" routerLinkActive="active">Admin</a>
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
      align-items: center;
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

    .nav-dropdown {
      position: relative;
      display: inline-block;

      &:hover .dropdown-menu {
        display: block;
      }
    }

    .dropdown-menu {
      display: none;
      position: absolute;
      background-color: var(--bg-card);
      min-width: 160px;
      box-shadow: 0px 8px 16px 0px rgba(0,0,0,0.2);
      z-index: 1;
      border-radius: var(--radius-md);
      border: 1px solid var(--border-color);
      top: 100%;
      left: 0;

      a {
        color: var(--text-secondary);
        padding: 12px 16px;
        text-decoration: none;
        display: block;
        transition: background 0.3s;

        &:hover {
          background-color: rgba(255, 255, 255, 0.05);
          color: var(--text-primary);
        }
      }
    }

    .dropdown-toggle::after {
      content: ' ▼';
      font-size: 0.6rem;
    }
  `]
})
export class HeaderComponent { }
