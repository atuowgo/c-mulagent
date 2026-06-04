import { useMemo, useCallback } from 'react';
import ReactFlow, {
  Background,
  Controls,
  MiniMap,
  type Node,
  type Edge,
  type NodeMouseHandler,
} from 'reactflow';
import 'reactflow/dist/style.css';
import type { TaskPlan, Subtask } from '../types/task';
import { layoutDAG } from '../utils/dagLayout';
import { SubtaskNode } from './SubtaskNode';

const nodeTypes = { subtaskNode: SubtaskNode };

interface Props {
  task: TaskPlan | null;
  onNodeClick?: (subtask: Subtask) => void;
}

export function DAGCanvas({ task, onNodeClick }: Props) {
  const { nodes, edges } = useMemo(() => {
    if (!task || task.subtasks.length === 0) return { nodes: [], edges: [] };
    return layoutDAG(task.subtasks);
  }, [task]);

  const defaultEdgeOptions = useMemo(
    () => ({
      type: 'smoothstep' as const,
      style: { stroke: 'var(--text-muted)', strokeWidth: 2 },
    }),
    [],
  );

  const handleNodeClick: NodeMouseHandler = useCallback(
    (_event, node) => {
      onNodeClick?.(node.data.subtask as Subtask);
    },
    [onNodeClick],
  );

  if (!task || task.subtasks.length === 0) {
    return (
      <div className="dag-canvas dag-canvas-empty">
        <div style={{ color: 'var(--text-muted)', fontSize: 14 }}>
          {task ? '任务尚未分解为子任务，提交后自动生成 DAG' : '选择或创建一个任务以查看执行 DAG'}
        </div>
      </div>
    );
  }

  return (
    <div className="dag-canvas">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        nodeTypes={nodeTypes}
        defaultEdgeOptions={defaultEdgeOptions}
        onNodeClick={handleNodeClick}
        fitView
        fitViewOptions={{ padding: 0.3 }}
        attributionPosition="bottom-left"
        minZoom={0.3}
        maxZoom={2}
      >
        <Background color="var(--border-color)" gap={20} />
        <Controls
          style={{
            background: 'var(--bg-secondary)',
            border: '1px solid var(--border-color)',
            borderRadius: 6,
          }}
        />
        <MiniMap
          style={{
            background: 'var(--bg-primary)',
            border: '1px solid var(--border-color)',
            borderRadius: 6,
          }}
          nodeColor={(node) => {
            const st = node.data?.subtask as Subtask | undefined;
            if (!st) return '#64748b';
            switch (st.status) {
              case 'running': return '#3b82f6';
              case 'done': return '#22c55e';
              case 'failed': return '#ef4444';
              default: return '#64748b';
            }
          }}
        />
      </ReactFlow>
    </div>
  );
}