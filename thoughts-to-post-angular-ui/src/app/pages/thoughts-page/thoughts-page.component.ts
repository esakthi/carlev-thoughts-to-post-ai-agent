import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ThoughtsService } from '../../services/thoughts.service';
import { ThoughtInputComponent } from '../../components/thought-input/thought-input.component';
import { EnrichedContentComponent } from '../../components/enriched-content/enriched-content.component';
import {
    ThoughtResponse,
    CreateThoughtRequest,
    ApproveThoughtRequest,
    PlatformType
} from '../../models/thought.models';

@Component({
    selector: 'app-thoughts-page',
    standalone: true,
    imports: [CommonModule, FormsModule, ThoughtInputComponent, EnrichedContentComponent],
    template: `
    <div class="container">
      <div class="page-header fade-in">
        <h1>Transform Your Thoughts</h1>
        <p class="text-secondary">
          Enter your idea and let AI create engaging social media content
        </p>
      </div>

      <div class="content-grid">
        <!-- Input Section -->
        <div class="input-section card fade-in">
          <app-thought-input 
            [isLoading]="isLoading()"
            (submitThought)="onSubmitThought($event)"
          />
        </div>

        <!-- Result Section -->
        @if (currentThought()) {
          <div class="result-section fade-in">
            <app-enriched-content
              [thought]="currentThought()!"
              [isProcessing]="isPolling()"
              (approve)="onApprove($event)"
              (reject)="onReject()"
              (updateContent)="onUpdateContent($event)"
              (reenrich)="onReenrich($event)"
            />
          </div>
        } @else if (!isLoading()) {
          <div class="placeholder-section card-glass fade-in">
            <div class="placeholder-content">
              <span class="placeholder-icon">🚀</span>
              <h3>Ready to Create</h3>
              <p class="text-muted">
                Enter your thought or topic above and select the platforms 
                where you'd like to share your content.
              </p>
            </div>
          </div>
        }
      </div>

      @if (error()) {
        <div class="error-toast fade-in">
          <span>⚠️ {{ error() }}</span>
          <button class="close-btn" (click)="clearError()">×</button>
        </div>
      }
    </div>
  `,
    styles: [`
    .page-header {
      text-align: center;
      margin-bottom: var(--spacing-2xl);

      h1 {
        font-size: 3rem;
        background: var(--primary-gradient);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
        margin-bottom: var(--spacing-sm);
      }

      p {
        font-size: 1.125rem;
        max-width: 500px;
        margin: 0 auto;
      }
    }

    .content-grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: var(--spacing-xl);
      align-items: start;
    }

    .input-section {
      width: 100%;
    }

    .placeholder-section {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 400px;
    }

    .placeholder-content {
      text-align: center;
      max-width: 300px;

      .placeholder-icon {
        font-size: 4rem;
        display: block;
        margin-bottom: var(--spacing-lg);
      }

      h3 {
        margin-bottom: var(--spacing-sm);
      }
    }

    .error-toast {
      position: fixed;
      bottom: var(--spacing-xl);
      left: 50%;
      transform: translateX(-50%);
      background: var(--error-gradient);
      color: white;
      padding: var(--spacing-md) var(--spacing-lg);
      border-radius: var(--radius-md);
      display: flex;
      align-items: center;
      gap: var(--spacing-md);
      box-shadow: var(--shadow-lg);
      z-index: 1000;

      .close-btn {
        background: none;
        border: none;
        color: white;
        font-size: 1.5rem;
        cursor: pointer;
        padding: 0;
        line-height: 1;
      }
    }
  `]
})
export class ThoughtsPageComponent {
    private readonly thoughtsService = inject(ThoughtsService);

    // State signals
    currentThought = signal<ThoughtResponse | null>(null);
    isLoading = signal(false);
    isPolling = signal(false);
    error = signal<string | null>(null);

    onSubmitThought(request: CreateThoughtRequest) {
        this.isLoading.set(true);
        this.error.set(null);
        this.currentThought.set(null);

        this.thoughtsService.createThought(request).subscribe({
            next: (thought) => {
                this.currentThought.set(thought);
                this.isLoading.set(false);

                // Start polling for updates
                if (thought.status === 'PENDING' || thought.status === 'PROCESSING') {
                    this.startPolling(thought.id);
                }
            },
            error: (err) => {
                this.error.set(err.message || 'Failed to create thought. Is the API running?');
                this.isLoading.set(false);
            }
        });
    }

    private startPolling(thoughtId: string) {
        this.isPolling.set(true);

        this.thoughtsService.pollForUpdates(thoughtId).subscribe({
            next: (thought) => {
                this.currentThought.set(thought);

                if (thought.status !== 'PENDING' && thought.status !== 'PROCESSING') {
                    this.isPolling.set(false);
                }
            },
            error: (err) => {
                this.error.set('Failed to get updates: ' + err.message);
                this.isPolling.set(false);
            },
            complete: () => {
                this.isPolling.set(false);
            }
        });
    }

    onApprove(request: ApproveThoughtRequest) {
        const thought = this.currentThought();
        if (!thought) return;

        this.isLoading.set(true);

        this.thoughtsService.approveAndPost(thought.id, request).subscribe({
            next: (updated) => {
                this.currentThought.set(updated);
                this.isLoading.set(false);
            },
            error: (err) => {
                this.error.set('Failed to approve: ' + err.message);
                this.isLoading.set(false);
            }
        });
    }

    onReject() {
        const thought = this.currentThought();
        if (!thought) return;

        this.isLoading.set(true);

        this.thoughtsService.rejectThought(thought.id).subscribe({
            next: (updated) => {
                this.currentThought.set(updated);
                this.isLoading.set(false);
            },
            error: (err) => {
                this.error.set('Failed to reject: ' + err.message);
                this.isLoading.set(false);
            }
        });
    }

    onUpdateContent(updatedThought: ThoughtResponse) {
        this.isLoading.set(true);
        this.thoughtsService.updateThought(updatedThought.id, updatedThought).subscribe({
            next: (updated) => {
                this.currentThought.set(updated);
                this.isLoading.set(false);
            },
            error: (err) => {
                this.error.set('Failed to update content: ' + err.message);
                this.isLoading.set(false);
            }
        });
    }

    onReenrich(comments: string) {
        const thought = this.currentThought();
        if (!thought) return;

        this.isLoading.set(true);
        this.error.set(null);

        this.thoughtsService.reenrichThought(thought.id, comments).subscribe({
            next: (updated) => {
                this.currentThought.set(updated);
                this.isLoading.set(false);
                this.startPolling(updated.id);
            },
            error: (err) => {
                this.error.set('Failed to resubmit: ' + err.message);
                this.isLoading.set(false);
            }
        });
    }

    clearError() {
        this.error.set(null);
    }
}
