package com.zeravorn.hero;

import com.zeravorn.ability.AbilitySlot;
import com.zeravorn.team.TeamId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class HeroRuntime {
    private final UUID owner;
    private final TeamId team;
    private final HeroDefinition definition;
    private final Map<String, Integer> abilityRanks = new HashMap<>();
    private int level = 1;
    private int experience;
	private int gold = HeroProgressionConfig.startingGold();
	private int availableSkillPoints = HeroProgressionConfig.startingSkillPoints();
    private int health;
    private int mana;
    private boolean alive = true;

    public HeroRuntime(UUID owner, TeamId team, HeroDefinition definition) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.team = Objects.requireNonNull(team, "team");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.health = definition.statsAt(level).health();
        this.mana = definition.statsAt(level).maxMana();
        for (var ability : definition.abilities().values()) abilityRanks.put(ability.id(), 0);
    }

    public UUID owner() { return owner; }
    public TeamId team() { return team; }
    public HeroDefinition definition() { return definition; }
	public HeroStats stats() { return definition.statsAt(level); }
	public EffectiveHeroStats effectiveStats() { return EffectiveHeroStats.from(definition, level); }
    public int level() { return level; }
    public int experience() { return experience; }
    public int gold() { return gold; }
    public int availableSkillPoints() { return availableSkillPoints; }
    public int health() { return health; }
    public int mana() { return mana; }
    public boolean alive() { return alive; }
    public Map<String, Integer> abilityRanks() { return Collections.unmodifiableMap(abilityRanks); }
    public int abilityRank(AbilitySlot slot) {
        var ability = definition.ability(slot);
        return ability == null ? 0 : abilityRank(ability.id());
    }
    public int abilityRank(String abilityId) { return abilityRanks.getOrDefault(abilityId, 0); }

	void applyProgression(int experience, int level, int skillPointsGained) {
		if (experience < 0 || level < 1 || level > HeroDefinition.MAX_LEVEL || skillPointsGained < 0) {
			throw new IllegalArgumentException("Invalid progression state");
		}
		this.experience = experience;
		this.level = level;
		this.health = Math.min(health, stats().health());
		this.mana = Math.min(mana, stats().maxMana());
		this.availableSkillPoints += skillPointsGained;
	}
	void addSkillPoints(int amount) { availableSkillPoints += amount; }
    public void spendSkillPoint() { if (availableSkillPoints <= 0) throw new IllegalStateException("No skill points"); availableSkillPoints--; }
	public void setAlive(boolean alive) { this.alive = alive; }
	public boolean upgradeAbility(AbilitySlot slot) {
		var ability = definition.ability(slot);
		return ability != null && upgradeAbility(ability.id());
	}
	public boolean upgradeAbility(String abilityId) {
		var ability = definition.abilityById(abilityId);
		int currentRank = abilityRank(abilityId);
		if (ability == null || level < ability.unlockLevel() || currentRank >= ability.maxRank() || availableSkillPoints <= 0) return false;
		spendSkillPoint();
		abilityRanks.put(abilityId, currentRank + 1);
		return true;
	}
}
