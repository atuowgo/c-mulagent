package com.cmulagent.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRecordEntity {
    private String id;
    private String agentExecutionId;
    private String role;
    private String content;
    private String model;
    private Integer tokenCount;
    private String metadata;
    private String createdAt;
}