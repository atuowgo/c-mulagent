package com.cmulagent.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SkillTemplateRepository {

    private final JdbcTemplate jdbcTemplate;

    public SkillTemplateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SkillTemplateEntity save(SkillTemplateEntity entity) {
        return entity;
    }

    public Optional<SkillTemplateEntity> findById(String id) {
        return Optional.empty();
    }

    public Optional<SkillTemplateEntity> findByName(String name) {
        return Optional.empty();
    }

    public List<SkillTemplateEntity> findAll() {
        return List.of();
    }

    public List<SkillTemplateEntity> findByCategory(String category) {
        return List.of();
    }

    public void deleteById(String id) {
    }
}