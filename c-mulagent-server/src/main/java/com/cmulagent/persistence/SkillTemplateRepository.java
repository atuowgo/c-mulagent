package com.cmulagent.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SkillTemplateRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<SkillTemplateEntity> rowMapper = (rs, rowNum) -> SkillTemplateEntity.builder()
            .id(rs.getString("id"))
            .name(rs.getString("name"))
            .description(rs.getString("description"))
            .category(rs.getString("category"))
            .promptTemplate(rs.getString("prompt_template"))
            .toolBindings(rs.getString("tool_bindings"))
            .inputSchema(rs.getString("input_schema"))
            .outputSchema(rs.getString("output_schema"))
            .version(rs.getString("version"))
            .enabled(rs.getInt("enabled") != 0)
            .createdAt(rs.getString("created_at"))
            .updatedAt(rs.getString("updated_at"))
            .build();

    public SkillTemplateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SkillTemplateEntity save(SkillTemplateEntity e) {
        jdbcTemplate.update(
                "INSERT OR REPLACE INTO skill_template (id, name, description, category, prompt_template, "
                        + "tool_bindings, input_schema, output_schema, version, enabled, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                e.getId(), e.getName(), e.getDescription(), e.getCategory(), e.getPromptTemplate(),
                e.getToolBindings(), e.getInputSchema(), e.getOutputSchema(),
                e.getVersion(), e.getEnabled() != null && e.getEnabled() ? 1 : 0,
                e.getCreatedAt(), e.getUpdatedAt());
        return e;
    }

    public Optional<SkillTemplateEntity> findById(String id) {
        List<SkillTemplateEntity> results = jdbcTemplate.query(
                "SELECT * FROM skill_template WHERE id = ?", rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<SkillTemplateEntity> findByName(String name) {
        List<SkillTemplateEntity> results = jdbcTemplate.query(
                "SELECT * FROM skill_template WHERE name = ?", rowMapper, name);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<SkillTemplateEntity> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM skill_template ORDER BY name", rowMapper);
    }

    public List<SkillTemplateEntity> findByCategory(String category) {
        return jdbcTemplate.query(
                "SELECT * FROM skill_template WHERE category = ? ORDER BY name", rowMapper, category);
    }

    public void deleteById(String id) {
        jdbcTemplate.update("DELETE FROM skill_template WHERE id = ?", id);
    }
}