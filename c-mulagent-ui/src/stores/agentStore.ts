import { create } from 'zustand';
import type { AgentSpec } from '../types/agent';
import { AgentState } from '../types/agent';

interface AgentStore {
  agents: AgentSpec[];
  selectedAgentId: string | null;
  setAgents: (agents: AgentSpec[]) => void;
  addAgent: (agent: AgentSpec) => void;
  updateAgentState: (id: string, state: AgentState, progress: number) => void;
  selectAgent: (id: string | null) => void;
}

const mockAgents: AgentSpec[] = [
  { id: '1', name: 'Planner', role: '任务规划', state: AgentState.Idle, progress: 0, model: 'gpt-4', skills: ['planning', 'decomposition'] },
  { id: '2', name: 'Coder', role: '代码生成', state: AgentState.Idle, progress: 0, model: 'gpt-4', skills: ['code_gen', 'refactor'] },
  { id: '3', name: 'Reviewer', role: '代码审查', state: AgentState.Idle, progress: 0, model: 'gpt-4', skills: ['review', 'testing'] },
  { id: '4', name: 'Writer', role: '文档撰写', state: AgentState.Idle, progress: 0, model: 'gpt-3.5-turbo', skills: ['writing', 'translation'] },
];

export const useAgentStore = create<AgentStore>((set) => ({
  agents: mockAgents,
  selectedAgentId: null,

  setAgents: (agents) => set({ agents }),

  addAgent: (agent) => set((s) => ({ agents: [...s.agents, agent] })),

  updateAgentState: (id, state, progress) =>
    set((s) => ({
      agents: s.agents.map((a) => (a.id === id ? { ...a, state, progress } : a)),
    })),

  selectAgent: (id) => set({ selectedAgentId: id }),
}));