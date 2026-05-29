import type { AgentSpec } from '../types/agent';
import { AgentState } from '../types/agent';

interface Props {
  agent: AgentSpec;
  selected: boolean;
  onClick: () => void;
}

const stateLabels: Record<AgentState, string> = {
  [AgentState.Idle]: '空闲',
  [AgentState.Running]: '运行中',
  [AgentState.Done]: '完成',
  [AgentState.Error]: '错误',
};

const stateIcons: Record<AgentState, string> = {
  [AgentState.Idle]: '○',
  [AgentState.Running]: '◉',
  [AgentState.Done]: '●',
  [AgentState.Error]: '✕',
};

export function AgentCard({ agent, selected, onClick }: Props) {
  return (
    <div className={`agent-card ${selected ? 'selected' : ''}`} onClick={onClick}>
      <div className="agent-card-header">
        <span className="agent-card-name">{stateIcons[agent.state]} {agent.name}</span>
        <span className={`badge badge-${agent.state}`}>{stateLabels[agent.state]}</span>
      </div>
      <div className="agent-card-role">{agent.role} | {agent.model}</div>
      <div className="agent-card-progress">
        <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>进度 {agent.progress}%</span>
        <div className="agent-card-progress-bar">
          <div className="agent-card-progress-fill" style={{ width: `${agent.progress}%` }} />
        </div>
      </div>
      <div className="agent-card-skills">
        {agent.skills.map((s) => (
          <span key={s} className="badge badge-idle">{s}</span>
        ))}
      </div>
    </div>
  );
}