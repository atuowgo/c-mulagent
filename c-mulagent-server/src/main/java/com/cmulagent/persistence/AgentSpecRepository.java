package com.cmulagent.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AgentSpecRepository {

    private final JdbcTemplate jdbcTemplate;

    public AgentSpecRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AgentSpecEntity save(AgentSpecEntity entity) {
        return entity;
    }

    public Optional<AgentSpecEntity> findById(String id) {
        return Optional.empty();
    }

    public Optional<AgentSpecEntity> findByName(String name) {
        return Optional.empty();
    }

    public List<AgentSpecEntity> findAll() {
        return List.of();
    }

    public void deleteById(String id) {
    }
}