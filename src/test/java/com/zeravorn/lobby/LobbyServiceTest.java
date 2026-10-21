package com.zeravorn.lobby;

import com.zeravorn.match.MatchSession;
import com.zeravorn.match.MatchState;
import com.zeravorn.network.ClientRequest;
import com.zeravorn.network.RequestError;
import com.zeravorn.spell.SummonerSpell;
import com.zeravorn.team.TeamId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LobbyServiceTest {
    private static ClientRequest.Lobby choice(ClientRequest.LobbyAction action, TeamId team,
                                               String hero, SummonerSpell spell,
                                               Boolean ready, Boolean jungler) {
        return new ClientRequest.Lobby(action, team, hero, spell, ready, jungler);
    }

    @Test void assignsFreeHeroAtLockWhenReadyWithoutHero() {
        var lobby = new LobbyService(new MatchSession(UUID.randomUUID()));
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        assertTrue(lobby.join(first, "first"));
        assertTrue(lobby.join(second, "second"));
        assertNull(lobby.apply(first, choice(ClientRequest.LobbyAction.SELECT_TEAM, TeamId.BLUE, null, null, null, null)));
        assertNull(lobby.apply(second, choice(ClientRequest.LobbyAction.SELECT_TEAM, TeamId.BLUE, null, null, null, null)));
        assertNull(lobby.apply(first, choice(ClientRequest.LobbyAction.SELECT_HERO, null, "jason", null, null, null)));
        assertEquals(RequestError.UNAVAILABLE_FOR_TEAM, lobby.apply(second,
                choice(ClientRequest.LobbyAction.SELECT_HERO, null, "jason", null, null, null)));
        assertNull(lobby.apply(first, choice(ClientRequest.LobbyAction.SELECT_SPELL, null, null, SummonerSpell.FLASH, null, null)));
        assertNull(lobby.apply(second, choice(ClientRequest.LobbyAction.SELECT_SPELL, null, null, SummonerSpell.CLEANSE, null, null)));
        assertNull(lobby.apply(first, choice(ClientRequest.LobbyAction.SET_READY, null, null, null, true, null)));
        assertEquals(MatchState.HERO_SELECT, lobby.match().state());
        assertNull(lobby.apply(second, choice(ClientRequest.LobbyAction.SET_READY, null, null, null, true, null)));
        assertEquals(MatchState.LOADING, lobby.match().state());
        var players = lobby.snapshot().players();
        assertNotEquals(players.get(0).heroId(), players.get(1).heroId());
        assertTrue(lobby.snapshot().locked());
        assertEquals(10, lobby.match().teams().roster(TeamId.BLUE).size()
                + lobby.match().teams().roster(TeamId.RED).size());
        assertEquals(8, lobby.botSlots().size());
        assertEquals(0, lobby.snapshot().blueVacantSlots());
        assertEquals(0, lobby.snapshot().redVacantSlots());
    }

    @Test void retriRequiresUniqueJunglerAndReadyRequiresTeamAndSpell() {
        var lobby = new LobbyService(new MatchSession(UUID.randomUUID()));
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        lobby.join(first, "first");
        lobby.join(second, "second");
        assertEquals(RequestError.INCOMPLETE_SELECTION, lobby.apply(first,
                choice(ClientRequest.LobbyAction.SET_READY, null, null, null, true, null)));
        lobby.apply(first, choice(ClientRequest.LobbyAction.SELECT_TEAM, TeamId.RED, null, null, null, null));
        lobby.apply(second, choice(ClientRequest.LobbyAction.SELECT_TEAM, TeamId.RED, null, null, null, null));
        assertEquals(RequestError.JUNGLER_ONLY, lobby.apply(first,
                choice(ClientRequest.LobbyAction.SELECT_SPELL, null, null, SummonerSpell.RETRIBUTION, null, null)));
        assertNull(lobby.apply(first, choice(ClientRequest.LobbyAction.SET_JUNGLER, null, null, null, null, true)));
        assertEquals(RequestError.JUNGLER_TAKEN, lobby.apply(second,
                choice(ClientRequest.LobbyAction.SET_JUNGLER, null, null, null, null, true)));
        assertNull(lobby.apply(first, choice(ClientRequest.LobbyAction.SELECT_SPELL, null, null, SummonerSpell.RETRIBUTION, null, null)));
        assertNull(lobby.apply(first, choice(ClientRequest.LobbyAction.SET_JUNGLER, null, null, null, null, false)));
        assertNull(lobby.snapshot().players().stream().filter(p -> p.playerId().equals(first)).findFirst().orElseThrow().spell());
    }

    @Test void disconnectReleasesSlotBeforeLock() {
        var lobby = new LobbyService(new MatchSession(UUID.randomUUID()));
        UUID player = UUID.randomUUID();
        lobby.join(player, "player");
        lobby.apply(player, choice(ClientRequest.LobbyAction.SELECT_TEAM, TeamId.BLUE, null, null, null, null));
        lobby.disconnect(player);
        assertEquals(5, lobby.snapshot().blueVacantSlots());
        assertTrue(lobby.snapshot().players().isEmpty());
    }

    @Test void reconnectToLockedSelectionKeepsOriginalHeroAndRejectsNewPlayers() {
        var lobby = new LobbyService(new MatchSession(UUID.randomUUID()));
        UUID player = UUID.randomUUID();
        lobby.join(player, "player");
        lobby.apply(player, choice(ClientRequest.LobbyAction.SELECT_TEAM, TeamId.BLUE, null, null, null, null));
        lobby.apply(player, choice(ClientRequest.LobbyAction.SELECT_SPELL, null, null, SummonerSpell.FLASH, null, null));
        lobby.apply(player, choice(ClientRequest.LobbyAction.SET_READY, null, null, null, true, null));
        String hero = lobby.snapshot().players().getFirst().heroId();
        lobby.disconnect(player);
        assertTrue(lobby.join(player, "renamed"));
        assertEquals(hero, lobby.snapshot().players().getFirst().heroId());
        assertFalse(lobby.join(UUID.randomUUID(), "late"));
        assertEquals(RequestError.WRONG_STATE, lobby.apply(player,
                choice(ClientRequest.LobbyAction.SELECT_HERO, null, "amelia", null, null, null)));
    }
}
