import { useUIStore, type PageKey } from './stores/uiStore';
import { useWebSocket } from './hooks/useWebSocket';
import { Layout } from './components/Layout';
import { Dashboard } from './pages/Dashboard';
import { Orchestrator } from './pages/Orchestrator';
import { Results } from './pages/Results';
import { Skills } from './pages/Skills';
import { useAgentState } from './hooks/useAgentState';
import { AgentState } from './types/agent';
import { AgentCard } from './components/AgentCard';
import { useTaskStore } from './stores/taskStore';
import './App.css';

const pageComponents: Record<PageKey, () => JSX.Element> = {
  dashboard: Dashboard,
  orchestrator: Orchestrator,
  results: Results,
  skills: Skills,
};

export default function App() {
  const currentPage = useUIStore((s) => s.currentPage);
  useWebSocket();

  const PageComponent = pageComponents[currentPage] ?? Dashboard;

  const rightPanel = <RightPanel page={currentPage} />;

  return (
    <Layout rightPanel={rightPanel}>
      <PageComponent />
    </Layout>
  );
}

function RightPanel({ page }: { page: PageKey }) {
  return (
    <div>
      {page === 'dashboard' && <DashboardRightPanel />}
      {page === 'orchestrator' && <span style={{ color: 'var(--text-muted)', fontSize: 13 }}>点击DAG节点查看Agent详情</span>}
      {page === 'results' && <span style={{ color: 'var(--text-muted)', fontSize: 13 }}>点击任务查看子结果详情</span>}
      {page === 'skills' && <span style={{ color: 'var(--text-muted)', fontSize: 13 }}>点击Skill查看详情与参数</span>}
    </div>
  );
}

function DashboardRightPanel() {
  const { agents, selectedAgent, selectAgent } = useAgentState();
  const tasks = useTaskStore((s) => s.tasks);

  if (!selectedAgent) {
    return (
      <div>
        <div style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 12 }}>
          选择Agent查看详情
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {agents.filter(a => a.state === AgentState.Running).map((agent) => (
            <AgentCard
              key={agent.id}
              agent={agent}
              selected={false}
              onClick={() => selectAgent(agent.id)}
            />
          ))}
          {agents.filter(a => a.state === AgentState.Running).length === 0 && (
            <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>暂无运行中Agent</div>
          )}
        </div>
      </div>
    );
  }

  return (
    <div>
      <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 12 }}>{selectedAgent.name}</div>
      <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4 }}>
        角色: {selectedAgent.role}
      </div>
      <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4 }}>
        模型: {selectedAgent.model}
      </div>
      <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 12 }}>
        状态: {selectedAgent.state}
      </div>
      <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 4 }}>关联任务</div>
      {tasks
        .flatMap((t) =>
          t.subtasks
            .filter((st) => st.agentId === selectedAgent.id)
            .map((st) => ({ task: t, subtask: st }))
        )
        .map(({ task, subtask }) => (
          <div key={subtask.id} style={{ fontSize: 12, padding: '4px 0', color: 'var(--text-secondary)' }}>
            {task.name} / {subtask.name}
          </div>
        ))}
      {tasks.flatMap((t) => t.subtasks.filter((st) => st.agentId === selectedAgent.id)).length === 0 && (
        <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>无关联任务</div>
      )}
    </div>
  );
}