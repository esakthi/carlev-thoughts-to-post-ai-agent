import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ThoughtsService } from '../../services/thoughts.service';
import { PostListComponent } from '../../components/post-list/post-list.component';
import { ThoughtResponse } from '../../models/thought.models';

@Component({
    selector: 'app-history-page',
    standalone: true,
    imports: [CommonModule, PostListComponent],
    template: `
    <div class="container">
      <div class="page-header fade-in">
        <h1>Post History</h1>
        <p class="text-secondary">Review your successfully shared content.</p>
      </div>

      @if (isLoading()) {
        <div class="loading-state">
          <span class="spinner"></span>
          <p>Loading post history...</p>
        </div>
      } @else {
        <app-post-list
          [thoughts]="postedThoughts()"
          emptyMessage="You haven't posted anything yet."
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
export class HistoryPageComponent implements OnInit {
    private readonly thoughtsService = inject(ThoughtsService);
    postedThoughts = signal<ThoughtResponse[]>([]);
    isLoading = signal(true);

    ngOnInit() {
        this.thoughtsService.getThoughtsByStatus('POSTED').subscribe({
            next: (thoughts) => {
                this.postedThoughts.set(thoughts);
                this.isLoading.set(false);
            },
            error: () => this.isLoading.set(false)
        });
    }
}
