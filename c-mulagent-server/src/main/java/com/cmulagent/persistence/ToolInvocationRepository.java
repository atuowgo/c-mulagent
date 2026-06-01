package com.cmulagent.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ToolInvocationRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<ToolInvocationEntity> ROW_MAPPER = (rs, __) -> {
        Object durationObj = rs.getObject("duration_ms");
        return ToolInvocationEntity.builder()
                .id(rs.getString("id"))
                .agentExecutionId(rs.getString("agent_execution_id"))
                .toolName(rs.getString("tool_name"))
                .inputParams(rs.getString("input_params"))
                .outputResult(rs.getString("output_result"))
                .status(rs.getString("status"))
                .durationMs(durationObj != null ? ((Number) durationObj).longValue() : null)
                .errorMessage(rs.getString("error_message"))
                .createdAt(rs.getString("created_at"))
                .completedAt(rs.getString("completed_at"))
                .build();
    };

    public ToolInvocationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ToolInvocationEntity save(ToolInvocationEntity entity) {
        jdbcTemplate.update(
                "INSERT OR REPLACE INTO tool_invocation (id, agent_execution_id, tool_name, input_params, output_result, status, duration_ms, error_message, created_at, completed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                entity.getId(),
                entity.getAgentExecutionId(),
                entity.getToolName(),
                entity.getInputParams(),
                entity.getOutputResult(),
                entity.getStatus(),
                entity.getDurationMs(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getCompletedAt()
        );
        return entity;
    }

    public Optional<ToolInvocationEntity> findById(String id) {
        List<ToolInvocationEntity> results = jdbcTemplate.query(
                "SELECT * FROM tool_invocation WHERE id = ?",
                ROW_MAPPER,
                id
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<ToolInvocationEntity> findByAgentExecutionId(String agentExecutionId) {
        return jdbcTemplate.query(
                "SELECT * FROM tool_invocation WHERE agent_execution_id = ? ORDER BY created_at",
                ROW_MAPPER,
                agentExecutionId
        );
    }

    public void updateResult(String id, String status, String outputResult, Long durationMs, String errorMessage) {
        jdbcTemplate.update(
                "UPDATE tool_invocation SET status = ?, output_result = ?, duration_ms = ?, error_message = ?, completed_at = datetime('now') WHERE id = ?",
                status,
                outputResult,
                durationMs,
                errorMessage,
                id
        );
    }
}