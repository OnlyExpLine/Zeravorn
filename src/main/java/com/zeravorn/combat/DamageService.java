package com.zeravorn.combat;

import com.zeravorn.hero.HeroRuntime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import com.zeravorn.buff.BuffService;

public final class DamageService {
    private final Set<UUID> processedInstances = new HashSet<>();
    private final BuffService buffs;

    public DamageService() { this(null); }
    public DamageService(BuffService buffs) { this.buffs = buffs; }

    public DamageResult apply(HeroRuntime target, DamageInstance instance) {
        if (!instance.target().equals(target.owner())) return DamageResult.rejected("INVALID_TARGET", target.health());
        if (!target.alive()) return DamageResult.rejected("DEAD", target.health());
        if (!processedInstances.add(instance.id())) return DamageResult.rejected("DUPLICATE_DAMAGE", target.health());
        int amount = buffs == null ? instance.amount() : (int) Math.floor(instance.amount() * buffs.outgoingHeroDamageMultiplier(instance.source(), instance.serverTick()));
        int applied = Math.min(amount, target.health());
        target.receiveDamage(applied);
        return new DamageResult(true, applied, target.health(), !target.alive(), "");
    }
}
