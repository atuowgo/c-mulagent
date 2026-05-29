package com.cmulagent.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SubtaskRepository {

    private final JdbcTemplate jdbcTemplate;

    public SubtaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SubtaskEntity save(SubtaskEntity entity) {
        return entity;
    }

    public Optional<SubtaskEntity> findById(String id) {
        return Optional.empty();
    }

    public List<SubtaskEntity> findByTaskPlanId(String taskPlanId) {
        return List.of();
    }

    public void deleteById(String id) {
    }
}