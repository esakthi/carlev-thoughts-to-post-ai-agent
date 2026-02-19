import { Component, EventEmitter, Input, Output, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CreateThoughtRequest, PlatformType, PLATFORM_CONFIG, ThoughtCategory, PlatformPrompt, PlatformSelection } from '../../models/thought.models';
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
        <label class="form-label">Select Platforms & Configure</label>
        <div class="platforms-table-container">
          <table class="platforms-table">
            <thead>
              <tr>
                <th style="width: 50px"></th>
                <th style="width: 150px">Platform</th>
                <th>Prompt Preset</th>
                <th>Additional Context</th>
              </tr>
            </thead>
            <tbody>
              @for (platform of platforms; track platform) {
                <tr [class.row-selected]="isPlatformSelected(platform)">
                  <td class="text-center">
                    <input
                      type="checkbox"
                      [checked]="isPlatformSelected(platform)"
                      (change)="togglePlatform(platform)"
                      [disabled]="isLoading"
                    />
                  </td>
                  <td>
                    <div class="platform-cell">
                      <span class="platform-mini-icon" [style.background]="getPlatformColor(platform)">
                        {{ getPlatformIcon(platform) }}
                      </span>
                      <span>{{ getPlatformLabel(platform) }}</span>
                    </div>
                  </td>
                  <td>
                    <select
                      class="form-input table-input"
                      [(ngModel)]="platformConfigs[platform].presetId"
                      [name]="platform + '_preset'"
                      [disabled]="isLoading || !isPlatformSelected(platform)"
                      required
                    >
                      <option value="" disabled selected>Select a preset...</option>
                      @for (preset of getPresetsForPlatform(platform); track preset.id) {
                        <option [value]="preset.id">{{ preset.name }}</option>
                      }
                    </select>
                  </td>
                  <td>
                    <input
                      type="text"
                      class="form-input table-input"
                      [(ngModel)]="platformConfigs[platform].additionalContext"
                      [name]="platform + '_context'"
                      placeholder="Optional per-platform instructions..."
                      [disabled]="isLoading || !isPlatformSelected(platform)"
                    />
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </div>

      <div class="form-group">
        <label class="form-label">Global Additional Instructions (Optional)</label>
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
    .platforms-table-container {
      background: var(--bg-glass);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      overflow-y: auto;
      max-height: 300px;
    }

    .platforms-table {
      width: 100%;
      border-collapse: collapse;
      
      th {
        text-align: left;
        padding: var(--spacing-md);
        background: rgba(255, 255, 255, 0.05);
        border-bottom: 1px solid var(--border-color);
        font-size: 0.8rem;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: var(--text-muted);
      }

      td {
        padding: var(--spacing-sm) var(--spacing-md);
        border-bottom: 1px solid rgba(255, 255, 255, 0.05);
      }

      tr.row-selected {
        background: rgba(102, 126, 234, 0.05);
      }
    }

    .platform-cell {
      display: flex;
      align-items: center;
      gap: var(--spacing-sm);
    }

    .platform-mini-icon {
      width: 24px;
      height: 24px;
      border-radius: 4px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.7rem;
      color: white;
      font-weight: bold;
    }

    .table-input {
      padding: 6px 10px;
      font-size: 0.9rem;
      margin: 0;
    }

    .text-center { text-align: center; }

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
    allPresets = signal<PlatformPrompt[]>([]);
    selectedPlatforms = signal<PlatformType[]>(['LINKEDIN']);
    isLinkedInAuthorized = signal(false);
    isCheckingAuth = signal(false);

    platforms: PlatformType[] = ['LINKEDIN', 'FACEBOOK', 'INSTAGRAM'];

    // Track configurations per platform
    platformConfigs: Record<string, { presetId: string; additionalContext: string }> = {
        'LINKEDIN': { presetId: '', additionalContext: '' },
        'FACEBOOK': { presetId: '', additionalContext: '' },
        'INSTAGRAM': { presetId: '', additionalContext: '' }
    };

    ngOnInit() {
        this.checkLinkedInStatus();
        this.loadCategories();
        this.loadPresets();
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

    loadPresets() {
        this.thoughtsService.getPlatformPrompts().subscribe({
            next: (presets) => {
                this.allPresets.set(presets);
                // Select first available preset for each platform by default
                this.platforms.forEach(p => {
                    const platformPresets = presets.filter(pr => pr.platform === p);
                    if (platformPresets.length > 0) {
                        this.platformConfigs[p].presetId = platformPresets[0].id!;
                    }
                });
            },
            error: (err) => console.error('Failed to load presets', err)
        });
    }

    getPresetsForPlatform(platform: PlatformType): PlatformPrompt[] {
        return this.allPresets().filter(p => p.platform === platform);
    }

    isPlatformSelected(platform: PlatformType): boolean {
        return this.selectedPlatforms().includes(platform);
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
        // Allow all social media platforms for enrichment
        return ['LINKEDIN', 'FACEBOOK', 'INSTAGRAM'].includes(platform);
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

        // Create platform specific configurations
        const configs: PlatformSelection[] = this.selectedPlatforms().map(p => ({
            platform: p,
            presetId: this.platformConfigs[p].presetId,
            additionalContext: this.platformConfigs[p].additionalContext.trim() || undefined
        }));

        const request: CreateThoughtRequest = {
            thought: this.thought.trim(),
            categoryId: this.selectedCategoryId,
            platforms: this.selectedPlatforms(),
            additionalInstructions: this.additionalInstructions.trim() || undefined,
            platformConfigs: configs
        };

        this.submitThought.emit(request);
    }
}
