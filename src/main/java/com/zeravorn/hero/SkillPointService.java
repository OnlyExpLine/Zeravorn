package com.zeravorn.hero;

public final class SkillPointService {
    public void grant(HeroRuntime runtime, int amount) {
        if (amount < 0) throw new IllegalArgumentException("Skill points cannot be negative");
        runtime.addSkillPoints(amount);
    }
}
