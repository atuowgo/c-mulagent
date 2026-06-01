package com.cmulagent.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AgentExecutionRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<AgentExecutionEntity> ROW_MAPPER = (rs, __) -> {
        Object durationObj = rs.getObject("duration_ms");
        Object totalTokensObj = rs.getObject("total_tokens");
        return AgentExecutionEntity.builder()
                .id(rs.getString("id"))
                .subtaskId(rs.getString("subtask_id"))
                .agentSpecId(rs.getString("agent_spec_id"))
                .status(rs.getString("status"))
                .startTime(rs.getString("start_time"))
                .endTime(rs.getString("end_time"))
                .durationMs(durationObj != null ? ((Number) durationObj).longValue() : null)
                .totalTokens(totalTokensObj != null ? ((Number) totalTokensObj).longValue() : null)
                .errorMessage(rs.getString("error_message"))
                .metadata(rs.getString("metadata"))
                .createdAt(rs.getString("created_at"))
                .updatedAt(rs.getString("updated_at"))
                .build();
    };

    public AgentExecutionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AgentExecutionEntity save(AgentExecutionEntity entity) {
        jdbcTemplate.update(
                "INSERT OR REPLACE INTO agent_execution (id, subtask_id, agent_spec_id, status, start_time, end_time, duration_ms, total_tokens, error_message, metadata, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                entity.getId(),
                entity.getSubtaskId(),
                entity.getAgentSpecId(),
                entity.getStatus(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getDurationMs(),
                entity.getTotalTokens(),
                entity.getErrorMessage(),
                entity.getMetadata(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
        return entity;
    }

    public Optional<AgentExecutionEntity> findById(String id) {
        List<AgentExecutionEntity> results = jdbcTemplate.query(
                "SELECT * FROM agent_execution WHERE id = ?",
                ROW_MAPPER,
                id
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<AgentExecutionEntity> findBySubtaskId(String subtaskId) {
        return jdbcTemplate.query(
                "SELECT * FROM agent_execution WHERE subtask_id = ?",
                ROW_MAPPER,
                subtaskId
        );
    }

    public void updateStatus(String id, String status, String errorMessage) {
        jdbcTemplate.update(
                "UPDATE agent_execution SET status = ?, error_message = ?, updated_at = datetime('now') WHERE id = ?",
                status,
                errorMessage,
                id
        );
    }

    public void deleteById(String id) {
        jdbcTemplate.update("DELETE FROM agent_execution WHERE id = ?", id);
    }
}