package com.zeravorn.ability;

import com.zeravorn.hero.HeroRuntime;

public final class AbilityCastValidator {
    public String validate(HeroRuntime caster, AbilitySlot slot, AbilityContext context) {
        if (!context.matchPlaying()) return "WRONG_STATE";
        if (!caster.alive()) return "DEAD";
        AbilityRuntime ability = caster.abilityRuntime(slot);
        if (ability == null) return "ABILITY_NOT_AVAILABLE";
        if (!ability.learned()) return "ABILITY_NOT_LEARNED";
        return "";
    }

    public boolean canCast(HeroRuntime caster, AbilitySlot slot, AbilityContext context) {
        return validate(caster, slot, context).isEmpty();
    }
}
