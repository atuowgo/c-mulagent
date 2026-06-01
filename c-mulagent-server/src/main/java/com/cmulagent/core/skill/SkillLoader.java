package com.cmulagent.core.skill;

import com.cmulagent.persistence.SkillTemplateEntity;
import com.cmulagent.persistence.SkillTemplateRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SkillLoader {

    private static final Logger log = LoggerFactory.getLogger(SkillLoader.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> STRING_OBJECT_MAP_TYPE = new TypeReference<>() {};

    private final SkillTemplateRepository repository;

    public SkillLoader(SkillTemplateRepository repository) {
        this.repository = repository;
    }

    public List<SkillTemplate> loadAll() {
        log.info("Loading all skill templates");
        List<SkillTemplateEntity> entities = repository.findAll();
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    public SkillTemplate loadByName(String name) {
        log.debug("Loading skill by name: {}", name);
        Optional<SkillTemplateEntity> entity = repository.findByName(name);
        if (entity.isEmpty()) {
            throw new SkillNotFoundException(name, "Skill not found: " + name);
        }
        return toDomain(entity.get());
    }

    public List<SkillTemplate> loadByCategory(String category) {
        log.debug("Loading skills by category: {}", category);
        List<SkillTemplateEntity> entities;
        if (category == null || category.isBlank()) {
            entities = repository.findAll();
        } else {
            entities = repository.findByCategory(category);
        }
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    public List<SkillTemplate> loadEnabled() {
        log.info("Loading enabled skill templates");
        List<SkillTemplateEntity> entities = repository.findAll();
        return entities.stream()
                .filter(e -> e.getEnabled() != null && e.getEnabled())
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private SkillTemplate toDomain(SkillTemplateEntity entity) {
        return SkillTemplate.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .promptTemplate(entity.getPromptTemplate())
                .version(entity.getVersion())
                .toolBindings(parseStringList(entity.getToolBindings()))
                .inputSchema(parseStringObjectMap(entity.getInputSchema()))
                .outputSchema(parseStringObjectMap(entity.getOutputSchema()))
                .enabled(entity.getEnabled())
                .build();
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (Exception e) {
            log.warn("Failed to parse toolBindings JSON: {}", json, e);
            return Collections.emptyList();
        }
    }

    private Map<String, Object> parseStringObjectMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, STRING_OBJECT_MAP_TYPE);
        } catch (Exception e) {
            log.warn("Failed to parse schema JSON: {}", json, e);
            return Collections.emptyMap();
        }
    }
}