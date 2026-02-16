import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
    selector: 'app-landing-page',
    standalone: true,
    imports: [CommonModule, RouterLink],
    template: `
    <div class="container">
      <div class="hero fade-in">
        <h1>Welcome to Thoughts to Post</h1>
        <p class="text-secondary">Transform your ideas into professional social media content effortlessly.</p>
      </div>

      <div class="dashboard-grid">
        <div class="dashboard-card card fade-in" routerLink="/thoughts/collection">
          <div class="card-icon">🔍</div>
          <h3>Thought Collection</h3>
          <p class="text-muted">Search the internet for inspiration and latest trends.</p>
          <span class="btn btn-primary">Collect Ideas →</span>
        </div>

        <div class="dashboard-card card fade-in" routerLink="/thoughts/create">
          <div class="card-icon">🚀</div>
          <h3>Create New Post</h3>
          <p class="text-muted">Transform your ideas into social media content.</p>
          <span class="btn btn-primary">Get Started →</span>
        </div>

        <div class="dashboard-card card fade-in" routerLink="/posts/pending">
          <div class="card-icon">⏳</div>
          <h3>Pending Review</h3>
          <p class="text-muted">Review and edit your enriched posts before they go live.</p>
          <span class="btn btn-secondary">Review Posts →</span>
        </div>

        <div class="dashboard-card card fade-in" routerLink="/posts/history">
          <div class="card-icon">📜</div>
          <h3>Post History</h3>
          <p class="text-muted">View your previously shared content and engagement.</p>
          <span class="btn btn-secondary">View History →</span>
        </div>
      </div>
    </div>
  `,
    styles: [`
    .hero {
      text-align: center;
      margin-bottom: var(--spacing-2xl);
      padding: var(--spacing-2xl) 0;

      h1 {
        font-size: 3.5rem;
        background: var(--primary-gradient);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
        margin-bottom: var(--spacing-md);
      }

      p {
        font-size: 1.25rem;
        max-width: 600px;
        margin: 0 auto;
      }
    }

    .dashboard-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: var(--spacing-xl);
      margin-bottom: var(--spacing-2xl);
    }

    .dashboard-card {
      padding: var(--spacing-xl);
      text-align: center;
      cursor: pointer;
      transition: transform 0.3s ease, box-shadow 0.3s ease;

      &:hover {
        transform: translateY(-5px);
        box-shadow: var(--shadow-xl);
      }

      .card-icon {
        font-size: 3rem;
        margin-bottom: var(--spacing-lg);
      }

      h3 {
        margin-bottom: var(--spacing-md);
      }

      p {
        margin-bottom: var(--spacing-xl);
        min-height: 3em;
      }

      .btn {
        display: inline-block;
        width: 100%;
      }
    }
  `]
})
export class LandingPageComponent { }
