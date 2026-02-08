import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CreateThoughtRequest, PlatformType, PLATFORM_CONFIG } from '../../models/thought.models';

@Component({
    selector: 'app-thought-input',
    standalone: true,
    imports: [CommonModule, FormsModule],
    template: `
    <form (ngSubmit)="onSubmit()" #thoughtForm="ngForm">
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
        [disabled]="!thought.trim() || selectedPlatforms().length === 0 || isLoading"
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
  `]
})
export class ThoughtInputComponent {
    @Input() isLoading = false;
    @Output() submitThought = new EventEmitter<CreateThoughtRequest>();

    thought = '';
    additionalInstructions = '';
    selectedPlatforms = signal<PlatformType[]>(['LINKEDIN']);

    platforms: PlatformType[] = ['LINKEDIN', 'FACEBOOK', 'INSTAGRAM'];

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
            platforms: this.selectedPlatforms(),
            additionalInstructions: this.additionalInstructions.trim() || undefined
        };

        this.submitThought.emit(request);
    }
}
