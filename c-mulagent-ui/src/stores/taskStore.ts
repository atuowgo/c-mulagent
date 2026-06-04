import { create } from 'zustand';
import type { TaskPlan, Subtask } from '../types/task';
import { TaskStatus } from '../types/task';
import { taskApi } from '../api/client';

function normalizeStatus(raw: string): TaskStatus {
  const mapping: Record<string, TaskStatus> = {
    CREATED: TaskStatus.Pending,
    READY: TaskStatus.Pending,
    RUNNING: TaskStatus.Running,
    COMPLETED: TaskStatus.Done,
    FAILED: TaskStatus.Failed,
    CANCELLED: TaskStatus.Failed,
  };
  return mapping[raw] ?? TaskStatus.Pending;
}

function toSubtask(raw: Record<string, unknown>): Subtask {
  const deps = raw.dependencies;
  return {
    id: raw.id as string,
    taskPlanId: raw.taskPlanId as string ?? '',
    name: raw.name as string,
    description: raw.description as string | undefined,
    status: normalizeStatus(raw.status as string),
    assignedAgent: raw.assignedAgent as string | undefined,
    inputData: raw.inputData as string | undefined,
    outputData: raw.outputData as string | undefined,
    priority: (raw.priority as number) ?? 0,
    dependencies: typeof deps === 'string' ? deps.split(',').filter(Boolean) : (Array.isArray(deps) ? deps as string[] : []),
    retryCount: (raw.retryCount as number) ?? 0,
    maxRetries: (raw.maxRetries as number) ?? 3,
    progress: 0,
  };
}

interface TaskStore {
  tasks: TaskPlan[];
  selectedTaskId: string | null;
  selectedSubtaskId: string | null;
  loading: boolean;
  error: string | null;
  fetchTasks: () => Promise<void>;
  fetchTask: (id: string) => Promise<TaskPlan | null>;
  addTask: (description: string) => Promise<TaskPlan | null>;
  startTask: (id: string) => Promise<void>;
  cancelTask: (id: string) => Promise<void>;
  updateTask: (id: string, status: TaskStatus, progress: number) => void;
  updateSubtask: (taskId: string, subtaskId: string, status: TaskStatus, progress: number, extra?: Partial<Subtask>) => void;
  selectTask: (id: string | null) => void;
  selectSubtask: (id: string | null) => void;
}

export const useTaskStore = create<TaskStore>((set, get) => ({
  tasks: [],
  selectedTaskId: null,
  selectedSubtaskId: null,
  loading: false,
  error: null,

  fetchTasks: async () => {
    set({ loading: true, error: null });
    try {
      const res = await taskApi.list();
      if (res.success && res.data) {
        const tasks: TaskPlan[] = res.data.items.map((r: TaskPlan) => {
          const status = normalizeStatus(r.status);
          return {
            id: r.id,
            name: r.name,
            description: r.description,
            input: r.description ?? '',
            status,
            priority: r.priority ?? 5,
            progress: 0,
            subtasks: [],
            createdAt: r.createdAt ?? new Date().toISOString(),
            updatedAt: r.updatedAt,
            completedAt: r.completedAt,
          };
        });
        set({ tasks, loading: false });
      } else {
        set({ error: res.error || 'Failed to fetch tasks', loading: false });
      }
    } catch (e) {
      set({ error: (e as Error).message, loading: false });
    }
  },

  fetchTask: async (id) => {
    try {
      const res = await taskApi.get(id);
      if (res.success && res.data) {
        const raw = res.data as unknown as Record<string, unknown>;
        const rawSubtasks = (raw.subtasks as Array<Record<string, unknown>>) ?? [];
        const task: TaskPlan = {
          id: raw.id as string,
          name: raw.name as string,
          description: raw.description as string | undefined,
          input: raw.description as string ?? '',
          status: normalizeStatus(raw.status as string),
          priority: (raw.priority as number) ?? 5,
          progress: (raw.progress as number) ?? 0,
          subtasks: rawSubtasks.map(toSubtask),
          createdAt: (raw.createdAt as string) ?? new Date().toISOString(),
          updatedAt: raw.updatedAt as string | undefined,
          completedAt: raw.completedAt as string | undefined,
        };
        set((s) => ({
          tasks: s.tasks.map((t) => (t.id === id ? task : t)),
        }));
        return task;
      }
      return null;
    } catch (e) {
      console.error('Failed to fetch task:', e);
      return null;
    }
  },

  addTask: async (description) => {
    set({ error: null });
    try {
      const res = await taskApi.create(description);
      if (res.success && res.data) {
        const raw = res.data as unknown as Record<string, unknown>;
        const taskId = raw.id as string;
        const task: TaskPlan = {
          id: taskId,
          name: raw.name as string,
          description: raw.description as string | undefined,
          input: description,
          status: normalizeStatus(raw.status as string),
          priority: (raw.priority as number) ?? 5,
          progress: 0,
          subtasks: [],
          createdAt: (raw.createdAt as string) ?? new Date().toISOString(),
        };
        set((s) => ({ tasks: [task, ...s.tasks], selectedTaskId: taskId }));

        // Fetch full task with subtasks from decomposition
        const fullTask = await get().fetchTask(taskId);
        return fullTask ?? task;
      } else {
        set({ error: res.error || 'Failed to create task' });
        return null;
      }
    } catch (e) {
      set({ error: (e as Error).message });
      return null;
    }
  },

  startTask: async (id) => {
    set({ error: null });
    try {
      const res = await taskApi.start(id);
      if (res.success) {
        set((s) => ({
          tasks: s.tasks.map((t) =>
            t.id === id ? { ...t, status: TaskStatus.Running } : t
          ),
        }));
      } else {
        set({ error: res.error || 'Failed to start task' });
      }
    } catch (e) {
      set({ error: (e as Error).message });
    }
  },

  cancelTask: async (id) => {
    set({ error: null });
    try {
      const res = await taskApi.cancel(id);
      if (res.success) {
        set((s) => ({
          tasks: s.tasks.map((t) =>
            t.id === id ? { ...t, status: TaskStatus.Failed } : t
          ),
        }));
      } else {
        set({ error: res.error || 'Failed to cancel task' });
      }
    } catch (e) {
      set({ error: (e as Error).message });
    }
  },

  updateTask: (id, status, progress) =>
    set((s) => ({
      tasks: s.tasks.map((t) =>
        t.id === id
          ? { ...t, status, progress, ...(status === TaskStatus.Done ? { completedAt: new Date().toISOString() } : {}) }
          : t
      ),
    })),

  updateSubtask: (taskId, subtaskId, status, progress, extra?: Partial<Subtask>) =>
    set((s) => ({
      tasks: s.tasks.map((t) =>
        t.id === taskId
          ? {
              ...t,
              subtasks: t.subtasks.map((st) =>
                st.id === subtaskId ? { ...st, status, progress, ...extra } : st
              ),
            }
          : t
      ),
    })),

  selectTask: (id) => set({ selectedTaskId: id, selectedSubtaskId: null }),
  selectSubtask: (id) => set({ selectedSubtaskId: id }),
}));