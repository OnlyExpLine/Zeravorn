package com.zeravorn.combat;

import com.zeravorn.hero.HeroDefinitions;
import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.ability.AbilitySlot;
import com.zeravorn.ability.AbilityCastValidator;
import com.zeravorn.ability.AbilityContext;
import com.zeravorn.team.TeamId;
import com.zeravorn.projectile.ProjectileService;
import com.zeravorn.projectile.ProjectileState;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CombatCoreTest {
    @Test
    void damageInstanceIsAppliedOnceAndDeathIsAuthoritative() {
        HeroRuntime target = new HeroRuntime(UUID.randomUUID(), TeamId.BLUE, HeroDefinitions.JASON);
        DamageService damage = new DamageService();
        DamageInstance hit = new DamageInstance(UUID.randomUUID(), UUID.randomUUID(), target.owner(), DamageType.PHYSICAL, 2000, 1);
        assertTrue(damage.apply(target, hit).applied());
        assertEquals("DEAD", damage.apply(target, hit).reason());
        assertFalse(target.alive());
    }

    @Test
    void cooldownAndManaAreServerValidated() {
        UUID owner = UUID.randomUUID();
        CooldownService cooldown = new CooldownService();
        assertTrue(cooldown.tryStart(owner, "q", 10, 20));
        assertFalse(cooldown.tryStart(owner, "q", 20, 20));
        assertTrue(cooldown.ready(owner, "q", 30));
        HeroRuntime hero = new HeroRuntime(owner, TeamId.BLUE, HeroDefinitions.AMELIA);
        ManaService mana = new ManaService();
        assertTrue(mana.trySpend(hero, 100));
        assertFalse(mana.trySpend(hero, 9999));
    }

    @Test
    void ccExpiresAndStrongestSlowWins() {
        CrowdControlService cc = new CrowdControlService();
        UUID target = UUID.randomUUID();
        UUID source = UUID.randomUUID();
        cc.apply(target, new CrowdControlEffect(source, CrowdControlType.SLOW, 0.20, 20, true));
        cc.apply(target, new CrowdControlEffect(source, CrowdControlType.SLOW, 0.50, 30, true));
        assertEquals(1, cc.effects(target).size());
        assertEquals(0.50, cc.effects(target).getFirst().strength());
        assertEquals(0.50, cc.movementMultiplier(target));
        cc.apply(target, new CrowdControlEffect(source, CrowdControlType.STUN, 1, 40, true));
        assertTrue(cc.blocksBasicOrAbility(target));
        cc.tick(40);
        assertTrue(cc.effects(target).isEmpty());
    }

    @Test
    void hardCrowdControlBlocksBasicAttack() {
        HeroRuntime caster = new HeroRuntime(UUID.randomUUID(), TeamId.BLUE, HeroDefinitions.JASON);
        HeroRuntime target = new HeroRuntime(UUID.randomUUID(), TeamId.RED, HeroDefinitions.AMELIA);
        CrowdControlService cc = new CrowdControlService();
        cc.apply(caster.owner(), new CrowdControlEffect(UUID.randomUUID(), CrowdControlType.STUN, 1, 20, true));
        BasicAttackService attacks = new BasicAttackService(new CooldownService(), new TargetingService(), new DamageService(), new AttackSpeedService(), cc);
        TargetSnapshot snapshot = new TargetSnapshot(target.owner(), target.team(), true, 2);
        assertEquals("STUNNED", attacks.attack(caster, target, snapshot, 3, 1, 0, 20).reason());
    }

    @Test
    void attackIntervalUsesConfiguredBaseIntervalAndMultiplier() {
        assertEquals(40, new AttackSpeedService().intervalTicks(2.0, 1.0, 20));
        assertEquals(20, new AttackSpeedService().intervalTicks(2.0, 2.0, 20));
    }

    @Test
    void basicAttackChecksTargetTeamRangeAndCooldown() {
        HeroRuntime caster = new HeroRuntime(UUID.randomUUID(), TeamId.BLUE, HeroDefinitions.JASON);
        HeroRuntime target = new HeroRuntime(UUID.randomUUID(), TeamId.RED, HeroDefinitions.AMELIA);
        BasicAttackService attacks = new BasicAttackService(new CooldownService(), new TargetingService(), new DamageService(), new AttackSpeedService());
        TargetSnapshot snapshot = new TargetSnapshot(target.owner(), target.team(), true, 2);
        assertTrue(attacks.attack(caster, target, snapshot, 3, 1, 0, 20).applied());
        assertFalse(attacks.attack(caster, target, snapshot, 3, 1, 0, 20).applied());
        assertFalse(attacks.attack(caster, target, new TargetSnapshot(target.owner(), TeamId.RED, true, 4), 3, 1, 20, 20).applied());
    }

    @Test
    void projectileConfirmsEachTargetOnlyOnceAndRemovesOnHit() {
        ProjectileService projectiles = new ProjectileService();
        ProjectileState state = projectiles.spawn(UUID.randomUUID(), new com.zeravorn.projectile.ProjectileDefinition(10, 100, 0.25, 0));
        UUID target = UUID.randomUUID();
        assertTrue(projectiles.confirmHit(state.id(), target));
        assertFalse(projectiles.active(state.id()));
        assertThrows(IllegalArgumentException.class, () -> projectiles.confirmHit(state.id(), target));
    }

    @Test
    void abilityRuntimeExposesAuthoritativeCurrentRank() {
        HeroRuntime hero = new HeroRuntime(UUID.randomUUID(), TeamId.BLUE, HeroDefinitions.JASON);
        assertFalse(hero.abilityRuntime(AbilitySlot.Q).learned());
        hero.upgradeAbility(AbilitySlot.Q);
        assertEquals(1, hero.abilityRuntime(AbilitySlot.Q).rank());
    }

    @Test
    void abilityCastIsRejectedByServerStateAndRankValidation() {
        HeroRuntime hero = new HeroRuntime(UUID.randomUUID(), TeamId.BLUE, HeroDefinitions.JASON);
        AbilityCastValidator validator = new AbilityCastValidator();
        assertEquals("WRONG_STATE", validator.validate(hero, AbilitySlot.Q, new AbilityContext(0, false)));
        assertEquals("ABILITY_NOT_LEARNED", validator.validate(hero, AbilitySlot.Q, new AbilityContext(0, true)));
        hero.upgradeAbility(AbilitySlot.Q);
        assertTrue(validator.canCast(hero, AbilitySlot.Q, new AbilityContext(1, true)));
        hero.setAlive(false);
        assertEquals("DEAD", validator.validate(hero, AbilitySlot.Q, new AbilityContext(2, true)));
    }
}
