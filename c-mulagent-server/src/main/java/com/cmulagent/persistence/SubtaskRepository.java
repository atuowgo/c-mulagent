package com.cmulagent.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class SubtaskRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<SubtaskEntity> rowMapper = (rs, rowNum) -> SubtaskEntity.builder()
            .id(rs.getString("id"))
            .taskPlanId(rs.getString("task_plan_id"))
            .name(rs.getString("name"))
            .description(rs.getString("description"))
            .status(rs.getString("status"))
            .assignedAgent(rs.getString("assigned_agent"))
            .inputData(rs.getString("input_data"))
            .outputData(rs.getString("output_data"))
            .priority(rs.getInt("priority"))
            .dependencies(rs.getString("dependencies"))
            .retryCount(rs.getInt("retry_count"))
            .maxRetries(rs.getInt("max_retries"))
            .createdAt(rs.getString("created_at"))
            .updatedAt(rs.getString("updated_at"))
            .startedAt(rs.getString("started_at"))
            .completedAt(rs.getString("completed_at"))
            .build();

    public SubtaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SubtaskEntity save(SubtaskEntity e) {
        jdbcTemplate.update(
                "INSERT OR REPLACE INTO subtask (id, task_plan_id, name, description, status, assigned_agent, "
                        + "input_data, output_data, priority, dependencies, retry_count, max_retries, "
                        + "created_at, updated_at, started_at, completed_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                e.getId(), e.getTaskPlanId(), e.getName(), e.getDescription(), e.getStatus(), e.getAssignedAgent(),
                e.getInputData(), e.getOutputData(), e.getPriority(), e.getDependencies(),
                e.getRetryCount(), e.getMaxRetries(),
                e.getCreatedAt(), e.getUpdatedAt(), e.getStartedAt(), e.getCompletedAt());
        return e;
    }

    public Optional<SubtaskEntity> findById(String id) {
        List<SubtaskEntity> results = jdbcTemplate.query(
                "SELECT * FROM subtask WHERE id = ?", rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<SubtaskEntity> findByTaskPlanId(String taskPlanId) {
        return jdbcTemplate.query(
                "SELECT * FROM subtask WHERE task_plan_id = ? ORDER BY priority, created_at",
                rowMapper, taskPlanId);
    }

    public void updateStatus(String id, String status) {
        jdbcTemplate.update(
                "UPDATE subtask SET status = ?, updated_at = datetime('now') WHERE id = ?",
                status, id);
    }

    public void updateOutput(String id, String outputData, String status) {
        jdbcTemplate.update(
                "UPDATE subtask SET output_data = ?, status = ?, updated_at = datetime('now') WHERE id = ?",
                outputData, status, id);
    }

    public void batchSave(List<SubtaskEntity> entities) {
        jdbcTemplate.batchUpdate(
                "INSERT OR REPLACE INTO subtask (id, task_plan_id, name, description, status, assigned_agent, "
                        + "input_data, output_data, priority, dependencies, retry_count, max_retries, "
                        + "created_at, updated_at, started_at, completed_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                entities,
                entities.size(),
                (PreparedStatement ps, SubtaskEntity e) -> {
                    ps.setString(1, e.getId());
                    ps.setString(2, e.getTaskPlanId());
                    ps.setString(3, e.getName());
                    ps.setString(4, e.getDescription());
                    ps.setString(5, e.getStatus());
                    ps.setString(6, e.getAssignedAgent());
                    ps.setString(7, e.getInputData());
                    ps.setString(8, e.getOutputData());
                    ps.setInt(9, e.getPriority());
                    ps.setString(10, e.getDependencies());
                    ps.setInt(11, e.getRetryCount());
                    ps.setInt(12, e.getMaxRetries());
                    ps.setString(13, e.getCreatedAt());
                    ps.setString(14, e.getUpdatedAt());
                    ps.setString(15, e.getStartedAt());
                    ps.setString(16, e.getCompletedAt());
                });
    }

    public void deleteById(String id) {
        jdbcTemplate.update("DELETE FROM subtask WHERE id = ?", id);
    }
}