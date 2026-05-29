const mockSkills = [
  { id: 'sk1', name: 'code_gen', description: '根据需求描述生成代码' },
  { id: 'sk2', name: 'code_review', description: '静态分析代码质量与安全问题' },
  { id: 'sk3', name: 'planning', description: '将复杂需求分解为可执行的子任务' },
  { id: 'sk4', name: 'refactor', description: '重构现有代码提升可维护性' },
  { id: 'sk5', name: 'testing', description: '自动生成单元测试与集成测试' },
  { id: 'sk6', name: 'writing', description: '生成技术文档与API说明' },
  { id: 'sk7', name: 'translation', description: '多语言翻译与本地化' },
  { id: 'sk8', name: 'decomposition', description: '将大任务拆分为小粒度子任务' },
];

export function Skills() {
  return (
    <div>
      <div className="skills-page-header">
        <div>
          <h2 style={{ fontSize: 18, fontWeight: 600, marginBottom: 4 }}>Skill库</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>
            管理Agent可用的技能模块
          </p>
        </div>
        <button>导入Skill</button>
      </div>

      <div className="skill-grid">
        {mockSkills.map((skill) => (
          <div key={skill.id} className="skill-card">
            <div className="skill-card-name">{skill.name}</div>
            <div className="skill-card-desc">{skill.description}</div>
          </div>
        ))}
      </div>
    </div>
  );
}