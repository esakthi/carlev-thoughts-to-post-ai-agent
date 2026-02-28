import { Component, EventEmitter, Input, Output, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { ThoughtResponse, PLATFORM_CONFIG, ApproveThoughtRequest, PlatformType, GeneratedImage, EnrichedContent } from '../../models/thought.models';

@Component({
    selector: 'app-enriched-content',
    standalone: true,
    imports: [CommonModule, FormsModule],
    template: `
    <div class="enriched-container">
      <!-- Status Header -->
      <div class="status-header card">
        <div class="status-info">
          <span class="status-badge" [ngClass]="thought.status.toLowerCase()">
            {{ getStatusLabel(thought.status) }}
          </span>
          <span class="version text-muted">v{{ thought.version }}</span>
        </div>
        <div class="status-actions">
          @if (isProcessing) {
            <div class="processing-indicator">
              <span class="spinner"></span>
              <span class="text-secondary">AI is working...</span>
            </div>
          } @else {
            @if (canRepost()) {
              <button class="btn btn-primary btn-sm" (click)="repost.emit()">🔄 Repost</button>
            }
            <button class="btn btn-danger btn-sm" (click)="onDelete()">🗑️ Delete</button>
          }
        </div>
      </div>

      @if (thought.status === 'ENRICHED' || thought.status === 'PARTIALLY_COMPLETED' || thought.status === 'PROCESSING') {
        <!-- Wizard Progress -->
        <div class="wizard-progress">
          <div class="wizard-step" [class.active]="currentStep() === 1" [class.completed]="currentStep() > 1">
            <span class="step-num">1</span>
            <span class="step-label">Review Text</span>
          </div>
          <div class="wizard-line"></div>
          <div class="wizard-step" [class.active]="currentStep() === 2" [class.completed]="currentStep() > 2">
            <span class="step-num">2</span>
            <span class="step-label">Review Images</span>
          </div>
        </div>
      }

      <!-- Step 1: Text Review -->
      @if (currentStep() === 1 || (thought.status !== 'ENRICHED' && thought.status !== 'PARTIALLY_COMPLETED' && thought.status !== 'PROCESSING')) {
        <div class="step-content fade-in">
          <!-- Original Thought -->
          <div class="original-thought card-glass">
            <h4 class="section-title">Original Thought</h4>
            <p>{{ thought.originalThought }}</p>
          </div>

          <!-- Enriched Content by Platform -->
          @if ((thought.enrichedContents?.length ?? 0) > 0) {
            <div class="enriched-content">
              <div class="section-header">
                <h4 class="section-title">Enriched Content</h4>
                @if (canEdit()) {
                  <button class="btn-text" (click)="toggleEdit()">
                    {{ isEditing() ? 'Cancel' : 'Edit Content' }}
                  </button>
                }
              </div>

              @if (!isEditing()) {
                @for (content of thought.enrichedContents; track content.platform) {
                  <div class="platform-content card">
                    <div class="platform-header">
                      <span class="platform-badge" [ngClass]="content.platform.toLowerCase()">
                        {{ PLATFORM_CONFIG[content.platform].label }}
                      </span>
                      <span class="char-count text-muted">
                        {{ content.characterCount }} characters
                      </span>
                    </div>
                    <div class="content-body">
                      <p class="content-text">{{ content.body }}</p>
                    </div>
                    @if ((content.hashtags?.length ?? 0) > 0) {
                      <div class="hashtags">
                        @for (tag of content.hashtags; track tag) {
                          <span class="hashtag">#{{ tag }}</span>
                        }
                      </div>
                    }
                  </div>
                }
              } @else {
                @for (content of editableEnrichedContents; track content.platform; let i = $index) {
                  <div class="platform-content card edit-mode">
                    <div class="platform-header">
                      <span class="platform-badge" [ngClass]="content.platform.toLowerCase()">
                        {{ PLATFORM_CONFIG[content.platform].label }}
                      </span>
                    </div>
                    <div class="edit-fields">
                      <div class="form-group">
                        <label>Post Body</label>
                        <textarea class="form-control" [(ngModel)]="content.body" rows="6"></textarea>
                      </div>
                    </div>
                  </div>
                }
                <button class="btn btn-primary btn-sm mt-md" (click)="saveEdits()">Save Changes</button>
              }
            </div>
          }

          <!-- Missing Platforms -->
          @if (getMissingPlatforms().length > 0) {
            <div class="missing-platforms card-glass">
              <h4 class="section-title">Missing Platforms</h4>
              <div class="platforms-list">
                @for (platform of getMissingPlatforms(); track platform) {
                  <div class="platform-missing">
                    <span class="platform-badge gray">{{ PLATFORM_CONFIG[platform].label }}</span>
                    <span class="text-muted">Not yet enriched or failed.</span>
                  </div>
                }
              </div>
            </div>
          }

          @if (canResubmit() || ((thought.status === 'ENRICHED' || thought.status === 'PARTIALLY_COMPLETED') && currentStep() === 1)) {
            <!-- User Comments for Text -->
            <div class="user-comments card">
              <h4 class="section-title">Your Comments / Instructions</h4>
              <textarea
                class="form-control"
                [(ngModel)]="textContentComments"
                placeholder="Add comments to resubmit to AI or for your own reference..."
                rows="3"
              ></textarea>
              @if (canResubmit()) {
                <button class="btn btn-secondary btn-sm mt-md" (click)="onResubmit()" [disabled]="!textContentComments">
                  🔄 Resubmit to AI with these comments
                </button>
              }
            </div>

            <div class="action-buttons">
              @if ((thought.status === 'ENRICHED' || thought.status === 'PARTIALLY_COMPLETED') && currentStep() === 1) {
                <button class="btn btn-primary" (click)="nextStep()">
                  Next: Review Images →
                </button>
                <button class="btn btn-secondary" (click)="reject.emit()">
                  ✕ Reject
                </button>
              }
            </div>
          }
        </div>
      }

      <!-- Step 2: Image Review -->
      @if (currentStep() === 2 && (thought.status === 'ENRICHED' || thought.status === 'PARTIALLY_COMPLETED' || thought.status === 'PROCESSING')) {
        <div class="step-content fade-in">
          @for (content of thought.enrichedContents; track content.platform) {
            <div class="platform-images card">
              <div class="platform-header">
                <span class="platform-badge" [ngClass]="content.platform.toLowerCase()">
                  {{ PLATFORM_CONFIG[content.platform].label }} Images
                </span>
              </div>

              <!-- Image Gallery -->
              @if ((content.images?.length ?? 0) > 0) {
                <div class="image-gallery">
                  @for (image of content.images; track image.id) {
                    <div class="image-item" [class.selected]="image.selected" (click)="toggleImageSelection(content, image)">
                      <div class="image-wrapper">
                        <img [src]="sanitizeUrl(image.url)" [alt]="image.prompt" />
                        @if (image.selected) {
                          <div class="selected-overlay">✓</div>
                        }
                      </div>
                      <div class="image-meta">
                        <input type="text" class="form-control tag-input"
                               [(ngModel)]="image.tag" placeholder="Tag (e.g. Intro)"
                               (click)="$event.stopPropagation()" />
                        <button class="btn-icon" (click)="openPreview(image); $event.stopPropagation()">🔍</button>
                      </div>
                    </div>
                  }
                </div>
              } @else {
                <p class="text-muted italic">No images generated yet for this platform.</p>
              }

              <!-- Image Refinement -->
              <div class="refinement-box mt-md">
                <textarea
                  class="form-control"
                  [(ngModel)]="platformRefinements[content.platform]"
                  placeholder="Instructions for new image (e.g. 'More blue, professional')..."
                  rows="2"
                ></textarea>
                <button class="btn btn-secondary btn-sm mt-sm"
                        (click)="onRefineImage(content.platform)"
                        [disabled]="!platformRefinements[content.platform] || isProcessing">
                  🎨 Generate Additional Image
                </button>
              </div>
            </div>
          }

          <!-- Post Options -->
          <div class="post-options card">
            <h4 class="section-title">Final Post Options</h4>
            <div class="checkbox-group">
              <label class="checkbox-label">
                <input type="checkbox" [(ngModel)]="postText" />
                <span>Post Text Content</span>
              </label>
              <label class="checkbox-label">
                <input type="checkbox" [(ngModel)]="postImage" [disabled]="!hasSelectedImages()" />
                <span>Post Selected Images</span>
              </label>
            </div>
          </div>

          <div class="action-buttons">
            <button class="btn btn-secondary" (click)="prevStep()">
              ← Back to Text
            </button>
            <button class="btn btn-success" (click)="onApprove()" [disabled]="!postText && (!postImage || !hasSelectedImages())">
              ✓ Approve & Post
            </button>
          </div>
        </div>
      }

      <!-- Posted Status -->
      @if (thought.status === 'POSTED') {
        <div class="success-message card">
          <span class="success-icon">🎉</span>
          <h3>Posted Successfully!</h3>
          <p class="text-secondary">Your content has been shared.</p>
        </div>
      }

      <!-- Image Modal -->
      @if (showImageModal() && selectedPreviewImage) {
        <div class="modal-backdrop" (click)="showImageModal.set(false)">
          <div class="modal-content image-modal" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h3>Image Preview</h3>
              <button class="close-btn" (click)="showImageModal.set(false)">×</button>
            </div>
            <div class="modal-body centered">
              <img [src]="sanitizeUrl(selectedPreviewImage.url)" class="full-preview" />
              <p class="prompt-text mt-md"><strong>Prompt:</strong> {{ selectedPreviewImage.prompt }}</p>
            </div>
            <div class="modal-footer">
              <a [href]="sanitizeUrl(selectedPreviewImage.url)" download="post-image.png" class="btn btn-primary">
                Download
              </a>
              <button class="btn btn-secondary" (click)="showImageModal.set(false)">Close</button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
    styles: [`
    .enriched-container { display: flex; flex-direction: column; gap: var(--spacing-lg); }
    .status-header { display: flex; justify-content: space-between; align-items: center; padding: var(--spacing-md) var(--spacing-lg); }
    .status-info { display: flex; align-items: center; gap: var(--spacing-md); }
    .processing-indicator { display: flex; align-items: center; gap: var(--spacing-sm); }
    .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--spacing-md); }
    .section-title { font-size: 0.875rem; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-muted); }
    .btn-text { background: none; border: none; color: var(--primary-color); font-weight: 500; cursor: pointer; padding: 0; font-size: 0.875rem; }
    .original-thought { padding: var(--spacing-lg); p { font-style: italic; color: var(--text-secondary); } }
    .platform-content { margin-bottom: var(--spacing-md); }
    .platform-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--spacing-md); }
    .content-text { white-space: pre-wrap; line-height: 1.7; }
    .wizard-progress { display: flex; align-items: center; justify-content: center; gap: var(--spacing-md); margin-bottom: var(--spacing-xl); padding: var(--spacing-md); background: var(--card-bg); border-radius: var(--radius-lg); border: 1px solid var(--border-color); }
    .wizard-step { display: flex; align-items: center; gap: var(--spacing-sm); opacity: 0.5; transition: all 0.3s ease; }
    .wizard-step.active { opacity: 1; color: var(--primary-color); font-weight: 600; }
    .wizard-step.active .step-num { background: var(--primary-gradient); color: white; border-color: transparent; }
    .wizard-step.completed { opacity: 0.8; color: var(--success-color); }
    .wizard-step.completed .step-num { background: var(--success-color); color: white; border-color: transparent; }
    .step-num { width: 24px; height: 24px; border-radius: 50%; border: 2px solid var(--text-muted); display: flex; align-items: center; justify-content: center; font-size: 0.75rem; }
    .wizard-line { flex: 0 0 40px; height: 2px; background: var(--border-color); }
    .image-gallery { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: var(--spacing-md); margin-top: var(--spacing-md); }
    .image-item { border-radius: var(--radius-md); overflow: hidden; border: 2px solid transparent; cursor: pointer; transition: all 0.2s; position: relative; background: rgba(0,0,0,0.2); }
    .image-item.selected { border-color: var(--primary-color); transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.3); }
    .image-wrapper { position: relative; aspect-ratio: 1; img { width: 100%; height: 100%; object-fit: cover; } }
    .selected-overlay { position: absolute; top: 5px; right: 5px; background: var(--primary-color); color: white; border-radius: 50%; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: bold; }
    .image-meta { padding: 5px; display: flex; gap: 5px; }
    .tag-input { font-size: 10px; padding: 2px 5px; height: 24px; }
    .btn-icon { background: none; border: none; cursor: pointer; font-size: 14px; }
    .action-buttons { display: flex; gap: var(--spacing-md); margin-top: var(--spacing-md); .btn { flex: 1; padding: var(--spacing-lg); } }
    .mt-md { margin-top: var(--spacing-md); }
    .mt-sm { margin-top: var(--spacing-sm); }
    .italic { font-style: italic; }
    .success-message { text-align: center; padding: var(--spacing-2xl); background: rgba(56, 239, 125, 0.1); border-color: rgba(56, 239, 125, 0.3); }
    .full-preview { max-width: 100%; max-height: 70vh; object-fit: contain; }
    .prompt-text { font-size: 0.8rem; color: var(--text-secondary); max-width: 600px; text-align: left; }
    .edit-fields textarea { width: 100%; background: rgba(255, 255, 255, 0.05); border: 1px solid var(--border-color); color: var(--text-primary); padding: var(--spacing-md); }
  `]
})
export class EnrichedContentComponent {
    private readonly sanitizer = inject(DomSanitizer);

    @Input({ required: true }) thought!: ThoughtResponse;
    @Input() isProcessing = false;
    @Output() approve = new EventEmitter<ApproveThoughtRequest>();
    @Output() reject = new EventEmitter<void>();
    @Output() delete = new EventEmitter<void>();
    @Output() repost = new EventEmitter<void>();
    @Output() updateContent = new EventEmitter<ThoughtResponse>();
    @Output() reenrich = new EventEmitter<string>();
    @Output() refineImage = new EventEmitter<{ platform: PlatformType, instructions: string }>();

    currentStep = signal(1);
    isEditing = signal(false);
    showImageModal = signal(false);
    selectedPreviewImage: GeneratedImage | null = null;
    editableEnrichedContents: ThoughtResponse['enrichedContents'] = [];
    platformRefinements: Record<string, string> = {};

    textContentComments = '';
    imageContentComments = '';
    postText = true;
    postImage = true;

    PLATFORM_CONFIG = PLATFORM_CONFIG;

    nextStep() { this.currentStep.set(2); }
    prevStep() { this.currentStep.set(1); }

    onApprove() {
        this.approve.emit({
            textContentComments: this.textContentComments,
            imageContentComments: this.imageContentComments,
            postText: this.postText,
            postImage: this.postImage
        });
    }

    toggleEdit() {
        if (!this.isEditing()) {
            this.editableEnrichedContents = JSON.parse(JSON.stringify(this.thought.enrichedContents));
        }
        this.isEditing.set(!this.isEditing());
    }

    saveEdits() {
        const updatedThought = { ...this.thought, enrichedContents: this.editableEnrichedContents };
        this.updateContent.emit(updatedThought);
        this.isEditing.set(false);
    }

    onResubmit() { this.reenrich.emit(this.textContentComments); }

    onRefineImage(platform: PlatformType) {
        const instructions = this.platformRefinements[platform];
        if (instructions) {
            this.refineImage.emit({ platform, instructions });
            this.platformRefinements[platform] = '';
        }
    }

    onDelete() {
        if (confirm('Are you sure you want to delete this thought?')) {
            this.delete.emit();
        }
    }

    sanitizeUrl(url: string): SafeUrl {
        return url && url.startsWith('data:') ? this.sanitizer.bypassSecurityTrustUrl(url) : url;
    }

    toggleImageSelection(content: EnrichedContent, image: GeneratedImage) {
        image.selected = !image.selected;
        this.updateContent.emit(this.thought);
    }

    openPreview(image: GeneratedImage) {
        this.selectedPreviewImage = image;
        this.showImageModal.set(true);
    }

    hasSelectedImages(): boolean {
        return this.thought.enrichedContents.some(c => c.images?.some(img => img.selected));
    }

    canEdit(): boolean {
        return this.thought.status !== 'POSTED' && this.thought.status !== 'POSTING' && this.currentStep() === 1;
    }

    getMissingPlatforms(): PlatformType[] {
        if (!this.thought.selectedPlatforms) return [];
        const enrichedPlatforms = this.thought.enrichedContents?.map(ec => ec.platform) || [];
        return this.thought.selectedPlatforms.filter(p => !enrichedPlatforms.includes(p));
    }

    canResubmit(): boolean {
        return ['ENRICHED', 'FAILED', 'REJECTED', 'PARTIALLY_COMPLETED'].includes(this.thought.status);
    }

    canRepost(): boolean {
        return ['POSTED', 'REJECTED', 'FAILED', 'PARTIALLY_COMPLETED'].includes(this.thought.status);
    }

    getStatusLabel(status: string): string {
        const labels: Record<string, string> = {
            'PENDING': '⏳ Pending',
            'PROCESSING': '🔄 Processing',
            'ENRICHED': '✨ Ready for Review',
            'APPROVED': '✓ Approved',
            'POSTING': '📤 Posting...',
            'POSTED': '🎉 Posted',
            'FAILED': '❌ Failed',
            'REJECTED': '✕ Rejected',
            'PARTIALLY_COMPLETED': '⚠️ Partially Completed'
        };
        return labels[status] || status;
    }
}
