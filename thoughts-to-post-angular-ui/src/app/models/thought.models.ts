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
    categoryId?: string;
    originalThought: string;
    enrichedContents: EnrichedContent[];
    generatedImageUrl?: string;
    selectedPlatforms: PlatformType[];
    status: PostStatus;
    version: number;
    createdAt: string;
    updatedAt: string;
    errorMessage?: string;
    textContentComments?: string;
    imageContentComments?: string;
    postText: boolean;
    postImage: boolean;
}

export interface CreateThoughtRequest {
    thought: string;
    categoryId?: string;
    platforms: PlatformType[];
    additionalInstructions?: string;
}

export interface ThoughtCategory {
    id?: string;
    thoughtCategory: string;
    searchDescription: string;
    modelRole: string;
    createdDateTime?: string;
    updatedDateTime?: string;
}

export interface PlatformPrompt {
    id?: string;
    platform: PlatformType;
    promptText: string;
    createdDateTime?: string;
    updatedDateTime?: string;
}

export interface ApproveThoughtRequest {
    textContentComments?: string;
    imageContentComments?: string;
    postText: boolean;
    postImage: boolean;
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
