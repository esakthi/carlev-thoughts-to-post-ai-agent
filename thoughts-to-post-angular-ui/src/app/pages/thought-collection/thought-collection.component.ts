import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ThoughtsService } from '../../services/thoughts.service';
import { ThoughtCategory } from '../../models/thought.models';

@Component({
  selector: 'app-thought-collection',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container py-4">
      <div class="card shadow-sm mb-4">
        <div class="card-body">
          <h2 class="card-title mb-4">Internet Thought Collection</h2>

          <!-- Step 1: Define Criteria -->
          <div class="mb-4">
            <label class="form-label">Step 1: Select Category & Describe Your Goal</label>
            <div class="row g-3">
              <div class="col-md-4">
                <select class="form-select" [(ngModel)]="selectedCategory" (change)="onCategoryChange()">
                  <option value="" disabled selected>Select Category</option>
                  <option *ngFor="let cat of categories" [ngValue]="cat">{{ cat.category }}</option>
                </select>
              </div>
              <div class="col-md-8">
                <input type="text" class="form-control" [(ngModel)]="collectionDescription"
                       placeholder="e.g., Latest trends in AI for software engineering">
              </div>
            </div>
            <button class="btn btn-primary mt-3"
                    [disabled]="!selectedCategory || !collectionDescription || loading"
                    (click)="generateSearchString()">
              {{ loading ? 'Generating...' : 'Suggest Search Criteria' }}
            </button>
          </div>

          <!-- Step 2: Update Search String -->
          <div *ngIf="suggestedSearchString" class="mb-4 animate-fade-in">
            <label class="form-label">Step 2: Update Final Search String</label>
            <div class="input-group">
              <input type="text" class="form-control" [(ngModel)]="suggestedSearchString">
              <button class="btn btn-success" (click)="executeSearch()" [disabled]="loading">
                {{ loading ? 'Searching...' : 'Search Internet' }}
              </button>
            </div>
          </div>

          <!-- Step 3: Response Display -->
          <div *ngIf="searchResult" class="mb-4 animate-fade-in">
            <label class="form-label">Step 3: Search Results / Inspiration</label>
            <textarea class="form-control mb-3" rows="10" [(ngModel)]="searchResult"></textarea>

            <div class="d-flex justify-content-end gap-2">
              <button class="btn btn-outline-secondary" (click)="searchResult = ''">Clear</button>
              <button class="btn btn-success btn-lg" (click)="useAsThought()">
                Use as Input for Thought Creation
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .container { max-width: 900px; }
    .animate-fade-in { animation: fadeIn 0.5s ease-in; }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
  `]
})
export class ThoughtCollectionComponent implements OnInit {
  private readonly thoughtsService = inject(ThoughtsService);
  private readonly router = inject(Router);

  categories: ThoughtCategory[] = [];
  selectedCategory: ThoughtCategory | null = null;
  collectionDescription = '';
  suggestedSearchString = '';
  searchResult = '';
  loading = false;

  ngOnInit(): void {
    this.thoughtsService.getFullCategories().subscribe({
      next: (data) => this.categories = data,
      error: (err) => console.error('Error loading categories:', err)
    });
  }

  onCategoryChange(): void {
    if (this.selectedCategory && this.selectedCategory.searchDescription && !this.collectionDescription) {
        this.collectionDescription = this.selectedCategory.searchDescription;
    }
  }

  generateSearchString(): void {
    if (!this.selectedCategory) return;
    this.loading = true;
    this.thoughtsService.generateSearchCriteria({
      category: this.selectedCategory.category,
      description: this.collectionDescription
    }).subscribe({
      next: (res) => {
        this.suggestedSearchString = res;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error generating criteria:', err);
        this.loading = false;
      }
    });
  }

  executeSearch(): void {
    this.loading = true;
    this.thoughtsService.executeSearch({
      searchString: this.suggestedSearchString
    }).subscribe({
      next: (res) => {
        this.searchResult = res;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error searching:', err);
        this.loading = false;
        this.searchResult = 'Failed to retrieve results. Please try again.';
      }
    });
  }

  useAsThought(): void {
    // Navigate to create page with the result as state
    this.router.navigate(['/thoughts/create'], {
        state: {
            content: this.searchResult,
            category: this.selectedCategory?.category
        }
    });
  }
}
