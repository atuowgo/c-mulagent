import { useEffect, useRef, useCallback } from 'react';
import type { AgentEvent } from '../types/events';
import { useAgentStore } from '../stores/agentStore';
import { useTaskStore } from '../stores/taskStore';
import { useLogStore, type LogEntry } from '../stores/logStore';
import { AgentState } from '../types/agent';
import { TaskStatus } from '../types/task';

function normalizeTaskStatus(raw: string): TaskStatus {
  const mapping: Record<string, TaskStatus> = {
    CREATED: TaskStatus.Pending,
    READY: TaskStatus.Pending,
    PENDING: TaskStatus.Pending,
    RUNNING: TaskStatus.Running,
    COMPLETED: TaskStatus.Done,
    FAILED: TaskStatus.Failed,
    CANCELLED: TaskStatus.Failed,
  };
  return mapping[raw] ?? TaskStatus.Pending;
}

function normalizeAgentState(raw: string): AgentState {
  const mapping: Record<string, AgentState> = {
    IDLE: AgentState.Idle,
    PENDING: AgentState.Running,
    RUNNING: AgentState.Running,
    COMPLETED: AgentState.Done,
    FAILED: AgentState.Error,
    CANCELLED: AgentState.Error,
  };
  return mapping[raw] ?? AgentState.Idle;
}

export function useWebSocket() {
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<number>(0);
  const reconnectAttemptRef = useRef(0);
  const maxReconnectAttempts = 10;

  const updateAgentState = useAgentStore((s) => s.updateAgentState);
  const updateTask = useTaskStore((s) => s.updateTask);
  const updateSubtask = useTaskStore((s) => s.updateSubtask);

  const connect = useCallback(() => {
    if (wsRef.current?.readyState === WebSocket.OPEN) return;

    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${location.host}/ws/events`;
    const ws = new WebSocket(wsUrl);

    ws.onopen = () => {
      console.log('[WS] connected');
      reconnectAttemptRef.current = 0;
    };

    ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data) as AgentEvent;
        dispatchEvent(msg);
      } catch (e) {
        console.warn('[WS] failed to parse message:', e);
      }
    };

    ws.onclose = () => {
      console.log('[WS] disconnected, reconnecting...');
      wsRef.current = null;
      if (reconnectAttemptRef.current < maxReconnectAttempts) {
        const delay = Math.min(1000 * 2 ** reconnectAttemptRef.current, 30000);
        reconnectAttemptRef.current++;
        reconnectTimeoutRef.current = window.setTimeout(connect, delay);
      }
    };

    ws.onerror = (err) => {
      console.warn('[WS] error:', err);
      ws.close();
    };

    wsRef.current = ws;
  }, [updateAgentState, updateTask, updateSubtask]);

  useEffect(() => {
    connect();
    return () => {
      clearTimeout(reconnectTimeoutRef.current);
      wsRef.current?.close();
    };
  }, [connect]);

  return { reconnect: connect };
}

function dispatchEvent(event: AgentEvent) {
  const { updateAgentState } = useAgentStore.getState();
  const { updateTask, updateSubtask } = useTaskStore.getState();
  const { addLog } = useLogStore.getState();
  const d = event.data || {};

  switch (event.type) {
    case 'AGENT_STATE_CHANGED': {
      const agentId = String(d.agentId || '');
      const agentState = normalizeAgentState(String(d.state || ''));
      const progress = Number(d.progress) || 0;
      updateAgentState(agentId, agentState, progress);
      addLog({
        id: event.id || `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        time: event.timestamp ? new Date(event.timestamp).toLocaleTimeString() : new Date().toLocaleTimeString(),
        level: agentState === AgentState.Error ? 'error' : 'info',
        source: agentId,
        message: `Agent state: ${agentState} (${d.state})`,
      });
      break;
    }
    case 'TASK_PROGRESS': {
      const subtaskId = d.subtaskId ? String(d.subtaskId) : undefined;
      if (subtaskId) {
        const extra: Record<string, unknown> = {};
        if (d.assignedAgent) extra.assignedAgent = String(d.assignedAgent);
        if (d.outputLength !== undefined) extra.progress = 100;
        if (d.error) extra.outputData = String(d.error);
        if (d.retryCount !== undefined) extra.retryCount = Number(d.retryCount);
        if (d.maxRetries !== undefined) extra.maxRetries = Number(d.maxRetries);
        updateSubtask(
          String(d.taskPlanId || ''),
          subtaskId,
          normalizeTaskStatus(String(d.status || '')),
          Number(d.progress) || 0,
          Object.keys(extra).length > 0 ? extra as Partial<import('../types/task').Subtask> : undefined,
        );
      } else {
        updateTask(
          String(d.taskPlanId || ''),
          normalizeTaskStatus(String(d.status || '')),
          Number(d.progress) || 0,
        );
      }
      break;
    }
    case 'AGENT_TOOL_INVOKED':
    case 'AGENT_TOOL_RESULT':
    case 'AGENT_LOG':
      addLog({
        id: event.id || `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        time: event.timestamp ? new Date(event.timestamp).toLocaleTimeString() : new Date().toLocaleTimeString(),
        level: (String(d.level || 'info') as LogEntry['level']) || 'info',
        source: event.source || String(d.agentId || ''),
        message: String(d.message || d.output || d.toolName || ''),
      });
      break;
    case 'RESOURCE_SLOT_CHANGED':
    default:
      break;
  }
}