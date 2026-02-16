import { Component, EventEmitter, Input, Output, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CreateThoughtRequest, PlatformType, PLATFORM_CONFIG, ThoughtCategory } from '../../models/thought.models';
import { ThoughtsService } from '../../services/thoughts.service';

@Component({
    selector: 'app-thought-input',
    standalone: true,
    imports: [CommonModule, FormsModule],
    template: `
    <form (ngSubmit)="onSubmit()" #thoughtForm="ngForm">
      <!-- LinkedIn Auth Prompt -->
      @if (selectedPlatforms().includes('LINKEDIN') && !isLinkedInAuthorized()) {
        <div class="auth-alert fade-in">
          <div class="auth-info">
            <span class="auth-icon">🔐</span>
            <div>
              <strong>LinkedIn Access Required</strong>
              <p>Authorize this app to post to your LinkedIn profile.</p>
            </div>
          </div>
          <button type="button" class="btn btn-secondary btn-sm" (click)="connectLinkedIn()" [disabled]="isCheckingAuth()">
            {{ isCheckingAuth() ? 'Checking...' : 'Connect LinkedIn' }}
          </button>
        </div>
      }

      <div class="form-group">
        <label class="form-label">Thought Category</label>
        <select
          class="form-input"
          [(ngModel)]="selectedCategoryId"
          name="categoryId"
          required
          [disabled]="isLoading"
        >
          <option value="" disabled selected>Select a category...</option>
          @for (cat of categories(); track cat.id) {
            <option [value]="cat.id">{{ cat.thoughtCategory }}</option>
          }
        </select>
      </div>

      <div class="form-group">
        <label class="form-label">Your Thought or Topic</label>
        <textarea
          class="form-textarea"
          [(ngModel)]="thought"
          name="thought"
          placeholder="Enter your idea, thought, or topic that you want to transform into social media content..."
          required
          [disabled]="isLoading"
          rows="5"
        ></textarea>
      </div>

      <div class="form-group">
        <label class="form-label">Select Platforms</label>
        <div class="platforms-grid">
          @for (platform of platforms; track platform) {
            <label 
              class="platform-option" 
              [class.selected]="selectedPlatforms().includes(platform)"
              [class.disabled]="!isPlatformEnabled(platform)"
            >
              <input
                type="checkbox"
                [checked]="selectedPlatforms().includes(platform)"
                (change)="togglePlatform(platform)"
                [disabled]="isLoading || !isPlatformEnabled(platform)"
              />
              <div class="platform-content">
                <span class="platform-icon" [style.background]="getPlatformColor(platform)">
                  {{ getPlatformIcon(platform) }}
                </span>
                <span class="platform-name">{{ getPlatformLabel(platform) }}</span>
                @if (!isPlatformEnabled(platform)) {
                  <span class="coming-soon">Coming Soon</span>
                }
              </div>
            </label>
          }
        </div>
      </div>

      <div class="form-group">
        <label class="form-label">Additional Instructions (Optional)</label>
        <input
          type="text"
          class="form-input"
          [(ngModel)]="additionalInstructions"
          name="additionalInstructions"
          placeholder="e.g., Make it more professional, add statistics, focus on benefits..."
          [disabled]="isLoading"
        />
      </div>

      <button 
        type="submit" 
        class="btn btn-primary submit-btn"
        [disabled]="!thought.trim() || selectedPlatforms().length === 0 || isLoading || (selectedPlatforms().includes('LINKEDIN') && !isLinkedInAuthorized())"
      >
        @if (isLoading) {
          <span class="spinner"></span>
          <span>Processing...</span>
        } @else {
          <span>✨</span>
          <span>Transform with AI</span>
        }
      </button>
    </form>
  `,
    styles: [`
    .platforms-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: var(--spacing-md);
    }

    .platform-option {
      cursor: pointer;
      
      input[type="checkbox"] {
        display: none;
      }

      .platform-content {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: var(--spacing-sm);
        padding: var(--spacing-lg);
        background: var(--bg-glass);
        border: 2px solid var(--border-color);
        border-radius: var(--radius-md);
        transition: all 0.3s ease;
      }

      &:hover:not(.disabled) .platform-content {
        border-color: var(--border-focus);
        background: rgba(255, 255, 255, 0.08);
      }

      &.selected .platform-content {
        border-color: #667eea;
        background: rgba(102, 126, 234, 0.1);
        box-shadow: 0 0 20px rgba(102, 126, 234, 0.2);
      }

      &.disabled {
        cursor: not-allowed;
        opacity: 0.5;
      }
    }

    .platform-icon {
      width: 48px;
      height: 48px;
      border-radius: var(--radius-md);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.25rem;
      font-weight: 700;
      color: white;
    }

    .platform-name {
      font-weight: 500;
    }

    .coming-soon {
      font-size: 0.625rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--text-muted);
      background: var(--bg-glass);
      padding: 2px 6px;
      border-radius: var(--radius-sm);
    }

    .submit-btn {
      width: 100%;
      padding: var(--spacing-lg);
      font-size: 1rem;
    }

    .auth-alert {
      display: flex;
      align-items: center;
      justify-content: space-between;
      background: rgba(102, 126, 234, 0.1);
      border: 1px solid rgba(102, 126, 234, 0.3);
      border-radius: var(--radius-md);
      padding: var(--spacing-md) var(--spacing-lg);
      margin-bottom: var(--spacing-lg);
      gap: var(--spacing-md);

      .auth-info {
        display: flex;
        align-items: center;
        gap: var(--spacing-md);

        strong {
          display: block;
          font-size: 0.9rem;
        }

        p {
          font-size: 0.8rem;
          color: var(--text-secondary);
          margin: 0;
        }
      }

      .auth-icon {
        font-size: 1.5rem;
      }
    }
  `]
})
export class ThoughtInputComponent implements OnInit {
    private readonly thoughtsService = inject(ThoughtsService);

