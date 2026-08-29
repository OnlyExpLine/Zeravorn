package com.zeravorn.ability;

import com.zeravorn.hero.HeroRuntime;

public interface AbilityExecution {
    AbilityExecutionResult execute(HeroRuntime caster, AbilityContext context);
}
