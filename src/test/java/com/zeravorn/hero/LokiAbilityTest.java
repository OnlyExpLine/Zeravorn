package com.zeravorn.hero;

import com.zeravorn.ability.AbilityContext;
import com.zeravorn.ability.AbilitySlot;
import com.zeravorn.combat.CooldownService;
import com.zeravorn.combat.CrowdControlService;
import com.zeravorn.combat.CrowdControlType;
import com.zeravorn.combat.DamageService;
import com.zeravorn.combat.TargetSnapshot;
import com.zeravorn.team.TeamId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class LokiAbilityTest {
    @Test void qDealsPhysicalDamageAndAppliesPull() {
        HeroRuntime caster = loki(); HeroRuntime target = target(); caster.upgradeAbility(AbilitySlot.Q); CrowdControlService cc = new CrowdControlService();
        var result = new LokiAbilityService(new CooldownService(), new DamageService(), cc).castQ(caster, target, snapshot(target, 9), new AbilityContext(0, true));
        assertTrue(result.executed()); assertEquals(45 + 31, result.damage()); assertTrue(cc.effects(target.owner()).stream().anyMatch(effect -> effect.type() == CrowdControlType.PULL));
    }
    @Test void eHasTimedConfiguredMoveBonus() {
        HeroRuntime caster = loki(); caster.upgradeAbility(AbilitySlot.E); LokiAbilityService service = service();
        assertTrue(service.castE(caster, new AbilityContext(10, true)).executed()); assertEquals(1.12, service.rampageMoveMultiplier(caster.owner(), 89)); assertEquals(1.0, service.rampageMoveMultiplier(caster.owner(), 90));
    }
    @Test void rUnlocksAtLevelFourAndRootsAllEnemiesInRadius() {
        HeroRuntime caster = loki(); HeroRuntime near = target(); HeroRuntime far = target(); new LevelService().addExperience(caster, 850); caster.upgradeAbility(AbilitySlot.R); CrowdControlService cc = new CrowdControlService();
        var result = new LokiAbilityService(new CooldownService(), new DamageService(), cc).castR(caster, List.of(snapshot(near, 4), snapshot(far, 4.1)), Map.of(near.owner(), near, far.owner(), far), new AbilityContext(0, true));
        int expected = 75 + (int) Math.floor(caster.effectiveStats().attack() * .4); assertTrue(result.executed()); assertEquals(expected, result.damage()); assertTrue(cc.effects(near.owner()).stream().anyMatch(effect -> effect.type() == CrowdControlType.ROOT)); assertTrue(cc.effects(far.owner()).isEmpty());
    }
    private static HeroRuntime loki() { return new HeroRuntime(UUID.randomUUID(), TeamId.BLUE, HeroDefinitions.LOKI); }
    private static HeroRuntime target() { return new HeroRuntime(UUID.randomUUID(), TeamId.RED, HeroDefinitions.AMELIA); }
    private static TargetSnapshot snapshot(HeroRuntime hero, double distance) { return new TargetSnapshot(hero.owner(), hero.team(), hero.alive(), distance); }
    private static LokiAbilityService service() { return new LokiAbilityService(new CooldownService(), new DamageService(), new CrowdControlService()); }
}
