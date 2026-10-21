package com.zeravorn.bot;

import com.zeravorn.hero.HeroRuntime;

/** M13 binds movement and combat to the same authoritative services as player requests. */
public interface BotActionExecutor {
    void execute(HeroRuntime hero, BotDecision decision, BotBuildProfile build);
}
