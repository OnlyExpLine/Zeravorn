package com.zeravorn.combat;

import com.zeravorn.hero.HeroRuntime;

public final class ManaService {
    public boolean trySpend(HeroRuntime runtime, int amount) {
        if (amount < 0) throw new IllegalArgumentException("Mana cost cannot be negative");
        if (runtime.mana() < amount) return false;
        runtime.spendMana(amount);
        return true;
    }
}
