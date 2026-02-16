import { Component, EventEmitter, Input, Output, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { ThoughtResponse, PLATFORM_CONFIG, ApproveThoughtRequest } from '../../models/thought.models';

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
        @if (isProcessing) {
          <div class="processing-indicator">
            <span class="spinner"></span>
            <span class="text-secondary">AI is working...</span>
          </div>
        }
      </div>

      @if (thought.status === 'ENRICHED') {
        <!-- Wizard Progress -->
        <div class="wizard-progress">
          <div class="wizard-step" [class.active]="currentStep() === 1" [class.completed]="currentStep() > 1">
            <span class="step-num">1</span>
            <span class="step-label">Review Text</span>
          </div>
          <div class="wizard-line"></div>
          <div class="wizard-step" [class.active]="currentStep() === 2" [class.completed]="currentStep() > 2">
            <span class="step-num">2</span>
            <span class="step-label">Review Image</span>
          </div>
        </div>
      }

      <!-- Step 1: Text Review -->
      @if (currentStep() === 1 || thought.status !== 'ENRICHED') {
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
                    <button class="btn btn-primary btn-sm mt-md" (click)="saveEdits()">Save Changes</button>
                  </div>
                }
              }
            </div>
          }

          @if (canResubmit() || (thought.status === 'ENRICHED' && currentStep() === 1)) {
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
              @if (thought.status === 'ENRICHED' && currentStep() === 1) {
                <button class="btn btn-primary" (click)="nextStep()">
                  Next: Review Image →
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
      @if (currentStep() === 2 && thought.status === 'ENRICHED') {
        <div class="step-content fade-in">
          <!-- Generated Image -->
          @if (thought.generatedImageUrl) {
            <div class="generated-image card">
              <div class="section-header">
                <h4 class="section-title">Generated Image</h4>
                <div class="image-actions">
                  <button class="btn-text" (click)="showImageModal.set(true)">View Image</button>
                  <a [href]="sanitizeUrl(thought.generatedImageUrl)" download="post-image.png" class="btn-text">
                    Download Image
                  </a>
                </div>
              </div>
              <div class="image-container">
                <img [src]="sanitizeUrl(thought.generatedImageUrl)" alt="Generated image for your post" />
              </div>
            </div>
          } @else {
            <div class="no-image card-glass">
              <p class="text-muted">No image was generated for this thought.</p>
            </div>
          }

          <!-- User Comments for Image -->
          <div class="user-comments card">
            <h4 class="section-title">Your Comments on Image</h4>
            <textarea
              class="form-control"
              [(ngModel)]="imageContentComments"
              placeholder="Add any specific comments or changes for the image..."
              rows="3"
            ></textarea>
          </div>

          <!-- Post Options -->
          <div class="post-options card">
            <h4 class="section-title">Post Options</h4>
            <div class="checkbox-group">
              <label class="checkbox-label">
                <input type="checkbox" [(ngModel)]="postText" />
                <span>Post Text Content</span>
              </label>
              <label class="checkbox-label">
                <input type="checkbox" [(ngModel)]="postImage" [disabled]="!thought.generatedImageUrl" />
                <span>Post Image</span>
              </label>
            </div>
          </div>

          <div class="action-buttons">
            <button class="btn btn-secondary" (click)="prevStep()">
              ← Back
            </button>
            <button class="btn btn-success" (click)="onApprove()" [disabled]="!postText && !postImage">
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
          <p class="text-secondary">Your content has been shared to the selected platforms.</p>
        </div>
      }

      <!-- Error Status -->
      @if (thought.status === 'FAILED' && thought.errorMessage) {
        <div class="error-message card">
          <span class="error-icon">⚠️</span>
          <h3>Something Went Wrong</h3>
          <p class="text-secondary">{{ thought.errorMessage }}</p>
        </div>
      }

      <!-- Image Modal -->
      @if (showImageModal() && thought.generatedImageUrl) {
        <div class="modal-backdrop" (click)="showImageModal.set(false)">
          <div class="modal-content image-modal" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h3>Preview Image</h3>
              <button class="close-btn" (click)="showImageModal.set(false)">×</button>
            </div>
            <div class="modal-body centered">
              <img [src]="sanitizeUrl(thought.generatedImageUrl)" class="full-preview" alt="Full image preview" />
            </div>
            <div class="modal-footer">
              <a [href]="sanitizeUrl(thought.generatedImageUrl)" download="post-image.png" class="btn btn-primary">
                Download Image
              </a>
              <button class="btn btn-secondary" (click)="showImageModal.set(false)">Close</button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
    styles: [`
    .enriched-container {
      display: flex;
      flex-direction: column;
      gap: var(--spacing-lg);
    }

    .status-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: var(--spacing-md) var(--spacing-lg);
    }

    .status-info {
      display: flex;
      align-items: center;
      gap: var(--spacing-md);
    }

    .version {
      font-size: 0.875rem;
    }

    .processing-indicator {
      display: flex;
      align-items: center;
      gap: var(--spacing-sm);
    }

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: var(--spacing-md);
    }

    .section-title {
      font-size: 0.875rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--text-muted);
      margin-bottom: 0;
    }

    .btn-text {
      background: none;
      border: none;
      color: var(--primary-color);
      font-weight: 500;
      cursor: pointer;
      padding: 0;
      font-size: 0.875rem;

      &:hover {
        text-decoration: underline;
      }
    }

    .original-thought {
      padding: var(--spacing-lg);
      
      p {
        font-style: italic;
        color: var(--text-secondary);
      }
    }

    .generated-image {
      .image-container {
        border-radius: var(--radius-md);
        overflow: hidden;
        
        img {
          width: 100%;
          height: auto;
          display: block;
          max-height: 400px;
          object-fit: cover;
        }
      }
    }

    .platform-content {
      margin-bottom: var(--spacing-md);

      &:last-child {
        margin-bottom: 0;
      }
    }

    .platform-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: var(--spacing-md);
    }

    .content-body {
      margin-bottom: var(--spacing-md);
    }

    .content-text {
      white-space: pre-wrap;
      line-height: 1.7;
    }

    .edit-fields {
      display: flex;
      flex-direction: column;
      gap: var(--spacing-md);
      margin-top: var(--spacing-md);

      .form-group {
        display: flex;
        flex-direction: column;
        gap: var(--spacing-xs);

        label {
          font-size: 0.75rem;
          color: var(--text-muted);
          font-weight: 600;
        }

        textarea {
          width: 100%;
          background: rgba(255, 255, 255, 0.05);
          border: 1px solid var(--border-color);
          border-radius: var(--radius-md);
          color: var(--text-primary);
          padding: var(--spacing-md);
          font-family: inherit;
          resize: vertical;

          &:focus {
            outline: none;
            border-color: var(--primary-color);
            background: rgba(255, 255, 255, 0.08);
          }
        }
      }
    }

    .hashtags {
      display: flex;
      flex-wrap: wrap;
      gap: var(--spacing-xs);
    }

    .hashtag {
      background: var(--primary-gradient);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      font-size: 0.875rem;
      font-weight: 500;
    }

    .wizard-progress {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: var(--spacing-md);
      margin-bottom: var(--spacing-xl);
      padding: var(--spacing-md);
      background: var(--card-bg);
      border-radius: var(--radius-lg);
      border: 1px solid var(--border-color);
    }

    .wizard-step {
      display: flex;
      align-items: center;
      gap: var(--spacing-sm);
      opacity: 0.5;
      transition: all 0.3s ease;

      &.active {
        opacity: 1;
        color: var(--primary-color);
        font-weight: 600;

        .step-num {
          background: var(--primary-gradient);
          color: white;
          border-color: transparent;
        }
      }

      &.completed {
        opacity: 0.8;
        color: var(--success-color);

        .step-num {
          background: var(--success-color);
          color: white;
          border-color: transparent;
        }
      }
    }

    .step-num {
      width: 24px;
      height: 24px;
      border-radius: 50%;
      border: 2px solid var(--text-muted);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.75rem;
    }

    .wizard-line {
      flex: 0 0 40px;
      height: 2px;
      background: var(--border-color);
    }

    .user-comments {
      textarea {
        width: 100%;
        background: rgba(255, 255, 255, 0.05);
        border: 1px solid var(--border-color);
        border-radius: var(--radius-md);
        color: var(--text-primary);
        padding: var(--spacing-md);
        font-family: inherit;
        resize: vertical;

        &:focus {
          outline: none;
          border-color: var(--primary-color);
          background: rgba(255, 255, 255, 0.08);
        }
      }
    }

    .post-options {
      .checkbox-group {
        display: flex;
        flex-direction: column;
        gap: var(--spacing-md);
      }

      .checkbox-label {
        display: flex;
        align-items: center;
        gap: var(--spacing-md);
        cursor: pointer;

        input[type="checkbox"] {
          width: 20px;
          height: 20px;
          cursor: pointer;
        }

        span {
          font-size: 1rem;
        }

        &[disabled] {
          opacity: 0.5;
          cursor: not-allowed;
        }
      }
    }

    .no-image {
      padding: var(--spacing-xl);
      text-align: center;
    }

    .action-buttons {
      display: flex;
      gap: var(--spacing-md);
      margin-top: var(--spacing-md);

      .btn {
        flex: 1;
        padding: var(--spacing-lg);
      }
    }

    .mt-md {
      margin-top: var(--spacing-md);
    }

    .btn-sm {
      padding: var(--spacing-sm) var(--spacing-md);
      font-size: 0.875rem;
    }

    .success-message,
    .error-message {
      text-align: center;
      padding: var(--spacing-2xl);

      .success-icon,
      .error-icon {
        font-size: 3rem;
        display: block;
        margin-bottom: var(--spacing-md);
      }

      h3 {
        margin-bottom: var(--spacing-sm);
      }
    }

    .success-message {
      background: linear-gradient(135deg, rgba(17, 153, 142, 0.1) 0%, rgba(56, 239, 125, 0.1) 100%);
      border-color: rgba(56, 239, 125, 0.3);
    }

    .error-message {
      background: linear-gradient(135deg, rgba(235, 51, 73, 0.1) 0%, rgba(244, 92, 67, 0.1) 100%);
      border-color: rgba(244, 92, 67, 0.3);
    }

    .image-actions {
      display: flex;
      gap: var(--spacing-md);
    }

    .image-modal {
      max-width: 90vw;
      width: auto;
    }

    .centered {
      display: flex;
      justify-content: center;
      align-items: center;
    }

    .full-preview {
      max-width: 100%;
      max-height: 70vh;
      border-radius: var(--radius-md);
      object-fit: contain;
    }
  `]
})
export class EnrichedContentComponent {
    private readonly sanitizer = inject(DomSanitizer);

    @Input({ required: true }) thought!: ThoughtResponse;
    @Input() isProcessing = false;
    @Output() approve = new EventEmitter<ApproveThoughtRequest>();
    @Output() reject = new EventEmitter<void>();
    @Output() updateContent = new EventEmitter<ThoughtResponse>();
    @Output() reenrich = new EventEmitter<string>();

    currentStep = signal(1);
    isEditing = signal(false);
    showImageModal = signal(false);
    editableEnrichedContents: ThoughtResponse['enrichedContents'] = [];

    // Form state
    textContentComments = '';
    imageContentComments = '';
    postText = true;
    postImage = true;

    PLATFORM_CONFIG = PLATFORM_CONFIG;

    nextStep() {
        this.currentStep.set(2);
    }

    prevStep() {
        this.currentStep.set(1);
    }

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

    onResubmit() {
        this.reenrich.emit(this.textContentComments);
    }

    sanitizeUrl(url: string): SafeUrl {
        if (url && url.startsWith('data:')) {
            return this.sanitizer.bypassSecurityTrustUrl(url);
        }
        return url;
    }

    canEdit(): boolean {
        return this.thought.status !== 'POSTED' && this.thought.status !== 'POSTING' && this.currentStep() === 1;
    }

    canResubmit(): boolean {
        return this.thought.status === 'ENRICHED' || this.thought.status === 'FAILED' || this.thought.status === 'REJECTED';
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
            'REJECTED': '✕ Rejected'
        };
        return labels[status] || status;
    }
}