    @Input() isLoading = false;
    @Output() submitThought = new EventEmitter<CreateThoughtRequest>();

    thought = '';
    selectedCategoryId = '';
    additionalInstructions = '';
    categories = signal<ThoughtCategory[]>([]);
    selectedPlatforms = signal<PlatformType[]>(['LINKEDIN']);
    isLinkedInAuthorized = signal(false);
    isCheckingAuth = signal(false);

    platforms: PlatformType[] = ['LINKEDIN', 'FACEBOOK', 'INSTAGRAM'];

    ngOnInit() {
        this.checkLinkedInStatus();
        this.loadCategories();
    }

    loadCategories() {
        this.thoughtsService.getCategories().subscribe({
            next: (cats) => {
                this.categories.set(cats);
                // If there's a Default category, select it
                const defaultCat = cats.find(c => c.thoughtCategory === 'Default');
                if (defaultCat) {
                    this.selectedCategoryId = defaultCat.id!;
                } else if (cats.length > 0) {
                    this.selectedCategoryId = cats[0].id!;
                }
            },
            error: (err) => console.error('Failed to load categories', err)
        });
    }

    checkLinkedInStatus() {
        this.isCheckingAuth.set(true);
        this.thoughtsService.getLinkedInStatus().subscribe({
            next: (status) => {
                this.isLinkedInAuthorized.set(status.authorized);
                this.isCheckingAuth.set(false);
            },
            error: () => {
                this.isLinkedInAuthorized.set(false);
                this.isCheckingAuth.set(false);
            }
        });
    }

    connectLinkedIn() {
        this.thoughtsService.getLinkedInAuthUrl().subscribe({
            next: (response) => {
                // Redirect to LinkedIn
                window.location.href = response.authorizationUrl;
            },
            error: (err) => {
                alert('Failed to get authorization URL: ' + err.message);
            }
        });
    }

    isPlatformEnabled(platform: PlatformType): boolean {
        // Only LinkedIn is implemented for now
        return platform === 'LINKEDIN';
    }

    togglePlatform(platform: PlatformType) {
        if (!this.isPlatformEnabled(platform)) return;

        const current = this.selectedPlatforms();
        if (current.includes(platform)) {
            this.selectedPlatforms.set(current.filter(p => p !== platform));
        } else {
            this.selectedPlatforms.set([...current, platform]);
        }
    }

    getPlatformLabel(platform: PlatformType): string {
        return PLATFORM_CONFIG[platform].label;
    }

    getPlatformIcon(platform: PlatformType): string {
        return PLATFORM_CONFIG[platform].icon;
    }

    getPlatformColor(platform: PlatformType): string {
        return PLATFORM_CONFIG[platform].color;
    }

    onSubmit() {
        if (!this.thought.trim() || this.selectedPlatforms().length === 0) return;

        const request: CreateThoughtRequest = {
            thought: this.thought.trim(),
            categoryId: this.selectedCategoryId,
            platforms: this.selectedPlatforms(),
            additionalInstructions: this.additionalInstructions.trim() || undefined
        };

        this.submitThought.emit(request);
    }
}
