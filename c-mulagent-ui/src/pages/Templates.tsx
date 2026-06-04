import { useEffect, useState } from 'react';
import { useTemplateStore } from '../stores/templateStore';
import { ErrorBanner } from '../components/ErrorBanner';
import type { TaskTemplate } from '../types/template';

const CATEGORIES = ['通用', '代码生成', '数据分析', '文档处理', '测试', '部署'];

export function Templates() {
  const {
    templates, loading, error, selectedTemplateId,
    fetchTemplates, createTemplate, updateTemplate, deleteTemplate, selectTemplate,
  } = useTemplateStore();
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState({ name: '', description: '', category: '', planTemplate: '' });
  const [filterCategory, setFilterCategory] = useState<string | null>(null);

  useEffect(() => { fetchTemplates(); }, [fetchTemplates]);

  const filtered = filterCategory
    ? templates.filter((t) => t.category === filterCategory)
    : templates;

  const selectedTemplate = templates.find((t) => t.id === selectedTemplateId) ?? null;

  const openCreate = () => {
    setEditingId(null);
    setForm({ name: '', description: '', category: '', planTemplate: '' });
    setShowForm(true);
  };

  const openEdit = (t: TaskTemplate) => {
    setEditingId(t.id);
    setForm({
      name: t.name,
      description: t.description,
      category: t.category ?? '',
      planTemplate: t.planTemplate,
    });
    setShowForm(true);
  };

  const handleSubmit = async () => {
    if (!form.name.trim() || !form.planTemplate.trim()) return;
    if (editingId) {
      await updateTemplate(editingId, {
        name: form.name.trim(),
        description: form.description.trim(),
        category: form.category || undefined,
        planTemplate: form.planTemplate.trim(),
      });
    } else {
      await createTemplate({
        name: form.name.trim(),
        description: form.description.trim(),
        category: form.category || undefined,
        planTemplate: form.planTemplate.trim(),
      });
    }
    setShowForm(false);
  };

  const handleDelete = async (id: string) => {
    if (window.confirm('确认删除此模板？')) {
      await deleteTemplate(id);
    }
  };

  return (
    <div className="app-page-scroll">
      <div className="skills-page-header">
        <div>
          <h2 style={{ fontSize: 18, fontWeight: 600, marginBottom: 4 }}>模板市场</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>
            预定义任务模板，快速复用常见任务流程
          </p>
        </div>
        <button onClick={openCreate}>新建模板</button>
      </div>

      {error && <ErrorBanner message={error} onDismiss={() => {}} />}

      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap' }}>
        <span
          onClick={() => setFilterCategory(null)}
          style={{
            padding: '4px 10px', borderRadius: 12, fontSize: 12, cursor: 'pointer',
            background: filterCategory === null ? 'var(--accent-blue)' : 'var(--bg-tertiary)',
            color: filterCategory === null ? '#fff' : 'var(--text-secondary)',
          }}
        >
          全部
        </span>
        {CATEGORIES.map((cat) => (
          <span
            key={cat}
            onClick={() => setFilterCategory(cat)}
            style={{
              padding: '4px 10px', borderRadius: 12, fontSize: 12, cursor: 'pointer',
              background: filterCategory === cat ? 'var(--accent-blue)' : 'var(--bg-tertiary)',
              color: filterCategory === cat ? '#fff' : 'var(--text-secondary)',
            }}
          >
            {cat}
          </span>
        ))}
      </div>

      {loading ? (
        <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>加载中...</p>
      ) : filtered.length === 0 ? (
        <div style={{ color: 'var(--text-muted)', textAlign: 'center', padding: 40 }}>
          {filterCategory ? `暂无"${filterCategory}"分类的模板` : '暂无模板，点击"新建模板"创建'}
        </div>
      ) : (
        <div className="skill-grid">
          {filtered.map((t) => (
            <div
              key={t.id}
              className={`skill-card ${selectedTemplateId === t.id ? 'selected' : ''}`}
              onClick={() => selectTemplate(t.id)}
              style={{ cursor: 'pointer', position: 'relative' }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <div className="skill-card-name">{t.name}</div>
                  {t.category && (
                    <span style={{
                      display: 'inline-block', padding: '1px 6px', borderRadius: 10,
                      fontSize: 10, background: 'var(--bg-tertiary)', color: 'var(--text-secondary)',
                      marginBottom: 4,
                    }}>
                      {t.category}
                    </span>
                  )}
                </div>
                <span style={{ fontSize: 10, color: 'var(--text-muted)' }}>v{t.version}</span>
              </div>
              <div className="skill-card-desc" style={{ marginBottom: 8 }}>
                {t.description || '无描述'}
              </div>
              <div style={{
                fontSize: 11, color: 'var(--text-muted)',
                background: 'var(--bg-primary)', padding: '6px 8px', borderRadius: 4,
                maxHeight: 60, overflow: 'hidden', textOverflow: 'ellipsis',
                whiteSpace: 'pre-wrap', wordBreak: 'break-all',
                fontFamily: 'monospace',
              }}>
                {t.planTemplate.length > 120
                  ? t.planTemplate.slice(0, 120) + '...'
                  : t.planTemplate}
              </div>
              <div style={{ display: 'flex', gap: 6, marginTop: 8 }}>
                <button
                  style={{ padding: '2px 8px', fontSize: 11 }}
                  onClick={(e) => { e.stopPropagation(); openEdit(t); }}
                >
                  编辑
                </button>
                <button
                  style={{
                    padding: '2px 8px', fontSize: 11,
                    background: 'transparent', border: '1px solid var(--accent-red)', color: 'var(--accent-red)',
                  }}
                  onClick={(e) => { e.stopPropagation(); handleDelete(t.id); }}
                >
                  删除
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {showForm && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100,
        }}
          onClick={() => setShowForm(false)}
        >
          <div
            style={{
              background: 'var(--bg-secondary)', borderRadius: 12, padding: 24,
              width: 520, maxHeight: '80vh', overflow: 'auto',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <h3 style={{ marginBottom: 16 }}>
              {editingId ? '编辑模板' : '新建模板'}
            </h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <div>
                <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>
                  名称 *
                </label>
                <input
                  className="nl-input-field"
                  value={form.name}
                  onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                  placeholder="模板名称"
                />
              </div>
              <div>
                <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>
                  描述
                </label>
                <input
                  className="nl-input-field"
                  value={form.description}
                  onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                  placeholder="模板用途说明"
                />
              </div>
              <div>
                <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>
                  分类
                </label>
                <select
                  className="nl-input-field"
                  value={form.category}
                  onChange={(e) => setForm((f) => ({ ...f, category: e.target.value }))}
                >
                  <option value="">未分类</option>
                  {CATEGORIES.map((c) => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              </div>
              <div>
                <label style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>
                  模板内容 * (使用 {'{{'}description{'}}'} 作为任务描述占位符)
                </label>
                <textarea
                  className="nl-input-field"
                  value={form.planTemplate}
                  onChange={(e) => setForm((f) => ({ ...f, planTemplate: e.target.value }))}
                  placeholder={`示例：
任务名称：代码审查
子任务：
1. 静态分析 - agent: code-analyzer - 对代码进行静态分析
2. 安全检查 - agent: security-scanner - 依赖: 静态分析 - 扫描安全漏洞
3. 生成报告 - agent: report-writer - 依赖: 静态分析, 安全检查 - 汇总结果`}
                  rows={8}
                  style={{ fontFamily: 'monospace', fontSize: 12, resize: 'vertical' }}
                />
              </div>
              <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 8 }}>
                <button onClick={() => setShowForm(false)} style={{ background: 'var(--bg-tertiary)' }}>
                  取消
                </button>
                <button
                  onClick={handleSubmit}
                  disabled={!form.name.trim() || !form.planTemplate.trim()}
                >
                  {editingId ? '保存' : '创建'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}