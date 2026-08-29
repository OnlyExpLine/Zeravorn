package com.zeravorn.ability;

import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.match.MatchSession;
import com.zeravorn.match.MatchState;

public final class AbilityLevelService {
    public AbilityUpgradeResult tryUpgrade(MatchSession session, HeroRuntime runtime, AbilitySlot slot) {
        AbilityDefinition ability = runtime.definition().ability(slot);
        if (ability == null) return AbilityUpgradeResult.rejected("ABILITY_NOT_AVAILABLE", 0);
        return tryUpgrade(session, runtime, ability.id());
    }

    public AbilityUpgradeResult tryUpgrade(MatchSession session, HeroRuntime runtime, String abilityId) {
        int currentRank = runtime.abilityRank(abilityId);
        if (session.state() != MatchState.PLAYING) return AbilityUpgradeResult.rejected("WRONG_STATE", currentRank);
        AbilityDefinition ability = runtime.definition().abilityById(abilityId);
        if (ability == null) return AbilityUpgradeResult.rejected("ABILITY_NOT_AVAILABLE", currentRank);
        if (runtime.level() < ability.unlockLevel()) return AbilityUpgradeResult.rejected("LEVEL_REQUIREMENT", currentRank);
        if (currentRank >= ability.maxRank()) return AbilityUpgradeResult.rejected("MAX_RANK", currentRank);
        if (runtime.availableSkillPoints() <= 0) return AbilityUpgradeResult.rejected("NO_SKILL_POINT", currentRank);
		runtime.upgradeAbility(abilityId);
		return AbilityUpgradeResult.success(currentRank + 1);
    }
}
