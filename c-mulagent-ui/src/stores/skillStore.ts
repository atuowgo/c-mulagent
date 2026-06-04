import { create } from 'zustand';
import type { Skill } from '../types/skill';
import { skillApi } from '../api/client';

function toSkill(raw: unknown): Skill {
  const r = raw as Record<string, unknown>;
  return {
    id: r.id as string,
    name: r.name as string,
    description: r.description as string ?? '',
    category: r.category as string | undefined,
    promptTemplate: r.promptTemplate as string | undefined,
    toolBindings: r.toolBindings,
    inputSchema: r.inputSchema,
    outputSchema: r.outputSchema,
    version: r.version as string ?? '1.0.0',
    enabled: (r.enabled as boolean) ?? true,
    createdAt: r.createdAt as string ?? '',
    updatedAt: r.updatedAt as string | undefined,
  };
}

interface SkillStore {
  skills: Skill[];
  loading: boolean;
  error: string | null;
  selectedSkillId: string | null;
  fetchSkills: () => Promise<void>;
  createSkill: (data: Partial<Skill>) => Promise<Skill | null>;
  updateSkill: (id: string, data: Partial<Skill>) => Promise<void>;
  deleteSkill: (id: string) => Promise<void>;
  selectSkill: (id: string | null) => void;
}

export const useSkillStore = create<SkillStore>((set, get) => ({
  skills: [],
  loading: false,
  error: null,
  selectedSkillId: null,

  fetchSkills: async () => {
    set({ loading: true, error: null });
    try {
      const res = await skillApi.list();
      if (res.success && res.data) {
        const items = res.data.items ?? [];
        set({ skills: items.map(toSkill), loading: false });
      } else {
        set({ error: res.error || '获取Skill列表失败', loading: false });
      }
    } catch (e) {
      set({ error: (e as Error).message, loading: false });
    }
  },

  createSkill: async (data) => {
    set({ error: null });
    try {
      const res = await skillApi.create(data);
      if (res.success && res.data) {
        const skill = toSkill(res.data);
        set((s) => ({ skills: [skill, ...s.skills] }));
        return skill;
      }
      set({ error: res.error || '创建Skill失败' });
      return null;
    } catch (e) {
      set({ error: (e as Error).message });
      return null;
    }
  },

  updateSkill: async (id, data) => {
    set({ error: null });
    try {
      const res = await skillApi.update(id, data);
      if (res.success && res.data) {
        const updated = toSkill(res.data);
        set((s) => ({
          skills: s.skills.map((sk) => (sk.id === id ? updated : sk)),
        }));
      } else {
        set({ error: res.error || '更新Skill失败' });
      }
    } catch (e) {
      set({ error: (e as Error).message });
    }
  },

  deleteSkill: async (id) => {
    set({ error: null });
    try {
      const res = await skillApi.delete(id);
      if (res.success) {
        set((s) => ({
          skills: s.skills.filter((sk) => sk.id !== id),
          selectedSkillId: s.selectedSkillId === id ? null : s.selectedSkillId,
        }));
      } else {
        set({ error: res.error || '删除Skill失败' });
      }
    } catch (e) {
      set({ error: (e as Error).message });
    }
  },

  selectSkill: (id) => set({ selectedSkillId: id }),
}));