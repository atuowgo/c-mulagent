import { AgentCard } from '../components/AgentCard';
import { TaskProgress } from '../components/TaskProgress';
import { ErrorBanner } from '../components/ErrorBanner';
import { useAgentState } from '../hooks/useAgentState';
import { useAgentStore } from '../stores/agentStore';
import { useTaskStore } from '../stores/taskStore';

export function Dashboard() {
  const { agents, selectedAgent, selectAgent, stats } = useAgentState();
  const tasks = useTaskStore((s) => s.tasks);
  const agentError = useAgentStore((s) => s.error);
  const taskError = useTaskStore((s) => s.error);
  const fetchAgents = useAgentStore((s) => s.fetchAgents);
  const fetchTasks = useTaskStore((s) => s.fetchTasks);

  return (
    <div className="app-page-scroll">
      <h2 style={{ fontSize: 18, fontWeight: 600, marginBottom: 4 }}>仪表盘</h2>
      <p style={{ color: 'var(--text-secondary)', fontSize: 13, marginBottom: 8 }}>
        Agent状态总览与当前任务进度
      </p>

      {agentError && (
        <ErrorBanner message={agentError} onRetry={fetchAgents} />
      )}
      {taskError && (
        <ErrorBanner message={taskError} onRetry={fetchTasks} />
      )}

      <div className="dashboard-stats">
        <div className="stat-card">
          <div className="stat-card-value" style={{ color: 'var(--text-primary)' }}>{stats.total}</div>
          <div className="stat-card-label">Agent总数</div>
        </div>
        <div className="stat-card">
          <div className="stat-card-value" style={{ color: 'var(--accent-blue)' }}>{stats.runningCount}</div>
          <div className="stat-card-label">运行中</div>
        </div>
        <div className="stat-card">
          <div className="stat-card-value" style={{ color: 'var(--accent-green)' }}>{stats.doneCount}</div>
          <div className="stat-card-label">已完成</div>
        </div>
        <div className="stat-card">
          <div className="stat-card-value" style={{ color: 'var(--accent-red)' }}>{stats.errorCount}</div>
          <div className="stat-card-label">异常</div>
        </div>
      </div>

      <div className="section-title">Agent 列表</div>
      <div className="agent-card-grid">
        {agents.map((agent) => (
          <AgentCard
            key={agent.id}
            agent={agent}
            selected={selectedAgent?.id === agent.id}
            onClick={() => selectAgent(agent.id)}
          />
        ))}
      </div>

      {tasks.length > 0 && (
        <>
          <div className="section-title" style={{ marginTop: 24 }}>当前任务</div>
          {tasks.map((task) => (
            <TaskProgress key={task.id} task={task} />
          ))}
        </>
      )}
    </div>
  );
}