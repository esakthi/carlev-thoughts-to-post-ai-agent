import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ThoughtResponse, PLATFORM_CONFIG, EnrichedContent } from '../../models/thought.models';

@Component({
    selector: 'app-enriched-content',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="enriched-container">
      <!-- Status Header -->
      <div class="status-header card">
        <div class="status-info">
          <span class="status-badge" [ngClass]="thought.status.toLowerCase()">
            {{ getStatusLabel(thought.status) }}
          </span>
          <span class="version text-muted">v{{ thought.version }}</span>
        </div>
        @if (isProcessing) {
          <div class="processing-indicator">
            <span class="spinner"></span>
            <span class="text-secondary">AI is working...</span>
          </div>
        }
      </div>

      <!-- Original Thought -->
      <div class="original-thought card-glass">
        <h4 class="section-title">Original Thought</h4>
        <p>{{ thought.originalThought }}</p>
      </div>

      <!-- Generated Image -->
      @if (thought.generatedImageUrl) {
        <div class="generated-image card">
          <h4 class="section-title">Generated Image</h4>
          <div class="image-container">
            <img [src]="thought.generatedImageUrl" alt="Generated image for your post" />
          </div>
        </div>
      }

      <!-- Enriched Content by Platform -->
      @if ((thought.enrichedContents?.length ?? 0) > 0) {
        <div class="enriched-content">
          <h4 class="section-title">Enriched Content</h4>
          @for (content of thought.enrichedContents; track content.platform) {
            <div class="platform-content card">
              <div class="platform-header">
                <span class="platform-badge" [ngClass]="content.platform.toLowerCase()">
                  {{ PLATFORM_CONFIG[content.platform].label }}
                </span>
                <span class="char-count text-muted">
                  {{ content.characterCount }} characters
                </span>
              </div>
              <div class="content-body">
                <p class="content-text">{{ content.body }}</p>
              </div>
              @if ((content.hashtags?.length ?? 0) > 0) {
                <div class="hashtags">
                  @for (tag of content.hashtags; track tag) {
                    <span class="hashtag">#{{ tag }}</span>
                  }
                </div>
              }
            </div>
          }
        </div>
      }

      <!-- Action Buttons -->
      @if (thought.status === 'ENRICHED') {
        <div class="action-buttons">
          <button class="btn btn-success" (click)="approve.emit()">
            ✓ Approve & Post
          </button>
          <button class="btn btn-secondary" (click)="reject.emit()">
            ✕ Reject
          </button>
        </div>
      }

      <!-- Posted Status -->
      @if (thought.status === 'POSTED') {
        <div class="success-message card">
          <span class="success-icon">🎉</span>
          <h3>Posted Successfully!</h3>
          <p class="text-secondary">Your content has been shared to the selected platforms.</p>
        </div>
      }

      <!-- Error Status -->
      @if (thought.status === 'FAILED' && thought.errorMessage) {
        <div class="error-message card">
          <span class="error-icon">⚠️</span>
          <h3>Something Went Wrong</h3>
          <p class="text-secondary">{{ thought.errorMessage }}</p>
        </div>
      }
    </div>
  `,
    styles: [`
    .enriched-container {
      display: flex;
      flex-direction: column;
      gap: var(--spacing-lg);
    }

    .status-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: var(--spacing-md) var(--spacing-lg);
    }

    .status-info {
      display: flex;
      align-items: center;
      gap: var(--spacing-md);
    }

    .version {
      font-size: 0.875rem;
    }

    .processing-indicator {
      display: flex;
      align-items: center;
      gap: var(--spacing-sm);
    }

    .section-title {
      font-size: 0.875rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--text-muted);
      margin-bottom: var(--spacing-md);
    }

    .original-thought {
      padding: var(--spacing-lg);
      
      p {
        font-style: italic;
        color: var(--text-secondary);
      }
    }

    .generated-image {
      .image-container {
        border-radius: var(--radius-md);
        overflow: hidden;
        
        img {
          width: 100%;
          height: auto;
          display: block;
          max-height: 400px;
          object-fit: cover;
        }
      }
    }

    .platform-content {
      margin-bottom: var(--spacing-md);

      &:last-child {
        margin-bottom: 0;
      }
    }

    .platform-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: var(--spacing-md);
    }

    .content-body {
      margin-bottom: var(--spacing-md);
    }

    .content-text {
      white-space: pre-wrap;
      line-height: 1.7;
    }

    .hashtags {
      display: flex;
      flex-wrap: wrap;
      gap: var(--spacing-xs);
    }

    .hashtag {
      background: var(--primary-gradient);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      font-size: 0.875rem;
      font-weight: 500;
    }

    .action-buttons {
      display: flex;
      gap: var(--spacing-md);

      .btn {
        flex: 1;
        padding: var(--spacing-lg);
      }
    }

    .success-message,
    .error-message {
      text-align: center;
      padding: var(--spacing-2xl);

      .success-icon,
      .error-icon {
        font-size: 3rem;
        display: block;
        margin-bottom: var(--spacing-md);
      }

      h3 {
        margin-bottom: var(--spacing-sm);
      }
    }

    .success-message {
      background: linear-gradient(135deg, rgba(17, 153, 142, 0.1) 0%, rgba(56, 239, 125, 0.1) 100%);
      border-color: rgba(56, 239, 125, 0.3);
    }

    .error-message {
      background: linear-gradient(135deg, rgba(235, 51, 73, 0.1) 0%, rgba(244, 92, 67, 0.1) 100%);
      border-color: rgba(244, 92, 67, 0.3);
    }
  `]
})
export class EnrichedContentComponent {
    @Input({ required: true }) thought!: ThoughtResponse;
    @Input() isProcessing = false;
    @Output() approve = new EventEmitter<void>();
    @Output() reject = new EventEmitter<void>();

    PLATFORM_CONFIG = PLATFORM_CONFIG;

    getStatusLabel(status: string): string {
        const labels: Record<string, string> = {
            'PENDING': '⏳ Pending',
            'PROCESSING': '🔄 Processing',
            'ENRICHED': '✨ Ready for Review',
            'APPROVED': '✓ Approved',
            'POSTING': '📤 Posting...',
            'POSTED': '🎉 Posted',
            'FAILED': '❌ Failed',
            'REJECTED': '✕ Rejected'
        };
        return labels[status] || status;
    }
}
