package com.cmulagent.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TaskTemplateRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<TaskTemplateEntity> rowMapper = (rs, rowNum) -> TaskTemplateEntity.builder()
            .id(rs.getString("id"))
            .name(rs.getString("name"))
            .description(rs.getString("description"))
            .category(rs.getString("category"))
            .planTemplate(rs.getString("plan_template"))
            .agentBindings(rs.getString("agent_bindings"))
            .skillBindings(rs.getString("skill_bindings"))
            .toolBindings(rs.getString("tool_bindings"))
            .version(rs.getString("version"))
            .enabled(rs.getInt("enabled") != 0)
            .createdAt(rs.getString("created_at"))
            .updatedAt(rs.getString("updated_at"))
            .build();

    public TaskTemplateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public TaskTemplateEntity save(TaskTemplateEntity e) {
        jdbcTemplate.update(
                "INSERT OR REPLACE INTO task_template (id, name, description, category, plan_template, "
                        + "agent_bindings, skill_bindings, tool_bindings, version, enabled, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                e.getId(), e.getName(), e.getDescription(), e.getCategory(), e.getPlanTemplate(),
                e.getAgentBindings(), e.getSkillBindings(), e.getToolBindings(),
                e.getVersion(), e.getEnabled() != null && e.getEnabled() ? 1 : 0,
                e.getCreatedAt(), e.getUpdatedAt());
        return e;
    }

    public Optional<TaskTemplateEntity> findById(String id) {
        List<TaskTemplateEntity> results = jdbcTemplate.query(
                "SELECT * FROM task_template WHERE id = ?", rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<TaskTemplateEntity> findByName(String name) {
        List<TaskTemplateEntity> results = jdbcTemplate.query(
                "SELECT * FROM task_template WHERE name = ?", rowMapper, name);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<TaskTemplateEntity> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM task_template ORDER BY name", rowMapper);
    }

    public void deleteById(String id) {
        jdbcTemplate.update("DELETE FROM task_template WHERE id = ?", id);
    }
}