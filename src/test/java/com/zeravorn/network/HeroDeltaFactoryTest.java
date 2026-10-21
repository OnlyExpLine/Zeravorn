package com.zeravorn.network;

import com.zeravorn.buff.BuffService;
import com.zeravorn.combat.CooldownService;
import com.zeravorn.combat.CrowdControlService;
import com.zeravorn.hero.HeroDefinitions;
import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.team.TeamId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeroDeltaFactoryTest {
    @Test
    void publishesAbilityCooldownUnderTheHudAbilityId() {
        HeroRuntime hero = new HeroRuntime(UUID.randomUUID(), TeamId.BLUE, HeroDefinitions.AMELIA);
        CooldownService cooldowns = new CooldownService();
        cooldowns.tryStart(hero.owner(), "amelia_q", 100, 160);
        var delta = HeroDeltaFactory.ownHero(hero, cooldowns, 140, 0, 0, 0,
                null, new BuffService(), new CrowdControlService());
        assertEquals(120L, delta.cooldownTicks().get("zeravorn:amelia_q"));
    }
}
