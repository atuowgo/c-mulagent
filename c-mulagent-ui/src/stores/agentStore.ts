import { create } from 'zustand';
import type { AgentSpec } from '../types/agent';
import { AgentState } from '../types/agent';
import { agentApi } from '../api/client';

function parseTools(raw: unknown): string[] | undefined {
  if (!raw) return undefined;
  if (Array.isArray(raw)) return raw as string[];
  if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw);
      // handle double-encoded JSON: parsed may still be a string
      return Array.isArray(parsed) ? parsed : (typeof parsed === 'string' ? JSON.parse(parsed) : []);
    } catch { return []; }
  }
  return undefined;
}

function serializeTools(tools: unknown): string | undefined {
  if (!tools) return undefined;
  if (typeof tools === 'string') {
    // verify it is valid JSON; if so, return as-is
    try { JSON.parse(tools); return tools; } catch { return JSON.stringify(tools); }
  }
  return JSON.stringify(tools);
}

function toAgentSpec(raw: Record<string, unknown>): AgentSpec {
  return {
    id: raw.id as string,
    name: raw.name as string,
    role: raw.role as string,
    baseUrl: raw.baseUrl as string | undefined,
    model: (raw.model as string) || '',
    apiKey: raw.apiKey as string | undefined,
    tools: parseTools(raw.tools),
    maxSteps: raw.maxSteps as number | undefined,
    outputFormat: raw.outputFormat as string | undefined,
    enabled: raw.enabled as boolean | undefined,
    state: AgentState.Idle,
    progress: 0,
  };
}

function toApiPayload(agent: Partial<AgentSpec>): Record<string, unknown> {
  const payload: Record<string, unknown> = {};
  if (agent.name !== undefined) payload.name = agent.name;
  if (agent.role !== undefined) payload.role = agent.role;
  if (agent.baseUrl !== undefined) payload.baseUrl = agent.baseUrl;
  if (agent.model !== undefined) payload.model = agent.model;
  if (agent.apiKey !== undefined) payload.apiKey = agent.apiKey;
  if (agent.tools !== undefined) payload.tools = serializeTools(agent.tools);
  if (agent.maxSteps !== undefined) payload.maxSteps = agent.maxSteps;
  if (agent.outputFormat !== undefined) payload.outputFormat = agent.outputFormat;
  if (agent.enabled !== undefined) payload.enabled = agent.enabled;
  return payload;
}

interface AgentStore {
  agents: AgentSpec[];
  loading: boolean;
  error: string | null;
  selectedAgentId: string | null;
  fetchAgents: () => Promise<void>;
  addAgent: (agent: Partial<AgentSpec>) => Promise<void>;
  updateAgent: (id: string, updates: Partial<AgentSpec>) => Promise<void>;
  removeAgent: (id: string) => Promise<void>;
  testAgent: (id: string, input: string) => Promise<string>;
  updateAgentState: (id: string, state: AgentState, progress: number) => void;
  selectAgent: (id: string | null) => void;
}

export const useAgentStore = create<AgentStore>((set, get) => ({
  agents: [],
  loading: false,
  error: null,
  selectedAgentId: null,

  fetchAgents: async () => {
    set({ loading: true, error: null });
    try {
      const res = await agentApi.list();
      if (res.success && res.data) {
        const agents = (res.data.items || []).map((r) => toAgentSpec(r as unknown as Record<string, unknown>));
        set({ agents, loading: false });
      } else {
        set({ error: res.error || 'Failed to fetch agents', loading: false });
      }
    } catch (e) {
      set({ error: (e as Error).message, loading: false });
    }
  },

  addAgent: async (agent) => {
    set({ error: null });
    try {
      const payload = toApiPayload(agent);
      const res = await agentApi.create(payload);
      if (res.success && res.data) {
        const newAgent = toAgentSpec(res.data as unknown as Record<string, unknown>);
        set((s) => ({ agents: [...s.agents, newAgent] }));
      } else {
        set({ error: res.error || 'Failed to create agent' });
      }
    } catch (e) {
      set({ error: (e as Error).message });
    }
  },

  updateAgent: async (id, updates) => {
    set({ error: null });
    try {
      const payload = toApiPayload(updates);
      const res = await agentApi.update(id, payload);
      if (res.success && res.data) {
        const updated = toAgentSpec(res.data as unknown as Record<string, unknown>);
        set((s) => ({
          agents: s.agents.map((a) => {
            if (a.id === id) {
              return { ...updated, state: a.state, progress: a.progress };
            }
            return a;
          }),
        }));
      } else {
        set({ error: res.error || 'Failed to update agent' });
      }
    } catch (e) {
      set({ error: (e as Error).message });
    }
  },

  removeAgent: async (id) => {
    set({ error: null });
    try {
      const res = await agentApi.delete(id);
      if (res.success) {
        set((s) => ({
          agents: s.agents.filter((a) => a.id !== id),
          selectedAgentId: s.selectedAgentId === id ? null : s.selectedAgentId,
        }));
      } else {
        set({ error: res.error || 'Failed to delete agent' });
      }
    } catch (e) {
      set({ error: (e as Error).message });
    }
  },

  testAgent: async (id, input) => {
    set({ error: null });
    const res = await agentApi.test(id, input);
    if (res.success && res.data) {
      return res.data.output;
    }
    throw new Error(res.error || 'Agent test failed');
  },

  updateAgentState: (id, state, progress) =>
    set((s) => ({
      agents: s.agents.map((a) => (a.id === id ? { ...a, state, progress } : a)),
    })),

  selectAgent: (id) => set({ selectedAgentId: id }),
}));