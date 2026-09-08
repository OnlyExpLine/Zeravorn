package com.zeravorn.combat;

import com.zeravorn.hero.HeroRuntime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.zeravorn.buff.BuffService;

public final class DamageService {
    /** Duplicate IDs are retained only for a bounded server-tick window. */
    private static final long DUPLICATE_WINDOW_TICKS = 20L * 60L;
    private final Map<UUID, Long> processedInstances = new HashMap<>();
    private final BuffService buffs;

    public DamageService() { this(null); }
    public DamageService(BuffService buffs) { this.buffs = buffs; }

    public DamageResult apply(HeroRuntime target, DamageInstance instance) {
        if (!instance.target().equals(target.owner())) return DamageResult.rejected("INVALID_TARGET", target.health());
        if (!target.alive()) return DamageResult.rejected("DEAD", target.health());
        processedInstances.entrySet().removeIf(entry -> entry.getValue() + DUPLICATE_WINDOW_TICKS < instance.serverTick());
        if (processedInstances.putIfAbsent(instance.id(), instance.serverTick()) != null) return DamageResult.rejected("DUPLICATE_DAMAGE", target.health());
        int amount = buffs == null || instance.type() == DamageType.TRUE ? instance.amount()
                : (int) Math.floor(instance.amount() * buffs.outgoingHeroDamageMultiplier(instance.source(), instance.serverTick()));
        int applied = Math.min(amount, target.health());
        target.receiveDamage(applied);
        return new DamageResult(true, applied, target.health(), !target.alive(), "");
    }

    public DamageResult applyBasic(HeroRuntime caster, HeroRuntime target, DamageInstance instance) {
        DamageResult result = apply(target, instance);
        if (result.applied()) caster.heal((int) Math.floor(result.amount() * caster.effectiveStats().lifesteal()));
        return result;
    }

    public DamageResult applyAbility(HeroRuntime caster, HeroRuntime target, DamageInstance instance) {
        DamageResult result = apply(target, instance);
        if (result.applied()) caster.heal((int) Math.floor(result.amount() * caster.effectiveStats().spellVamp()));
        return result;
    }
}
