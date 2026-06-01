export enum AgentState {
  Idle = 'idle',
  Running = 'running',
  Done = 'done',
  Error = 'error',
}

export interface AgentSpec {
  id: string;
  name: string;
  role: string;
  baseUrl?: string;
  model: string;
  apiKey?: string;
  tools?: string[];
  maxSteps?: number;
  outputFormat?: string;
  enabled?: boolean;
  // UI runtime state (client-side only)
  state: AgentState;
  progress: number;
}