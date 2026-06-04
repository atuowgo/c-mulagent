import { create } from 'zustand';

export type PageKey = 'dashboard' | 'orchestrator' | 'results' | 'templates' | 'skills';

interface UIStore {
  currentPage: PageKey;
  rightPanelOpen: boolean;
  setPage: (page: PageKey) => void;
  toggleRightPanel: () => void;
  setRightPanelOpen: (open: boolean) => void;
}

export const useUIStore = create<UIStore>((set) => ({
  currentPage: 'dashboard',
  rightPanelOpen: true,

  setPage: (page) => set({ currentPage: page }),
  toggleRightPanel: () => set((s) => ({ rightPanelOpen: !s.rightPanelOpen })),
  setRightPanelOpen: (open) => set({ rightPanelOpen: open }),
}));