package com.zeravorn.hero;

import com.zeravorn.ability.AbilityDefinition;
import com.zeravorn.ability.AbilitySlot;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class HeroDefinition {
    public static final int MAX_LEVEL = 10;

    private final String id;
    private final HeroClass heroClass;
    private final DamageType damageType;
    private final List<HeroStats> statsByLevel;
    private final double hitboxWidth;
    private final double hitboxHeight;
    private final double moveSpeed;
    private final double baseAttackInterval;
    private final Map<AbilitySlot, AbilityDefinition> abilities;

    public HeroDefinition(String id, HeroClass heroClass, DamageType damageType, List<HeroStats> statsByLevel,
                          double hitboxWidth, double hitboxHeight, double moveSpeed, double baseAttackInterval,
                          Map<AbilitySlot, AbilityDefinition> abilities) {
        this.id = Objects.requireNonNull(id, "id");
        this.heroClass = Objects.requireNonNull(heroClass, "heroClass");
        this.damageType = Objects.requireNonNull(damageType, "damageType");
        this.statsByLevel = List.copyOf(statsByLevel);
        if (id.isBlank() || this.statsByLevel.size() != MAX_LEVEL || hitboxWidth <= 0 || hitboxHeight <= 0
                || moveSpeed <= 0 || baseAttackInterval <= 0) {
            throw new IllegalArgumentException("Invalid hero definition");
        }
        this.hitboxWidth = hitboxWidth;
        this.hitboxHeight = hitboxHeight;
        this.moveSpeed = moveSpeed;
        this.baseAttackInterval = baseAttackInterval;
        EnumMap<AbilitySlot, AbilityDefinition> copied = new EnumMap<>(AbilitySlot.class);
        copied.putAll(Objects.requireNonNull(abilities, "abilities"));
        this.abilities = Collections.unmodifiableMap(copied);
    }

    public String id() { return id; }
    public HeroClass heroClass() { return heroClass; }
    public DamageType damageType() { return damageType; }
    public List<HeroStats> statsByLevel() { return statsByLevel; }
    public HeroStats statsAt(int level) {
        if (level < 1 || level > MAX_LEVEL) throw new IllegalArgumentException("Level must be 1-10");
        return statsByLevel.get(level - 1);
    }
    public double hitboxWidth() { return hitboxWidth; }
    public double hitboxHeight() { return hitboxHeight; }
    public double moveSpeed() { return moveSpeed; }
    public double baseAttackInterval() { return baseAttackInterval; }
    public Map<AbilitySlot, AbilityDefinition> abilities() { return abilities; }
    public AbilityDefinition ability(AbilitySlot slot) { return abilities.get(slot); }
    public AbilityDefinition abilityById(String abilityId) {
        return abilities.values().stream()
                .filter(ability -> ability.id().equals(abilityId))
                .findFirst()
                .orElse(null);
    }
}
