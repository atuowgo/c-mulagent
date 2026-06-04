import { useState, useMemo, useCallback } from 'react';
import { NLInput } from '../components/NLInput';
import { DAGCanvas } from '../components/DAGCanvas';
import { LogViewer } from '../components/LogViewer';
import { ErrorBanner } from '../components/ErrorBanner';
import { useTaskStore } from '../stores/taskStore';

export function Orchestrator() {
  const { addTask, startTask, cancelTask, tasks, selectedTaskId, selectTask, selectSubtask, error } = useTaskStore();
  const [running, setRunning] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);

  const selectedTask = useMemo(
    () => tasks.find((t) => t.id === selectedTaskId) ?? null,
    [tasks, selectedTaskId],
  );

  const displayError = localError ?? error;

  const handleSubmit = async (text: string) => {
    setRunning(true);
    setLocalError(null);
    try {
      const task = await addTask(text);
      if (task) {
        await startTask(task.id);
      }
    } catch (e) {
      setLocalError((e as Error).message);
    } finally {
      setRunning(false);
    }
  };

  const handleCancel = useCallback(async () => {
    if (selectedTask) {
      try {
        await cancelTask(selectedTask.id);
      } catch (e) {
        setLocalError((e as Error).message);
      }
    }
  }, [selectedTask, cancelTask]);

  const taskRunning = selectedTask?.status === 'running';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <h2 style={{ fontSize: 18, fontWeight: 600, marginBottom: 4 }}>任务编排</h2>
      <p style={{ color: 'var(--text-secondary)', fontSize: 13, marginBottom: 12 }}>
        通过自然语言描述任务需求，由系统自动编排Agent执行
      </p>
      <NLInput onSubmit={handleSubmit} disabled={running} />

      {displayError && (
        <ErrorBanner
          message={displayError}
          onDismiss={() => { setLocalError(null); }}
        />
      )}

      {taskRunning && (
        <div style={{
          display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8,
          padding: '6px 12px', background: 'rgba(59,130,246,0.1)', borderRadius: 6,
          fontSize: 12, color: 'var(--accent-blue)',
        }}>
          <span style={{ animation: 'pulse 1.5s infinite' }}>◉</span>
          任务运行中...
          <button
            onClick={handleCancel}
            style={{
              marginLeft: 8, padding: '2px 8px', fontSize: 11,
              background: 'transparent', border: '1px solid var(--accent-red)',
              color: 'var(--accent-red)', borderRadius: 3, cursor: 'pointer',
            }}
          >
            取消
          </button>
        </div>
      )}

      <div className="section-title">
        DAG 流程
        {selectedTask && (
          <span style={{ fontWeight: 400, fontSize: 12, color: 'var(--text-secondary)', marginLeft: 8 }}>
            — {selectedTask.name} ({selectedTask.subtasks.length} 子任务)
          </span>
        )}
      </div>
      <div style={{ flex: 1, minHeight: 0 }}>
        <DAGCanvas
          task={selectedTask}
          onNodeClick={(subtask) => selectSubtask(subtask.id)}
        />
      </div>
      <div className="section-title" style={{ marginTop: 16 }}>
        运行日志
      </div>
      <LogViewer />
    </div>
  );
}