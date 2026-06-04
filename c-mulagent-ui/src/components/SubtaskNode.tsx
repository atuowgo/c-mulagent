import { Handle, Position, type NodeProps } from 'reactflow';
import type { Subtask } from '../types/task';

const statusColors: Record<string, string> = {
  pending: '#64748b',
  planning: '#f59e0b',
  running: '#3b82f6',
  paused: '#f59e0b',
  done: '#22c55e',
  failed: '#ef4444',
};

const statusChars: Record<string, string> = {
  pending: '○',
  planning: '◐',
  running: '◉',
  paused: '⏸',
  done: '●',
  failed: '✕',
};

export function SubtaskNode({ data }: NodeProps) {
  const subtask = data.subtask as Subtask;
  const color = statusColors[subtask.status] ?? statusColors.pending;
  const isActive = subtask.status === 'running';

  return (
    <div
      style={{
        padding: '10px 14px',
        borderRadius: 8,
        background: 'var(--bg-secondary)',
        border: `2px solid ${color}`,
        color: 'var(--text-primary)',
        fontSize: 12,
        minWidth: 180,
        maxWidth: 220,
        boxShadow: isActive ? `0 0 8px ${color}40` : undefined,
        transition: 'border-color 0.3s, box-shadow 0.3s',
      }}
    >
      <Handle type="target" position={Position.Top} style={{ background: color }} />
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4 }}>
        <span style={{ color, fontWeight: 700, fontSize: 14 }}>
          {statusChars[subtask.status] ?? '○'}
        </span>
        <span style={{ fontWeight: 600, fontSize: 13, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {subtask.name}
        </span>
      </div>

      {subtask.assignedAgent && (
        <div style={{ color: 'var(--text-secondary)', fontSize: 11, marginBottom: 2 }}>
          🤖 {subtask.assignedAgent}
        </div>
      )}

      {subtask.status === 'running' && (
        <div style={{ marginTop: 4 }}>
          <div style={{
            height: 4,
            borderRadius: 2,
            background: 'var(--border-color)',
            overflow: 'hidden',
          }}>
            <div style={{
              height: '100%',
              width: `${subtask.progress}%`,
              background: color,
              transition: 'width 0.5s',
              borderRadius: 2,
            }} />
          </div>
          <div style={{ fontSize: 10, color: 'var(--text-muted)', textAlign: 'right', marginTop: 2 }}>
            {subtask.progress}%
          </div>
        </div>
      )}

      {subtask.status === 'failed' && subtask.retryCount > 0 && (
        <div style={{ fontSize: 10, color: '#ef4444', marginTop: 2 }}>
          重试 {subtask.retryCount}/{subtask.maxRetries}
        </div>
      )}

      <Handle type="source" position={Position.Bottom} style={{ background: color }} />
    </div>
  );
}