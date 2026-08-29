package com.zeravorn.hero;

import com.zeravorn.ability.AbilityCastValidator;
import com.zeravorn.ability.AbilityContext;
import com.zeravorn.ability.AbilityEvent;
import com.zeravorn.ability.AbilitySlot;
import com.zeravorn.combat.CooldownService;
import com.zeravorn.combat.CrowdControlEffect;
import com.zeravorn.combat.CrowdControlService;
import com.zeravorn.combat.CrowdControlType;
import com.zeravorn.combat.DamageInstance;
import com.zeravorn.combat.DamageService;
import com.zeravorn.combat.DamageType;
import com.zeravorn.combat.ManaService;
import com.zeravorn.combat.TargetSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative Esaki Q/E/R execution. Projectile flight/rendering remains in ProjectileService. */
public final class EsakiAbilityService {
    private final CooldownService cooldowns; private final ManaService mana = new ManaService(); private final DamageService damage;
    private final CrowdControlService crowdControl; private final AbilityCastValidator validator; private final List<AbilityEvent> events = new ArrayList<>();
    public EsakiAbilityService(CooldownService cooldowns, DamageService damage, CrowdControlService crowdControl) { this.cooldowns = cooldowns; this.damage = damage; this.crowdControl = crowdControl; this.validator = new AbilityCastValidator(crowdControl); }
    public EsakiAbilityResult castQ(HeroRuntime caster, HeroRuntime target, TargetSnapshot snapshot, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.Q, context); if (!error.isEmpty()) return EsakiAbilityResult.rejected(error);
        var config = EsakiAbilityDefinitions.q(caster.abilityRank(AbilitySlot.Q));
        if (!enemy(caster, target, snapshot, config.range())) return EsakiAbilityResult.rejected("INVALID_TARGET");
        if (!readyAndPay(caster, "esaki_q", config.cooldownTicks(), config.mana(), context.serverTick())) return EsakiAbilityResult.rejected(cooldowns.ready(caster.owner(), "esaki_q", context.serverTick()) ? "NO_MANA" : "COOLDOWN");
        return hit(caster, target, AbilitySlot.Q, config.damage() + scaledAttack(caster, config.attackRatio()), context.serverTick());
    }
    public EsakiAbilityResult castE(HeroRuntime caster, List<TargetSnapshot> snapshots, Map<UUID, HeroRuntime> targets, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.E, context); if (!error.isEmpty()) return EsakiAbilityResult.rejected(error);
        var config = EsakiAbilityDefinitions.e(caster.abilityRank(AbilitySlot.E));
        if (!readyAndPay(caster, "esaki_e", config.cooldownTicks(), config.mana(), context.serverTick())) return EsakiAbilityResult.rejected(cooldowns.ready(caster.owner(), "esaki_e", context.serverTick()) ? "NO_MANA" : "COOLDOWN");
        int total = 0; events.add(new AbilityEvent(caster.owner(), AbilitySlot.E, AbilityEvent.Phase.START, context.serverTick()));
        for (TargetSnapshot snapshot : snapshots) { HeroRuntime target = targets.get(snapshot.id()); if (!enemy(caster, target, snapshot, config.radius())) continue; total += hit(caster, target, AbilitySlot.E, config.damage() + scaledAttack(caster, config.attackRatio()), context.serverTick()).damage(); crowdControl.apply(target.owner(), new CrowdControlEffect(caster.owner(), CrowdControlType.KNOCKBACK, config.knockback(), context.serverTick() + 7, false)); }
        return new EsakiAbilityResult(true, "EARTH_REPULSE", total);
    }
    public EsakiAbilityResult castR(HeroRuntime caster, List<TargetSnapshot> snapshots, Map<UUID, HeroRuntime> targets, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.R, context); if (!error.isEmpty()) return EsakiAbilityResult.rejected(error);
        var config = EsakiAbilityDefinitions.r(caster.abilityRank(AbilitySlot.R));
        if (!readyAndPay(caster, "esaki_r", config.cooldownTicks(), config.mana(), context.serverTick())) return EsakiAbilityResult.rejected(cooldowns.ready(caster.owner(), "esaki_r", context.serverTick()) ? "NO_MANA" : "COOLDOWN");
        int total = 0, damagePerPulse = config.damagePerPulse() + scaledAttack(caster, config.attackRatio()); events.add(new AbilityEvent(caster.owner(), AbilitySlot.R, AbilityEvent.Phase.START, context.serverTick()));
        for (int pulse = 0; pulse < config.pulses(); pulse++) for (TargetSnapshot snapshot : snapshots) { HeroRuntime target = targets.get(snapshot.id()); if (enemy(caster, target, snapshot, config.radius())) total += hit(caster, target, AbilitySlot.R, damagePerPulse, context.serverTick() + pulse * 20L).damage(); }
        return new EsakiAbilityResult(true, "EARTHQUAKE", total);
    }
    public List<AbilityEvent> events() { return List.copyOf(events); }
    private boolean readyAndPay(HeroRuntime caster, String action, long cooldown, int manaCost, long tick) { if (!cooldowns.ready(caster.owner(), action, tick) || !mana.trySpend(caster, manaCost)) return false; cooldowns.tryStart(caster.owner(), action, tick, cooldown); return true; }
    private EsakiAbilityResult hit(HeroRuntime caster, HeroRuntime target, AbilitySlot slot, int amount, long tick) { var result = damage.apply(target, new DamageInstance(UUID.randomUUID(), caster.owner(), target.owner(), DamageType.PHYSICAL, amount, tick)); if (!result.applied()) return EsakiAbilityResult.rejected(result.reason()); events.add(new AbilityEvent(caster.owner(), slot, AbilityEvent.Phase.HIT, tick)); return new EsakiAbilityResult(true, "", result.amount()); }
    private static int scaledAttack(HeroRuntime hero, double ratio) { return (int) Math.floor(hero.effectiveStats().attack() * ratio); }
    private static boolean enemy(HeroRuntime caster, HeroRuntime target, TargetSnapshot snapshot, double range) { return target != null && snapshot != null && target.owner().equals(snapshot.id()) && target.alive() && snapshot.alive() && caster.team() != target.team() && snapshot.distance() <= range; }
}
