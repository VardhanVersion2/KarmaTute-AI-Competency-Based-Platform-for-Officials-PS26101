export interface UserContext {
  id: number;
  fullName: string;
  role: string;
  department: string;
  designation: string;
  targetRole: string;
}

export interface CompetencyPulse {
  overallMastery: number;
  verifiedCount: number;
  totalTracked: number;
  resolutionProgress: number;
  pulseStatus: string;
}

export interface PriorityGap {
  competencyId: number;
  competencyName: string;
  domain: string;
  currentLevel: number;
  targetLevel: number;
  gapPercentage: number;
  priority: string;
}

export interface NextBestAction {
  id: number;
  title: string;
  description: string;
  whyThis: string;
  actionType: string;
  estimatedMinutes: number;
  actionRoute: string;
}

export interface RecentEvidence {
  id: number;
  title: string;
  category: string;
  provenance: string;
  confidenceScore: number;
  status: string;
  verifiedDateFormatted: string;
}

export interface ProgressData {
  overallProgressPercent: number;
  completedMilestones: number;
  totalMilestones: number;
  activeStreakDays: number;
}

export type CommandCenterReadState = 'loading' | 'empty' | 'success' | 'partial' | 'error' | 'provider-unavailable';

export interface CommandCenterData {
  state: CommandCenterReadState;
  stateMessage?: string;
  userContext?: UserContext;
  competencyPulse?: CompetencyPulse;
  priorityGap?: PriorityGap;
  nextBestAction?: NextBestAction;
  recentEvidence?: RecentEvidence[];
  progress?: ProgressData;
}

import { fetchWithAuth } from './apiClient';

export async function fetchCommandCenterData(stateOverride?: string): Promise<CommandCenterData> {
  const queryParam = stateOverride && stateOverride !== 'live' ? '?state=' + encodeURIComponent(stateOverride) : '';
  
  try {
    const res = await fetchWithAuth(`/api/v1/me/command-center${queryParam}`, {
      headers: { 'Accept': 'application/json' },
    });
    if (res.ok) {
      return await res.json();
    }
  } catch (e) {
    console.error('Command center fetch failed:', e);
  }

  // Graceful degradation
  return {
    state: 'provider-unavailable',
    stateMessage: 'Backend API connection offline or Unauthorized. Please ensure you are logged in.',
  };
}
