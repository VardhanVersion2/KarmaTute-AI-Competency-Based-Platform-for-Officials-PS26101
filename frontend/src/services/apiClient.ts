import { API_BASE_URL } from './apiConfig';

export const getAuthToken = () => localStorage.getItem('karmatute_token');

export const setAuthToken = (token: string) => localStorage.setItem('karmatute_token', token);

export const clearAuthToken = () => {
  localStorage.removeItem('karmatute_token');
  localStorage.removeItem('karmatute_auth');
  localStorage.removeItem('karmatute_onboarding');
  localStorage.removeItem('karmatute_profile');
};

export const fetchWithAuth = async (endpoint: string, options: RequestInit = {}) => {
  const token = getAuthToken();
  
  const headers = {
    ...options.headers,
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
  };

  // Automatically add Content-Type if body is JSON
  if (options.body && typeof options.body === 'string' && !headers['Content-Type' as keyof typeof headers]) {
    (headers as any)['Content-Type'] = 'application/json';
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (response.status === 401 || response.status === 403) {
    // Handle unauthorized/forbidden globally (likely stale token due to backend restart)
    clearAuthToken();
    window.location.reload();
  }

  return response;
};
