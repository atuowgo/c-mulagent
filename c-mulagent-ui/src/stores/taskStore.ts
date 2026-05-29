import { create } from 'zustand';
import type { TaskPlan, Subtask } from '../types/task';
import { TaskStatus } from '../types/task';

interface TaskStore {
  tasks: TaskPlan[];
  selectedTaskId: string | null;
  setTasks: (tasks: TaskPlan[]) => void;
  addTask: (task: TaskPlan) => void;
  updateTask: (id: string, status: TaskStatus, progress: number) => void;
  updateSubtask: (taskId: string, subtaskId: string, status: TaskStatus, progress: number) => void;
  selectTask: (id: string | null) => void;
}

const mockSubtask: Subtask[] = [
  { id: 's1', name: '需求分析', agentId: '1', status: TaskStatus.Done, progress: 100 },
  { id: 's2', name: '架构设计', agentId: '1', status: TaskStatus.Running, progress: 65 },
  { id: 's3', name: '代码实现', agentId: '2', status: TaskStatus.Pending, progress: 0 },
  { id: 's4', name: '代码审查', agentId: '3', status: TaskStatus.Pending, progress: 0 },
  { id: 's5', name: '文档生成', agentId: '4', status: TaskStatus.Pending, progress: 0 },
];

const mockTasks: TaskPlan[] = [
  {
    id: 't1',
    name: '实现用户认证模块',
    input: '为Web应用实现基于JWT的用户认证功能',
    status: TaskStatus.Running,
    progress: 42,
    subtasks: mockSubtask,
    createdAt: Date.now() - 3600000,
  },
];

export const useTaskStore = create<TaskStore>((set) => ({
  tasks: mockTasks,
  selectedTaskId: null,

  setTasks: (tasks) => set({ tasks }),

  addTask: (task) => set((s) => ({ tasks: [task, ...s.tasks] })),

  updateTask: (id, status, progress) =>
    set((s) => ({
      tasks: s.tasks.map((t) =>
        t.id === id
          ? { ...t, status, progress, ...(status === TaskStatus.Done ? { completedAt: Date.now() } : {}) }
          : t
      ),
    })),

  updateSubtask: (taskId, subtaskId, status, progress) =>
    set((s) => ({
      tasks: s.tasks.map((t) =>
        t.id === taskId
          ? {
              ...t,
              subtasks: t.subtasks.map((st) =>
                st.id === subtaskId ? { ...st, status, progress } : st
              ),
            }
          : t
      ),
    })),

  selectTask: (id) => set({ selectedTaskId: id }),
}));