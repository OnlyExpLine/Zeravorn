package com.zeravorn.network;

import com.zeravorn.ability.AbilityLevelService;
import com.zeravorn.hero.HeroDefinitions;
import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.item.ItemCatalog;
import com.zeravorn.item.ShopService;
import com.zeravorn.match.MatchSession;
import com.zeravorn.match.MatchState;
import com.zeravorn.spell.SummonerSpell;
import com.zeravorn.team.MatchParticipant;
import com.zeravorn.team.TeamId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ServerRequestProcessorTest {
    private static RequestValidationContext context() {
        UUID player = UUID.randomUUID();
        var match = new MatchSession(UUID.randomUUID());
        match.teams().add(TeamId.BLUE, new MatchParticipant(player, "Player", false));
        match.transitionTo(MatchState.HERO_SELECT);
        match.transitionTo(MatchState.LOADING);
        match.transitionTo(MatchState.COUNTDOWN);
        match.transitionTo(MatchState.PLAYING);
        return new RequestValidationContext(match, player,
                new HeroRuntime(player, TeamId.BLUE, HeroDefinitions.JASON), SummonerSpell.FLASH, false);
    }

    private static ServerRequestProcessor processor() {
        return new ServerRequestProcessor(new RequestSequenceGuard(), new GameplayRequestValidator(),
                new AbilityLevelService(), new ShopService(new ItemCatalog()),
                GameplayActionAdapter.unavailable());
    }

    @Test
    void duplicateUpgradeCannotSpendSecondSkillPoint() {
        var context = context();
        var processor = processor();
        var request = new RequestEnvelope(1, 1, new ClientRequest.UpgradeAbility("zeravorn:jason_q"));
        assertTrue(processor.process(context, request).success());
        assertEquals(0, context.hero().availableSkillPoints());
        assertEquals(1, context.hero().abilityRank("zeravorn:jason_q"));
        assertEquals(RequestError.DUPLICATE_SEQUENCE, processor.process(context, request).errorCode());
        assertEquals(1, context.hero().abilityRank("zeravorn:jason_q"));
    }

    @Test
    void unboundWorldActionCannotReportSuccess() {
        var context = context();
        var result = processor().process(context, new RequestEnvelope(1, 1,
                new ClientRequest.BasicAttack(new com.zeravorn.map.Position(1, 0, 0), null)));
        assertEquals(RequestError.WRONG_STATE, result.errorCode());
    }

    @Test
    void failedShopRequestDoesNotSpendGold() {
        var context = context();
        int gold = context.hero().gold();
        var result = processor().process(context, new RequestEnvelope(1, 1,
                new ClientRequest.BuyItem("iron_blade")));
        assertEquals(RequestError.NO_GOLD, result.errorCode());
        assertEquals(gold, context.hero().gold());
    }
}
