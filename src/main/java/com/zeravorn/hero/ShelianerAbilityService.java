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

/** Authoritative Shelianer combat implementation. Client animation is driven only by emitted events. */
public final class ShelianerAbilityService {
    private final CooldownService cooldowns; private final ManaService mana = new ManaService();
    private final DamageService damage; private final CrowdControlService crowdControl; private final AbilityCastValidator validator; private final BuffService buffs;
    private final Map<UUID, Poison> poisons = new HashMap<>(); private final Map<UUID, DeathDance> deathDances = new HashMap<>(); private final Map<UUID, PendingKnife> knives = new HashMap<>(); private final List<AbilityEvent> events = new ArrayList<>(); private final ProjectileService projectiles;
    public ShelianerAbilityService(CooldownService cooldowns, DamageService damage, CrowdControlService crowdControl) {
        this(cooldowns, damage, crowdControl, null, new ProjectileService());
    }
    public ShelianerAbilityService(CooldownService cooldowns, DamageService damage, CrowdControlService crowdControl, BuffService buffs) {
        this(cooldowns, damage, crowdControl, buffs, new ProjectileService());
    }
    public ShelianerAbilityService(CooldownService cooldowns, DamageService damage, CrowdControlService crowdControl, BuffService buffs, ProjectileService projectiles) {
        this.cooldowns = cooldowns; this.damage = damage; this.crowdControl = crowdControl; this.buffs = buffs; this.projectiles = projectiles; this.validator = new AbilityCastValidator(crowdControl);
    }
    public ShelianerAbilityResult castQ(HeroRuntime caster, HeroRuntime target, TargetSnapshot targetSnapshot, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.Q, context); if (!error.isEmpty()) return ShelianerAbilityResult.rejected(error);
        var config = ShelianerAbilityDefinitions.q(caster.abilityRank(AbilitySlot.Q));
        if (!enemyInRange(caster, target, targetSnapshot, config.range())) return ShelianerAbilityResult.rejected("INVALID_TARGET");
        if (!cooldowns.ready(caster.owner(), "shelianer_q", context.serverTick())) return ShelianerAbilityResult.rejected("COOLDOWN");
        if (!mana.trySpend(caster, config.mana())) return ShelianerAbilityResult.rejected("NO_MANA");
        cooldowns.tryStart(caster.owner(), "shelianer_q", context.serverTick(), cooldown(caster, config.cooldownTicks(), context.serverTick()));
        var projectile = projectiles.spawn(caster.owner(), new ProjectileDefinition(config.projectileSpeed(), config.range(), 0.25, 0));
        knives.put(projectile.id(), new PendingKnife(caster, config.damage() + scaledAttack(caster, config.attackRatio())));
        events.add(new AbilityEvent(caster.owner(), AbilitySlot.Q, AbilityEvent.Phase.START, context.serverTick()));
        return new ShelianerAbilityResult(true, "PROJECTILE_FIRED", 0);
    }
    public void advanceProjectile(UUID projectileId, double distance) {
        if (!knives.containsKey(projectileId)) throw new IllegalArgumentException("Unknown Shelianer projectile");
        projectiles.advance(projectileId, distance);
        if (!projectiles.active(projectileId)) knives.remove(projectileId);
    }
    public void advanceProjectileTicks(UUID projectileId, long elapsedTicks, int ticksPerSecond) {
        if (!knives.containsKey(projectileId)) throw new IllegalArgumentException("Unknown Shelianer projectile");
        projectiles.advanceTicks(projectileId, elapsedTicks, ticksPerSecond);
        if (!projectiles.active(projectileId)) knives.remove(projectileId);
    }
    public java.util.Set<UUID> activeProjectileIds() { return java.util.Set.copyOf(knives.keySet()); }
    /** Server collision code confirms the projectile hit before this method applies damage. */
    public ShelianerAbilityResult confirmProjectileHit(UUID projectileId, HeroRuntime target, TargetSnapshot snapshot, long serverTick) {
        PendingKnife pending = knives.get(projectileId);
        if (pending == null || target == null || snapshot == null || !target.owner().equals(snapshot.id()) || !target.alive() || target.team() == pending.caster().team()) return ShelianerAbilityResult.rejected("INVALID_TARGET");
        if (!projectiles.confirmHit(projectileId, target.owner())) return ShelianerAbilityResult.rejected("INVALID_PROJECTILE");
        knives.remove(projectileId);
        if (pending.caster().alive()) return damage(pending.caster(), target, AbilitySlot.Q, pending.damage(), DamageType.PHYSICAL, serverTick);
        var result = damage.apply(target, new DamageInstance(UUID.randomUUID(), pending.caster().owner(), target.owner(), DamageType.PHYSICAL, pending.damage(), serverTick));
        if (!result.applied()) return ShelianerAbilityResult.rejected(result.reason());
        events.add(new AbilityEvent(pending.caster().owner(), AbilitySlot.Q, AbilityEvent.Phase.HIT, serverTick));
        return new ShelianerAbilityResult(true, "", result.amount());
    }
    /** Starts the poison DoT after server-side projectile/collision validation has selected an enemy target. */
    public ShelianerAbilityResult castE(HeroRuntime caster, HeroRuntime target, TargetSnapshot targetSnapshot, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.E, context); if (!error.isEmpty()) return ShelianerAbilityResult.rejected(error);
        if (!enemyInRange(caster, target, targetSnapshot, Double.MAX_VALUE)) return ShelianerAbilityResult.rejected("INVALID_TARGET");
        var config = ShelianerAbilityDefinitions.e(caster.abilityRank(AbilitySlot.E));
        if (!cooldowns.ready(caster.owner(), "shelianer_e", context.serverTick())) return ShelianerAbilityResult.rejected("COOLDOWN");
        if (!mana.trySpend(caster, config.mana())) return ShelianerAbilityResult.rejected("NO_MANA");
        cooldowns.tryStart(caster.owner(), "shelianer_e", context.serverTick(), cooldown(caster, config.cooldownTicks(), context.serverTick()));
        poisons.put(target.owner(), new Poison(caster.owner(), context.serverTick(), 4, config.damagePerTick() + scaledAttack(caster, config.attackRatio()), config.slow()));
        crowdControl.apply(target.owner(), new CrowdControlEffect(caster.owner(), CrowdControlType.SLOW, config.slow(), context.serverTick() + config.durationTicks(), true));
        events.add(new AbilityEvent(caster.owner(), AbilitySlot.E, AbilityEvent.Phase.START, context.serverTick()));
        return new ShelianerAbilityResult(true, "POISON_APPLIED", 0);
    }
    /** Processes the four 0.5-second poison ticks. */
    public void tick(long serverTick, Map<UUID, HeroRuntime> heroes) { tick(serverTick, true, heroes); }
    /** Processes detached poison and active Death Dance hits from the authoritative server tick. */
    public void tick(long serverTick, boolean matchPlaying, Map<UUID, HeroRuntime> heroes) {
        poisons.entrySet().removeIf(entry -> {
            Poison poison = entry.getValue(); HeroRuntime target = heroes.get(entry.getKey());
            if (target == null || !target.alive() || poison.ticksRemaining() == 0) return true;
            if (serverTick >= poison.nextTick()) {
                HeroRuntime caster = heroes.get(poison.caster());
                // Once the server-confirmed flask has applied its DoT, caster death
                // does not cancel the detached damage instance.
                if (caster != null) damage(caster, target, AbilitySlot.E, poison.damagePerTick(), DamageType.MAGICAL, serverTick);
                poison = poison.next(); entry.setValue(poison);
            }
            return poison.ticksRemaining() == 0;
        });
        deathDances.entrySet().removeIf(entry -> {
            DeathDance dance = entry.getValue(); HeroRuntime caster = heroes.get(dance.caster()); HeroRuntime target = heroes.get(dance.target());
            if (!matchPlaying || caster == null || target == null || !caster.alive() || !target.alive()) return true;
            if (serverTick < dance.nextHitTick()) return false;
            damage(caster, target, AbilitySlot.R, dance.damagePerHit(), DamageType.PHYSICAL, serverTick);
            dance.advance(); return dance.hitsRemaining() == 0;
        });
    }
    public ShelianerAbilityResult castF(HeroRuntime caster, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.F, context); if (!error.isEmpty()) return ShelianerAbilityResult.rejected(error);
        if (crowdControl.blocksMovement(caster.owner())) return ShelianerAbilityResult.rejected("ROOT_BLOCK");
        var config = ShelianerAbilityDefinitions.f(caster.abilityRank(AbilitySlot.F));
        if (!cooldowns.ready(caster.owner(), "shelianer_f", context.serverTick())) return ShelianerAbilityResult.rejected("COOLDOWN");
        if (!mana.trySpend(caster, config.mana())) return ShelianerAbilityResult.rejected("NO_MANA");
        cooldowns.tryStart(caster.owner(), "shelianer_f", context.serverTick(), cooldown(caster, config.cooldownTicks(), context.serverTick()));
        events.add(new AbilityEvent(caster.owner(), AbilitySlot.F, AbilityEvent.Phase.START, context.serverTick()));
        return new ShelianerAbilityResult(true, "DASH_" + config.dashDistance(), 0);
    }
    public ShelianerAbilityResult castR(HeroRuntime caster, HeroRuntime target, TargetSnapshot targetSnapshot, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.R, context); if (!error.isEmpty()) return ShelianerAbilityResult.rejected(error);
        var config = ShelianerAbilityDefinitions.r(caster.abilityRank(AbilitySlot.R));
        if (!enemyInRange(caster, target, targetSnapshot, config.range())) return ShelianerAbilityResult.rejected("INVALID_TARGET");
        if (!cooldowns.ready(caster.owner(), "shelianer_r", context.serverTick())) return ShelianerAbilityResult.rejected("COOLDOWN");
        if (!mana.trySpend(caster, config.mana())) return ShelianerAbilityResult.rejected("NO_MANA");
        cooldowns.tryStart(caster.owner(), "shelianer_r", context.serverTick(), cooldown(caster, config.cooldownTicks(), context.serverTick()));
        int perHit = config.damagePerHit() + scaledAttack(caster, config.attackRatio());
        crowdControl.apply(target.owner(), new CrowdControlEffect(caster.owner(), CrowdControlType.STUN, 1, context.serverTick() + config.stunTicks(), true));
        events.add(new AbilityEvent(caster.owner(), AbilitySlot.R, AbilityEvent.Phase.START, context.serverTick()));
        deathDances.put(caster.owner(), new DeathDance(caster.owner(), target.owner(), context.serverTick(), config.hits(), perHit));
        return new ShelianerAbilityResult(true, "SIX_HIT", 0);
    }
    public List<AbilityEvent> events() { return List.copyOf(events); }
    private long cooldown(HeroRuntime caster, long base, long tick) { return buffs == null ? base : buffs.heroAbilityCooldownTicks(caster.owner(), base, tick); }
    private ShelianerAbilityResult damage(HeroRuntime caster, HeroRuntime target, AbilitySlot slot, int amount, DamageType type, long tick) {
        var result = damage.applyAbility(caster, target, new DamageInstance(UUID.randomUUID(), caster.owner(), target.owner(), type, amount, tick));
        if (!result.applied()) return ShelianerAbilityResult.rejected(result.reason());
        events.add(new AbilityEvent(caster.owner(), slot, AbilityEvent.Phase.HIT, tick)); return new ShelianerAbilityResult(true, "", result.amount());
    }
    private static int scaledAttack(HeroRuntime hero, double ratio) { return (int) Math.floor(hero.effectiveStats().attack() * ratio); }
    private static boolean enemyInRange(HeroRuntime caster, HeroRuntime target, TargetSnapshot snapshot, double range) { return target != null && snapshot != null && target.owner().equals(snapshot.id()) && target.alive() && snapshot.alive() && target.team() != caster.team() && snapshot.distance() <= range; }
    private record Poison(UUID caster, long nextTick, int ticksRemaining, int damagePerTick, double slow) { Poison next() { return new Poison(caster, nextTick + 20, ticksRemaining - 1, damagePerTick, slow); } }
    private static final class DeathDance {
        private final UUID caster; private final UUID target; private long nextHitTick; private int hitsRemaining; private final int damagePerHit;
        private DeathDance(UUID caster, UUID target, long nextHitTick, int hitsRemaining, int damagePerHit) { this.caster = caster; this.target = target; this.nextHitTick = nextHitTick; this.hitsRemaining = hitsRemaining; this.damagePerHit = damagePerHit; }
        UUID caster() { return caster; } UUID target() { return target; } long nextHitTick() { return nextHitTick; } int hitsRemaining() { return hitsRemaining; } int damagePerHit() { return damagePerHit; }
        void advance() { nextHitTick++; hitsRemaining--; }
    }
    private record PendingKnife(HeroRuntime caster, int damage) { }
}
