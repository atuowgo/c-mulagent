import { useEffect, useState } from 'react';
import { skillApi } from '../api/client';
import type { Skill } from '../types/skill';

export function Skills() {
  const [skills, setSkills] = useState<Skill[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    skillApi.list().then((res) => {
      if (res.success && res.data) {
        setSkills(res.data.items ?? []);
      }
    }).catch(() => {}).finally(() => setLoading(false));
  }, []);

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

      {loading ? (
        <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>加载中...</p>
      ) : (
        <div className="skill-grid">
          {skills.map((skill) => (
            <div key={skill.id} className="skill-card">
              <div className="skill-card-name">{skill.name}</div>
              <div className="skill-card-desc">{skill.description}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}