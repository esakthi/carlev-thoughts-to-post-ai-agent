import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ThoughtsService } from '../../services/thoughts.service';
import { ThoughtCategory } from '../../models/thought.models';

@Component({
  selector: 'app-admin-categories',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container py-4">
      <div class="d-flex justify-content-between align-items-center mb-4">
        <h2>Manage Thought Categories</h2>
        <button class="btn btn-primary" (click)="showAddForm = !showAddForm">
          {{ showAddForm ? 'Cancel' : 'Add New Category' }}
        </button>
      </div>

      <!-- Add/Edit Form -->
      <div *ngIf="showAddForm || editingCategory" class="card mb-4 shadow-sm">
        <div class="card-body">
          <h4 class="card-title">{{ editingCategory ? 'Edit' : 'Add' }} Category</h4>
          <form (ngSubmit)="saveCategory()">
            <div class="mb-3">
              <label class="form-label">Category Name</label>
              <input type="text" class="form-control" [(ngModel)]="currentCategory.category" name="category" required>
            </div>
            <div class="mb-3">
              <label class="form-label">Search Description</label>
              <textarea class="form-control" [(ngModel)]="currentCategory.searchDescription" name="searchDescription" rows="2"></textarea>
              <div class="form-text">Used to suggest internet search strings.</div>
            </div>
            <div class="mb-3">
              <label class="form-label">Model Role / System Prompt</label>
              <textarea class="form-control" [(ngModel)]="currentCategory.modelRole" name="modelRole" rows="5"></textarea>
              <div class="form-text">The system prompt given to the AI agent for this category.</div>
            </div>
            <div class="d-flex gap-2">
              <button type="submit" class="btn btn-success">Save</button>
              <button type="button" class="btn btn-secondary" (click)="resetForm()">Cancel</button>
            </div>
          </form>
        </div>
      </div>

      <!-- Categories List -->
      <div class="table-responsive">
        <table class="table table-hover align-middle">
          <thead class="table-light">
            <tr>
              <th>Category</th>
              <th>Search Description</th>
              <th>Model Role (Prefix)</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let cat of categories">
              <td><strong>{{ cat.category }}</strong></td>
              <td>{{ cat.searchDescription | slice:0:50 }}{{ cat.searchDescription.length > 50 ? '...' : '' }}</td>
              <td>{{ cat.modelRole | slice:0:50 }}{{ cat.modelRole.length > 50 ? '...' : '' }}</td>
              <td>
                <div class="btn-group btn-group-sm">
                  <button class="btn btn-outline-primary" (click)="editCategory(cat)">Edit</button>
                  <button class="btn btn-outline-danger" (click)="deleteCategory(cat)">Delete</button>
                </div>
              </td>
            </tr>
            <tr *ngIf="categories.length === 0">
              <td colspan="4" class="text-center py-4">No categories found. Add one to get started!</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .container { max-width: 1000px; }
  `]
})
export class AdminCategoriesComponent implements OnInit {
  private readonly thoughtsService = inject(ThoughtsService);

  categories: ThoughtCategory[] = [];
  showAddForm = false;
  editingCategory: string | null = null;
  currentCategory: ThoughtCategory = {
    category: '',
    searchDescription: '',
    modelRole: ''
  };

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.thoughtsService.getFullCategories().subscribe({
      next: (data) => this.categories = data,
      error: (err) => console.error('Error loading categories:', err)
    });
  }

  editCategory(cat: ThoughtCategory): void {
    this.editingCategory = cat.id || null;
    this.currentCategory = { ...cat };
    this.showAddForm = false;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  resetForm(): void {
    this.showAddForm = false;
    this.editingCategory = null;
    this.currentCategory = {
      category: '',
      searchDescription: '',
      modelRole: ''
    };
  }

  saveCategory(): void {
    if (!this.currentCategory.category) return;

    if (this.editingCategory && this.currentCategory.id) {
      this.thoughtsService.updateCategory(this.currentCategory.id, this.currentCategory).subscribe({
        next: () => {
          this.loadCategories();
          this.resetForm();
        },
        error: (err) => console.error('Error updating category:', err)
      });
    } else {
      this.thoughtsService.createCategory(this.currentCategory).subscribe({
        next: () => {
          this.loadCategories();
          this.resetForm();
        },
        error: (err) => console.error('Error creating category:', err)
      });
    }
  }

  deleteCategory(cat: ThoughtCategory): void {
    if (cat.id && confirm(`Are you sure you want to delete the category "${cat.category}"?`)) {
      this.thoughtsService.deleteCategory(cat.id).subscribe({
        next: () => this.loadCategories(),
        error: (err) => console.error('Error deleting category:', err)
      });
    }
  }
}
