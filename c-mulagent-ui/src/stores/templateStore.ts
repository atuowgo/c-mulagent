import { create } from 'zustand';
import type { TaskTemplate } from '../types/template';
import { templateApi } from '../api/client';

function toTemplate(raw: Record<string, unknown>): TaskTemplate {
  return {
    id: raw.id as string,
    name: raw.name as string,
    description: raw.description as string ?? '',
    category: raw.category as string | undefined,
    planTemplate: raw.planTemplate as string ?? '',
    agentBindings: raw.agentBindings,
    skillBindings: raw.skillBindings,
    toolBindings: raw.toolBindings,
    version: raw.version as string ?? '1.0.0',
    enabled: (raw.enabled as boolean) ?? true,
    createdAt: raw.createdAt as string ?? '',
    updatedAt: raw.updatedAt as string | undefined,
  };
}

interface TemplateStore {
  templates: TaskTemplate[];
  loading: boolean;
  error: string | null;
  selectedTemplateId: string | null;
  fetchTemplates: () => Promise<void>;
  createTemplate: (data: Partial<TaskTemplate>) => Promise<TaskTemplate | null>;
  updateTemplate: (id: string, data: Partial<TaskTemplate>) => Promise<void>;
  deleteTemplate: (id: string) => Promise<void>;
  selectTemplate: (id: string | null) => void;
}

export const useTemplateStore = create<TemplateStore>((set, get) => ({
  templates: [],
  loading: false,
  error: null,
  selectedTemplateId: null,

  fetchTemplates: async () => {
    set({ loading: true, error: null });
    try {
      const res = await templateApi.list();
      if (res.success && res.data) {
        const items = (res.data.items as Array<Record<string, unknown>>) ?? [];
        set({ templates: items.map(toTemplate), loading: false });
      } else {
        set({ error: res.error || '获取模板列表失败', loading: false });
      }
    } catch (e) {
      set({ error: (e as Error).message, loading: false });
    }
  },

  createTemplate: async (data) => {
    set({ error: null });
    try {
      const res = await templateApi.create(data);
      if (res.success && res.data) {
        const tmpl = toTemplate(res.data as unknown as Record<string, unknown>);
        set((s) => ({ templates: [tmpl, ...s.templates] }));
        return tmpl;
      }
      set({ error: res.error || '创建模板失败' });
      return null;
    } catch (e) {
      set({ error: (e as Error).message });
      return null;
    }
  },

  updateTemplate: async (id, data) => {
    set({ error: null });
    try {
      const res = await templateApi.update(id, data);
      if (res.success && res.data) {
        const updated = toTemplate(res.data as unknown as Record<string, unknown>);
        set((s) => ({
          templates: s.templates.map((t) => (t.id === id ? updated : t)),
        }));
      } else {
        set({ error: res.error || '更新模板失败' });
      }
    } catch (e) {
      set({ error: (e as Error).message });
    }
  },

  deleteTemplate: async (id) => {
    set({ error: null });
    try {
      const res = await templateApi.delete(id);
      if (res.success) {
        set((s) => ({
          templates: s.templates.filter((t) => t.id !== id),
          selectedTemplateId: s.selectedTemplateId === id ? null : s.selectedTemplateId,
        }));
      } else {
        set({ error: res.error || '删除模板失败' });
      }
    } catch (e) {
      set({ error: (e as Error).message });
    }
  },

  selectTemplate: (id) => set({ selectedTemplateId: id }),
}));