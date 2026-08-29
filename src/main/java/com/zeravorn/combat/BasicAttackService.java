package com.zeravorn.combat;

import com.zeravorn.hero.HeroRuntime;
import java.util.UUID;

public final class BasicAttackService {
    private final CooldownService cooldowns;
    private final TargetingService targeting;
    private final DamageService damage;
    private final AttackSpeedService attackSpeed;
    private final CrowdControlService crowdControl;

    public BasicAttackService(CooldownService cooldowns, TargetingService targeting, DamageService damage, AttackSpeedService attackSpeed) {
        this(cooldowns, targeting, damage, attackSpeed, null);
    }
    public BasicAttackService(CooldownService cooldowns, TargetingService targeting, DamageService damage, AttackSpeedService attackSpeed, CrowdControlService crowdControl) {
        this.cooldowns = cooldowns; this.targeting = targeting; this.damage = damage; this.attackSpeed = attackSpeed; this.crowdControl = crowdControl;
    }
    public DamageResult attack(HeroRuntime caster, HeroRuntime target, TargetSnapshot snapshot, double range, double attackSpeedMultiplier, long tick, int ticksPerSecond) {
        if (!caster.alive()) return DamageResult.rejected("DEAD", target.health());
        if (crowdControl != null && crowdControl.blocksBasicOrAbility(caster.owner())) return DamageResult.rejected("STUNNED", target.health());
        if (!snapshot.id().equals(target.owner())) return DamageResult.rejected("INVALID_TARGET", target.health());
        if (!targeting.validEnemy(caster, snapshot, range)) return DamageResult.rejected("INVALID_TARGET", target.health());
        long interval = attackSpeed.intervalTicks(caster.definition().baseAttackInterval(), attackSpeedMultiplier, ticksPerSecond);
        if (!cooldowns.tryStart(caster.owner(), "basic_attack", tick, interval)) return DamageResult.rejected("COOLDOWN", target.health());
        return damage.apply(target, new DamageInstance(UUID.randomUUID(), caster.owner(), target.owner(), DamageType.PHYSICAL, caster.stats().attack(), tick));
    }
}
