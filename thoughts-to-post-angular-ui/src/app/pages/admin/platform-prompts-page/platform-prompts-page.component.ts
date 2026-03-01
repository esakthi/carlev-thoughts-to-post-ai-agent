import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ThoughtsService } from '../../../services/thoughts.service';
import { PlatformPrompt, PLATFORM_CONFIG, PlatformType } from '../../../models/thought.models';

@Component({
    selector: 'app-platform-prompts-page',
    standalone: true,
    imports: [CommonModule, FormsModule],
    template: `
    <div class="container fade-in">
      <div class="page-header">
        <h1>Manage Platform Prompts</h1>
        <button class="btn btn-primary" (click)="addNewPrompt()">+ Add New Preset</button>
      </div>

      <div class="card-grid">
        @for (prompt of prompts(); track prompt.id) {
          <div class="card">
            <div class="card-header">
              <div class="platform-info">
                <span class="platform-icon" [style.background]="PLATFORM_CONFIG[prompt.platform].color">
                  {{ PLATFORM_CONFIG[prompt.platform].icon }}
                </span>
                <div>
                  <h3>{{ prompt.name || 'Untitled Preset' }}</h3>
                  <span class="badge" [class]="'badge-' + (prompt.type?.toLowerCase() || 'text')">{{ prompt.type || 'TEXT' }}</span>
                  <p class="preset-description">{{ prompt.description }}</p>
                  <small class="platform-label">{{ PLATFORM_CONFIG[prompt.platform].label }}</small>
                </div>
              </div>
              <div class="card-actions">
                <button class="btn-icon" (click)="editPrompt(prompt)">✏️ Edit</button>
                <button class="btn-icon delete" (click)="deletePrompt(prompt)">🗑️ Delete</button>
              </div>
            </div>
            <div class="prompt-preview">
              <pre>{{ prompt.promptText }}</pre>
            </div>
          </div>
        }
      </div>
    </div>

    <!-- Edit Modal -->
    @if (showModal()) {
      <div class="modal-overlay">
        <div class="modal-content card">
          <h2>{{ currentPrompt().id ? 'Edit' : 'Add' }} Prompt Preset</h2>
          <form (ngSubmit)="savePrompt()">
            <div class="form-group">
              <label class="form-label">Platform</label>
              <select class="form-input" [(ngModel)]="currentPrompt().platform" name="platform" required>
                @for (p of platforms; track p) {
                  <option [value]="p">{{ PLATFORM_CONFIG[p].label }}</option>
                }
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Prompt Type</label>
              <select class="form-input" [(ngModel)]="currentPrompt().type" name="type" required>
                <option value="TEXT">TEXT</option>
                <option value="IMAGE">IMAGE</option>
                <option value="VIDEO">VIDEO</option>
                <option value="OTHERS">OTHERS</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Preset Name</label>
              <input type="text" class="form-input" [(ngModel)]="currentPrompt().name" name="name" required placeholder="e.g., Professional Hook">
            </div>
            <div class="form-group">
              <label class="form-label">Description</label>
              <input type="text" class="form-input" [(ngModel)]="currentPrompt().description" name="description" required placeholder="e.g., Focuses on establishing authority...">
            </div>
            <div class="form-group">
              <label class="form-label">Prompt Text</label>
              <textarea class="form-textarea" [(ngModel)]="currentPrompt().promptText" name="prompt" rows="10" required></textarea>
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
      grid-template-columns: 1fr;
      gap: var(--spacing-lg);
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: var(--spacing-md);
    }

    .platform-info {
      display: flex;
      align-items: flex-start;
      gap: var(--spacing-md);
    }

    .preset-description {
      margin: 0;
      color: var(--text-secondary);
      font-size: 0.9rem;
    }

    .platform-label {
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.05em;
      font-size: 0.7rem;
      display: block;
      margin-top: 0.25rem;
    }

    .badge {
      display: inline-block;
      padding: 0.2rem 0.4rem;
      border-radius: var(--radius-sm);
      font-size: 0.65rem;
      font-weight: 600;
      text-transform: uppercase;
      margin-bottom: 0.25rem;
    }
    .badge-text { background: #4a90e2; color: white; }
    .badge-image { background: #e1306c; color: white; }
    .badge-video { background: #f5a623; color: white; }
    .badge-others { background: #7b7b7b; color: white; }

    .platform-icon {
      width: 40px;
      height: 40px;
      border-radius: var(--radius-sm);
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-weight: bold;
    }

    .card-actions {
      display: flex;
      gap: var(--spacing-sm);
    }

    .btn-icon {
      background: rgba(255,255,255,0.05);
      border: 1px solid var(--border-color);
      color: var(--text-primary);
      cursor: pointer;
      padding: var(--spacing-sm) var(--spacing-md);
      border-radius: var(--radius-md);
      transition: all 0.3s;
      &:hover { background: rgba(255,255,255,0.1); }
      &.delete:hover { border-color: #f56565; color: #f56565; }
    }

    .prompt-preview {
      background: rgba(0,0,0,0.2);
      padding: var(--spacing-md);
      border-radius: var(--radius-sm);
      pre {
        white-space: pre-wrap;
        font-size: 0.9rem;
        max-height: 300px;
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
export class PlatformPromptsPageComponent implements OnInit {
    private readonly thoughtsService = inject(ThoughtsService);

    prompts = signal<PlatformPrompt[]>([]);
    showModal = signal(false);
    currentPrompt = signal<PlatformPrompt>({
        name: '',
        description: '',
        platform: 'LINKEDIN',
        type: 'TEXT',
        promptText: ''
    });

    platforms: PlatformType[] = ['LINKEDIN', 'FACEBOOK', 'INSTAGRAM'];
    PLATFORM_CONFIG = PLATFORM_CONFIG;

    ngOnInit() {
        this.loadPrompts();
    }

    loadPrompts() {
        this.thoughtsService.getPlatformPrompts().subscribe(p => this.prompts.set(p));
    }

    addNewPrompt() {
        this.currentPrompt.set({
            name: '',
            description: '',
            platform: 'LINKEDIN',
            type: 'TEXT',
            promptText: ''
        });
        this.showModal.set(true);
    }

    editPrompt(prompt: PlatformPrompt) {
        this.currentPrompt.set({ ...prompt });
        this.showModal.set(true);
    }

    savePrompt() {
        const prompt = this.currentPrompt();
        if (prompt.id) {
            this.thoughtsService.updatePlatformPrompt(prompt.id, prompt)
                .subscribe(() => {
                    this.loadPrompts();
                    this.closeModal();
                });
        } else {
            this.thoughtsService.createPlatformPrompt(prompt)
                .subscribe(() => {
                    this.loadPrompts();
                    this.closeModal();
                });
        }
    }

    deletePrompt(prompt: PlatformPrompt) {
        if (confirm(`Are you sure you want to delete the preset "${prompt.name}"?`)) {
            this.thoughtsService.deletePlatformPrompt(prompt.id!)
                .subscribe(() => this.loadPrompts());
        }
    }

    closeModal() {
        this.showModal.set(false);
    }
}
