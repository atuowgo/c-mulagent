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
  taskPlanId: string;
  name: string;
  description?: string;
  status: TaskStatus;
  assignedAgent?: string;
  inputData?: string;
  outputData?: string;
  priority: number;
  dependencies: string[];
  retryCount: number;
  maxRetries: number;
  progress: number;
}

export interface TaskPlan {
  id: string;
  name: string;
  description?: string;
  input: string;
  status: TaskStatus;
  priority: number;
  progress: number;
  subtasks: Subtask[];
  createdAt: string;
  updatedAt?: string;
  completedAt?: string;
}