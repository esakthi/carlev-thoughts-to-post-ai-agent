import { Routes } from '@angular/router';
import { ThoughtsPageComponent } from './pages/thoughts-page/thoughts-page.component';
import { LandingPageComponent } from './pages/landing-page/landing-page.component';
import { PendingPostsPageComponent } from './pages/pending-posts-page/pending-posts-page.component';
import { HistoryPageComponent } from './pages/history-page/history-page.component';
import { ViewPostPageComponent } from './pages/view-post-page/view-post-page.component';

export const routes: Routes = [
    { path: '', component: LandingPageComponent },
    {
        path: 'thoughts', children: [
            { path: 'create', component: ThoughtsPageComponent }
        ]
    },
    {
        path: 'posts', children: [
            { path: 'pending', component: PendingPostsPageComponent },
            { path: 'history', component: HistoryPageComponent },
            { path: 'view/:id', component: ViewPostPageComponent }
        ]
    },
    {
        path: 'platforms/:platform',
        loadComponent: () => import('./pages/platform-posts-page/platform-posts-page.component').then(m => m.PlatformPostsPageComponent)
    },
    {
        path: 'admin', children: [
            {
                path: 'categories',
                loadComponent: () => import('./pages/admin/categories-page/categories-page.component').then(m => m.CategoriesPageComponent)
            },
            {
                path: 'platform-prompts',
                loadComponent: () => import('./pages/admin/platform-prompts-page/platform-prompts-page.component').then(m => m.PlatformPromptsPageComponent)
            }
        ]
    },
    { path: '**', redirectTo: '' }
];
