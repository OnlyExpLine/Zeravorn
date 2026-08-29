package com.zeravorn.hero;

public final class LevelService {
    public int addExperience(HeroRuntime runtime, int amount) {
        if (amount < 0) throw new IllegalArgumentException("Experience cannot be negative");
        int previousLevel = runtime.level();
        int newExperience = Math.min(HeroProgressionConfig.maxXp(), runtime.experience() + amount);
        int newLevel = previousLevel;
        while (newLevel < HeroDefinition.MAX_LEVEL && newExperience >= HeroProgressionConfig.xpToLevel(newLevel + 1)) newLevel++;
        if (newLevel > previousLevel) {
			runtime.applyProgression(newExperience, newLevel, newLevel - previousLevel);
		} else {
			runtime.applyProgression(newExperience, previousLevel, 0);
        }
        return newLevel - previousLevel;
    }
}
