package com.zeravorn.hero;

import com.zeravorn.ability.AbilityContext;
import com.zeravorn.ability.AbilitySlot;
import com.zeravorn.combat.CooldownService;
import com.zeravorn.combat.CrowdControlEffect;
import com.zeravorn.combat.CrowdControlService;
import com.zeravorn.combat.CrowdControlType;
import com.zeravorn.combat.DamageService;
import com.zeravorn.combat.TargetSnapshot;
import com.zeravorn.team.TeamId;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ShelianerAbilityTest {
    @Test void qUsesConfiguredRankDamageManaAndCooldown() {
        HeroRuntime caster = shelianer(); HeroRuntime target = target(); caster.upgradeAbility(AbilitySlot.Q);
        ShelianerAbilityService service = service(); int mana = caster.mana();
        var result = service.castQ(caster, target, snapshot(target, 7), new AbilityContext(0, true));
        assertTrue(result.executed()); assertEquals(35 + 45, result.damage()); assertEquals(mana - 35, caster.mana());
        assertEquals("COOLDOWN", service.castQ(caster, target, snapshot(target, 7), new AbilityContext(1, true)).reason());
    }
    @Test void poisonTicksFourTimesAndAppliesSlow() {
        HeroRuntime caster = shelianer(); HeroRuntime target = target(); caster.upgradeAbility(AbilitySlot.E);
        CrowdControlService cc = new CrowdControlService(); ShelianerAbilityService service = new ShelianerAbilityService(new CooldownService(), new DamageService(), cc);
        assertTrue(service.castE(caster, target, snapshot(target, 2), new AbilityContext(0, true)).executed());
        int before = target.health(); for (long tick : new long[] {0, 20, 40, 60}) service.tick(tick, Map.of(caster.owner(), caster, target.owner(), target));
        assertEquals(before - 4 * (18 + 6), target.health()); assertEquals(0.85, cc.movementMultiplier(target.owner()));
    }
    @Test void fIsShelianerOnlyAndRootBlocksDash() {
        HeroRuntime caster = shelianer(); caster.upgradeAbility(AbilitySlot.F); CrowdControlService cc = new CrowdControlService();
        ShelianerAbilityService service = new ShelianerAbilityService(new CooldownService(), new DamageService(), cc);
        assertTrue(service.castF(caster, new AbilityContext(0, true)).executed());
        HeroRuntime rooted = shelianer(); rooted.upgradeAbility(AbilitySlot.F); cc.apply(rooted.owner(), new CrowdControlEffect(UUID.randomUUID(), CrowdControlType.ROOT, 1, 50, true));
        assertEquals("ROOT_BLOCK", service.castF(rooted, new AbilityContext(1, true)).reason());
    }
    @Test void rRequiresLevelFourAndCreatesSixPhysicalHits() {
        HeroRuntime caster = shelianer(); HeroRuntime target = target();
        assertEquals("ABILITY_NOT_LEARNED", service().castR(caster, target, snapshot(target, 3), new AbilityContext(0, true)).reason());
        new LevelService().addExperience(caster, 850); caster.upgradeAbility(AbilitySlot.R); int before = target.health();
        var result = service().castR(caster, target, snapshot(target, 3), new AbilityContext(0, true));
        int expectedPerHit = 24 + (int) Math.floor(caster.effectiveStats().attack() * 0.23);
        assertTrue(result.executed()); assertEquals(before - 6 * expectedPerHit, target.health()); assertTrue(result.damage() > 0);
    }
    private static HeroRuntime shelianer() { return new HeroRuntime(UUID.randomUUID(), TeamId.BLUE, HeroDefinitions.SHELIANER); }
    private static HeroRuntime target() { return new HeroRuntime(UUID.randomUUID(), TeamId.RED, HeroDefinitions.AMELIA); }
    private static TargetSnapshot snapshot(HeroRuntime target, double distance) { return new TargetSnapshot(target.owner(), target.team(), true, distance); }
    private static ShelianerAbilityService service() { return new ShelianerAbilityService(new CooldownService(), new DamageService(), new CrowdControlService()); }
}
