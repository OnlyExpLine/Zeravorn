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
import com.zeravorn.projectile.ProjectileDefinition;
import com.zeravorn.projectile.ProjectileService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.zeravorn.buff.BuffService;

/** Server-owned Loki ability effects; map integration applies actual forced movement from the PULL event. */
public final class LokiAbilityService {
    private final CooldownService cooldowns; private final ManaService mana = new ManaService(); private final DamageService damage; private final CrowdControlService cc; private final AbilityCastValidator validator; private final BuffService buffs;
    private final Map<UUID, Rampage> rampages = new HashMap<>(); private final Map<UUID, PendingHook> hooks = new HashMap<>(); private final List<AbilityEvent> events = new ArrayList<>(); private final ProjectileService projectiles;
    public LokiAbilityService(CooldownService cooldowns, DamageService damage, CrowdControlService cc) { this(cooldowns, damage, cc, null, new ProjectileService()); }
    public LokiAbilityService(CooldownService cooldowns, DamageService damage, CrowdControlService cc, BuffService buffs) { this(cooldowns, damage, cc, buffs, new ProjectileService()); }
    public LokiAbilityService(CooldownService cooldowns, DamageService damage, CrowdControlService cc, BuffService buffs, ProjectileService projectiles) { this.cooldowns = cooldowns; this.damage = damage; this.cc = cc; this.buffs = buffs; this.projectiles = projectiles; this.validator = new AbilityCastValidator(cc); }
    public LokiAbilityResult castQ(HeroRuntime caster, HeroRuntime target, TargetSnapshot snapshot, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.Q, context); if (!error.isEmpty()) return LokiAbilityResult.rejected(error); var config = LokiAbilityDefinitions.q(caster.abilityRank(AbilitySlot.Q));
        if (!enemy(caster, target, snapshot, config.range())) return LokiAbilityResult.rejected("INVALID_TARGET"); if (!pay(caster, "loki_q", config.cooldownTicks(), config.mana(), context.serverTick())) return rejected(caster, "loki_q", context.serverTick());
        var projectile = projectiles.spawn(caster.owner(), new ProjectileDefinition(config.outboundSpeed(), config.range(), 0.30, 0));
        hooks.put(projectile.id(), new PendingHook(caster, config.damage() + attack(caster, config.attackRatio()), config.pullTicks(), config.returnSpeed()));
        events.add(new AbilityEvent(caster.owner(), AbilitySlot.Q, AbilityEvent.Phase.START, context.serverTick()));
        return new LokiAbilityResult(true, "HOOK_OUTBOUND", 0);
    }
    public void advanceHook(UUID projectileId, double distance) {
        if (!hooks.containsKey(projectileId)) throw new IllegalArgumentException("Unknown Loki hook");
        projectiles.advance(projectileId, distance);
        if (!projectiles.active(projectileId)) hooks.remove(projectileId);
    }
    public void advanceHookTicks(UUID projectileId, long elapsedTicks, int ticksPerSecond) {
        if (!hooks.containsKey(projectileId)) throw new IllegalArgumentException("Unknown Loki hook");
        projectiles.advanceTicks(projectileId, elapsedTicks, ticksPerSecond);
        if (!projectiles.active(projectileId)) hooks.remove(projectileId);
    }
    public java.util.Set<UUID> activeHookIds() { return java.util.Set.copyOf(hooks.keySet()); }
    /** Applies Hook damage/PULL only after server collision confirmation. */
    public LokiAbilityResult confirmHookHit(UUID projectileId, HeroRuntime target, TargetSnapshot snapshot, long serverTick) {
        PendingHook hook = hooks.get(projectileId);
        if (hook == null || target == null || snapshot == null || !target.owner().equals(snapshot.id()) || !target.alive() || target.team() == hook.caster().team()) return LokiAbilityResult.rejected("INVALID_TARGET");
        if (!projectiles.confirmHit(projectileId, target.owner())) return LokiAbilityResult.rejected("INVALID_PROJECTILE");
        hooks.remove(projectileId);
        LokiAbilityResult result;
        if (hook.caster().alive()) result = hit(hook.caster(), target, AbilitySlot.Q, hook.damage(), serverTick);
        else {
            var applied = damage.apply(target, new DamageInstance(UUID.randomUUID(), hook.caster().owner(), target.owner(), DamageType.PHYSICAL, hook.damage(), serverTick));
            if (!applied.applied()) return LokiAbilityResult.rejected(applied.reason());
            events.add(new AbilityEvent(hook.caster().owner(), AbilitySlot.Q, AbilityEvent.Phase.HIT, serverTick)); result = new LokiAbilityResult(true, "", applied.amount());
        }
        if (result.executed()) cc.apply(target.owner(), new CrowdControlEffect(hook.caster().owner(), CrowdControlType.PULL, 1, serverTick + hook.pullTicks(), false));
        return result;
    }
    public LokiAbilityResult castE(HeroRuntime caster, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.E, context); if (!error.isEmpty()) return LokiAbilityResult.rejected(error); var config = LokiAbilityDefinitions.e(caster.abilityRank(AbilitySlot.E));
        if (!pay(caster, "loki_e", config.cooldownTicks(), config.mana(), context.serverTick())) return rejected(caster, "loki_e", context.serverTick()); rampages.put(caster.owner(), new Rampage(context.serverTick() + config.durationTicks(), config.moveBonus())); events.add(new AbilityEvent(caster.owner(), AbilitySlot.E, AbilityEvent.Phase.START, context.serverTick())); return new LokiAbilityResult(true, "RAMPAGE", 0);
    }
    public LokiAbilityResult castR(HeroRuntime caster, List<TargetSnapshot> snapshots, Map<UUID, HeroRuntime> targets, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.R, context); if (!error.isEmpty()) return LokiAbilityResult.rejected(error); var config = LokiAbilityDefinitions.r(caster.abilityRank(AbilitySlot.R));
        if (!pay(caster, "loki_r", config.cooldownTicks(), config.mana(), context.serverTick())) return rejected(caster, "loki_r", context.serverTick()); int total = 0, amount = config.damage() + attack(caster, config.attackRatio()); events.add(new AbilityEvent(caster.owner(), AbilitySlot.R, AbilityEvent.Phase.START, context.serverTick()));
        for (TargetSnapshot snapshot : snapshots) { HeroRuntime target = targets.get(snapshot.id()); if (!enemy(caster, target, snapshot, config.radius())) continue; total += hit(caster, target, AbilitySlot.R, amount, context.serverTick()).damage(); cc.apply(target.owner(), new CrowdControlEffect(caster.owner(), CrowdControlType.ROOT, 1, context.serverTick() + config.rootTicks(), true)); }
        return new LokiAbilityResult(true, "IRON_PRISON", total);
    }
    public double rampageMoveMultiplier(UUID hero, long serverTick) { Rampage active = rampages.get(hero); if (active == null || active.expiresAt() <= serverTick) { rampages.remove(hero); return 1.0; } return 1.0 + active.moveBonus(); }
    public List<AbilityEvent> events() { return List.copyOf(events); }
    private boolean pay(HeroRuntime caster, String action, long cd, int cost, long tick) { if (!cooldowns.ready(caster.owner(), action, tick) || !mana.trySpend(caster, cost)) return false; cooldowns.tryStart(caster.owner(), action, tick, buffs == null ? cd : buffs.heroAbilityCooldownTicks(caster.owner(), cd, tick)); return true; }
    private LokiAbilityResult rejected(HeroRuntime caster, String action, long tick) { return LokiAbilityResult.rejected(cooldowns.ready(caster.owner(), action, tick) ? "NO_MANA" : "COOLDOWN"); }
    private LokiAbilityResult hit(HeroRuntime caster, HeroRuntime target, AbilitySlot slot, int amount, long tick) { var result = damage.applyAbility(caster, target, new DamageInstance(UUID.randomUUID(), caster.owner(), target.owner(), DamageType.PHYSICAL, amount, tick)); if (!result.applied()) return LokiAbilityResult.rejected(result.reason()); events.add(new AbilityEvent(caster.owner(), slot, AbilityEvent.Phase.HIT, tick)); return new LokiAbilityResult(true, "", result.amount()); }
    private static int attack(HeroRuntime hero, double ratio) { return (int) Math.floor(hero.effectiveStats().attack() * ratio); }
    private static boolean enemy(HeroRuntime caster, HeroRuntime target, TargetSnapshot snapshot, double range) { return target != null && snapshot != null && target.owner().equals(snapshot.id()) && target.alive() && snapshot.alive() && caster.team() != target.team() && snapshot.distance() <= range; }
    private record Rampage(long expiresAt, double moveBonus) { }
    private record PendingHook(HeroRuntime caster, int damage, long pullTicks, double returnSpeed) { }
}
