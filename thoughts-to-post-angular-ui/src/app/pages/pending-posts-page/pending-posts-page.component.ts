import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ThoughtsService } from '../../services/thoughts.service';
import { PostListComponent } from '../../components/post-list/post-list.component';
import { ThoughtResponse } from '../../models/thought.models';

import { computed } from '@angular/core';

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
        <div class="page-layout">
          <aside class="sidebar card-glass">
            <h3 class="sidebar-title">Categories</h3>
            <ul class="category-list">
              <li
                [class.active]="selectedCategory() === 'All'"
                (click)="selectedCategory.set('All')"
              >
                All Posts
                <span class="count">{{ allThoughts().length }}</span>
              </li>
              @for (cat of categories(); track cat) {
                <li
                  [class.active]="selectedCategory() === cat"
                  (click)="selectedCategory.set(cat)"
                >
                  {{ cat }}
                  <span class="count">{{ getCategoryCount(cat) }}</span>
                </li>
              }
            </ul>
          </aside>

          <main class="content">
            <app-post-list
              [thoughts]="filteredThoughts()"
              emptyMessage="No posts found in this category."
              (deleted)="onDeleted($event)"
            />
          </main>
        </div>
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
    .page-layout {
      display: grid;
      grid-template-columns: 250px 1fr;
      gap: var(--spacing-xl);
      align-items: start;
    }
    .sidebar {
      padding: var(--spacing-lg);
      position: sticky;
      top: var(--spacing-xl);
    }
    .sidebar-title {
      font-size: 0.875rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--text-muted);
      margin-bottom: var(--spacing-lg);
    }
    .category-list {
      list-style: none;
      padding: 0;
      margin: 0;
      display: flex;
      flex-direction: column;
      gap: var(--spacing-xs);
    }
    .category-list li {
      padding: var(--spacing-sm) var(--spacing-md);
      border-radius: var(--radius-md);
      cursor: pointer;
      display: flex;
      justify-content: space-between;
      align-items: center;
      transition: all 0.2s;
      color: var(--text-secondary);
    }
    .category-list li:hover {
      background: rgba(255, 255, 255, 0.05);
      color: var(--text-primary);
    }
    .category-list li.active {
      background: var(--primary-gradient);
      color: white;
    }
    .count {
      font-size: 0.75rem;
      background: rgba(0, 0, 0, 0.2);
      padding: 2px 6px;
      border-radius: 10px;
    }
    @media (max-width: 768px) {
      .page-layout {
        grid-template-columns: 1fr;
      }
      .sidebar {
        position: static;
      }
    }
  `]
})
export class PendingPostsPageComponent implements OnInit {
    private readonly thoughtsService = inject(ThoughtsService);

    allThoughts = signal<ThoughtResponse[]>([]);
    isLoading = signal(true);
    selectedCategory = signal<string>('All');
    categories = signal<string[]>([]);

    filteredThoughts = computed(() => {
        const cat = this.selectedCategory();
        const thoughts = this.allThoughts();
        if (cat === 'All') return thoughts;
        return thoughts.filter(t => t.category === cat);
    });

    ngOnInit() {
        this.loadThoughts();
        this.loadCategories();
    }

    loadThoughts() {
        this.isLoading.set(true);
        this.thoughtsService.getThoughtsExcludingStatus('POSTED').subscribe({
            next: (thoughts) => {
                this.allThoughts.set(thoughts);
                this.isLoading.set(false);
            },
            error: () => this.isLoading.set(false)
        });
    }

    loadCategories() {
        this.thoughtsService.getCategories().subscribe({
            next: (cats) => this.categories.set(cats),
            error: () => this.categories.set(['Tech', 'Politics', 'Social', 'Others'])
        });
    }

    getCategoryCount(category: string): number {
        return this.allThoughts().filter(t => t.category === category).length;
    }

    onDeleted(id: string) {
        this.allThoughts.set(this.allThoughts().filter(t => t.id !== id));
    }
}
