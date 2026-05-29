package com.cmulagent.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MessageRecordRepository {

    private final JdbcTemplate jdbcTemplate;

    public MessageRecordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public MessageRecordEntity save(MessageRecordEntity entity) {
        return entity;
    }

    public Optional<MessageRecordEntity> findById(String id) {
        return Optional.empty();
    }

    public List<MessageRecordEntity> findByAgentExecutionId(String agentExecutionId) {
        return List.of();
    }
}