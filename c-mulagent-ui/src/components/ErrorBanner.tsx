interface Props {
  message: string;
  onDismiss?: () => void;
  onRetry?: () => void;
}

export function ErrorBanner({ message, onDismiss, onRetry }: Props) {
  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '10px 14px',
      marginBottom: 12,
      background: 'rgba(239, 68, 68, 0.1)',
      border: '1px solid rgba(239, 68, 68, 0.3)',
      borderRadius: 6,
      fontSize: 13,
      color: 'var(--accent-red)',
    }}>
      <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <span style={{ fontSize: 16 }}>⚠</span>
        <span>{message}</span>
      </span>
      <span style={{ display: 'flex', gap: 8 }}>
        {onRetry && (
          <button
            onClick={onRetry}
            style={{
              padding: '4px 12px',
              fontSize: 12,
              background: 'transparent',
              border: '1px solid var(--accent-red)',
              color: 'var(--accent-red)',
              borderRadius: 4,
              cursor: 'pointer',
            }}
          >
            重试
          </button>
        )}
        {onDismiss && (
          <button
            onClick={onDismiss}
            style={{
              padding: '4px 8px',
              fontSize: 12,
              background: 'transparent',
              border: 'none',
              color: 'var(--text-secondary)',
              cursor: 'pointer',
            }}
          >
            ✕
          </button>
        )}
      </span>
    </div>
  );
}