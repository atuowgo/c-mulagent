package com.cmulagent.core.skill;

import com.cmulagent.core.agent.AgentOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SkillExecutor {

    private static final Logger log = LoggerFactory.getLogger(SkillExecutor.class);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*\\}\\}");

    private final SkillLoader skillLoader;
    private final AgentOrchestrator orchestrator;

    public SkillExecutor(SkillLoader skillLoader, AgentOrchestrator orchestrator) {
        this.skillLoader = skillLoader;
        this.orchestrator = orchestrator;
    }

    public CompletableFuture<String> executeSkill(String skillName, Map<String, Object> input, String agentId) {
        SkillTemplate skill = skillLoader.loadByName(skillName);

        if (skill.getEnabled() != null && !skill.getEnabled()) {
            throw new IllegalStateException("Skill is disabled: " + skillName);
        }

        String resolvedPrompt = resolveTemplate(skill.getPromptTemplate(), input);

        log.info("Executing skill '{}' with agent '{}', resolved prompt length: {}",
                skillName, agentId, resolvedPrompt.length());

        String subtaskId = UUID.randomUUID().toString();
        return orchestrator.executeWithSkill(agentId, subtaskId, resolvedPrompt, skillName);
    }

    String resolveTemplate(String template, Map<String, Object> variables) {
        if (template == null || template.isBlank()) {
            return "";
        }
        if (variables == null || variables.isEmpty()) {
            return template;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = variables.get(varName);
            String replacement = value != null ? String.valueOf(value) : "{{" + varName + "}}";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}