import { Routes } from '@angular/router';
import { ThoughtsPageComponent } from './pages/thoughts-page/thoughts-page.component';
import { LandingPageComponent } from './pages/landing-page/landing-page.component';
import { PendingPostsPageComponent } from './pages/pending-posts-page/pending-posts-page.component';
import { HistoryPageComponent } from './pages/history-page/history-page.component';
import { ViewPostPageComponent } from './pages/view-post-page/view-post-page.component';
import { AdminCategoriesComponent } from './pages/admin-categories/admin-categories.component';
import { ThoughtCollectionComponent } from './pages/thought-collection/thought-collection.component';

export const routes: Routes = [
    { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    { path: 'dashboard', component: LandingPageComponent },
    {
        path: 'thoughts', children: [
            { path: 'create', component: ThoughtsPageComponent },
            { path: 'collection', component: ThoughtCollectionComponent }
        ]
    },
    {
        path: 'posts', children: [
            { path: 'pending', component: PendingPostsPageComponent },
            { path: 'history', component: HistoryPageComponent },
            { path: 'view/:id', component: ViewPostPageComponent }
        ]
    },
    { path: 'admin/categories', component: AdminCategoriesComponent },
    { path: '**', redirectTo: 'dashboard' }
];
