package com.cmulagent.core.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SkillRenderer {

    private static final Logger log = LoggerFactory.getLogger(SkillRenderer.class);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*\\}\\}");

    public String renderPrompt(SkillTemplate skill, Map<String, Object> variables) {
        log.debug("Rendering prompt for skill: {}", skill.getName());
        String template = skill.getPromptTemplate();
        if (template == null || template.isBlank()) {
            log.warn("Empty prompt template for skill: {}", skill.getName());
            return "";
        }
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        return replacePlaceholders(template, variables);
    }

    public String renderSystemPrompt(SkillTemplate skill, Map<String, Object> variables) {
        log.debug("Rendering system prompt for skill: {}", skill.getName());

        StringBuilder prompt = new StringBuilder();

        // Role and description
        prompt.append("You are a ").append(skill.getName()).append(" agent.\n");
        if (skill.getDescription() != null && !skill.getDescription().isBlank()) {
            prompt.append(skill.getDescription()).append("\n");
        }
        prompt.append("\n");

        // Available tools
        List<String> toolBindings = skill.getToolBindings();
        if (toolBindings != null && !toolBindings.isEmpty()) {
            prompt.append("Available tools:\n");
            for (String tool : toolBindings) {
                prompt.append("- ").append(tool).append("\n");
            }
            prompt.append("\n");
        }

        // Rendered prompt template
        String rendered = renderPrompt(skill, variables);
        prompt.append(rendered);

        return prompt.toString();
    }

    private String replacePlaceholders(String template, Map<String, Object> variables) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = variables.get(key);
            String replacement = value != null ? value.toString() : matcher.group(0);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}