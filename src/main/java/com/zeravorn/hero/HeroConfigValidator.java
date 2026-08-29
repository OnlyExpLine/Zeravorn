package com.zeravorn.hero;

import com.zeravorn.ability.AbilityDefinition;
import com.zeravorn.ability.AbilitySlot;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Validates the M2 constraints that must hold before a match can use definitions. */
public final class HeroConfigValidator {
	private static final Map<String, HeroClass> STARTER_ROLES = Map.of(
			"jason", HeroClass.FIGHTER,
			"shelianer", HeroClass.ASSASSIN,
			"esaki", HeroClass.MARKSMAN,
			"amelia", HeroClass.MAGE,
			"loki", HeroClass.TANK);

    private HeroConfigValidator() {
    }

    public static void validateHeroes(List<HeroDefinition> heroes) {
        if (heroes.size() != 5) {
            throw new IllegalArgumentException("Exactly five starter heroes are required");
        }
        Set<String> ids = new HashSet<>();
		Set<String> abilityIds = new HashSet<>();
        for (HeroDefinition hero : heroes) {
            if (!ids.add(hero.id())) {
                throw new IllegalArgumentException("Duplicate hero id: " + hero.id());
            }
			if (STARTER_ROLES.get(hero.id()) != hero.heroClass()) {
				throw new IllegalArgumentException("Invalid starter role for " + hero.id());
			}
            validateAbilities(hero);
			for (AbilityDefinition ability : hero.abilities().values()) {
				if (!abilityIds.add(ability.id())) {
					throw new IllegalArgumentException("Duplicate ability id: " + ability.id());
				}
			}
        }
		if (!ids.equals(STARTER_ROLES.keySet())) {
			throw new IllegalArgumentException("Starter hero definitions are incomplete");
		}
    }

    private static void validateAbilities(HeroDefinition hero) {
        boolean shelianer = hero.id().equals("shelianer");
        if (hero.abilities().containsKey(AbilitySlot.F) != shelianer) {
            throw new IllegalArgumentException("F ability is exclusive to Shelianer");
        }
        validate(hero.ability(AbilitySlot.Q), shelianer ? 3 : 4, 1, hero.id(), AbilitySlot.Q);
        validate(hero.ability(AbilitySlot.E), shelianer ? 3 : 4, 1, hero.id(), AbilitySlot.E);
        if (shelianer) {
            validate(hero.ability(AbilitySlot.F), 2, 1, hero.id(), AbilitySlot.F);
        }
        validate(hero.ability(AbilitySlot.R), 2, 4, hero.id(), AbilitySlot.R);
    }

    private static void validate(AbilityDefinition ability, int maxRank, int unlockLevel, String heroId, AbilitySlot slot) {
        if (ability == null || ability.maxRank() != maxRank || ability.unlockLevel() != unlockLevel) {
            throw new IllegalArgumentException("Invalid " + slot + " definition for " + heroId);
        }
    }
}
