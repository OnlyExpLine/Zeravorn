package com.zeravorn.hero;

/** The authoritative stats currently effective for a hero. Item and buff modifiers extend this model later. */
public record EffectiveHeroStats(int maxHealth, double healthRegen, int maxMana, double manaRegen,
                                 int attack, double moveSpeed, double attackInterval) {
    public static EffectiveHeroStats from(HeroDefinition definition, int level) {
        HeroStats base = definition.statsAt(level);
        return new EffectiveHeroStats(base.health(), base.healthRegen(), base.maxMana(), base.manaRegen(),
                base.attack(), definition.moveSpeed(), definition.baseAttackInterval());
    }
}
