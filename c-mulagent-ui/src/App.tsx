import { useEffect } from 'react';
import { useUIStore, type PageKey } from './stores/uiStore';
import { useWebSocket } from './hooks/useWebSocket';
import { Layout } from './components/Layout';
import { ErrorBoundary } from './components/ErrorBoundary';
import { Dashboard } from './pages/Dashboard';
import { Orchestrator } from './pages/Orchestrator';
import { Results } from './pages/Results';
import { Templates } from './pages/Templates';
import { Skills } from './pages/Skills';
import { useAgentState } from './hooks/useAgentState';
import { useAgentStore } from './stores/agentStore';
import { AgentState } from './types/agent';
import { AgentCard } from './components/AgentCard';
import { useTaskStore } from './stores/taskStore';
import { useTemplateStore } from './stores/templateStore';
import './App.css';

const pageComponents: Record<PageKey, () => JSX.Element> = {
  dashboard: Dashboard,
  orchestrator: Orchestrator,
  results: Results,
  templates: Templates,
  skills: Skills,
};

export default function App() {
  const currentPage = useUIStore((s) => s.currentPage);
  const fetchAgents = useAgentStore((s) => s.fetchAgents);
  useWebSocket();

  useEffect(() => {
    fetchAgents();
  }, [fetchAgents]);

  const PageComponent = pageComponents[currentPage] ?? Dashboard;

  const rightPanel = <RightPanel page={currentPage} />;

  return (
    <Layout rightPanel={rightPanel}>
      <ErrorBoundary>
        <PageComponent />
      </ErrorBoundary>
    </Layout>
  );
}

function RightPanel({ page }: { page: PageKey }) {
  return (
    <div>
      {page === 'dashboard' && <DashboardRightPanel />}
      {page === 'orchestrator' && <OrchestratorRightPanel />}
      {page === 'results' && <span style={{ color: 'var(--text-muted)', fontSize: 13 }}>点击任务查看子结果详情</span>}
      {page === 'templates' && <TemplateRightPanel />}
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
            .filter((st) => st.assignedAgent === selectedAgent.id)
            .map((st) => ({ task: t, subtask: st }))
        )
        .map(({ task, subtask }) => (
          <div key={subtask.id} style={{ fontSize: 12, padding: '4px 0', color: 'var(--text-secondary)' }}>
            {task.name} / {subtask.name}
          </div>
        ))}
      {tasks.flatMap((t) => t.subtasks.filter((st) => st.assignedAgent === selectedAgent.id)).length === 0 && (
        <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>无关联任务</div>
      )}
    </div>
  );
}

