package com.zeravorn.jungle;

import com.zeravorn.buff.BuffService;
import com.zeravorn.combat.CooldownService;
import com.zeravorn.combat.CrowdControlEffect;
import com.zeravorn.combat.CrowdControlService;
import com.zeravorn.combat.CrowdControlType;
import com.zeravorn.economy.ExperienceService;
import com.zeravorn.economy.GoldService;
import com.zeravorn.hero.HeroDefinitions;
import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.map.Position;
import com.zeravorn.match.MatchState;
import com.zeravorn.spell.SpellConfigLoader;
import com.zeravorn.spell.SpellService;
import com.zeravorn.team.TeamId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class JungleAndSpellTest {
    private HeroRuntime hero() { return new HeroRuntime(UUID.randomUUID(), TeamId.BLUE, HeroDefinitions.all().getFirst()); }
    @Test void campSpawnsResetsOnLeashAndRespawns() {
        JungleMobType type = JungleConfigLoader.loadDefaultDefinitions().get("GREEN_A");
        JungleCamp camp = new JungleCamp("BLUE_GREEN_A", type, new Position(0, 64, 0));
        camp.tick(699, true, JungleService.FIRST_SPAWN_TICK); assertEquals(JungleCampState.WAITING, camp.state());
        camp.tick(700, true, JungleService.FIRST_SPAWN_TICK); assertEquals(JungleCampState.ALIVE, camp.state());
        camp.damage(100, 701); camp.beginLeashReturn(); camp.completeLeashReturn(); assertEquals(type.health(), camp.health());
        camp.damage(type.health(), 702); assertEquals(JungleCampState.DEAD_WAIT_RESPAWN, camp.state());
        camp.tick(702 + type.respawnSeconds() * 20L, true, JungleService.FIRST_SPAWN_TICK); assertEquals(JungleCampState.ALIVE, camp.state());
        assertEquals(1120, camp.scaledMaxHealth(6L * 180L * 20L));
    }
    @Test void buffsExpireAndAreRemovedOnDeath() {
        HeroRuntime hero = hero(); BuffService buffs = new BuffService(); JungleCamp camp = new JungleCamp("RED", JungleConfigLoader.loadDefaultDefinitions().get("RED"), new Position(0, 0, 0)); camp.spawn(); camp.damage(1000, 10);
        assertTrue(new JungleService(new GoldService(), new ExperienceService(), buffs).kill(hero, camp, 10, 20, 1.10, 1.15, .10));
        assertEquals(1.10, buffs.outgoingHeroDamageMultiplier(hero.owner(), 11)); buffs.onDeath(hero.owner()); assertEquals(1.0, buffs.outgoingHeroDamageMultiplier(hero.owner(), 11));
    }
    @Test void validatesRetributionFlashBoundaryAndCleanse() {
        HeroRuntime hero = hero(); CrowdControlService cc = new CrowdControlService(); SpellService spells = new SpellService(SpellConfigLoader.loadDefaults(), new CooldownService(), cc);
        JungleCamp camp = new JungleCamp("RED", JungleConfigLoader.loadDefaultDefinitions().get("RED"), new Position(0, 0, 0)); camp.spawn();
        assertFalse(spells.retribution(hero, MatchState.PLAYING, false, camp, 1).accepted());
        assertTrue(spells.retribution(hero, MatchState.PLAYING, true, camp, 1).accepted());
        assertFalse(spells.flash(hero, MatchState.PLAYING, new Position(0, 0, 0), new Position(6, 0, 0), p -> true, 2).accepted());
        assertFalse(spells.flash(hero, MatchState.PLAYING, new Position(0, 0, 0), new Position(4, 0, 0), p -> false, 2).accepted());
        cc.apply(hero.owner(), new CrowdControlEffect(UUID.randomUUID(), CrowdControlType.STUN, 0, 100, true));
        assertTrue(spells.cleanse(hero, MatchState.PLAYING, 3).accepted()); assertTrue(cc.effects(hero.owner()).isEmpty());
        hero.receiveDamage(500);
        assertEquals(312, spells.regeneration(hero, MatchState.PLAYING, 4).amount());
    }
}
