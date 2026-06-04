import type { TaskPlan } from '../types/task';
import type { Skill } from '../types/skill';

const BASE_URL = '/api';

type ApiResponse<T> = { success: boolean; data: T; error: string | null };

/** Paginated list response wrapper */
type ListResponse<T> = { items: T[]; total: number };

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

/** Raw agent spec from API (no UI runtime fields) */
export interface AgentSpecRaw {
  id: string;
  name: string;
  role: string;
  baseUrl?: string;
  model?: string;
  apiKey?: string;
  tools?: string;
  maxSteps?: number;
  outputFormat?: string;
  enabled?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export const agentApi = {
  list: () =>
    api.get<ApiResponse<ListResponse<AgentSpecRaw>>>('/agents'),
  get: (id: string) => api.get<ApiResponse<AgentSpecRaw>>(`/agents/${id}`),
  create: (spec: Record<string, unknown>) =>
    api.post<ApiResponse<AgentSpecRaw>>('/agents', spec),
  update: (id: string, spec: Record<string, unknown>) =>
    api.put<ApiResponse<AgentSpecRaw>>(`/agents/${id}`, spec),
  delete: (id: string) => api.delete<ApiResponse<void>>(`/agents/${id}`),
  test: (id: string, input: string) =>
    api.post<ApiResponse<{ agentId: string; agentName: string; input: string; output: string; subtaskId: string }>>(
      `/agents/${id}/test`,
      { input },
    ),
};

export const taskApi = {
  list: () => api.get<ApiResponse<ListResponse<TaskPlan>>>('/tasks'),
  get: (id: string) => api.get<ApiResponse<TaskPlan>>(`/tasks/${id}`),
  create: (description: string) => api.post<ApiResponse<TaskPlan>>('/tasks', { description }),
  start: (id: string) => api.post<ApiResponse<TaskPlan>>(`/tasks/${id}/start`, {}),
  cancel: (id: string) => api.post<ApiResponse<void>>(`/tasks/${id}/cancel`, {}),
  deleteTask: (id: string) => api.delete<ApiResponse<{id: string; status: string}>>(`/tasks/${id}`),
  retryTask: (id: string) => api.post<ApiResponse<{id: string; status: string; retriedCount: number}>>(`/tasks/${id}/retry`, {}),
  progress: (id: string) => api.get<ApiResponse<unknown>>(`/tasks/${id}/progress`),
};

export const skillApi = {
  list: () => api.get<ApiResponse<ListResponse<Skill>>>('/skills'),
  get: (id: string) => api.get<ApiResponse<Skill>>(`/skills/${id}`),
  create: (spec: unknown) => api.post<ApiResponse<Skill>>('/skills', spec),
  update: (id: string, spec: unknown) => api.put<ApiResponse<Skill>>(`/skills/${id}`, spec),
  delete: (id: string) => api.delete<ApiResponse<void>>(`/skills/${id}`),
};

export const templateApi = {
  list: () => api.get<ApiResponse<ListResponse<unknown>>>('/templates'),
  get: (id: string) => api.get<ApiResponse<unknown>>(`/templates/${id}`),
  create: (spec: unknown) => api.post<ApiResponse<unknown>>('/templates', spec),
  update: (id: string, spec: unknown) => api.put<ApiResponse<unknown>>(`/templates/${id}`, spec),
  delete: (id: string) => api.delete<ApiResponse<void>>(`/templates/${id}`),
};

export const toolApi = {
  list: () => api.get<ApiResponse<ListResponse<unknown>>>('/tools'),
  invoke: (name: string, params: Record<string, unknown>) =>
    api.post<ApiResponse<unknown>>(`/tools/${name}/invoke`, params),
};