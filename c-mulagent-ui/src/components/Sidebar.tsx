import { useUIStore, type PageKey } from '../stores/uiStore';

const navItems: { key: PageKey; label: string; icon: string }[] = [
  { key: 'dashboard', label: '仪表盘', icon: '~' },
  { key: 'orchestrator', label: '任务编排', icon: '>' },
  { key: 'results', label: '运行结果', icon: '#' },
  { key: 'templates', label: '模板市场', icon: '#' },
  { key: 'skills', label: 'Skill库', icon: '*' },
];

export function Sidebar() {
  const currentPage = useUIStore((s) => s.currentPage);
  const setPage = useUIStore((s) => s.setPage);

  return (
    <aside className="sidebar">
      <div className="sidebar-logo">C-MulAgent</div>
      <nav className="sidebar-nav">
        {navItems.map((item) => (
          <div
            key={item.key}
            className={`sidebar-nav-item ${currentPage === item.key ? 'active' : ''}`}
            onClick={() => setPage(item.key)}
          >
            <span className="sidebar-nav-icon">{item.icon}</span>
            {item.label}
          </div>
        ))}
      </nav>
    </aside>
  );
}