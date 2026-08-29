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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Authoritative Shelianer combat implementation. Client animation is driven only by emitted events. */
public final class ShelianerAbilityService {
    private final CooldownService cooldowns; private final ManaService mana = new ManaService();
    private final DamageService damage; private final CrowdControlService crowdControl; private final AbilityCastValidator validator;
    private final Map<UUID, Poison> poisons = new HashMap<>(); private final List<AbilityEvent> events = new ArrayList<>();
    public ShelianerAbilityService(CooldownService cooldowns, DamageService damage, CrowdControlService crowdControl) {
        this.cooldowns = cooldowns; this.damage = damage; this.crowdControl = crowdControl; this.validator = new AbilityCastValidator(crowdControl);
    }
    public ShelianerAbilityResult castQ(HeroRuntime caster, HeroRuntime target, TargetSnapshot targetSnapshot, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.Q, context); if (!error.isEmpty()) return ShelianerAbilityResult.rejected(error);
        var config = ShelianerAbilityDefinitions.q(caster.abilityRank(AbilitySlot.Q));
        if (!enemyInRange(caster, target, targetSnapshot, config.range())) return ShelianerAbilityResult.rejected("INVALID_TARGET");
        if (!cooldowns.ready(caster.owner(), "shelianer_q", context.serverTick())) return ShelianerAbilityResult.rejected("COOLDOWN");
        if (!mana.trySpend(caster, config.mana())) return ShelianerAbilityResult.rejected("NO_MANA");
        cooldowns.tryStart(caster.owner(), "shelianer_q", context.serverTick(), config.cooldownTicks());
        return damage(caster, target, AbilitySlot.Q, config.damage() + scaledAttack(caster, config.attackRatio()), DamageType.PHYSICAL, context.serverTick());
    }
    /** Starts the poison DoT after server-side projectile/collision validation has selected an enemy target. */
    public ShelianerAbilityResult castE(HeroRuntime caster, HeroRuntime target, TargetSnapshot targetSnapshot, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.E, context); if (!error.isEmpty()) return ShelianerAbilityResult.rejected(error);
        if (!enemyInRange(caster, target, targetSnapshot, Double.MAX_VALUE)) return ShelianerAbilityResult.rejected("INVALID_TARGET");
        var config = ShelianerAbilityDefinitions.e(caster.abilityRank(AbilitySlot.E));
        if (!cooldowns.ready(caster.owner(), "shelianer_e", context.serverTick())) return ShelianerAbilityResult.rejected("COOLDOWN");
        if (!mana.trySpend(caster, config.mana())) return ShelianerAbilityResult.rejected("NO_MANA");
        cooldowns.tryStart(caster.owner(), "shelianer_e", context.serverTick(), config.cooldownTicks());
        poisons.put(target.owner(), new Poison(caster.owner(), context.serverTick(), 4, config.damagePerTick() + scaledAttack(caster, config.attackRatio()), config.slow()));
        crowdControl.apply(target.owner(), new CrowdControlEffect(caster.owner(), CrowdControlType.SLOW, config.slow(), context.serverTick() + config.durationTicks(), true));
        events.add(new AbilityEvent(caster.owner(), AbilitySlot.E, AbilityEvent.Phase.START, context.serverTick()));
        return new ShelianerAbilityResult(true, "POISON_APPLIED", 0);
    }
    /** Processes the four 0.5-second poison ticks. */
    public void tick(long serverTick, Map<UUID, HeroRuntime> heroes) {
        poisons.entrySet().removeIf(entry -> {
            Poison poison = entry.getValue(); HeroRuntime target = heroes.get(entry.getKey());
            if (target == null || !target.alive() || poison.ticksRemaining() == 0) return true;
            if (serverTick >= poison.nextTick()) {
                HeroRuntime caster = heroes.get(poison.caster());
                if (caster != null && caster.alive()) damage(caster, target, AbilitySlot.E, poison.damagePerTick(), DamageType.MAGICAL, serverTick);
                poison = poison.next(); entry.setValue(poison);
            }
            return poison.ticksRemaining() == 0;
        });
    }
    public ShelianerAbilityResult castF(HeroRuntime caster, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.F, context); if (!error.isEmpty()) return ShelianerAbilityResult.rejected(error);
        if (crowdControl.blocksMovement(caster.owner())) return ShelianerAbilityResult.rejected("ROOT_BLOCK");
        var config = ShelianerAbilityDefinitions.f(caster.abilityRank(AbilitySlot.F));
        if (!cooldowns.ready(caster.owner(), "shelianer_f", context.serverTick())) return ShelianerAbilityResult.rejected("COOLDOWN");
        if (!mana.trySpend(caster, config.mana())) return ShelianerAbilityResult.rejected("NO_MANA");
        cooldowns.tryStart(caster.owner(), "shelianer_f", context.serverTick(), config.cooldownTicks());
        events.add(new AbilityEvent(caster.owner(), AbilitySlot.F, AbilityEvent.Phase.START, context.serverTick()));
        return new ShelianerAbilityResult(true, "DASH_" + config.dashDistance(), 0);
    }
    public ShelianerAbilityResult castR(HeroRuntime caster, HeroRuntime target, TargetSnapshot targetSnapshot, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.R, context); if (!error.isEmpty()) return ShelianerAbilityResult.rejected(error);
        var config = ShelianerAbilityDefinitions.r(caster.abilityRank(AbilitySlot.R));
        if (!enemyInRange(caster, target, targetSnapshot, config.range())) return ShelianerAbilityResult.rejected("INVALID_TARGET");
        if (!cooldowns.ready(caster.owner(), "shelianer_r", context.serverTick())) return ShelianerAbilityResult.rejected("COOLDOWN");
        if (!mana.trySpend(caster, config.mana())) return ShelianerAbilityResult.rejected("NO_MANA");
        cooldowns.tryStart(caster.owner(), "shelianer_r", context.serverTick(), config.cooldownTicks());
        int perHit = config.damagePerHit() + scaledAttack(caster, config.attackRatio()), total = 0;
        crowdControl.apply(target.owner(), new CrowdControlEffect(caster.owner(), CrowdControlType.STUN, 1, context.serverTick() + config.stunTicks(), true));
        events.add(new AbilityEvent(caster.owner(), AbilitySlot.R, AbilityEvent.Phase.START, context.serverTick()));
        for (int hit = 0; hit < config.hits() && target.alive(); hit++) total += damage(caster, target, AbilitySlot.R, perHit, DamageType.PHYSICAL, context.serverTick() + hit).damage();
        return new ShelianerAbilityResult(true, "SIX_HIT", total);
    }
    public List<AbilityEvent> events() { return List.copyOf(events); }
    private ShelianerAbilityResult damage(HeroRuntime caster, HeroRuntime target, AbilitySlot slot, int amount, DamageType type, long tick) {
        var result = damage.apply(target, new DamageInstance(UUID.randomUUID(), caster.owner(), target.owner(), type, amount, tick));
        if (!result.applied()) return ShelianerAbilityResult.rejected(result.reason());
        events.add(new AbilityEvent(caster.owner(), slot, AbilityEvent.Phase.HIT, tick)); return new ShelianerAbilityResult(true, "", result.amount());
    }
    private static int scaledAttack(HeroRuntime hero, double ratio) { return (int) Math.floor(hero.effectiveStats().attack() * ratio); }
    private static boolean enemyInRange(HeroRuntime caster, HeroRuntime target, TargetSnapshot snapshot, double range) { return target != null && snapshot != null && target.owner().equals(snapshot.id()) && target.alive() && snapshot.alive() && target.team() != caster.team() && snapshot.distance() <= range; }
    private record Poison(UUID caster, long nextTick, int ticksRemaining, int damagePerTick, double slow) { Poison next() { return new Poison(caster, nextTick + 20, ticksRemaining - 1, damagePerTick, slow); } }
}
