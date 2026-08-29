package com.zeravorn.hero;

import com.zeravorn.ability.AbilityContext;
import com.zeravorn.ability.AbilityEvent;
import com.zeravorn.ability.AbilitySlot;
import com.zeravorn.ability.AbilityCastValidator;
import com.zeravorn.combat.CooldownService;
import com.zeravorn.combat.CrowdControlEffect;
import com.zeravorn.combat.CrowdControlService;
import com.zeravorn.combat.CrowdControlType;
import com.zeravorn.combat.DamageInstance;
import com.zeravorn.combat.DamageResult;
import com.zeravorn.combat.DamageService;
import com.zeravorn.combat.DamageType;
import com.zeravorn.combat.TargetSnapshot;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;

public final class JasonAbilityService {
    private final CooldownService cooldowns;
    private final DamageService damage;
    private final CrowdControlService crowdControl;
    private final AbilityCastValidator validator = new AbilityCastValidator();
    private final Map<UUID, Long> pendingDoubleDash = new HashMap<>();
    private final List<AbilityEvent> events = new ArrayList<>();

    public JasonAbilityService(CooldownService cooldowns, DamageService damage, CrowdControlService crowdControl) {
        this.cooldowns = cooldowns; this.damage = damage; this.crowdControl = crowdControl;
    }

    public JasonAbilityResult castQ(HeroRuntime caster, HeroRuntime target, TargetSnapshot snapshot, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.Q, context);
        if (!error.isEmpty()) return JasonAbilityResult.rejected(error);
        if (!validEnemy(caster, target, snapshot, 2)) return JasonAbilityResult.rejected("INVALID_TARGET");
        int rank = caster.abilityRank(AbilitySlot.Q); JasonAbilityDefinitions.Q config = JasonAbilityDefinitions.q(rank);
        if (!cooldowns.tryStart(caster.owner(), "jason_q", context.serverTick(), config.cooldownTicks())) return JasonAbilityResult.rejected("COOLDOWN");
        int amount = config.baseDamage() + caster.stats().attack();
        DamageResult result = damage.apply(target, new DamageInstance(UUID.randomUUID(), caster.owner(), target.owner(), DamageType.PHYSICAL, amount, context.serverTick()));
        if (!result.applied()) return JasonAbilityResult.rejected(result.reason());
        int healing = (int) Math.floor(result.amount() * config.healRatio()); caster.heal(healing);
        events.add(new AbilityEvent(caster.owner(), AbilitySlot.Q, AbilityEvent.Phase.START, context.serverTick()));
        events.add(new AbilityEvent(caster.owner(), AbilitySlot.Q, AbilityEvent.Phase.HIT, context.serverTick()));
        return new JasonAbilityResult(true, "", result.amount(), healing);
    }

    public JasonAbilityResult beginE(HeroRuntime caster, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.E, context);
        if (!error.isEmpty()) return JasonAbilityResult.rejected(error);
        int rank = caster.abilityRank(AbilitySlot.E); JasonAbilityDefinitions.E config = JasonAbilityDefinitions.e(rank);
        if (!cooldowns.tryStart(caster.owner(), "jason_e", context.serverTick(), config.cooldownTicks())) return JasonAbilityResult.rejected("COOLDOWN");
        pendingDoubleDash.put(caster.owner(), context.serverTick() + config.windowTicks());
        events.add(new AbilityEvent(caster.owner(), AbilitySlot.E, AbilityEvent.Phase.START, context.serverTick()));
        return new JasonAbilityResult(true, "DASH_1", 0, 0);
    }

    public JasonAbilityResult resolveE(HeroRuntime caster, HeroRuntime target, TargetSnapshot snapshot, AbilityContext context) {
        Long expires = pendingDoubleDash.get(caster.owner());
        if (expires == null || context.serverTick() > expires) { pendingDoubleDash.remove(caster.owner()); return JasonAbilityResult.rejected("E_WINDOW_EXPIRED"); }
        if (!caster.alive()) return JasonAbilityResult.rejected("DEAD");
        if (!validEnemy(caster, target, snapshot, 2)) return JasonAbilityResult.rejected("INVALID_TARGET");
        pendingDoubleDash.remove(caster.owner());
        JasonAbilityDefinitions.E config = JasonAbilityDefinitions.e(caster.abilityRank(AbilitySlot.E));
        int amount = config.baseDamage() + (int) Math.floor(caster.stats().attack() * config.attackRatio());
        DamageResult result = damage.apply(target, new DamageInstance(UUID.randomUUID(), caster.owner(), target.owner(), DamageType.PHYSICAL, amount, context.serverTick()));
        if (!result.applied()) return JasonAbilityResult.rejected(result.reason());
        crowdControl.apply(target.owner(), new CrowdControlEffect(caster.owner(), CrowdControlType.KNOCKUP, 1, context.serverTick() + config.stunTicks(), true));
        crowdControl.apply(target.owner(), new CrowdControlEffect(caster.owner(), CrowdControlType.STUN, 1, context.serverTick() + config.stunTicks(), true));
        events.add(new AbilityEvent(caster.owner(), AbilitySlot.E, AbilityEvent.Phase.HIT, context.serverTick()));
        return new JasonAbilityResult(true, "DASH_2", result.amount(), 0);
    }

    public JasonAbilityResult castR(HeroRuntime caster, List<TargetSnapshot> snapshots, Map<UUID, HeroRuntime> targets, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.R, context);
        if (!error.isEmpty()) return JasonAbilityResult.rejected(error);
        int rank = caster.abilityRank(AbilitySlot.R); JasonAbilityDefinitions.R config = JasonAbilityDefinitions.r(rank);
        if (!cooldowns.tryStart(caster.owner(), "jason_r", context.serverTick(), config.cooldownTicks())) return JasonAbilityResult.rejected("COOLDOWN");
        int totalDamage = 0;
        for (TargetSnapshot snapshot : snapshots) {
            HeroRuntime target = targets.get(snapshot.id());
            if (!validEnemy(caster, target, snapshot, config.radius())) continue;
            int amount = config.baseDamage() + (int) Math.floor(caster.stats().attack() * config.attackRatio());
            DamageResult result = damage.apply(target, new DamageInstance(UUID.randomUUID(), caster.owner(), target.owner(), DamageType.PHYSICAL, amount, context.serverTick()));
            if (!result.applied()) continue;
            crowdControl.apply(target.owner(), new CrowdControlEffect(caster.owner(), CrowdControlType.STUN, 1, context.serverTick() + config.stunTicks(), true));
            totalDamage += result.amount();
        }
        events.add(new AbilityEvent(caster.owner(), AbilitySlot.R, AbilityEvent.Phase.START, context.serverTick()));
        events.add(new AbilityEvent(caster.owner(), AbilitySlot.R, AbilityEvent.Phase.HIT, context.serverTick()));
        return new JasonAbilityResult(true, "LANDING", totalDamage, 0);
    }

    public List<AbilityEvent> events() { return Collections.unmodifiableList(events); }

    private boolean validEnemy(HeroRuntime caster, HeroRuntime target, TargetSnapshot snapshot, double range) {
        return target != null && snapshot != null && snapshot.id().equals(target.owner()) && snapshot.alive() && target.alive()
                && snapshot.team() != caster.team() && snapshot.distance() <= range;
    }
}
