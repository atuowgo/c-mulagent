package com.cmulagent.core.skill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillTemplate {
    private String id;
    private String name;
    private String description;
    private String category;
    private String promptTemplate;
    private String version;
}