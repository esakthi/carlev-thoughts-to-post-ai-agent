import { Routes } from '@angular/router';
import { ThoughtsPageComponent } from './pages/thoughts-page/thoughts-page.component';

export const routes: Routes = [
    { path: '', component: ThoughtsPageComponent },
    { path: '**', redirectTo: '' }
];
