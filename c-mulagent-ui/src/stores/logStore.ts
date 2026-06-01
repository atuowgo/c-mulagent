import { create } from 'zustand';

export interface LogEntry {
  id: string;
  time: string;
  level: 'info' | 'warn' | 'error';
  source: string;
  message: string;
}

interface LogStore {
  logs: LogEntry[];
  addLog: (entry: LogEntry) => void;
  clearLogs: () => void;
}

export const useLogStore = create<LogStore>((set) => ({
  logs: [],
  addLog: (entry) => set((s) => ({ logs: [...s.logs, entry] })),
  clearLogs: () => set({ logs: [] }),
}));