package com.zeravorn.hero;

import com.zeravorn.ability.AbilityContext;
import com.zeravorn.ability.AbilitySlot;
import com.zeravorn.combat.CooldownService;
import com.zeravorn.combat.CrowdControlService;
import com.zeravorn.combat.DamageService;
import com.zeravorn.combat.TargetSnapshot;
import com.zeravorn.team.TeamId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class EsakiAbilityTest {
    @Test void qUsesRankOneConfigAndRejectsOutOfRangeTarget() {
        HeroRuntime caster = esaki(); HeroRuntime target = target(); caster.upgradeAbility(AbilitySlot.Q); EsakiAbilityService service = service();
        assertEquals("INVALID_TARGET", service.castQ(caster, target, snapshot(target, 7.1), new AbilityContext(0, true)).reason());
        int mana = caster.mana(); var result = service.castQ(caster, target, snapshot(target, 7), new AbilityContext(0, true));
        assertTrue(result.executed()); assertEquals(70 + 99, result.damage()); assertEquals(mana - 75, caster.mana());
    }
    @Test void eHitsOnlyEnemiesInConfiguredRadiusAndAppliesKnockback() {
        HeroRuntime caster = esaki(); HeroRuntime near = target(); HeroRuntime far = target(); caster.upgradeAbility(AbilitySlot.E); CrowdControlService cc = new CrowdControlService();
        var result = new EsakiAbilityService(new CooldownService(), new DamageService(), cc).castE(caster, List.of(snapshot(near, 3), snapshot(far, 3.1)), Map.of(near.owner(), near, far.owner(), far), new AbilityContext(0, true));
        assertTrue(result.executed()); assertEquals(40 + 45, result.damage()); assertEquals(1, cc.effects(near.owner()).size()); assertTrue(cc.effects(far.owner()).isEmpty());
    }
    @Test void rUnlocksAtLevelFourAndProducesThreePulses() {
        HeroRuntime caster = esaki(); HeroRuntime target = target(); new LevelService().addExperience(caster, 850); caster.upgradeAbility(AbilitySlot.R); int before = target.health();
        var result = service().castR(caster, List.of(snapshot(target, 6)), Map.of(target.owner(), target), new AbilityContext(0, true));
        int perPulse = 55 + (int) Math.floor(caster.effectiveStats().attack() * .35);
        assertTrue(result.executed()); assertEquals(before - 3 * perPulse, target.health()); assertEquals(3 * perPulse, result.damage());
    }
    private static HeroRuntime esaki() { return new HeroRuntime(UUID.randomUUID(), TeamId.BLUE, HeroDefinitions.ESAKI); }
    private static HeroRuntime target() { return new HeroRuntime(UUID.randomUUID(), TeamId.RED, HeroDefinitions.AMELIA); }
    private static TargetSnapshot snapshot(HeroRuntime hero, double distance) { return new TargetSnapshot(hero.owner(), hero.team(), hero.alive(), distance); }
    private static EsakiAbilityService service() { return new EsakiAbilityService(new CooldownService(), new DamageService(), new CrowdControlService()); }
}
