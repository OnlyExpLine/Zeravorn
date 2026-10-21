package com.zeravorn.bot;

import com.zeravorn.hero.HeroDefinitions;
import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.map.Position;
import com.zeravorn.minion.Lane;
import com.zeravorn.team.TeamId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BotCoreTest {
    private static final Position ORIGIN = new Position(0, 0, 0);
    private static final BotRouteProfile ROUTE = new BotRouteProfile(Lane.MID,
            List.of(new Position(-10, 0, 0), new Position(10, 0, 0)));

    private BotPerception view(int health, boolean wave, List<BotPerception.Target> targets) {
        return new BotPerception(true, health, 100, ORIGIN, false, true, wave, targets);
    }

    @Test void lowHealthRetreatsBeforeFighting() {
        var target = new BotPerception.Target(UUID.randomUUID(), BotPerception.Kind.HERO, ORIGIN, true);
        assertEquals(BotState.RECALL, new BotBrain().decide(view(29, true, List.of(target)), ROUTE, 0).state());
        assertEquals(BotState.FIGHT, new BotBrain().decide(view(30, true, List.of(target)), ROUTE, 0).state());
    }

    @Test void ignoresInvisibleAndIllegalTargetsAndNeverDivesWithoutWave() {
        var illegal = new BotPerception.Target(UUID.randomUUID(), BotPerception.Kind.HERO, ORIGIN, false);
        var tower = new BotPerception.Target(UUID.randomUUID(), BotPerception.Kind.TOWER, ORIGIN, true);
        assertEquals(BotState.MOVE_ROUTE,
                new BotBrain().decide(view(100, false, List.of(illegal, tower)), ROUTE, 0).state());
        assertEquals(BotState.ATTACK_STRUCTURE,
                new BotBrain().decide(view(100, true, List.of(tower)), ROUTE, 0).state());
        assertEquals(BotState.MOVE_ROUTE, new BotBrain().decide(view(100, true, List.of()), ROUTE, 0).state());
    }

    @Test void plannerFillsOnlyVacanciesWithoutDuplicatingAllyHeroes() {
        var planner = new BotRosterPlanner();
        UUID match = UUID.randomUUID();
        var blue = planner.fill(match, TeamId.BLUE, List.of("amelia"), 1);
        assertEquals(4, blue.size());
        assertTrue(blue.stream().noneMatch(slot -> slot.heroId().equals("amelia")));
        assertEquals(4, blue.stream().map(BotRosterPlanner.Slot::heroId).distinct().count());
        assertEquals(blue, planner.fill(match, TeamId.BLUE, List.of("amelia"), 1));
        assertTrue(planner.fill(match, TeamId.RED, List.of(), 5).isEmpty());
    }

    @Test void controllerLimitsDecisionRate() {
        var calls = new AtomicInteger();
        var hero = new HeroRuntime(UUID.randomUUID(), TeamId.BLUE, HeroDefinitions.JASON);
        var controller = new BotController(hero, ROUTE, BotBuildProfile.forClass(hero.definition().heroClass()),
                new BotBrain(), (h, decision, build) -> calls.incrementAndGet());
        for (int tick = 0; tick < 20; tick++)
            controller.tick(tick, view(100, false, List.of()));
        assertEquals(4, calls.get());
    }

    @Test void fountainDecisionBuysAndLeavesAlongFirstWaypoint() {
        var perception = new BotPerception(true, 100, 100, ORIGIN, true, true,
                false, List.of());
        var decision = new BotBrain().decide(perception, ROUTE, 0);
        assertEquals(BotState.SPAWN_BUY, decision.state());
        assertEquals(ROUTE.waypoints().getFirst(), decision.waypoint());
    }

    @Test void fleetDoesNotDecideBeforePlaying() {
        MatchSessionBuilder fixture = new MatchSessionBuilder();
        var calls = new AtomicInteger();
        var fleet = new BotFleet(fixture.match, ignored -> view(100, false, List.of()),
                (hero, decision, build) -> calls.incrementAndGet());
        fleet.register(fixture.hero, ROUTE);
        fleet.tick(0);
        assertEquals(0, calls.get());
        fixture.match.transitionTo(com.zeravorn.match.MatchState.HERO_SELECT);
        fixture.match.transitionTo(com.zeravorn.match.MatchState.LOADING);
        fixture.match.transitionTo(com.zeravorn.match.MatchState.COUNTDOWN);
        fixture.match.transitionTo(com.zeravorn.match.MatchState.PLAYING);
        fleet.tick(1);
        assertEquals(1, calls.get());
    }

    private static final class MatchSessionBuilder {
        final com.zeravorn.match.MatchSession match = new com.zeravorn.match.MatchSession(UUID.randomUUID());
        final HeroRuntime hero = new HeroRuntime(UUID.randomUUID(), TeamId.BLUE, HeroDefinitions.JASON);
        MatchSessionBuilder() {
            match.teams().add(TeamId.BLUE,
                    new com.zeravorn.team.MatchParticipant(hero.owner(), "Bot", true));
        }
    }
}
