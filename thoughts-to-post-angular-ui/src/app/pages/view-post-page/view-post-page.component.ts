import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ThoughtsService } from '../../services/thoughts.service';
import { EnrichedContentComponent } from '../../components/enriched-content/enriched-content.component';
import { ThoughtResponse, ApproveThoughtRequest } from '../../models/thought.models';

@Component({
    selector: 'app-view-post-page',
    standalone: true,
    imports: [CommonModule, EnrichedContentComponent, RouterLink],
    template: `
    <div class="container">
      <div class="page-header fade-in">
        <button class="btn-back" (click)="goBack()">← Back</button>
        <h1>Post Details</h1>
      </div>

      @if (isLoading()) {
        <div class="loading-state">
          <span class="spinner"></span>
          <p>Loading post details...</p>
        </div>
      } @else if (thought()) {
        <div class="fade-in">
          <app-enriched-content
            [thought]="thought()!"
            [isProcessing]="isPolling()"
            (approve)="onApprove($event)"
            (reject)="onReject()"
            (updateContent)="onUpdate($event)"
            (reenrich)="onReenrich($event)"
            (delete)="onDelete()"
            (repost)="onRepost()"
          />
        </div>
      } @else {
        <div class="error-state">
          <p>Post not found.</p>
          <button class="btn btn-primary" routerLink="/">Go Home</button>
        </div>
      }

      @if (error()) {
        <div class="error-toast fade-in">
          <span>⚠️ {{ error() }}</span>
          <button class="close-btn" (click)="error.set(null)">×</button>
        </div>
      }
    </div>
  `,
    styles: [`
    .page-header {
      display: flex;
      align-items: center;
      gap: var(--spacing-lg);
      margin-bottom: var(--spacing-xl);

      h1 { margin: 0; }
    }

    .btn-back {
      background: none;
      border: 1px solid var(--border-color);
      color: var(--text-secondary);
      padding: var(--spacing-sm) var(--spacing-md);
      border-radius: var(--radius-md);
      cursor: pointer;
      transition: all 0.3s ease;

      &:hover {
        background: var(--bg-card);
        color: var(--text-primary);
      }
    }

    .loading-state, .error-state {
      text-align: center;
      padding: var(--spacing-2xl);
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
export class ViewPostPageComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly thoughtsService = inject(ThoughtsService);

    thought = signal<ThoughtResponse | null>(null);
    isLoading = signal(true);
    isPolling = signal(false);
    error = signal<string | null>(null);

    ngOnInit() {
        const id = this.route.snapshot.paramMap.get('id');
        if (id) {
            this.loadThought(id);
        } else {
            this.isLoading.set(false);
        }
    }

    loadThought(id: string) {
        this.isLoading.set(true);
        this.thoughtsService.getThought(id).subscribe({
            next: (thought) => {
                this.thought.set(thought);
                this.isLoading.set(false);
                if (thought.status === 'PENDING' || thought.status === 'PROCESSING') {
                    this.startPolling(id);
                }
            },
            error: (err) => {
                this.error.set('Failed to load thought: ' + err.message);
                this.isLoading.set(false);
            }
        });
    }

    private startPolling(id: string) {
        this.isPolling.set(true);
        this.thoughtsService.pollForUpdates(id).subscribe({
            next: (thought) => {
                this.thought.set(thought);
                if (thought.status !== 'PENDING' && thought.status !== 'PROCESSING') {
                    this.isPolling.set(false);
                }
            },
            error: (err) => {
                this.error.set('Polling failed: ' + err.message);
                this.isPolling.set(false);
            }
        });
    }

    onApprove(request: ApproveThoughtRequest) {
        const t = this.thought();
        if (!t) return;
        this.thoughtsService.approveAndPost(t.id, request).subscribe({
            next: (updated) => this.thought.set(updated),
            error: (err) => this.error.set('Approval failed: ' + err.message)
        });
    }

    onReject() {
        const t = this.thought();
        if (!t) return;
        this.thoughtsService.rejectThought(t.id).subscribe({
            next: (updated) => this.thought.set(updated),
            error: (err) => this.error.set('Rejection failed: ' + err.message)
        });
    }

    onUpdate(updatedThought: ThoughtResponse) {
        this.thoughtsService.updateThought(updatedThought.id, updatedThought).subscribe({
            next: (updated) => this.thought.set(updated),
            error: (err) => this.error.set('Update failed: ' + err.message)
        });
    }

    onReenrich(additionalInstructions: string) {
        const t = this.thought();
        if (!t) return;
        this.thoughtsService.reenrichThought(t.id, additionalInstructions).subscribe({
            next: (updated) => {
                this.thought.set(updated);
                this.startPolling(updated.id);
            },
            error: (err) => this.error.set('Re-enrichment failed: ' + err.message)
        });
    }

    onDelete() {
        const t = this.thought();
        if (!t) return;
        this.thoughtsService.deleteThought(t.id).subscribe({
            next: () => this.router.navigate(['/thoughts']),
            error: (err) => this.error.set('Delete failed: ' + err.message)
        });
    }

    onRepost() {
        const t = this.thought();
        if (!t) return;
        this.thoughtsService.repostThought(t.id).subscribe({
            next: (updated) => {
                this.thought.set(updated);
                this.startPolling(updated.id);
            },
            error: (err) => this.error.set('Repost failed: ' + err.message)
        });
    }

    goBack() {
        window.history.back();
    }
}
