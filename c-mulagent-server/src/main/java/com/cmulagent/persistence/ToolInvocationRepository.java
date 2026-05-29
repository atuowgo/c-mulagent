package com.cmulagent.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ToolInvocationRepository {

    private final JdbcTemplate jdbcTemplate;

    public ToolInvocationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ToolInvocationEntity save(ToolInvocationEntity entity) {
        return entity;
    }

    public Optional<ToolInvocationEntity> findById(String id) {
        return Optional.empty();
    }

    public List<ToolInvocationEntity> findByAgentExecutionId(String agentExecutionId) {
        return List.of();
    }
}