import { Routes } from '@angular/router';
import { ThoughtsPageComponent } from './pages/thoughts-page/thoughts-page.component';
import { LandingPageComponent } from './pages/landing-page/landing-page.component';
import { PendingPostsPageComponent } from './pages/pending-posts-page/pending-posts-page.component';
import { HistoryPageComponent } from './pages/history-page/history-page.component';
import { ViewPostPageComponent } from './pages/view-post-page/view-post-page.component';
import { LoginPageComponent } from './pages/login-page/login-page.component';
import { RegisterPageComponent } from './pages/register-page/register-page.component';
import { authGuard } from './services/auth.guard';

export const routes: Routes = [
    { path: 'login', component: LoginPageComponent },
    { path: 'register', component: RegisterPageComponent },
    { path: '', component: LandingPageComponent, canActivate: [authGuard] },
    {
        path: 'thoughts', canActivate: [authGuard], children: [
            { path: 'create', component: ThoughtsPageComponent }
        ]
    },
    {
        path: 'posts', canActivate: [authGuard], children: [
            { path: 'pending', component: PendingPostsPageComponent },
            { path: 'history', component: HistoryPageComponent },
            { path: 'view/:id', component: ViewPostPageComponent }
        ]
    },
    {
        path: 'platforms/:platform',
        canActivate: [authGuard],
        loadComponent: () => import('./pages/platform-posts-page/platform-posts-page.component').then(m => m.PlatformPostsPageComponent)
    },
    {
        path: 'admin', canActivate: [authGuard], children: [
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
    { path: '**', redirectTo: 'login' }
];
