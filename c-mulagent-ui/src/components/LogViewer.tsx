import { useRef, useEffect } from 'react';
import { useLogStore, type LogEntry } from '../stores/logStore';

export function LogViewer() {
  const logs = useLogStore((s) => s.logs);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [logs]);

  return (
    <div className="log-viewer">
      {logs.length === 0 && (
        <div style={{ color: 'var(--text-muted)', fontSize: 13, padding: 12 }}>
          暂无日志，发起任务后将在此显示运行日志
        </div>
      )}
      {logs.map((entry) => (
        <div key={entry.id} className="log-entry">
          <span className="log-time">{entry.time}</span>
          <span className={`log-level-${entry.level}`}>[{entry.level.toUpperCase()}]</span>
          <span className="log-source">[{entry.source}]</span>
          <span>{entry.message}</span>
        </div>
      ))}
      <div ref={bottomRef} />
    </div>
  );
}