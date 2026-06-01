import { useState } from 'react';
import { NLInput } from '../components/NLInput';
import { DAGCanvas } from '../components/DAGCanvas';
import { LogViewer } from '../components/LogViewer';
import { useAgentState } from '../hooks/useAgentState';
import { useTaskStore } from '../stores/taskStore';

export function Orchestrator() {
  const { agents } = useAgentState();
  const { addTask, startTask, tasks, error } = useTaskStore();
  const [running, setRunning] = useState(false);

  const handleSubmit = async (text: string) => {
    setRunning(true);
    try {
      const task = await addTask(text);
      if (task) {
        await startTask(task.id);
      }
    } finally {
      setRunning(false);
    }
  };

  return (
    <div>
      <h2 style={{ fontSize: 18, fontWeight: 600, marginBottom: 4 }}>任务编排</h2>
      <p style={{ color: 'var(--text-secondary)', fontSize: 13, marginBottom: 12 }}>
        通过自然语言描述任务需求，由系统自动编排Agent执行
      </p>
      <NLInput onSubmit={handleSubmit} disabled={running} />
      <div className="section-title">DAG 流程</div>
      <DAGCanvas agents={agents} />
      <div className="section-title" style={{ marginTop: 16 }}>
        运行日志
      </div>
      <LogViewer />
    </div>
  );
}