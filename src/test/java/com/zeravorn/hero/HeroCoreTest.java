package com.zeravorn.hero;

import com.zeravorn.ability.AbilityLevelService;
import com.zeravorn.ability.AbilitySlot;
import com.zeravorn.match.MatchSession;
import com.zeravorn.match.MatchState;
import com.zeravorn.team.TeamId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeroCoreTest {
    @Test
    void definitionsContainAllFiveHeroesAndDocumentedCaps() {
        assertEquals(5, HeroDefinitions.all().size());
        assertEquals(4, HeroDefinitions.JASON.ability(AbilitySlot.Q).maxRank());
        assertEquals(2, HeroDefinitions.JASON.ability(AbilitySlot.R).maxRank());
        assertNull(HeroDefinitions.JASON.ability(AbilitySlot.F));
		assertEquals(2, HeroDefinitions.SHELIANER.ability(AbilitySlot.F).maxRank());
		assertEquals(HeroClass.ASSASSIN, HeroDefinitions.SHELIANER.heroClass());
		assertEquals(HeroClass.MARKSMAN, HeroDefinitions.ESAKI.heroClass());
        assertEquals(4, HeroDefinitions.SHELIANER.ability(AbilitySlot.R).unlockLevel());
        assertEquals(0, HeroDefinitions.JASON.statsAt(1).maxMana());
        assertEquals(625, HeroDefinitions.SHELIANER.statsAt(10).maxMana());
        assertEquals(135, HeroDefinitions.ESAKI.statsAt(10).attack());
	}

	@Test
	void progressionUsesEveryDocumentedXpThresholdAndEffectiveStats() {
		HeroRuntime runtime = runtime(HeroDefinitions.AMELIA);
		LevelService levels = new LevelService();
		int[] thresholds = {0, 180, 450, 850, 1300, 1800, 2350, 2950, 3600, 4300};
		for (int index = 0; index < thresholds.length; index++) {
			levels.addExperience(runtime, index == 0 ? 0 : thresholds[index] - runtime.experience());
			assertEquals(index + 1, runtime.level());
			assertEquals(thresholds[index], runtime.experience());
		}
		assertEquals(1260, runtime.effectiveStats().maxHealth());
		assertEquals(820, runtime.effectiveStats().maxMana());
		assertEquals(1.0, runtime.effectiveStats().attackInterval());
	}

    @Test
    void runtimeStartsWithConfiguredResourcesAndLevelOnePoint() {
        HeroRuntime runtime = runtime(HeroDefinitions.LOKI);
        assertEquals(1, runtime.level());
        assertEquals(0, runtime.experience());
        assertEquals(500, runtime.gold());
        assertEquals(1, runtime.availableSkillPoints());
        assertEquals(1600, runtime.health());
        assertEquals(400, runtime.mana());
		assertTrue(runtime.abilityRanks().containsKey("zeravorn:loki_q"));
    }

    @Test
    void experienceLevelsUpAndGrantsOnePointPerLevel() {
        HeroRuntime runtime = runtime(HeroDefinitions.JASON);
        LevelService levels = new LevelService();
        assertEquals(3, levels.addExperience(runtime, 850));
        assertEquals(4, runtime.level());
        assertEquals(4, runtime.availableSkillPoints());
        assertEquals(850, runtime.experience());
        assertEquals(6, levels.addExperience(runtime, 99999));
        assertEquals(10, runtime.level());
        assertEquals(10, runtime.availableSkillPoints());
        assertEquals(4300, runtime.experience());
    }

    @Test
    void abilityUpgradeValidatesUnlockCapsAndHeroSpecificF() {
        AbilityLevelService abilities = new AbilityLevelService();
        HeroRuntime jason = runtime(HeroDefinitions.JASON);
        assertFalse(abilities.tryUpgrade(playingSession(), jason, AbilitySlot.R).upgraded());
        assertFalse(abilities.tryUpgrade(playingSession(), jason, AbilitySlot.F).upgraded());
        assertTrue(abilities.tryUpgrade(playingSession(), jason, AbilitySlot.Q).upgraded());
		assertEquals(1, jason.abilityRank("zeravorn:jason_q"));
        new LevelService().addExperience(jason, HeroProgressionConfig.xpToLevel(2));
        jason.setAlive(false);
        assertEquals(1, jason.abilityRank(AbilitySlot.Q));
		assertTrue(abilities.tryUpgrade(playingSession(), jason, AbilitySlot.E).upgraded());

        HeroRuntime shelianer = runtime(HeroDefinitions.SHELIANER);
        assertTrue(abilities.tryUpgrade(playingSession(), shelianer, AbilitySlot.F).upgraded());
        assertEquals(0, abilities.tryUpgrade(playingSession(), shelianer, AbilitySlot.R).newRank());
    }

    @Test
	void skillPointIsNotSpentWhenUpgradeIsRejected() {
        HeroRuntime runtime = runtime(HeroDefinitions.JASON);
        AbilityLevelService abilities = new AbilityLevelService();
        assertFalse(abilities.tryUpgrade(playingSession(), runtime, AbilitySlot.R).upgraded());
        assertEquals(1, runtime.availableSkillPoints());
	}

	@Test
	void abilityRanksCannotExceedConfiguredCaps() {
		HeroRuntime jason = runtime(HeroDefinitions.JASON);
		LevelService levels = new LevelService();
		AbilityLevelService abilities = new AbilityLevelService();
		MatchSession session = playingSession();
		levels.addExperience(jason, HeroProgressionConfig.maxXp());
		for (int rank = 0; rank < 4; rank++) assertTrue(abilities.tryUpgrade(session, jason, AbilitySlot.Q).upgraded());
		assertFalse(abilities.tryUpgrade(session, jason, AbilitySlot.Q).upgraded());
		assertEquals(4, jason.abilityRank(AbilitySlot.Q));
		assertTrue(abilities.tryUpgrade(session, jason, AbilitySlot.R).upgraded());
		assertTrue(abilities.tryUpgrade(session, jason, AbilitySlot.R).upgraded());
		assertFalse(abilities.tryUpgrade(session, jason, AbilitySlot.R).upgraded());
	}

    private static HeroRuntime runtime(HeroDefinition definition) {
        return new HeroRuntime(UUID.randomUUID(), TeamId.BLUE, definition);
    }

	private static MatchSession playingSession() {
		MatchSession session = new MatchSession(UUID.randomUUID());
		session.transitionTo(MatchState.HERO_SELECT);
		session.transitionTo(MatchState.LOADING);
		session.transitionTo(MatchState.COUNTDOWN);
		session.transitionTo(MatchState.PLAYING);
		return session;
	}
}
