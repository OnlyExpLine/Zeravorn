package com.zeravorn.ability;

import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.combat.CrowdControlService;

public final class AbilityCastValidator {
    private final CrowdControlService crowdControl;
    public AbilityCastValidator() { this(null); }
    public AbilityCastValidator(CrowdControlService crowdControl) { this.crowdControl = crowdControl; }
    public String validate(HeroRuntime caster, AbilitySlot slot, AbilityContext context) {
        if (!context.matchPlaying()) return "WRONG_STATE";
        if (!caster.alive()) return "DEAD";
        if (crowdControl != null && crowdControl.blocksBasicOrAbility(caster.owner())) return "STUNNED";
        AbilityRuntime ability = caster.abilityRuntime(slot);
        if (ability == null) return "ABILITY_NOT_AVAILABLE";
        if (!ability.learned()) return "ABILITY_NOT_LEARNED";
        return "";
    }

    public boolean canCast(HeroRuntime caster, AbilitySlot slot, AbilityContext context) {
        return validate(caster, slot, context).isEmpty();
    }
}
