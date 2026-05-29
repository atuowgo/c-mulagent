import { useEffect, useRef, useCallback } from 'react';
import type { AgentEvent, WsMessage } from '../types/events';
import { useAgentStore } from '../stores/agentStore';
import { useTaskStore } from '../stores/taskStore';

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
        const msg: WsMessage = JSON.parse(event.data);
        dispatchEvent(msg as unknown as AgentEvent);
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

  switch (event.type) {
    case 'agent_state_changed':
      updateAgentState(event.payload.agentId, event.payload.state, event.payload.progress);
      break;
    case 'task_progress':
      if (event.payload.subtaskId) {
        updateSubtask(event.payload.taskId, event.payload.subtaskId, event.payload.status, event.payload.progress);
      } else {
        updateTask(event.payload.taskId, event.payload.status, event.payload.progress);
      }
      break;
    case 'task_completed':
      updateTask(event.payload.taskId, event.payload.status, event.payload.progress);
      break;
    case 'task_failed':
      updateTask(event.payload.taskId, event.payload.status, event.payload.progress);
      break;
    default:
      break;
  }
}