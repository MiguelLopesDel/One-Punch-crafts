package com.onepunchcrafts.common.skills;

public class SkillExecutionResult {
    public static final SkillExecutionResult CONTINUE = new SkillExecutionResult(Type.CONTINUE, null);

    public enum Type {CONTINUE, SWITCH_SKILL, PERFORM_ATTACK_AND_SWITCH_SKILL}

    private final Type type;
    private final Object data;

    private SkillExecutionResult(Type type, Object data) {
        this.type = type;
        this.data = data;
    }

    public static SkillExecutionResult switchTo(int skillIndex) {
        return new SkillExecutionResult(Type.SWITCH_SKILL, skillIndex);
    }
}
