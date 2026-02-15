import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ThoughtsService } from '../../services/thoughts.service';
import { PostListComponent } from '../../components/post-list/post-list.component';
import { ThoughtResponse } from '../../models/thought.models';

@Component({
    selector: 'app-pending-posts-page',
    standalone: true,
    imports: [CommonModule, PostListComponent],
    template: `
    <div class="container">
      <div class="page-header fade-in">
        <h1>Pending Review</h1>
        <p class="text-secondary">Enriched posts awaiting your approval.</p>
      </div>

      @if (isLoading()) {
        <div class="loading-state">
          <span class="spinner"></span>
          <p>Loading pending posts...</p>
        </div>
      } @else {
        <app-post-list
          [thoughts]="pendingThoughts()"
          emptyMessage="You don't have any posts pending review."
        />
      }
    </div>
  `,
    styles: [`
    .page-header {
      margin-bottom: var(--spacing-xl);
    }
    .loading-state {
      text-align: center;
      padding: var(--spacing-2xl);
    }
  `]
})
export class PendingPostsPageComponent implements OnInit {
    private readonly thoughtsService = inject(ThoughtsService);
    pendingThoughts = signal<ThoughtResponse[]>([]);
    isLoading = signal(true);

    ngOnInit() {
        this.thoughtsService.getThoughtsExcludingStatus('POSTED').subscribe({
            next: (thoughts) => {
                this.pendingThoughts.set(thoughts);
                this.isLoading.set(false);
            },
            error: () => this.isLoading.set(false)
        });
    }
}
