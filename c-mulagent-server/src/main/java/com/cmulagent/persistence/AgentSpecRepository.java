package com.cmulagent.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class AgentSpecRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<AgentSpecEntity> agentSpecRowMapper = new RowMapper<AgentSpecEntity>() {
        @Override
        public AgentSpecEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            return AgentSpecEntity.builder()
                    .id(rs.getString("id"))
                    .name(rs.getString("name"))
                    .role(rs.getString("role"))
                    .baseUrl(rs.getString("base_url"))
                    .model(rs.getString("model"))
                    .apiKey(rs.getString("api_key"))
                    .tools(rs.getString("tools"))
                    .maxSteps(rs.getObject("max_steps") != null ? rs.getInt("max_steps") : null)
                    .outputFormat(rs.getString("output_format"))
                    .enabled(rs.getInt("enabled") == 1)
                    .createdAt(rs.getString("created_at"))
                    .updatedAt(rs.getString("updated_at"))
                    .build();
        }
    };

    public AgentSpecRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AgentSpecEntity save(AgentSpecEntity entity) {
        if (entity.getId() == null || entity.getId().isBlank()) {
            entity.setId(java.util.UUID.randomUUID().toString());
        }
        String now = java.time.LocalDateTime.now().toString();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);

        jdbcTemplate.update(
                "INSERT OR REPLACE INTO agent_spec (id, name, role, base_url, model, api_key, tools, max_steps, output_format, enabled, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                entity.getId(),
                entity.getName(),
                entity.getRole(),
                entity.getBaseUrl(),
                entity.getModel(),
                entity.getApiKey(),
                entity.getTools(),
                entity.getMaxSteps(),
                entity.getOutputFormat(),
                entity.getEnabled() != null && entity.getEnabled() ? 1 : 0,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
        return entity;
    }

    public Optional<AgentSpecEntity> findById(String id) {
        List<AgentSpecEntity> results = jdbcTemplate.query(
                "SELECT * FROM agent_spec WHERE id = ?",
                agentSpecRowMapper,
                id
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<AgentSpecEntity> findByName(String name) {
        List<AgentSpecEntity> results = jdbcTemplate.query(
                "SELECT * FROM agent_spec WHERE name = ?",
                agentSpecRowMapper,
                name
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<AgentSpecEntity> findAll() {
        return jdbcTemplate.query("SELECT * FROM agent_spec ORDER BY name", agentSpecRowMapper);
    }

    public void deleteById(String id) {
        jdbcTemplate.update("DELETE FROM agent_spec WHERE id = ?", id);
    }
}