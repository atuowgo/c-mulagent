package com.cmulagent.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TaskPlanRepository {

    private final JdbcTemplate jdbcTemplate;

    public TaskPlanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public TaskPlanEntity save(TaskPlanEntity entity) {
        // TODO: implement
        return entity;
    }

    public Optional<TaskPlanEntity> findById(String id) {
        // TODO: implement
        return Optional.empty();
    }

    public List<TaskPlanEntity> findAll() {
        // TODO: implement
        return List.of();
    }

    public void deleteById(String id) {
        // TODO: implement
    }
}