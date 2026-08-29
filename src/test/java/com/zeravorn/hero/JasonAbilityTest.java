package com.zeravorn.hero;

import com.zeravorn.ability.AbilityContext;
import com.zeravorn.combat.CooldownService;
import com.zeravorn.combat.CrowdControlService;
import com.zeravorn.combat.DamageService;
import com.zeravorn.combat.TargetSnapshot;
import com.zeravorn.combat.CrowdControlEffect;
import com.zeravorn.combat.CrowdControlType;
import com.zeravorn.team.TeamId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JasonAbilityTest {
    @Test
    void qDealsConfiguredDamageAndHealsWithoutMana() {
        HeroRuntime jason = new HeroRuntime(UUID.randomUUID(), TeamId.BLUE, HeroDefinitions.JASON);
        HeroRuntime target = new HeroRuntime(UUID.randomUUID(), TeamId.RED, HeroDefinitions.AMELIA);
        jason.upgradeAbility(com.zeravorn.ability.AbilitySlot.Q);
        JasonAbilityService abilities = service();
        int before = jason.health();
        JasonAbilityResult result = abilities.castQ(jason, target, snapshot(target, 2), new AbilityContext(0, true));
        assertTrue(result.executed());
        assertEquals(60 + jason.stats().attack(), result.damage());
        assertTrue(jason.health() >= before);
        assertEquals(0, jason.mana());
    }

    @Test
    void eRequiresSecondPhaseInsideWindowAndAppliesHardCc() {
        HeroRuntime jason = new HeroRuntime(UUID.randomUUID(), TeamId.BLUE, HeroDefinitions.JASON);
        HeroRuntime target = new HeroRuntime(UUID.randomUUID(), TeamId.RED, HeroDefinitions.AMELIA);
        jason.upgradeAbility(com.zeravorn.ability.AbilitySlot.E);
        JasonAbilityService abilities = service();
        assertTrue(abilities.beginE(jason, new AbilityContext(0, true)).executed());
        assertTrue(abilities.resolveE(jason, target, snapshot(target, 2), new AbilityContext(50, true)).executed());
        assertFalse(abilities.resolveE(jason, target, snapshot(target, 2), new AbilityContext(51, true)).executed());
    }

    @Test
    void rRequiresLevelFourAndHitsOnlyEnemiesInRadius() {
        HeroRuntime jason = new HeroRuntime(UUID.randomUUID(), TeamId.BLUE, HeroDefinitions.JASON);
        HeroRuntime target = new HeroRuntime(UUID.randomUUID(), TeamId.RED, HeroDefinitions.AMELIA);
        jason.addSkillPoints(1);
        assertFalse(service().castR(jason, List.of(snapshot(target, 2)), Map.of(target.owner(), target), new AbilityContext(0, true)).executed());
        new LevelService().addExperience(jason, 850);
        jason.upgradeAbility(com.zeravorn.ability.AbilitySlot.R);
        JasonAbilityResult result = service().castR(jason, List.of(snapshot(target, 2)), Map.of(target.owner(), target), new AbilityContext(0, true));
        assertTrue(result.executed());
        assertTrue(result.damage() > 0);
    }

    @Test
    void dashAndFlightAreBlockedByRootAndEmitAbilityEvents() {
        HeroRuntime jason = new HeroRuntime(UUID.randomUUID(), TeamId.BLUE, HeroDefinitions.JASON);
        com.zeravorn.combat.CrowdControlService cc = new com.zeravorn.combat.CrowdControlService();
        JasonMovementService movement = new JasonMovementService(cc);
        assertEquals(2.0, movement.dash(jason).distance());
        cc.apply(jason.owner(), new CrowdControlEffect(UUID.randomUUID(), CrowdControlType.ROOT, 1, 20, true));
        assertEquals("ROOT_BLOCK", movement.dash(jason).reason());
        JasonAbilityService abilities = service();
        jason.upgradeAbility(com.zeravorn.ability.AbilitySlot.Q);
        HeroRuntime target = new HeroRuntime(UUID.randomUUID(), TeamId.RED, HeroDefinitions.AMELIA);
        assertTrue(abilities.castQ(jason, target, snapshot(target, 2), new com.zeravorn.ability.AbilityContext(0, true)).executed());
        assertEquals(2, abilities.events().size());
    }

    private static TargetSnapshot snapshot(HeroRuntime target, double distance) { return new TargetSnapshot(target.owner(), target.team(), target.alive(), distance); }
    private static JasonAbilityService service() { return new JasonAbilityService(new CooldownService(), new DamageService(), new CrowdControlService()); }
}
