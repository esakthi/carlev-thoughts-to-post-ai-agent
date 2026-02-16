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

    // User ID header - in production, this would come from auth service
    private readonly userId = 'user-123';

    private get headers(): HttpHeaders {
        return new HttpHeaders({
            'Content-Type': 'application/json',
            'X-User-Id': this.userId
        });
    }

    /**
     * Get thoughts by platform
     */
    getThoughtsByPlatform(platform: string): Observable<ThoughtResponse[]> {
        return this.http.get<ThoughtResponse[]>(`${this.apiUrl}?platform=${platform.toUpperCase()}`, { headers: this.headers });
    }

    /**
     * Create a new thought and send for AI enrichment
     */
    createThought(request: CreateThoughtRequest): Observable<ThoughtResponse> {
        return this.http.post<ThoughtResponse>(this.apiUrl, request, { headers: this.headers });
    }

    /**
     * Get a thought by ID
     */
    getThought(id: string): Observable<ThoughtResponse> {
        return this.http.get<ThoughtResponse>(`${this.apiUrl}/${id}`, { headers: this.headers });
    }

    /**
     * Get all thoughts for the current user
     */
    getUserThoughts(): Observable<ThoughtResponse[]> {
        return this.http.get<ThoughtResponse[]>(this.apiUrl, { headers: this.headers });
    }

    /**
     * Get thoughts by status
     */
    getThoughtsByStatus(status: string): Observable<ThoughtResponse[]> {
        return this.http.get<ThoughtResponse[]>(`${this.apiUrl}?status=${status}`, { headers: this.headers });
    }

    /**
     * Get thoughts excluding a status
     */
    getThoughtsExcludingStatus(notStatus: string): Observable<ThoughtResponse[]> {
        return this.http.get<ThoughtResponse[]>(`${this.apiUrl}?notStatus=${notStatus}`, { headers: this.headers });
    }

    /**
     * Get history for a thought
     */
    getThoughtHistory(id: string): Observable<ThoughtHistory[]> {
        return this.http.get<ThoughtHistory[]>(`${this.apiUrl}/${id}/history`, { headers: this.headers });
    }

    /**
     * Approve a thought and post to social media
     */
    approveAndPost(id: string, request: ApproveThoughtRequest): Observable<ThoughtResponse> {
        return this.http.post<ThoughtResponse>(`${this.apiUrl}/${id}/approve`, request, { headers: this.headers });
    }

    /**
     * Reject a thought
     */
    rejectThought(id: string): Observable<ThoughtResponse> {
        return this.http.post<ThoughtResponse>(`${this.apiUrl}/${id}/reject`, {}, { headers: this.headers });
    }

    /**
     * Update enriched content of a thought
     */
    updateThought(id: string, thought: ThoughtResponse): Observable<ThoughtResponse> {
        return this.http.put<ThoughtResponse>(`${this.apiUrl}/${id}`, thought, { headers: this.headers });
    }

    /**
     * Resubmit a thought for re-enrichment
     */
    reenrichThought(id: string, additionalInstructions: string): Observable<ThoughtResponse> {
        return this.http.post<ThoughtResponse>(`${this.apiUrl}/${id}/re-enrich`, { additionalInstructions }, { headers: this.headers });
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
        return this.http.get<{ authorized: boolean }>(`${this.oauthUrl}/linkedin/status`, { headers: this.headers });
    }

    /**
     * Get LinkedIn authorization URL
     */
    getLinkedInAuthUrl(): Observable<{ authorizationUrl: string; state: string }> {
        return this.http.get<{ authorizationUrl: string; state: string }>(`${this.oauthUrl}/linkedin/authorize`, { headers: this.headers });
    }

    // Admin API - Thought Categories
    getCategories(): Observable<any[]> {
        return this.http.get<any[]>(`${this.adminUrl}/categories`, { headers: this.headers });
    }

    createCategory(category: any): Observable<any> {
        return this.http.post<any>(`${this.adminUrl}/categories`, category, { headers: this.headers });
    }

    updateCategory(id: string, category: any): Observable<any> {
        return this.http.put<any>(`${this.adminUrl}/categories/${id}`, category, { headers: this.headers });
    }

    deleteCategory(id: string): Observable<any> {
        return this.http.delete<any>(`${this.adminUrl}/categories/${id}`, { headers: this.headers });
    }

    // Admin API - Platform Prompts
    getPlatformPrompts(): Observable<any[]> {
        return this.http.get<any[]>(`${this.adminUrl}/platform-prompts`, { headers: this.headers });
    }

    createPlatformPrompt(prompt: any): Observable<any> {
        return this.http.post<any>(`${this.adminUrl}/platform-prompts`, prompt, { headers: this.headers });
    }

    updatePlatformPrompt(id: string, prompt: any): Observable<any> {
        return this.http.put<any>(`${this.adminUrl}/platform-prompts/${id}`, prompt, { headers: this.headers });
    }
}
