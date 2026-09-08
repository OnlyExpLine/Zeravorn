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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.zeravorn.buff.BuffService;

/** Server-authoritative Esaki Q/E/R execution. Projectile flight/rendering remains in ProjectileService. */
public final class EsakiAbilityService {
    private final CooldownService cooldowns; private final ManaService mana = new ManaService(); private final DamageService damage;
    private final CrowdControlService crowdControl; private final AbilityCastValidator validator; private final List<AbilityEvent> events = new ArrayList<>(); private final List<Earthquake> earthquakes = new ArrayList<>(); private final Map<UUID, PendingProjectile> pendingProjectiles = new java.util.HashMap<>(); private final ProjectileService projectiles; private final BuffService buffs;
    public EsakiAbilityService(CooldownService cooldowns, DamageService damage, CrowdControlService crowdControl) { this(cooldowns, damage, crowdControl, null, new ProjectileService()); }
    public EsakiAbilityService(CooldownService cooldowns, DamageService damage, CrowdControlService crowdControl, BuffService buffs) { this(cooldowns, damage, crowdControl, buffs, new ProjectileService()); }
    public EsakiAbilityService(CooldownService cooldowns, DamageService damage, CrowdControlService crowdControl, BuffService buffs, ProjectileService projectiles) { this.cooldowns = cooldowns; this.damage = damage; this.crowdControl = crowdControl; this.buffs = buffs; this.projectiles = projectiles; this.validator = new AbilityCastValidator(crowdControl); }
    public EsakiAbilityResult castQ(HeroRuntime caster, HeroRuntime target, TargetSnapshot snapshot, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.Q, context); if (!error.isEmpty()) return EsakiAbilityResult.rejected(error);
        var config = EsakiAbilityDefinitions.q(caster.abilityRank(AbilitySlot.Q));
        if (!enemy(caster, target, snapshot, config.range())) return EsakiAbilityResult.rejected("INVALID_TARGET");
        if (!readyAndPay(caster, "esaki_q", config.cooldownTicks(), config.mana(), context.serverTick())) return EsakiAbilityResult.rejected(cooldowns.ready(caster.owner(), "esaki_q", context.serverTick()) ? "NO_MANA" : "COOLDOWN");
        var projectile = projectiles.spawn(caster.owner(), new ProjectileDefinition(config.projectileSpeed(), config.range(), 0.25, 0));
        pendingProjectiles.put(projectile.id(), new PendingProjectile(caster, AbilitySlot.Q, config.damage() + scaledAttack(caster, config.attackRatio())));
        events.add(new AbilityEvent(caster.owner(), AbilitySlot.Q, AbilityEvent.Phase.START, context.serverTick()));
        return new EsakiAbilityResult(true, "PROJECTILE_FIRED", 0);
    }
    public void advanceProjectile(UUID projectileId, double distance) {
        if (!pendingProjectiles.containsKey(projectileId)) throw new IllegalArgumentException("Unknown Esaki projectile");
        projectiles.advance(projectileId, distance);
        if (!projectiles.active(projectileId)) pendingProjectiles.remove(projectileId);
    }
    public void advanceProjectileTicks(UUID projectileId, long elapsedTicks, int ticksPerSecond) {
        if (!pendingProjectiles.containsKey(projectileId)) throw new IllegalArgumentException("Unknown Esaki projectile");
        projectiles.advanceTicks(projectileId, elapsedTicks, ticksPerSecond);
        if (!projectiles.active(projectileId)) pendingProjectiles.remove(projectileId);
    }
    public Set<UUID> activeProjectileIds() { return Set.copyOf(pendingProjectiles.keySet()); }
    /** Called only after server collision code confirms the first legal hit. */
    public EsakiAbilityResult confirmProjectileHit(UUID projectileId, HeroRuntime target, TargetSnapshot snapshot, long serverTick) {
        PendingProjectile pending = pendingProjectiles.get(projectileId);
        if (pending == null || target == null || snapshot == null || !target.owner().equals(snapshot.id()) || !target.alive() || target.team() == pending.caster().team()) return EsakiAbilityResult.rejected("INVALID_TARGET");
        if (!projectiles.confirmHit(projectileId, target.owner())) return EsakiAbilityResult.rejected("INVALID_PROJECTILE");
        pendingProjectiles.remove(projectileId);
        if (pending.caster().alive()) return hit(pending.caster(), target, pending.slot(), pending.damage(), serverTick);
        var result = damage.apply(target, new DamageInstance(UUID.randomUUID(), pending.caster().owner(), target.owner(), DamageType.PHYSICAL, pending.damage(), serverTick));
        if (!result.applied()) return EsakiAbilityResult.rejected(result.reason());
        events.add(new AbilityEvent(pending.caster().owner(), pending.slot(), AbilityEvent.Phase.HIT, serverTick));
        return new EsakiAbilityResult(true, "", result.amount());
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
        Set<UUID> targetIds = new HashSet<>();
        for (TargetSnapshot snapshot : snapshots) { HeroRuntime target = targets.get(snapshot.id()); if (enemy(caster, target, snapshot, config.radius())) targetIds.add(target.owner()); }
        earthquakes.add(new Earthquake(caster.owner(), Set.copyOf(targetIds), context.serverTick(), config.pulses(), config.damagePerPulse() + scaledAttack(caster, config.attackRatio())));
        events.add(new AbilityEvent(caster.owner(), AbilitySlot.R, AbilityEvent.Phase.START, context.serverTick()));
        return new EsakiAbilityResult(true, "EARTHQUAKE", 0);
    }
    /** Resolves Earthquake pulses only from the server tick loop. */
    public void tick(long serverTick, boolean matchPlaying, Map<UUID, HeroRuntime> heroes) {
        earthquakes.removeIf(effect -> {
            HeroRuntime caster = heroes.get(effect.caster());
            if (!matchPlaying || caster == null || !caster.alive()) return true;
            if (serverTick < effect.nextPulseTick()) return false;
            for (UUID targetId : effect.targetIds()) {
                HeroRuntime target = heroes.get(targetId);
                if (target != null && target.alive() && target.team() != caster.team()) hit(caster, target, AbilitySlot.R, effect.damagePerPulse(), serverTick);
            }
            Earthquake next = effect.next();
            if (next.pulsesRemaining() == 0) return true;
            effect.replaceWith(next);
            return false;
        });
    }
    public List<AbilityEvent> events() { return List.copyOf(events); }
    private boolean readyAndPay(HeroRuntime caster, String action, long cooldown, int manaCost, long tick) { if (!cooldowns.ready(caster.owner(), action, tick) || !mana.trySpend(caster, manaCost)) return false; cooldowns.tryStart(caster.owner(), action, tick, buffs == null ? cooldown : buffs.heroAbilityCooldownTicks(caster.owner(), cooldown, tick)); return true; }
    private EsakiAbilityResult hit(HeroRuntime caster, HeroRuntime target, AbilitySlot slot, int amount, long tick) { var result = damage.applyAbility(caster, target, new DamageInstance(UUID.randomUUID(), caster.owner(), target.owner(), DamageType.PHYSICAL, amount, tick)); if (!result.applied()) return EsakiAbilityResult.rejected(result.reason()); events.add(new AbilityEvent(caster.owner(), slot, AbilityEvent.Phase.HIT, tick)); return new EsakiAbilityResult(true, "", result.amount()); }
    private static int scaledAttack(HeroRuntime hero, double ratio) { return (int) Math.floor(hero.effectiveStats().attack() * ratio); }
    private static boolean enemy(HeroRuntime caster, HeroRuntime target, TargetSnapshot snapshot, double range) { return target != null && snapshot != null && target.owner().equals(snapshot.id()) && target.alive() && snapshot.alive() && caster.team() != target.team() && snapshot.distance() <= range; }
    private static final class Earthquake {
        private final UUID caster; private final Set<UUID> targetIds; private long nextPulseTick; private int pulsesRemaining; private final int damagePerPulse;
        private Earthquake(UUID caster, Set<UUID> targetIds, long nextPulseTick, int pulsesRemaining, int damagePerPulse) { this.caster = caster; this.targetIds = targetIds; this.nextPulseTick = nextPulseTick; this.pulsesRemaining = pulsesRemaining; this.damagePerPulse = damagePerPulse; }
        UUID caster() { return caster; } Set<UUID> targetIds() { return targetIds; } long nextPulseTick() { return nextPulseTick; } int pulsesRemaining() { return pulsesRemaining; } int damagePerPulse() { return damagePerPulse; }
        Earthquake next() { return new Earthquake(caster, targetIds, nextPulseTick + 20L, pulsesRemaining - 1, damagePerPulse); }
        void replaceWith(Earthquake next) { nextPulseTick = next.nextPulseTick; pulsesRemaining = next.pulsesRemaining; }
    }
    private record PendingProjectile(HeroRuntime caster, AbilitySlot slot, int damage) { }
}
