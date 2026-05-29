package com.cmulagent.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TaskTemplateRepository {

    private final JdbcTemplate jdbcTemplate;

    public TaskTemplateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public TaskTemplateEntity save(TaskTemplateEntity entity) {
        return entity;
    }

    public Optional<TaskTemplateEntity> findById(String id) {
        return Optional.empty();
    }

    public Optional<TaskTemplateEntity> findByName(String name) {
        return Optional.empty();
    }

    public List<TaskTemplateEntity> findAll() {
        return List.of();
    }

    public void deleteById(String id) {
    }
}