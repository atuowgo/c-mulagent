const BASE_URL = '/api';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`API ${res.status}: ${text}`);
  }
  return res.json();
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'POST', body: JSON.stringify(body) }),
  put: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'PUT', body: JSON.stringify(body) }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
};

// Agent API
export const agentApi = {
  list: () => api.get<any[]>('/agents'),
  get: (id: string) => api.get<any>(`/agents/${id}`),
  create: (spec: any) => api.post<any>('/agents', spec),
  delete: (id: string) => api.delete<void>(`/agents/${id}`),
};

// Task API
export const taskApi = {
  list: () => api.get<any[]>('/tasks'),
  get: (id: string) => api.get<any>(`/tasks/${id}`),
  create: (input: any) => api.post<any>('/tasks', input),
  cancel: (id: string) => api.post<void>(`/tasks/${id}/cancel`, {}),
};

// Skill API
export const skillApi = {
  list: () => api.get<any[]>('/skills'),
  import: (spec: any) => api.post<any>('/skills/import', spec),
};