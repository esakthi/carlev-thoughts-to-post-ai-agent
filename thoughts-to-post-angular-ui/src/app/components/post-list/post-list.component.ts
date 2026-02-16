import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ThoughtResponse, PLATFORM_CONFIG } from '../../models/thought.models';

@Component({
    selector: 'app-post-list',
    standalone: true,
    imports: [CommonModule, RouterLink],
    template: `
    <div class="post-list">
      @if (thoughts.length === 0) {
        <div class="empty-state card-glass fade-in">
          <p class="text-secondary">{{ emptyMessage }}</p>
          <a routerLink="/thoughts/create" class="btn btn-primary mt-md">Create Your First Post</a>
        </div>
      } @else {
        <div class="list-container">
          @for (thought of thoughts; track thought.id) {
            <div class="post-item card fade-in" [routerLink]="['/posts/view', thought.id]">
              <div class="post-header">
                <span class="status-badge" [ngClass]="thought.status.toLowerCase()">
                  {{ getStatusLabel(thought.status) }}
                </span>
                <span class="date text-muted">{{ thought.createdAt | date:'short' }}</span>
              </div>
              <div class="post-preview">
                <p class="thought-text text-truncate">{{ thought.originalThought }}</p>
              </div>
              <div class="post-footer">
                <div class="platforms">
                  @for (platform of thought.selectedPlatforms; track platform) {
                    <span class="platform-dot" [ngClass]="platform.toLowerCase()" [title]="PLATFORM_CONFIG[platform].label"></span>
                  }
                </div>
                <span class="view-link">View Details →</span>
              </div>
            </div>
          }
        </div>
      }
    </div>
  `,
    styles: [`
    .post-list {
      margin-top: var(--spacing-xl);
    }

    .list-container {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
      gap: var(--spacing-lg);
    }

    .post-item {
      cursor: pointer;
      transition: all 0.3s ease;
      display: flex;
      flex-direction: column;
      gap: var(--spacing-md);

      &:hover {
        transform: translateY(-3px);
        border-color: var(--primary-color);
        box-shadow: var(--shadow-lg);
      }
    }

    .post-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .date {
      font-size: 0.875rem;
    }

    .thought-text {
      font-size: 1rem;
      color: var(--text-primary);
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
      font-style: italic;
    }

    .post-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: auto;
      padding-top: var(--spacing-sm);
      border-top: 1px solid var(--border-color);
    }

    .platforms {
      display: flex;
      gap: var(--spacing-xs);
    }

    .platform-dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;

      &.linkedin { background: #0077b5; }
      &.facebook { background: #1877f2; }
      &.instagram { background: #e4405f; }
    }

    .view-link {
      font-size: 0.875rem;
      color: var(--primary-color);
      font-weight: 500;
    }

    .empty-state {
      text-align: center;
      padding: var(--spacing-2xl);
      border: 2px dashed var(--border-color);
    }

    .mt-md {
      margin-top: var(--spacing-md);
    }
  `]
})
export class PostListComponent {
    @Input() thoughts: ThoughtResponse[] = [];
    @Input() emptyMessage = 'No posts found.';

    PLATFORM_CONFIG = PLATFORM_CONFIG;

    getStatusLabel(status: string): string {
        const labels: Record<string, string> = {
            'PENDING': '⏳ Pending',
            'PROCESSING': '🔄 Processing',
            'ENRICHED': '✨ Ready',
            'APPROVED': '✓ Approved',
            'POSTING': '📤 Posting',
            'POSTED': '🎉 Posted',
            'FAILED': '❌ Failed',
            'REJECTED': '✕ Rejected'
        };
        return labels[status] || status;
    }
}
