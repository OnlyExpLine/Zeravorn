package com.zeravorn.combat;

import com.zeravorn.hero.HeroRuntime;

public final class TargetingService {
    public boolean validEnemy(HeroRuntime caster, TargetSnapshot target, double range) {
        if (range < 0) throw new IllegalArgumentException("Range cannot be negative");
        return target.alive() && target.team() != caster.team() && target.distance() <= range;
    }
}
