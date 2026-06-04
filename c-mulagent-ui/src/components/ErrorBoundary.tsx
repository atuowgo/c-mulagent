import { Component, type ReactNode } from 'react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, error: null };

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) return this.props.fallback;
      return (
        <div style={{
          padding: 24,
          margin: 16,
          background: 'var(--bg-secondary)',
          border: '1px solid var(--accent-red)',
          borderRadius: 8,
        }}>
          <h3 style={{ color: 'var(--accent-red)', marginBottom: 8 }}>页面渲染错误</h3>
          <pre style={{
            fontSize: 12,
            color: 'var(--text-secondary)',
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-word',
          }}>
            {this.state.error?.message}
          </pre>
          <button
            onClick={() => this.setState({ hasError: false, error: null })}
            style={{ marginTop: 12 }}
          >
            重试
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}