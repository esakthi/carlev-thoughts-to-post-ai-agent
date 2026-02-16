import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { ThoughtsService } from '../../services/thoughts.service';
import { PostListComponent } from '../../components/post-list/post-list.component';
import { ThoughtResponse, PLATFORM_CONFIG, PlatformType } from '../../models/thought.models';

@Component({
  selector: 'app-platform-posts-page',
  standalone: true,
  imports: [CommonModule, PostListComponent, RouterModule],
  template: `
    <div class="container">
      <div class="page-header fade-in">
        <div class="header-content">
          <div class="platform-icon-large" [ngClass]="platform()?.toLowerCase() || ''">
            @if (platform() === 'LINKEDIN') { <span>in</span> }
            @else if (platform() === 'FACEBOOK') { <span>f</span> }
            @else if (platform() === 'INSTAGRAM') { <span>ig</span> }
          </div>
          <h1>{{ platform() ? PLATFORM_CONFIG[platform()!].label : '' }} Posts</h1>
        </div>
        <p class="text-secondary">
          Manage and track all your thoughts and posts specifically for {{ platform() ? PLATFORM_CONFIG[platform()!].label : '' }}
        </p>
      </div>

      @if (isLoading()) {
        <div class="loading-state">
          <span class="spinner"></span>
          <p>Loading posts...</p>
        </div>
      } @else if (posts().length > 0) {
        <div class="posts-section fade-in">
          <app-post-list [thoughts]="posts()" />
        </div>
      } @else {
        <div class="empty-state card-glass fade-in">
          <p>No {{ platform() ? PLATFORM_CONFIG[platform()!].label : '' }} posts found yet.</p>
          <a routerLink="/thoughts/create" class="btn btn-primary mt-lg">Create Your First Post</a>
        </div>
      }
    </div>
  `,
  styles: [`
    .page-header {
      text-align: center;
      margin-bottom: var(--spacing-2xl);
    }
    .header-content {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: var(--spacing-md);
      margin-bottom: var(--spacing-sm);
    }
    .platform-icon-large {
      width: 48px;
      height: 48px;
      border-radius: var(--radius-md);
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: bold;
      font-size: 1.5rem;
      color: white;
    }
    .linkedin { background: #0077b5; }
    .facebook { background: #1877f2; }
    .instagram { background: linear-gradient(45deg, #f09433 0%, #e6683c 25%, #dc2743 50%, #cc2366 75%, #bc1888 100%); }
    .loading-state, .empty-state {
      text-align: center;
      padding: var(--spacing-3xl);
    }
  `]
})
export class PlatformPostsPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly thoughtsService = inject(ThoughtsService);

  platform = signal<PlatformType | null>(null);
  posts = signal<ThoughtResponse[]>([]);
  isLoading = signal(true);
  PLATFORM_CONFIG = PLATFORM_CONFIG as any;

  ngOnInit() {
    this.route.params.subscribe(params => {
      const platformParam = params['platform']?.toUpperCase() as PlatformType;
      if (platformParam) {
        this.platform.set(platformParam);
        this.loadPosts(platformParam);
      }
    });
  }

  loadPosts(platform: PlatformType) {
    this.isLoading.set(true);
    this.thoughtsService.getThoughtsByPlatform(platform).subscribe({
      next: (posts) => {
        this.posts.set(posts);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load platform posts', err);
        this.isLoading.set(false);
      }
    });
  }
}
