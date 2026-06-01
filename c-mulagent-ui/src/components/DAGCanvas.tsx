import { useCallback, useMemo } from 'react';
import ReactFlow, {
  Background,
  Controls,
  type Node,
  type Edge,
  type NodeProps,
  Handle,
  Position,
} from 'reactflow';
import 'reactflow/dist/style.css';
import type { AgentSpec } from '../types/agent';
import { AgentState } from '../types/agent';

function AgentNode({ data }: NodeProps) {
  const agent = data.agent as AgentSpec;
  const stateChars: Record<AgentState, string> = {
    [AgentState.Idle]: '○',
    [AgentState.Running]: '◉',
    [AgentState.Done]: '●',
    [AgentState.Error]: '✕',
  };
  return (
    <div
      style={{
        padding: '10px 16px',
        borderRadius: 8,
        background: 'var(--bg-secondary)',
        border: '2px solid var(--border-color)',
        color: 'var(--text-primary)',
        fontSize: 12,
        minWidth: 140,
        textAlign: 'center',
      }}
    >
      <Handle type="target" position={Position.Top} />
      <div style={{ fontSize: 14, fontWeight: 600 }}>
        {stateChars[agent.state] || '○'} {agent.name}
      </div>
      <div style={{ color: 'var(--text-secondary)', marginTop: 2 }}>{agent.role}</div>
      <Handle type="source" position={Position.Bottom} />
    </div>
  );
}

const nodeTypes = { agentNode: AgentNode };

interface Props {
  agents: AgentSpec[];
}

export function DAGCanvas({ agents }: Props) {
  const nodes: Node[] = useMemo(() => {
    const spacing = 180;
    return agents.map((agent, i) => ({
      id: agent.id,
      type: 'agentNode',
      position: { x: 60 + (i % 2) * spacing, y: 40 + Math.floor(i / 2) * 120 },
      data: { agent },
    }));
  }, [agents]);

  const edges: Edge[] = useMemo(() => {
    const result: Edge[] = [];
    for (let i = 0; i < agents.length - 1; i++) {
      result.push({
        id: `e-${agents[i].id}-${agents[i + 1].id}`,
        source: agents[i].id,
        target: agents[i + 1].id,
        animated: agents[i].state === AgentState.Running,
        style: { stroke: 'var(--text-muted)' },
      });
    }
    return result;
  }, [agents]);

  const defaultEdgeOptions = { style: { stroke: 'var(--text-muted)' } };

  const onInit = useCallback(() => {}, []);

  return (
    <div className="dag-canvas">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        nodeTypes={nodeTypes}
        defaultEdgeOptions={defaultEdgeOptions}
        onInit={onInit}
        fitView
        attributionPosition="bottom-left"
      >
        <Background color="var(--border-color)" gap={20} />
        <Controls
          style={{
            background: 'var(--bg-secondary)',
            border: '1px solid var(--border-color)',
            borderRadius: 6,
          }}
        />
      </ReactFlow>
    </div>
  );
}