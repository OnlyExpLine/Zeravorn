package com.zeravorn.hero;

import com.zeravorn.item.ItemStats;

/** Authoritative effective stats after the currently equipped MOBA items. */
public record EffectiveHeroStats(int maxHealth, double healthRegen, int maxMana, double manaRegen,
                                 int attack, int abilityPower, double attackSpeedBonus,
                                 double lifesteal, double spellVamp, double moveSpeed,
                                 double attackInterval) {
    public static EffectiveHeroStats from(HeroRuntime runtime) {
        HeroDefinition definition = runtime.definition();
        HeroStats base = definition.statsAt(runtime.level());
        ItemStats items = runtime.inventory().totalStats();
        double attackSpeed = Math.min(1.0, items.attackSpeed());
        double moveBonus = Math.min(0.30, items.moveSpeed());
        return new EffectiveHeroStats(base.health() + items.maxHealth(), base.healthRegen(),
                base.maxMana() + items.maxMana(), base.manaRegen() + items.manaRegen(),
                base.attack() + items.attack(), items.abilityPower(), attackSpeed,
                Math.min(0.35, items.lifesteal()), Math.min(0.30, items.spellVamp()),
                definition.moveSpeed() * (1.0 + moveBonus),
                definition.baseAttackInterval() / (1.0 + attackSpeed));
    }
}
