import type { ReactNode } from 'react';
import { Sidebar } from './Sidebar';
import { Header } from './Header';
import { useUIStore } from '../stores/uiStore';

interface Props {
  children: ReactNode;
  rightPanel?: ReactNode;
}

export function Layout({ children, rightPanel }: Props) {
  const rightPanelOpen = useUIStore((s) => s.rightPanelOpen);
  const toggleRightPanel = useUIStore((s) => s.toggleRightPanel);

  return (
    <div className="app-layout">
      <Sidebar />
      <div className="app-main">
        <Header />
        <div className="app-content">
          <main className="app-page">{children}</main>
          <aside className={`right-panel ${rightPanelOpen ? '' : 'collapsed'}`}>
            <div className="right-panel-header">
              <span>详情面板</span>
              <button onClick={toggleRightPanel} style={{ padding: '2px 8px', fontSize: 12 }}>
                {rightPanelOpen ? '-' : '+'}
              </button>
            </div>
            <div className="right-panel-content">{rightPanel ?? <span style={{ color: 'var(--text-muted)', fontSize: 13 }}>选择一个Agent或任务查看详情</span>}</div>
          </aside>
        </div>
      </div>
    </div>
  );
}