package com.zeravorn.bot;

import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.match.MatchSession;
import com.zeravorn.match.MatchState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Match-scoped scheduler. M13 supplies a team-vision-filtered source and world executor. */
public final class BotFleet {
    @FunctionalInterface
    public interface PerceptionSource {
        BotPerception perceive(HeroRuntime bot);
    }

    private final MatchSession match;
    private final PerceptionSource source;
    private final BotActionExecutor executor;
    private final Map<UUID, BotController> controllers = new LinkedHashMap<>();
    private final Map<UUID, HeroRuntime> heroes = new LinkedHashMap<>();

    public BotFleet(MatchSession match, PerceptionSource source, BotActionExecutor executor) {
        this.match = Objects.requireNonNull(match);
        this.source = Objects.requireNonNull(source);
        this.executor = Objects.requireNonNull(executor);
    }

    public void register(HeroRuntime hero, BotRouteProfile route) {
        Objects.requireNonNull(hero);
        if (match.teams().roster(hero.team()).participants().stream()
                .noneMatch(slot -> slot.playerId().equals(hero.owner()) && slot.bot()))
            throw new IllegalArgumentException("Bot is not in match roster");
        controllers.put(hero.owner(), new BotController(hero, route,
                BotBuildProfile.forClass(hero.definition().heroClass()), new BotBrain(), executor));
        heroes.put(hero.owner(), hero);
    }

    public void tick(long serverTick) {
        if (match.state() != MatchState.PLAYING) return;
        for (var entry : controllers.entrySet()) {
            HeroRuntime hero = heroes.get(entry.getKey());
            BotPerception perception = source.perceive(hero);
            if (perception != null) entry.getValue().tick(serverTick, perception);
        }
    }

    public int size() { return controllers.size(); }
}
