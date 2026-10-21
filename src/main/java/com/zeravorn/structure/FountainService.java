package com.zeravorn.structure;

import com.zeravorn.hero.HeroRuntime;

/** Balance v1.1 section 10: 20% resources per second, 35% true damage every 0.5s. */
public final class FountainService {
    public void heal(HeroRuntime hero, long tick) {
        if (!hero.alive() || tick % 20 != 0) return;
        hero.heal((int)Math.ceil(hero.effectiveStats().maxHealth() * .20));
        hero.restoreMana((int)Math.ceil(hero.effectiveStats().maxMana() * .20));
    }
    public int danger(HeroRuntime hero, long tick) {
        if (!hero.alive() || tick % 10 != 0) return 0;
        int before = hero.health();
        hero.receiveDamage((int)Math.ceil(hero.effectiveStats().maxHealth() * .35));
        return before - hero.health();
    }
}
