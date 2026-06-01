package com.cmulagent.core.skill;

public class SkillNotFoundException extends RuntimeException {

    private final String skillName;

    public SkillNotFoundException(String skillName, String message) {
        super(message);
        this.skillName = skillName;
    }

    public SkillNotFoundException(String skillName, String message, Throwable cause) {
        super(message, cause);
        this.skillName = skillName;
    }

    public String getSkillName() {
        return skillName;
    }
}