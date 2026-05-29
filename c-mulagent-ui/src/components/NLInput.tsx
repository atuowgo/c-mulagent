import { useState, type FormEvent } from 'react';

interface Props {
  onSubmit: (text: string) => void;
  disabled?: boolean;
}

export function NLInput({ onSubmit, disabled }: Props) {
  const [value, setValue] = useState('');

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!value.trim()) return;
    onSubmit(value.trim());
    setValue('');
  };

  return (
    <div className="nl-input-area">
      <form className="nl-input-form" onSubmit={handleSubmit}>
        <input
          className="nl-input-field"
          type="text"
          placeholder="描述你的需求，例如：帮我实现一个用户认证模块..."
          value={value}
          onChange={(e) => setValue(e.target.value)}
          disabled={disabled}
        />
        <button className="nl-input-submit" type="submit" disabled={disabled || !value.trim()}>
          发起任务
        </button>
      </form>
    </div>
  );
}