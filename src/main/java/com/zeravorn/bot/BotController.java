package com.zeravorn.bot;

import com.zeravorn.hero.HeroRuntime;

import java.util.Objects;

/** Five ticks per decision = four decisions per second on a 20 TPS server. */
public final class BotController {
    public static final int DECISION_INTERVAL_TICKS = 5;
    private final HeroRuntime hero;
    private final BotRouteProfile route;
    private final BotBuildProfile build;
    private final BotBrain brain;
    private final BotActionExecutor executor;
    private int waypointIndex;
    private long lastDecisionTick = Long.MIN_VALUE;

    public BotController(HeroRuntime hero, BotRouteProfile route, BotBuildProfile build,
                         BotBrain brain, BotActionExecutor executor) {
        this.hero = Objects.requireNonNull(hero);
        this.route = Objects.requireNonNull(route);
        this.build = Objects.requireNonNull(build);
        this.brain = Objects.requireNonNull(brain);
        this.executor = Objects.requireNonNull(executor);
    }

    public boolean tick(long serverTick, BotPerception perception) {
        if (lastDecisionTick != Long.MIN_VALUE && serverTick - lastDecisionTick < DECISION_INTERVAL_TICKS)
            return false;
        lastDecisionTick = serverTick;
        if (perception.position().distanceSquared(route.waypoints().get(waypointIndex)) <= 2.25
                && waypointIndex < route.waypoints().size() - 1) waypointIndex++;
        executor.execute(hero, brain.decide(perception, route, waypointIndex), build);
        return true;
    }
}
