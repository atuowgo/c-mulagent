import type { TaskPlan } from '../types/task';
import { TaskStatus } from '../types/task';

interface Props {
  task: TaskPlan;
}

const statusLabels: Record<TaskStatus, string> = {
  [TaskStatus.Pending]: '等待中',
  [TaskStatus.Planning]: '规划中',
  [TaskStatus.Running]: '运行中',
  [TaskStatus.Paused]: '已暂停',
  [TaskStatus.Done]: '已完成',
  [TaskStatus.Failed]: '失败',
};

export function TaskProgress({ task }: Props) {
  return (
    <div className="task-progress">
      <div className="task-progress-header">
        <span style={{ fontWeight: 600 }}>{task.name}</span>
        <span className={`badge badge-${task.status}`}>{statusLabels[task.status]}</span>
      </div>
      <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 8 }}>
        {task.input}
      </div>
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginBottom: 4 }}>
        <span style={{ color: 'var(--text-secondary)' }}>整体进度</span>
        <span>{task.progress}%</span>
      </div>
      <div className="task-progress-bar">
        <div className="task-progress-fill" style={{ width: `${task.progress}%` }} />
      </div>
      <div className="task-subtask-list">
        {task.subtasks.map((st) => (
          <div key={st.id} className="task-subtask-item">
            <span>{st.name}</span>
            <span className={`badge badge-${st.status}`}>
              {statusLabels[st.status]} {st.progress}%
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}