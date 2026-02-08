/**
 * Type definitions for the Thoughts API
 */

export type PlatformType = 'LINKEDIN' | 'FACEBOOK' | 'INSTAGRAM';

export type PostStatus =
    | 'PENDING'
    | 'PROCESSING'
    | 'ENRICHED'
    | 'APPROVED'
    | 'POSTING'
    | 'POSTED'
    | 'FAILED'
    | 'REJECTED';

export interface EnrichedContent {
    platform: PlatformType;
    title?: string;
    body: string;
    hashtags: string[];
    callToAction?: string;
    characterCount: number;
}

export interface ThoughtResponse {
    id: string;
    userId: string;
    originalThought: string;
    enrichedContents: EnrichedContent[];
    generatedImageUrl?: string;
    selectedPlatforms: PlatformType[];
    status: PostStatus;
    version: number;
    createdAt: string;
    updatedAt: string;
    errorMessage?: string;
}

export interface CreateThoughtRequest {
    thought: string;
    platforms: PlatformType[];
    additionalInstructions?: string;
}

export interface ThoughtHistory {
    id: string;
    thoughtsToPostId: string;
    version: number;
    actionType: string;
    performedBy: string;
    createdAt: string;
    status: PostStatus;
}

export const PLATFORM_CONFIG: Record<PlatformType, { label: string; icon: string; color: string }> = {
    LINKEDIN: { label: 'LinkedIn', icon: 'in', color: '#0077b5' },
    FACEBOOK: { label: 'Facebook', icon: 'f', color: '#1877f2' },
    INSTAGRAM: { label: 'Instagram', icon: '📷', color: '#e1306c' }
};
