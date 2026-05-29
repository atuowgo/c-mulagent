export enum TaskStatus {
  Pending = 'pending',
  Planning = 'planning',
  Running = 'running',
  Paused = 'paused',
  Done = 'done',
  Failed = 'failed',
}

export interface Subtask {
  id: string;
  name: string;
  agentId: string;
  status: TaskStatus;
  progress: number;
  startTime?: number;
  endTime?: number;
}

export interface TaskPlan {
  id: string;
  name: string;
  input: string;
  status: TaskStatus;
  progress: number;
  subtasks: Subtask[];
  createdAt: number;
  completedAt?: number;
}