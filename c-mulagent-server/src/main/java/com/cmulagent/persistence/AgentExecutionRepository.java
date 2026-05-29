package com.cmulagent.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AgentExecutionRepository {

    private final JdbcTemplate jdbcTemplate;

    public AgentExecutionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AgentExecutionEntity save(AgentExecutionEntity entity) {
        return entity;
    }

    public Optional<AgentExecutionEntity> findById(String id) {
        return Optional.empty();
    }

    public List<AgentExecutionEntity> findBySubtaskId(String subtaskId) {
        return List.of();
    }

    public void deleteById(String id) {
    }
}