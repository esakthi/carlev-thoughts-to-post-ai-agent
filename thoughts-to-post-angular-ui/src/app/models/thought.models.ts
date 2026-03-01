/**
 * Type definitions for the Thoughts API
 */

export type PlatformType = 'LINKEDIN' | 'FACEBOOK' | 'INSTAGRAM';

export type PromptType = 'TEXT' | 'IMAGE' | 'VIDEO' | 'OTHERS';

export type PostStatus =
    | 'PENDING'
    | 'PROCESSING'
    | 'ENRICHED'
    | 'APPROVED'
    | 'POSTING'
    | 'POSTED'
    | 'FAILED'
    | 'REJECTED'
    | 'PARTIALLY_COMPLETED';

export interface GeneratedImage {
    id: string;
    url: string;
    prompt: string;
    format: string;
    width: number;
    height: number;
    selected: boolean;
    tag?: string;
    createdAt: string;
}

export interface GeneratedVideo {
    id: string;
    videoUrl: string;
    promptUsed: string;
    cloudProvider?: string;
    status: string;
    createdAt: string;
}

export interface EnrichedContent {
    platform: PlatformType;
    title?: string;
    body: string;
    hashtags: string[];
    callToAction?: string;
    characterCount: number;
    images?: GeneratedImage[];
    videos?: GeneratedVideo[];
}

export interface ThoughtResponse {
    id: string;
    userId: string;
    categoryId?: string;
    originalThought: string;
    enrichedContents: EnrichedContent[];
    generatedImageUrl?: string;
    selectedPlatforms: PlatformType[];
    additionalInstructions?: string;
    platformSelections?: PlatformSelection[];
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

export interface GenerationParameters {
    resolution?: string;
    steps?: number;
    cfgScale?: number;
    seed?: number;
    modelType?: string;
    sampler?: string;
    batchSize?: number;
    asyncMode?: boolean;
    duration?: number;
    fps?: number;
}

export interface PlatformSelection {
    platform: PlatformType;
    presetId?: string;
    imagePresetId?: string;
    videoPresetId?: string;
    additionalContext?: string;
    imageParams?: GenerationParameters;
    videoParams?: GenerationParameters;
}

export interface CreateThoughtRequest {
    thought: string;
    categoryId?: string;
    platforms: PlatformType[];
    additionalInstructions?: string;
    platformConfigs?: PlatformSelection[];
}

export interface ThoughtCategory {
    id?: string;
    thoughtCategory: string;
    type: PromptType;
    searchDescription: string;
    modelRole: string;
    createdDateTime?: string;
    updatedDateTime?: string;
}

export interface PlatformPrompt {
    id?: string;
    name: string;
    description: string;
    platform: PlatformType;
    type: PromptType;
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
