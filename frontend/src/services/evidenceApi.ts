import { type EvidenceType } from '../types/competency';

export interface EvidenceRecord {
  id?: number;
  userId: number;
  competencyId: number;
  type: EvidenceType;
  source: string;
  sourceRef?: string;
  score: number;
  normalizedScore: number;
  confidence: number;
  provenance: string;
  reviewStatus?: string;
  timestamp?: string;
  engineVersion?: string;
  ruleVersion?: string;
}

export interface CompetencySnapshot {
  id: number;
  competencyId: number;
  current: number;
  target: number;
  gap: number;
  confidence: number;
  evidenceIds: string;
  engineVersion: string;
  ruleVersion: string;
  timestamp: string;
}

import { fetchWithAuth } from './apiClient';

const API = `/api/evidence`;

async function api<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetchWithAuth(url, {
    ...options,
    headers: { 'Content-Type': 'application/json', 'Accept': 'application/json', ...options?.headers },
  });
  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText);
    throw new Error(`API error ${res.status}: ${text}`);
  }
  return res.json();
}

export async function submitEvidence(record: EvidenceRecord): Promise<EvidenceRecord> {
  return api<EvidenceRecord>(API, { method: 'POST', body: JSON.stringify(record) });
}

export async function getUserEvidence(): Promise<EvidenceRecord[]> {
  return api<EvidenceRecord[]>(`${API}/me`);
}

export async function getCompetencySnapshots(): Promise<CompetencySnapshot[]> {
  return api<CompetencySnapshot[]>(`${API}/me/competency-snapshot`);
}

export async function getCompetencyHistory(competencyId: number): Promise<CompetencySnapshot[]> {
  return api<CompetencySnapshot[]>(`${API}/me/competency-snapshot/competency/${competencyId}`);
}
