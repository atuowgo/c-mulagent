import { useRef, useEffect } from 'react';

interface LogEntry {
  time: string;
  level: 'info' | 'warn' | 'error';
  msg: string;
}

const mockLogs: LogEntry[] = [
  { time: '14:30:01', level: 'info', msg: '[Planner] 解析用户需求...' },
  { time: '14:30:02', level: 'info', msg: '[Planner] 生成任务规划, 共 5 个子任务' },
  { time: '14:30:03', level: 'info', msg: '[Coder] 开始执行 subtask: 架构设计' },
  { time: '14:30:05', level: 'warn', msg: '[Coder] 依赖項缺失，尝试自动补全' },
  { time: '14:30:08', level: 'info', msg: '[Reviewer] 收到代码审查请求' },
  { time: '14:30:10', level: 'error', msg: '[Reviewer] 静态分析发现 3 个问题' },
  { time: '14:30:12', level: 'info', msg: '[Writer] 开始生成 API 文档' },
];

interface Props {
  logs?: LogEntry[];
}

export function LogViewer({ logs = mockLogs }: Props) {
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [logs]);

  return (
    <div className="log-viewer">
      {logs.map((entry, i) => (
        <div key={i} className="log-entry">
          <span className="log-time">{entry.time}</span>
          <span className={`log-level-${entry.level}`}>[{entry.level.toUpperCase()}]</span>
          <span>{entry.msg}</span>
        </div>
      ))}
      <div ref={bottomRef} />
    </div>
  );
}