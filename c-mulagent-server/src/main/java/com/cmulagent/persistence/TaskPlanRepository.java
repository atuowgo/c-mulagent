package com.cmulagent.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TaskPlanRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<TaskPlanEntity> rowMapper = (rs, rowNum) -> TaskPlanEntity.builder()
            .id(rs.getString("id"))
            .name(rs.getString("name"))
            .description(rs.getString("description"))
            .status(rs.getString("status"))
            .priority(rs.getInt("priority"))
            .parentId(rs.getString("parent_id"))
            .context(rs.getString("context"))
            .metadata(rs.getString("metadata"))
            .createdAt(rs.getString("created_at"))
            .updatedAt(rs.getString("updated_at"))
            .completedAt(rs.getString("completed_at"))
            .build();

    public TaskPlanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public TaskPlanEntity save(TaskPlanEntity e) {
        jdbcTemplate.update(
                "INSERT OR REPLACE INTO task_plan (id, name, description, status, priority, parent_id, context, metadata, created_at, updated_at, completed_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                e.getId(), e.getName(), e.getDescription(), e.getStatus(), e.getPriority(),
                e.getParentId(), e.getContext(), e.getMetadata(),
                e.getCreatedAt(), e.getUpdatedAt(), e.getCompletedAt());
        return e;
    }

    public Optional<TaskPlanEntity> findById(String id) {
        List<TaskPlanEntity> results = jdbcTemplate.query(
                "SELECT * FROM task_plan WHERE id = ?", rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<TaskPlanEntity> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM task_plan ORDER BY created_at DESC", rowMapper);
    }

    public void updateStatus(String id, String status) {
        jdbcTemplate.update(
                "UPDATE task_plan SET status = ?, updated_at = datetime('now') WHERE id = ?",
                status, id);
    }

    public void deleteById(String id) {
        jdbcTemplate.update("DELETE FROM task_plan WHERE id = ?", id);
    }
}