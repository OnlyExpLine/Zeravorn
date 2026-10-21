package com.zeravorn.bot;

import com.zeravorn.ability.AbilityLevelService;
import com.zeravorn.ability.AbilitySlot;
import com.zeravorn.hero.HeroDefinitions;
import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.item.ItemCatalog;
import com.zeravorn.item.ShopService;
import com.zeravorn.map.Position;
import com.zeravorn.match.MatchSession;
import com.zeravorn.match.MatchState;
import com.zeravorn.network.ClientRequest;
import com.zeravorn.network.GameplayRequestValidator;
import com.zeravorn.network.RequestSequenceGuard;
import com.zeravorn.network.ServerRequestProcessor;
import com.zeravorn.team.MatchParticipant;
import com.zeravorn.team.TeamId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ServerBotActionExecutorTest {
    @Test void botUsesThePlayerRequestProcessorAndRechecksVisibility() {
        UUID botId = UUID.randomUUID(), targetId = UUID.randomUUID();
        MatchSession match = new MatchSession(UUID.randomUUID());
        match.teams().add(TeamId.BLUE, new MatchParticipant(botId, "Bot", true));
        match.transitionTo(MatchState.HERO_SELECT);
        match.transitionTo(MatchState.LOADING);
        match.transitionTo(MatchState.COUNTDOWN);
        match.transitionTo(MatchState.PLAYING);
        HeroRuntime hero = new HeroRuntime(botId, TeamId.BLUE, HeroDefinitions.JASON);
        List<ClientRequest> executed = new ArrayList<>();
        var processor = new ServerRequestProcessor(new RequestSequenceGuard(),
                new GameplayRequestValidator(), new AbilityLevelService(),
                new ShopService(new ItemCatalog()), (context, request) -> {
                    executed.add(request);
                    return null;
                });
        class World implements ServerBotActionExecutor.WorldPort {
            boolean visible;
            @Override public Position position(UUID id) { return new Position(0, 0, 0); }
            @Override public Position visibleLegalTarget(UUID id, UUID target) {
                return visible ? new Position(2, 0, 0) : null;
            }
            @Override public boolean canCast(HeroRuntime bot, AbilitySlot slot, UUID target) {
                return slot == AbilitySlot.Q;
            }
            @Override public void moveToward(UUID id, Position waypoint) { }
            @Override public Position safeRetreat(UUID id) { return new Position(-10, 0, 0); }
        }
        var world = new World();
        var executor = new ServerBotActionExecutor(match, processor, world);
        executor.execute(hero, new BotDecision(BotState.SPAWN_BUY, null, null),
                BotBuildProfile.forClass(hero.definition().heroClass()));
        assertEquals(1, hero.abilityRank(AbilitySlot.Q));
        executor.execute(hero, new BotDecision(BotState.FIGHT, targetId, null),
                BotBuildProfile.forClass(hero.definition().heroClass()));
        assertTrue(executed.isEmpty());
        world.visible = true;
        executor.execute(hero, new BotDecision(BotState.FIGHT, targetId, null),
                BotBuildProfile.forClass(hero.definition().heroClass()));
        assertEquals(2, executed.size());
        assertInstanceOf(ClientRequest.CastAbility.class, executed.getFirst());
        assertEquals("zeravorn:jason_q", ((ClientRequest.CastAbility) executed.getFirst()).abilityId());
        assertInstanceOf(ClientRequest.BasicAttack.class, executed.getLast());
    }
}
