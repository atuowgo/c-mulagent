import { useUIStore, type PageKey } from '../stores/uiStore';

const pageTitles: Record<PageKey, string> = {
  dashboard: '仪表盘',
  orchestrator: '任务编排',
  results: '运行结果',
  skills: 'Skill库',
};

export function Header() {
  const currentPage = useUIStore((s) => s.currentPage);
  return (
    <header className="header">
      <div className="header-left">
        <span className="header-title">C-MulAgent / {pageTitles[currentPage]}</span>
      </div>
      <div className="header-right">
        <select defaultValue="default" style={{ padding: '4px 10px', fontSize: 13 }}>
          <option value="default">默认项目</option>
        </select>
        <button>新建任务</button>
        <button>通知</button>
      </div>
    </header>
  );
}