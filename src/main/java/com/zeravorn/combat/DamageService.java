package com.zeravorn.combat;

import com.zeravorn.hero.HeroRuntime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class DamageService {
    private final Set<UUID> processedInstances = new HashSet<>();

    public DamageResult apply(HeroRuntime target, DamageInstance instance) {
        if (!instance.target().equals(target.owner())) return DamageResult.rejected("INVALID_TARGET", target.health());
        if (!target.alive()) return DamageResult.rejected("DEAD", target.health());
        if (!processedInstances.add(instance.id())) return DamageResult.rejected("DUPLICATE_DAMAGE", target.health());
        int applied = Math.min(instance.amount(), target.health());
        target.receiveDamage(applied);
        return new DamageResult(true, applied, target.health(), !target.alive(), "");
    }
}
