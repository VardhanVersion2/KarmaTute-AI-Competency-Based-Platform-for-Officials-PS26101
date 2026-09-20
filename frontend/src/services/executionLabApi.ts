import { fetchWithAuth } from './apiClient';

// --- Material Types ---
export interface MaterialUploadResponse {
  materialId: number;
  filename: string;
  status: 'PENDING' | 'READY' | 'FAILED';
  detectedTopic: string;
  pageCount: number;
  error: string;
}

// --- Session Types ---
export interface SessionQuestion {
  questionId: number;
  questionText: string;
  topic: string;
  subtopic: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  options: string[];
}

export interface StartSessionResponse {
  sessionId: number;
  state: string;
  topic: string;
  materialName: string;
  questionCount: number;
  questions: SessionQuestion[];
}

export interface L1SubmitResponse {
  sessionId: number;
  score: number;
  correct: number;
  total: number;
  passed: boolean;
  state: string;
  topic: string;
}

export interface L2StartResponse {
  sessionId: number;
  state: string;
  topic: string;
  subtopic: string;
  question: string;
  questionId: number;
  materialName: string;
  durationMinutes: number;
  assessmentType: string;
}

export interface L2SubmitResponse {
  sessionId: number;
  score: number;
  passed: boolean;
  state: string;
  ocrConfidence: number;
  feedback: string;
  topic: string;
  message?: string;
}

export interface L3StartResponse {
  sessionId: number;
  state: string;
  topic: string;
  subtopic: string;
  question: string;
  questionId: number;
  materialName: string;
  assessmentType: string;
}

export interface L3SubmitResponse {
  sessionId: number;
  score: number;
  state: string;
  feedback: string;
  topic: string;
  l1Score: number;
  l2Score: number;
  l3Score: number;
  overallScore: number;
}

export interface SessionStatus {
  sessionId: number;
  state: string;
  topic: string;
  materialName: string;
  materialId: number;
  l1Score: number | null;
  l2Score: number | null;
  l3Score: number | null;
  l2OcrConfidence: number | null;
  l2NeedsReview: boolean | null;
  startedAt: string;
  completedAt: string | null;
}

// --- Material API ---

export async function uploadMaterial(file: File): Promise<MaterialUploadResponse> {
  const formData = new FormData();
  formData.append('file', file);
  const res = await fetchWithAuth('/api/assessment-material/upload', {
    method: 'POST',
    body: formData,
    // Do NOT set Content-Type for FormData — browser sets it automatically with boundary
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || 'Material upload failed');
  return data;
}

export async function getMyMaterials(): Promise<MaterialUploadResponse[]> {
  const res = await fetchWithAuth('/api/assessment-material/my');
  if (!res.ok) throw new Error('Failed to fetch materials');
  return res.json();
}

// --- Session API ---

export async function startLevel1Session(materialId: number): Promise<StartSessionResponse> {
  const res = await fetchWithAuth('/api/assessment-session/start', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ materialId }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || 'Failed to start session');
  return data;
}

export async function submitLevel1(sessionId: number, answers: Record<number, string>): Promise<L1SubmitResponse> {
  const res = await fetchWithAuth(`/api/assessment-session/${sessionId}/l1/submit`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(answers),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || 'Failed to submit Level 1');
  return data;
}

export async function startLevel2(sessionId: number): Promise<L2StartResponse> {
  const res = await fetchWithAuth(`/api/assessment-session/${sessionId}/l2/start`, {
    method: 'POST',
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || 'Failed to start Level 2');
  return data;
}

export async function submitLevel2(sessionId: number, answerFile: File): Promise<L2SubmitResponse> {
  const formData = new FormData();
  formData.append('answerFile', answerFile);
  const res = await fetchWithAuth(`/api/assessment-session/${sessionId}/l2/submit`, {
    method: 'POST',
    body: formData,
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || 'Failed to submit Level 2');
  return data;
}

export async function startLevel3(sessionId: number): Promise<L3StartResponse> {
  const res = await fetchWithAuth(`/api/assessment-session/${sessionId}/l3/start`, {
    method: 'POST',
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || 'Failed to start Level 3');
  return data;
}


export async function demoSubmitLevel2(sessionId: number): Promise<L2SubmitResponse> {
  const res = await fetchWithAuth('/api/assessment-session/' + sessionId + '/l2/demo-submit', {
    method: 'POST',
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || 'Failed to demo submit Level 2');
  return data;
}

export async function submitLevel3(sessionId: number, answerText: string): Promise<L3SubmitResponse> {
  const res = await fetchWithAuth(`/api/assessment-session/${sessionId}/l3/submit`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ answerText }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || 'Failed to submit Level 3');
  return data;
}

export async function getCurrentSession(): Promise<SessionStatus | null> {
  const res = await fetchWithAuth('/api/assessment-session/current');
  if (res.status === 204) return null;
  if (!res.ok) throw new Error('Failed to fetch current session');
  return res.json();
}

export async function getSessionStatus(sessionId: number): Promise<SessionStatus> {
  const res = await fetchWithAuth(`/api/assessment-session/${sessionId}`);
  if (!res.ok) throw new Error('Failed to fetch session status');
  return res.json();
}

export async function demoPassSession(sessionId: number): Promise<{ state: string, overallScore: number }> {
  const res = await fetchWithAuth('/api/assessment-session/' + sessionId + '/demo-pass', {
    method: 'POST'
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || 'Failed to demo pass');
  return data;
}
