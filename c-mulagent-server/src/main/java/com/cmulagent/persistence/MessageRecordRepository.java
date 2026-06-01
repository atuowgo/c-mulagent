package com.cmulagent.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class MessageRecordRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<MessageRecordEntity> ROW_MAPPER = (rs, __) -> {
        Object tokenCountObj = rs.getObject("token_count");
        return MessageRecordEntity.builder()
                .id(rs.getString("id"))
                .agentExecutionId(rs.getString("agent_execution_id"))
                .role(rs.getString("role"))
                .content(rs.getString("content"))
                .model(rs.getString("model"))
                .tokenCount(tokenCountObj != null ? ((Number) tokenCountObj).intValue() : null)
                .metadata(rs.getString("metadata"))
                .createdAt(rs.getString("created_at"))
                .build();
    };

    public MessageRecordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public MessageRecordEntity save(MessageRecordEntity entity) {
        jdbcTemplate.update(
                "INSERT INTO message_record (id, agent_execution_id, role, content, model, token_count, metadata, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                entity.getId(),
                entity.getAgentExecutionId(),
                entity.getRole(),
                entity.getContent(),
                entity.getModel(),
                entity.getTokenCount(),
                entity.getMetadata(),
                entity.getCreatedAt()
        );
        return entity;
    }

    public Optional<MessageRecordEntity> findById(String id) {
        List<MessageRecordEntity> results = jdbcTemplate.query(
                "SELECT * FROM message_record WHERE id = ?",
                ROW_MAPPER,
                id
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<MessageRecordEntity> findByAgentExecutionId(String agentExecutionId) {
        return jdbcTemplate.query(
                "SELECT * FROM message_record WHERE agent_execution_id = ? ORDER BY created_at",
                ROW_MAPPER,
                agentExecutionId
        );
    }

    public void batchSave(List<MessageRecordEntity> entities) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO message_record (id, agent_execution_id, role, content, model, token_count, metadata, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                entities,
                entities.size(),
                (PreparedStatement ps, MessageRecordEntity entity) -> {
                    ps.setString(1, entity.getId());
                    ps.setString(2, entity.getAgentExecutionId());
                    ps.setString(3, entity.getRole());
                    ps.setString(4, entity.getContent());
                    ps.setString(5, entity.getModel());
                    if (entity.getTokenCount() != null) {
                        ps.setInt(6, entity.getTokenCount());
                    } else {
                        ps.setNull(6, java.sql.Types.INTEGER);
                    }
                    ps.setString(7, entity.getMetadata());
                    ps.setString(8, entity.getCreatedAt());
                }
        );
    }
}