function OrchestratorRightPanel() {
  const tasks = useTaskStore((s) => s.tasks);
  const selectedTaskId = useTaskStore((s) => s.selectedTaskId);
  const selectedSubtaskId = useTaskStore((s) => s.selectedSubtaskId);

  const selectedTask = tasks.find((t) => t.id === selectedTaskId);
  const selectedSubtask = selectedTask?.subtasks.find((st) => st.id === selectedSubtaskId);

  if (!selectedSubtask) {
    return (
      <div>
        <div style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 12 }}>
          点击DAG节点查看子任务详情
        </div>
        {selectedTask && (
          <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
            <div>当前任务: {selectedTask.name}</div>
            <div style={{ marginTop: 4 }}>子任务数: {selectedTask.subtasks.length}</div>
            <div>状态: {selectedTask.status}</div>
          </div>
        )}
      </div>
    );
  }

  const statusLabel: Record<string, string> = {
    pending: '待执行',
    planning: '规划中',
    running: '执行中',
    paused: '已暂停',
    done: '已完成',
    failed: '失败',
  };

  return (
    <div>
      <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 12 }}>{selectedSubtask.name}</div>
      <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4 }}>
        状态: {statusLabel[selectedSubtask.status] ?? selectedSubtask.status}
      </div>
      {selectedSubtask.assignedAgent && (
        <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4 }}>
          执行Agent: {selectedSubtask.assignedAgent}
        </div>
      )}
      <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4 }}>
        优先级: {selectedSubtask.priority}
      </div>
      {selectedSubtask.dependencies.length > 0 && (
        <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4 }}>
          依赖: {selectedSubtask.dependencies.join(', ')}
        </div>
      )}
      {selectedSubtask.status === 'running' && (
        <div style={{ margin: '8px 0' }}>
          <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 2 }}>进度</div>
          <div style={{
            height: 6,
            borderRadius: 3,
            background: 'var(--border-color)',
            overflow: 'hidden',
          }}>
            <div style={{
              height: '100%',
              width: `${selectedSubtask.progress}%`,
              background: '#3b82f6',
              borderRadius: 3,
              transition: 'width 0.5s',
            }} />
          </div>
          <div style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 2 }}>
            {selectedSubtask.progress}%
          </div>
        </div>
      )}
      {selectedSubtask.description && (
        <div style={{ marginTop: 8 }}>
          <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 2 }}>描述</div>
          <div style={{ fontSize: 12, color: 'var(--text-secondary)', lineHeight: 1.5 }}>
            {selectedSubtask.description}
          </div>
        </div>
      )}
      {selectedSubtask.outputData && (
        <div style={{ marginTop: 8 }}>
          <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 2 }}>输出</div>
          <div style={{
            fontSize: 11,
            color: 'var(--text-secondary)',
            background: 'var(--bg-primary)',
            padding: 8,
            borderRadius: 4,
            maxHeight: 200,
            overflow: 'auto',
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-all',
          }}>
            {selectedSubtask.outputData}
          </div>
        </div>
      )}
      {selectedSubtask.status === 'failed' && (
        <div style={{ marginTop: 8, fontSize: 11, color: '#ef4444' }}>
          重试: {selectedSubtask.retryCount}/{selectedSubtask.maxRetries}
        </div>
      )}
    </div>
  );
}

function TemplateRightPanel() {
  const templates = useTemplateStore((s) => s.templates);
  const selectedId = useTemplateStore((s) => s.selectedTemplateId);
  const selected = templates.find((t) => t.id === selectedId) ?? null;

  if (!selected) {
    return (
      <div>
        <div style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 12 }}>
          点击模板查看详情
        </div>
        <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
          共 {templates.length} 个模板
        </div>
      </div>
    );
  }

  return (
    <div>
      <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 12 }}>{selected.name}</div>
      <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4 }}>
        版本: v{selected.version}
      </div>
      {selected.category && (
        <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4 }}>
          分类: {selected.category}
        </div>
      )}
      <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4 }}>
        状态: {selected.enabled ? '启用' : '禁用'}
      </div>
      {selected.description && (
        <div style={{ marginTop: 8 }}>
          <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 2 }}>描述</div>
          <div style={{ fontSize: 12, color: 'var(--text-secondary)', lineHeight: 1.5 }}>
            {selected.description}
          </div>
        </div>
      )}
      <div style={{ marginTop: 12 }}>
        <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 4 }}>模板内容</div>
        <pre style={{
          fontSize: 11, color: 'var(--text-secondary)', background: 'var(--bg-primary)',
          padding: 10, borderRadius: 6, maxHeight: 300, overflow: 'auto',
          whiteSpace: 'pre-wrap', wordBreak: 'break-all', lineHeight: 1.6,
          fontFamily: 'monospace',
        }}>
          {selected.planTemplate}
        </pre>
      </div>
      <div style={{ marginTop: 8, fontSize: 11, color: 'var(--text-muted)' }}>
        创建: {new Date(selected.createdAt).toLocaleString()}
        {selected.updatedAt && ` | 更新: ${new Date(selected.updatedAt).toLocaleString()}`}
      </div>
    </div>
  );
}