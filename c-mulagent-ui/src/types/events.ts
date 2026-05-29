import type { AgentState } from './agent';
import type { TaskStatus } from './task';

export type EventType =
  | 'agent_state_changed'
  | 'task_created'
  | 'task_progress'
  | 'task_completed'
  | 'task_failed'
  | 'log_entry'
  | 'resource_update';

export interface AgentStateChangedEvent {
  agentId: string;
  state: AgentState;
  progress: number;
  timestamp: number;
}

export interface TaskProgressEvent {
  taskId: string;
  subtaskId?: string;
  status: TaskStatus;
  progress: number;
  timestamp: number;
}

export interface LogEntryEvent {
  agentId: string;
  level: 'info' | 'warn' | 'error';
  message: string;
  timestamp: number;
}

export type AgentEvent =
  | { type: 'agent_state_changed'; payload: AgentStateChangedEvent }
  | { type: 'task_created'; payload: TaskProgressEvent }
  | { type: 'task_progress'; payload: TaskProgressEvent }
  | { type: 'task_completed'; payload: TaskProgressEvent }
  | { type: 'task_failed'; payload: TaskProgressEvent }
  | { type: 'log_entry'; payload: LogEntryEvent }
  | { type: 'resource_update'; payload: Record<string, unknown> };

export interface WsMessage {
  type: EventType;
  payload: unknown;
  timestamp: number;
}