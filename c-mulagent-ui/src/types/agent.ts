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
  state: AgentState;
  progress: number;
  model: string;
  skills: string[];
}