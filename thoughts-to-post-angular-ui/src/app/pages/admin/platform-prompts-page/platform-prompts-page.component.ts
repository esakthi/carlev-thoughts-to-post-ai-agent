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
      </div>

      <div class="card-grid">
        @for (prompt of prompts(); track prompt.id) {
          <div class="card">
            <div class="card-header">
              <div class="platform-info">
                <span class="platform-icon" [style.background]="PLATFORM_CONFIG[prompt.platform].color">
                  {{ PLATFORM_CONFIG[prompt.platform].icon }}
                </span>
                <h3>{{ PLATFORM_CONFIG[prompt.platform].label }}</h3>
              </div>
              <button class="btn-icon" (click)="editPrompt(prompt)">✏️ Edit</button>
            </div>
            <div class="prompt-preview">
              <pre>{{ prompt.promptText }}</pre>
            </div>
          </div>
        }
      </div>

      <!-- Edit Modal -->
      @if (showModal()) {
        <div class="modal-overlay">
          <div class="modal-content card">
            <h2>Edit {{ PLATFORM_CONFIG[currentPrompt().platform].label }} Prompt</h2>
            <form (ngSubmit)="savePrompt()">
              <div class="form-group">
                <label class="form-label">Prompt Text</label>
                <textarea class="form-textarea" [(ngModel)]="currentPrompt().promptText" name="prompt" rows="15" required></textarea>
              </div>
              <div class="modal-actions">
                <button type="button" class="btn btn-secondary" (click)="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">Save Changes</button>
              </div>
            </form>
          </div>
        </div>
      }
    </div>
  `,
    styles: [`
    .page-header {
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
      align-items: center;
      gap: var(--spacing-md);
    }

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

    .btn-icon {
      background: rgba(255,255,255,0.05);
      border: 1px solid var(--border-color);
      color: var(--text-primary);
      cursor: pointer;
      padding: var(--spacing-sm) var(--spacing-md);
      border-radius: var(--radius-md);
      transition: all 0.3s;
      &:hover { background: rgba(255,255,255,0.1); }
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
      background: rgba(0,0,0,0.7);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
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
        platform: 'LINKEDIN',
        promptText: ''
    });

    PLATFORM_CONFIG = PLATFORM_CONFIG;

    ngOnInit() {
        this.loadPrompts();
    }

    loadPrompts() {
        this.thoughtsService.getPlatformPrompts().subscribe(p => this.prompts.set(p));
    }

    editPrompt(prompt: PlatformPrompt) {
        this.currentPrompt.set({ ...prompt });
        this.showModal.set(true);
    }

    savePrompt() {
        this.thoughtsService.updatePlatformPrompt(this.currentPrompt().id!, this.currentPrompt())
            .subscribe(() => {
                this.loadPrompts();
                this.closeModal();
            });
    }

    closeModal() {
        this.showModal.set(false);
    }
}
