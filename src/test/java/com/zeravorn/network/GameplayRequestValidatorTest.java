package com.zeravorn.network;

import com.zeravorn.hero.HeroDefinitions;
import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.map.Position;
import com.zeravorn.match.MatchSession;
import com.zeravorn.match.MatchState;
import com.zeravorn.spell.SummonerSpell;
import com.zeravorn.team.MatchParticipant;
import com.zeravorn.team.TeamId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GameplayRequestValidatorTest {
    private final GameplayRequestValidator validator = new GameplayRequestValidator();

    private static RequestValidationContext context() {
        var player = UUID.randomUUID();
        var match = new MatchSession(UUID.randomUUID());
        match.teams().add(TeamId.BLUE, new MatchParticipant(player, "Player", false));
        match.transitionTo(MatchState.HERO_SELECT);
        match.transitionTo(MatchState.LOADING);
        match.transitionTo(MatchState.COUNTDOWN);
        match.transitionTo(MatchState.PLAYING);
        var hero = new HeroRuntime(player, TeamId.BLUE, HeroDefinitions.JASON);
        return new RequestValidationContext(match, player, hero, SummonerSpell.FLASH, false);
    }

    @Test
    void rejectsSpoofedHeroAndNonRosterSender() {
        var original = context();
        var attack = new ClientRequest.BasicAttack(new Position(1, 0, 0), null);
        var spoofed = new RequestValidationContext(original.match(), original.senderId(),
                new HeroRuntime(UUID.randomUUID(), TeamId.RED, HeroDefinitions.JASON),
                SummonerSpell.FLASH, false);
        assertEquals(RequestError.INVALID_TARGET, validator.validate(spoofed, attack));
        var outsider = new RequestValidationContext(original.match(), UUID.randomUUID(), original.hero(),
                SummonerSpell.FLASH, false);
        assertEquals(RequestError.INVALID_TARGET, validator.validate(outsider, attack));
        var wrongTeam = new RequestValidationContext(original.match(), original.senderId(),
                new HeroRuntime(original.senderId(), TeamId.RED, HeroDefinitions.JASON),
                SummonerSpell.FLASH, false);
        assertEquals(RequestError.INVALID_TARGET, validator.validate(wrongTeam, attack));
    }

    @Test
    void allowsUnassignedPlayerToRequestTeamDuringLobby() {
        var match = new MatchSession(UUID.randomUUID());
        var sender = UUID.randomUUID();
        var context = new RequestValidationContext(match, sender, null, null, false);
        assertNull(validator.validate(context,
                new ClientRequest.Lobby(ClientRequest.LobbyAction.SELECT_TEAM, TeamId.BLUE,
                        null, null, null)));
        assertEquals(RequestError.INVALID_TARGET, validator.validate(context,
                new ClientRequest.BuyItem("iron_blade")));
        assertNull(validator.validate(context, new ClientRequest.Lobby(
                ClientRequest.LobbyAction.SET_JUNGLER, null, null, null, null, true)));
        assertEquals(RequestError.INVALID_TARGET, validator.validate(context, new ClientRequest.Lobby(
                ClientRequest.LobbyAction.SET_JUNGLER, null, null, null, null, null)));
    }

    @Test
    void allowsShopAndSkillUpgradeWhileDeadButNotCombat() {
        var context = context();
        context.hero().setAlive(false);
        assertNull(validator.validate(context, new ClientRequest.BuyItem("iron_blade")));
        assertNull(validator.validate(context, new ClientRequest.SellItem(0)));
        assertNull(validator.validate(context, new ClientRequest.UpgradeAbility("zeravorn:jason_q")));
        assertEquals(RequestError.DEAD, validator.validate(context,
                new ClientRequest.BasicAttack(new Position(1, 0, 0), null)));
    }

    @Test
    void rejectsMalformedDirectionsAndUnselectedSpell() {
        var context = context();
        assertEquals(RequestError.INVALID_TARGET, validator.validate(context,
                new ClientRequest.BasicAttack(new Position(Double.NaN, 0, 0), null)));
        assertEquals(RequestError.INVALID_TARGET, validator.validate(context,
                new ClientRequest.BasicAttack(new Position(0, 0, 0), null)));
        assertEquals(RequestError.INVALID_TARGET, validator.validate(context,
                new ClientRequest.UseSpell(SummonerSpell.CLEANSE, null, null, null)));
        assertEquals(RequestError.INVALID_TARGET, validator.validate(context,
                new ClientRequest.UseSpell(SummonerSpell.FLASH, null, null,
                        new Position(Double.POSITIVE_INFINITY, 0, 0))));
    }

    @Test
    void rejectsUnavailableAbilityAndGameplayOutsidePlaying() {
        var context = context();
        assertEquals(RequestError.ABILITY_NOT_AVAILABLE, validator.validate(context,
                new ClientRequest.CastAbility("jason_f", null, null, null)));
        assertEquals(RequestError.ABILITY_NOT_AVAILABLE, validator.validate(context,
                new ClientRequest.CastAbility("zeravorn:jason_q", null, null, null)));
        context.match().finish(TeamId.BLUE, com.zeravorn.match.MatchFinishReason.THRONE_DESTROYED);
        assertEquals(RequestError.WRONG_STATE, validator.validate(context,
                new ClientRequest.BuyItem("iron_blade")));
    }
}
