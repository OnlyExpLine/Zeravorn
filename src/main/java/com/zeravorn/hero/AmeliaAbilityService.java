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

/** Server-authoritative Amelia ability resolution. All damage remains independent from client animation. */
public final class AmeliaAbilityService {
    private final CooldownService cooldowns; private final ManaService mana = new ManaService(); private final DamageService damage;
    private final CrowdControlService crowdControl; private final AbilityCastValidator validator; private final List<AbilityEvent> events = new ArrayList<>();
    public AmeliaAbilityService(CooldownService cooldowns, DamageService damage, CrowdControlService crowdControl) { this.cooldowns = cooldowns; this.damage = damage; this.crowdControl = crowdControl; this.validator = new AbilityCastValidator(crowdControl); }
    public AmeliaAbilityResult castQ(HeroRuntime caster, List<TargetSnapshot> snapshots, Map<UUID, HeroRuntime> targets, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.Q, context); if (!error.isEmpty()) return AmeliaAbilityResult.rejected(error);
        var config = AmeliaAbilityDefinitions.q(caster.abilityRank(AbilitySlot.Q)); if (!pay(caster, "amelia_q", config.cooldownTicks(), config.mana(), context.serverTick())) return rejected(caster, "amelia_q", context.serverTick());
        int total = 0, perTick = config.damagePerTick() + scaledAp(caster, config.apRatio()); events.add(new AbilityEvent(caster.owner(), AbilitySlot.Q, AbilityEvent.Phase.START, context.serverTick()));
        for (int tick = 0; tick < config.ticks(); tick++) for (TargetSnapshot snapshot : snapshots) { HeroRuntime target = targets.get(snapshot.id()); if (!enemy(caster, target, snapshot, config.range())) continue; total += hit(caster, target, AbilitySlot.Q, perTick, context.serverTick() + tick * 10L).damage(); crowdControl.apply(target.owner(), new CrowdControlEffect(caster.owner(), CrowdControlType.SLOW, config.slow(), context.serverTick() + (tick + 1L) * 10L, true)); }
        return new AmeliaAbilityResult(true, "FROST_BREATH", total);
    }
    public AmeliaAbilityResult castE(HeroRuntime caster, List<TargetSnapshot> snapshots, Map<UUID, HeroRuntime> targets, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.E, context); if (!error.isEmpty()) return AmeliaAbilityResult.rejected(error);
        var config = AmeliaAbilityDefinitions.e(caster.abilityRank(AbilitySlot.E)); if (!pay(caster, "amelia_e", config.cooldownTicks(), config.mana(), context.serverTick())) return rejected(caster, "amelia_e", context.serverTick());
        int total = 0, amount = config.damage() + scaledAp(caster, config.apRatio()); events.add(new AbilityEvent(caster.owner(), AbilitySlot.E, AbilityEvent.Phase.START, context.serverTick()));
        for (TargetSnapshot snapshot : snapshots) { HeroRuntime target = targets.get(snapshot.id()); if (!enemy(caster, target, snapshot, config.radius())) continue; total += hit(caster, target, AbilitySlot.E, amount, context.serverTick()).damage(); crowdControl.apply(target.owner(), new CrowdControlEffect(caster.owner(), CrowdControlType.KNOCKBACK, config.knockback(), context.serverTick() + 7, false)); }
        return new AmeliaAbilityResult(true, "BLIZZARD_BURST", total);
    }
    /** Resolves the documented 0.55-second ground-target delay after the server accepts the target area. */
    public AmeliaAbilityResult castR(HeroRuntime caster, List<TargetSnapshot> snapshots, Map<UUID, HeroRuntime> targets, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.R, context); if (!error.isEmpty()) return AmeliaAbilityResult.rejected(error);
        var config = AmeliaAbilityDefinitions.r(caster.abilityRank(AbilitySlot.R)); if (!pay(caster, "amelia_r", config.cooldownTicks(), config.mana(), context.serverTick())) return rejected(caster, "amelia_r", context.serverTick());
        int total = 0, amount = config.damage() + scaledAp(caster, config.apRatio()); long hitTick = context.serverTick() + config.delayTicks(); events.add(new AbilityEvent(caster.owner(), AbilitySlot.R, AbilityEvent.Phase.START, context.serverTick()));
        for (TargetSnapshot snapshot : snapshots) { HeroRuntime target = targets.get(snapshot.id()); if (enemy(caster, target, snapshot, config.radius())) total += hit(caster, target, AbilitySlot.R, amount, hitTick).damage(); }
        return new AmeliaAbilityResult(true, "ICE_SPIKES", total);
    }
    public List<AbilityEvent> events() { return List.copyOf(events); }
    private boolean pay(HeroRuntime caster, String action, long cooldown, int manaCost, long tick) { if (!cooldowns.ready(caster.owner(), action, tick) || !mana.trySpend(caster, manaCost)) return false; cooldowns.tryStart(caster.owner(), action, tick, cooldown); return true; }
    private AmeliaAbilityResult rejected(HeroRuntime caster, String action, long tick) { return AmeliaAbilityResult.rejected(cooldowns.ready(caster.owner(), action, tick) ? "NO_MANA" : "COOLDOWN"); }
    private AmeliaAbilityResult hit(HeroRuntime caster, HeroRuntime target, AbilitySlot slot, int amount, long tick) { var result = damage.apply(target, new DamageInstance(UUID.randomUUID(), caster.owner(), target.owner(), DamageType.MAGICAL, amount, tick)); if (!result.applied()) return AmeliaAbilityResult.rejected(result.reason()); events.add(new AbilityEvent(caster.owner(), slot, AbilityEvent.Phase.HIT, tick)); return new AmeliaAbilityResult(true, "", result.amount()); }
    private static int scaledAp(HeroRuntime hero, double ratio) { return (int) Math.floor(hero.effectiveStats().abilityPower() * ratio); }
    private static boolean enemy(HeroRuntime caster, HeroRuntime target, TargetSnapshot snapshot, double range) { return target != null && snapshot != null && target.owner().equals(snapshot.id()) && target.alive() && snapshot.alive() && caster.team() != target.team() && snapshot.distance() <= range; }
}
