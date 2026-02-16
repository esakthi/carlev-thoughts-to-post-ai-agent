import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ThoughtsService } from '../../../services/thoughts.service';
import { ThoughtCategory } from '../../../models/thought.models';

@Component({
    selector: 'app-categories-page',
    standalone: true,
    imports: [CommonModule, FormsModule],
    template: `
    <div class="container fade-in">
      <div class="page-header">
        <h1>Manage Thought Categories</h1>
        <button class="btn btn-primary" (click)="showCreateForm()">+ New Category</button>
      </div>

      <!-- Category List -->
      <div class="card-grid">
        @for (cat of categories(); track cat.id) {
          <div class="card">
            <div class="card-header">
              <h3>{{ cat.thoughtCategory }}</h3>
              <div class="actions">
                <button class="btn-icon" (click)="editCategory(cat)">✏️</button>
                <button class="btn-icon" (click)="deleteCategory(cat.id!)" [disabled]="cat.thoughtCategory === 'Default'">🗑️</button>
              </div>
            </div>
            <p class="description">{{ cat.description }}</p>
            <div class="prompt-preview">
              <strong>System Prompt:</strong>
              <pre>{{ cat.systemPrompt }}</pre>
            </div>
          </div>
        }
      </div>
    </div>

    <!-- Edit/Create Modal -->
    @if (showModal()) {
      <div class="modal-overlay">
        <div class="modal-content card">
          <h2>{{ editingId() ? 'Edit' : 'Create' }} Category</h2>
          <form (ngSubmit)="saveCategory()">
            <div class="form-group">
              <label class="form-label">Category Name</label>
              <input type="text" class="form-input" [(ngModel)]="currentCat().thoughtCategory" name="name" required />
            </div>
            <div class="form-group">
              <label class="form-label">Description (Context for AI)</label>
              <textarea class="form-textarea" [(ngModel)]="currentCat().description" name="desc" rows="3" required></textarea>
            </div>
            <div class="form-group">
              <label class="form-label">System Prompt</label>
              <textarea class="form-textarea" [(ngModel)]="currentCat().systemPrompt" name="prompt" rows="10" required></textarea>
            </div>
            <div class="modal-actions">
              <button type="button" class="btn btn-secondary" (click)="closeModal()">Cancel</button>
              <button type="submit" class="btn btn-primary">Save Changes</button>
            </div>
          </form>
        </div>
      </div>
    }
  `,
    styles: [`
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: var(--spacing-xl);
    }

    .card-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
      gap: var(--spacing-lg);
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: var(--spacing-md);
    }

    .actions {
      display: flex;
      gap: var(--spacing-xs);
    }

    .btn-icon {
      background: none;
      border: none;
      cursor: pointer;
      font-size: 1.25rem;
      padding: var(--spacing-xs);
      border-radius: var(--radius-sm);
      transition: background 0.3s;
      &:hover:not(:disabled) { background: rgba(255,255,255,0.1); }
      &:disabled { opacity: 0.3; cursor: not-allowed; }
    }

    .description {
      color: var(--text-secondary);
      margin-bottom: var(--spacing-md);
    }

    .prompt-preview {
      background: rgba(0,0,0,0.2);
      padding: var(--spacing-md);
      border-radius: var(--radius-sm);
      pre {
        margin-top: var(--spacing-xs);
        white-space: pre-wrap;
        font-size: 0.85rem;
        max-height: 200px;
        overflow-y: auto;
      }
    }

    .modal-overlay {
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0,0,0,0.85);
      display: flex;
      align-items: flex-start;
      justify-content: center;
      z-index: 2000;
      padding-top: 100px;
      overflow-y: auto;
    }

    .modal-content {
      width: 100%;
      max-width: 800px;
      max-height: 90vh;
      overflow-y: auto;
      padding: var(--spacing-xl);
    }

    .modal-actions {
      display: flex;
      justify-content: flex-end;
      gap: var(--spacing-md);
      margin-top: var(--spacing-lg);
    }
  `]
})
export class CategoriesPageComponent implements OnInit {
    private readonly thoughtsService = inject(ThoughtsService);

    categories = signal<ThoughtCategory[]>([]);
    showModal = signal(false);
    editingId = signal<string | null>(null);
    currentCat = signal<ThoughtCategory>({
        thoughtCategory: '',
        description: '',
        systemPrompt: ''
    });

    ngOnInit() {
        this.loadCategories();
    }

    loadCategories() {
        this.thoughtsService.getCategories().subscribe(cats => this.categories.set(cats));
    }

    showCreateForm() {
        this.editingId.set(null);
        this.currentCat.set({
            thoughtCategory: '',
            description: '',
            systemPrompt: ''
        });
        this.showModal.set(true);
    }

    editCategory(cat: ThoughtCategory) {
        this.editingId.set(cat.id!);
        this.currentCat.set({ ...cat });
        this.showModal.set(true);
    }

    saveCategory() {
        const obs = this.editingId()
            ? this.thoughtsService.updateCategory(this.editingId()!, this.currentCat())
            : this.thoughtsService.createCategory(this.currentCat());

        obs.subscribe(() => {
            this.loadCategories();
            this.closeModal();
        });
    }

    deleteCategory(id: string) {
        if (confirm('Are you sure you want to delete this category?')) {
            this.thoughtsService.deleteCategory(id).subscribe(() => this.loadCategories());
        }
    }

    closeModal() {
        this.showModal.set(false);
    }
}
