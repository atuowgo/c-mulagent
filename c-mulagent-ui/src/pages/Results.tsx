import { useState } from 'react';
import { useTaskStore } from '../stores/taskStore';
import { TaskStatus } from '../types/task';

const statusLabels: Record<TaskStatus, string> = {
  [TaskStatus.Pending]: '等待中',
  [TaskStatus.Planning]: '规划中',
  [TaskStatus.Running]: '运行中',
  [TaskStatus.Paused]: '已暂停',
  [TaskStatus.Done]: '已完成',
  [TaskStatus.Failed]: '失败',
};

export function Results() {
  const tasks = useTaskStore((s) => s.tasks);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  return (
    <div>
      <h2 style={{ fontSize: 18, fontWeight: 600, marginBottom: 4 }}>运行结果</h2>
      <p style={{ color: 'var(--text-secondary)', fontSize: 13, marginBottom: 16 }}>
        查看所有已执行任务的结果与子产物
      </p>

      {tasks.length === 0 && (
        <div style={{ color: 'var(--text-muted)', textAlign: 'center', padding: 40 }}>
          暂无执行记录
        </div>
      )}

      <div className="result-list">
        {tasks.map((task) => (
          <div
            key={task.id}
            className="result-item"
            onClick={() => setExpandedId(expandedId === task.id ? null : task.id)}
          >
            <div className="result-item-header">
              <span style={{ fontWeight: 600 }}>{task.name}</span>
              <span className={`badge badge-${task.status}`}>
                {statusLabels[task.status]}
              </span>
            </div>
            <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
              输入: {task.input}
            </div>
            <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 4 }}>
              创建: {new Date(task.createdAt).toLocaleString()}
              {task.completedAt &&
                ` | 完成: ${new Date(task.completedAt).toLocaleString()}`}
            </div>

            {expandedId === task.id && (
              <div className="result-item-expanded">
                <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 8 }}>
                  子任务详情
                </div>
                {task.subtasks.map((st) => (
                  <div
                    key={st.id}
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      padding: '4px 0',
                      fontSize: 12,
                      color: 'var(--text-secondary)',
                    }}
                  >
                    <span>{st.name} (Agent: {st.assignedAgent})</span>
                    <span className={`badge badge-${st.status}`}>
                      {statusLabels[st.status]} {st.progress}%
                    </span>
                  </div>
                ))}

                {/* placeholder artifacts */}
                <div style={{ marginTop: 12, padding: 12, background: 'var(--bg-primary)', borderRadius: 6, fontSize: 13 }}>
                  <div style={{ fontWeight: 600, marginBottom: 6 }}>产物预览</div>
                  <div style={{ color: 'var(--text-muted)', fontSize: 12, fontFamily: 'monospace' }}>
                    --- artifact placeholder ---<br />
                    task_id: {task.id}<br />
                    status: {task.status}<br />
                    --- end ---
                  </div>
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}