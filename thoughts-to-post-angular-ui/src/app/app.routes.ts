import { Routes } from '@angular/router';
import { ThoughtsPageComponent } from './pages/thoughts-page/thoughts-page.component';
import { LandingPageComponent } from './pages/landing-page/landing-page.component';
import { PendingPostsPageComponent } from './pages/pending-posts-page/pending-posts-page.component';
import { HistoryPageComponent } from './pages/history-page/history-page.component';
import { ViewPostPageComponent } from './pages/view-post-page/view-post-page.component';

export const routes: Routes = [
    { path: '', component: LandingPageComponent },
    { path: 'create', component: ThoughtsPageComponent },
    { path: 'pending', component: PendingPostsPageComponent },
    { path: 'history', component: HistoryPageComponent },
    { path: 'view/:id', component: ViewPostPageComponent },
    { path: '**', redirectTo: '' }
];
