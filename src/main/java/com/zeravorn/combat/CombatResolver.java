package com.zeravorn.combat;

import com.zeravorn.hero.HeroRuntime;

public final class CombatResolver {
    private final DamageService damageService;

    public CombatResolver(DamageService damageService) { this.damageService = damageService; }
    public DamageResult resolve(HeroRuntime target, DamageInstance instance) { return damageService.apply(target, instance); }
}
