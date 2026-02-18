import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject, interval, switchMap, takeWhile, tap } from 'rxjs';
import {
    ThoughtResponse,
    CreateThoughtRequest,
    ApproveThoughtRequest,
    ThoughtHistory
} from '../models/thought.models';

@Injectable({
    providedIn: 'root'
})
export class ThoughtsService {
    private readonly http = inject(HttpClient);
    private readonly apiUrl = 'http://localhost:8080/api/thoughts';
    private readonly oauthUrl = 'http://localhost:8080/api/oauth';
    private readonly adminUrl = 'http://localhost:8080/api/admin';

    /**
     * Get thoughts by platform
     */
    getThoughtsByPlatform(platform: string): Observable<ThoughtResponse[]> {
        return this.http.get<ThoughtResponse[]>(`${this.apiUrl}?platform=${platform.toUpperCase()}`);
    }

    /**
     * Create a new thought and send for AI enrichment
     */
    createThought(request: CreateThoughtRequest): Observable<ThoughtResponse> {
        return this.http.post<ThoughtResponse>(this.apiUrl, request);
    }

    /**
     * Get a thought by ID
     */
    getThought(id: string): Observable<ThoughtResponse> {
        return this.http.get<ThoughtResponse>(`${this.apiUrl}/${id}`);
    }

    /**
     * Get all thoughts for the current user
     */
    getUserThoughts(): Observable<ThoughtResponse[]> {
        return this.http.get<ThoughtResponse[]>(this.apiUrl);
    }

    /**
     * Get thoughts by status
     */
    getThoughtsByStatus(status: string): Observable<ThoughtResponse[]> {
        return this.http.get<ThoughtResponse[]>(`${this.apiUrl}?status=${status}`);
    }

    /**
     * Get thoughts excluding a status
     */
    getThoughtsExcludingStatus(notStatus: string): Observable<ThoughtResponse[]> {
        return this.http.get<ThoughtResponse[]>(`${this.apiUrl}?notStatus=${notStatus}`);
    }

    /**
     * Get history for a thought
     */
    getThoughtHistory(id: string): Observable<ThoughtHistory[]> {
        return this.http.get<ThoughtHistory[]>(`${this.apiUrl}/${id}/history`);
    }

    /**
     * Approve a thought and post to social media
     */
    approveAndPost(id: string, request: ApproveThoughtRequest): Observable<ThoughtResponse> {
        return this.http.post<ThoughtResponse>(`${this.apiUrl}/${id}/approve`, request);
    }

    /**
     * Reject a thought
     */
    rejectThought(id: string): Observable<ThoughtResponse> {
        return this.http.post<ThoughtResponse>(`${this.apiUrl}/${id}/reject`, {});
    }

    /**
     * Update enriched content of a thought
     */
    updateThought(id: string, thought: ThoughtResponse): Observable<ThoughtResponse> {
        return this.http.put<ThoughtResponse>(`${this.apiUrl}/${id}`, thought);
    }

    /**
     * Resubmit a thought for re-enrichment
     */
    reenrichThought(id: string, additionalInstructions: string): Observable<ThoughtResponse> {
        return this.http.post<ThoughtResponse>(`${this.apiUrl}/${id}/re-enrich`, { additionalInstructions });
    }

    /**
     * Delete a thought
     */
    deleteThought(id: string): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${id}`);
    }

    /**
     * Repost a thought
     */
    repostThought(id: string): Observable<ThoughtResponse> {
        return this.http.post<ThoughtResponse>(`${this.apiUrl}/${id}/repost`, {});
    }

    /**
     * Poll for thought status updates until it's no longer processing
     */
    pollForUpdates(id: string, intervalMs = 2000): Observable<ThoughtResponse> {
        return interval(intervalMs).pipe(
            switchMap(() => this.getThought(id)),
            takeWhile(thought =>
                thought.status === 'PENDING' || thought.status === 'PROCESSING',
                true
            )
        );
    }

    /**
     * Get LinkedIn authorization status
     */
    getLinkedInStatus(): Observable<{ authorized: boolean }> {
        return this.http.get<{ authorized: boolean }>(`${this.oauthUrl}/linkedin/status`);
    }

    /**
     * Get LinkedIn authorization URL
     */
    getLinkedInAuthUrl(): Observable<{ authorizationUrl: string; state: string }> {
        return this.http.get<{ authorizationUrl: string; state: string }>(`${this.oauthUrl}/linkedin/authorize`);
    }

    // Admin API - Thought Categories
    getCategories(): Observable<any[]> {
        return this.http.get<any[]>(`${this.adminUrl}/categories`);
    }

    createCategory(category: any): Observable<any> {
        return this.http.post<any>(`${this.adminUrl}/categories`, category);
    }

    updateCategory(id: string, category: any): Observable<any> {
        return this.http.put<any>(`${this.adminUrl}/categories/${id}`, category);
    }

    deleteCategory(id: string): Observable<any> {
        return this.http.delete<any>(`${this.adminUrl}/categories/${id}`);
    }

    // Admin API - Platform Prompts
    getPlatformPrompts(): Observable<any[]> {
        return this.http.get<any[]>(`${this.adminUrl}/platform-prompts`);
    }

    createPlatformPrompt(prompt: any): Observable<any> {
        return this.http.post<any>(`${this.adminUrl}/platform-prompts`, prompt);
    }

    updatePlatformPrompt(id: string, prompt: any): Observable<any> {
        return this.http.put<any>(`${this.adminUrl}/platform-prompts/${id}`, prompt);
    }

    deletePlatformPrompt(id: string): Observable<any> {
        return this.http.delete<any>(`${this.adminUrl}/platform-prompts/${id}`);
    }
}
