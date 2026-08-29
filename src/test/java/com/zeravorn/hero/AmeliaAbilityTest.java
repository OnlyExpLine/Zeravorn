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

final class AmeliaAbilityTest {
    @Test void qChannelsSixMagicTicksAndConsumesMana() {
        HeroRuntime caster = amelia(); HeroRuntime target = target(); caster.upgradeAbility(AbilitySlot.Q); int mana = caster.mana();
        var result = service().castQ(caster, List.of(snapshot(target, 5)), Map.of(target.owner(), target), new AbilityContext(0, true));
        assertTrue(result.executed()); assertEquals(6 * 23, result.damage()); assertEquals(mana - 90, caster.mana());
    }
    @Test void eDamagesOnlyTargetsInsideRadius() {
        HeroRuntime caster = amelia(); HeroRuntime near = target(); HeroRuntime far = target(); caster.upgradeAbility(AbilitySlot.E);
        var result = service().castE(caster, List.of(snapshot(near, 4), snapshot(far, 4.1)), Map.of(near.owner(), near, far.owner(), far), new AbilityContext(0, true));
        assertTrue(result.executed()); assertEquals(60, result.damage()); assertEquals(840, near.health()); assertEquals(900, far.health());
    }
    @Test void rUnlocksAtFourAndUsesConfiguredDelayEventTick() {
        HeroRuntime caster = amelia(); HeroRuntime target = target(); new LevelService().addExperience(caster, 850); caster.upgradeAbility(AbilitySlot.R);
        AmeliaAbilityService service = service(); assertTrue(service.castR(caster, List.of(snapshot(target, 2)), Map.of(target.owner(), target), new AbilityContext(100, true)).executed());
        assertEquals(111, service.events().getLast().serverTick()); assertEquals(725, target.health());
    }
    private static HeroRuntime amelia() { return new HeroRuntime(UUID.randomUUID(), TeamId.BLUE, HeroDefinitions.AMELIA); }
    private static HeroRuntime target() { return new HeroRuntime(UUID.randomUUID(), TeamId.RED, HeroDefinitions.AMELIA); }
    private static TargetSnapshot snapshot(HeroRuntime hero, double distance) { return new TargetSnapshot(hero.owner(), hero.team(), hero.alive(), distance); }
    private static AmeliaAbilityService service() { return new AmeliaAbilityService(new CooldownService(), new DamageService(), new CrowdControlService()); }
}
