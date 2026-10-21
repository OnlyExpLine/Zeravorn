package com.zeravorn.network;

import com.zeravorn.map.Position;
import com.zeravorn.match.MatchState;
import com.zeravorn.lobby.LobbyPlayer;
import com.zeravorn.lobby.LobbySnapshot;
import com.zeravorn.spell.SummonerSpell;
import com.zeravorn.team.TeamId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NetworkWireCodecTest {
    @Test
    void requestRoundTripKeepsVersionSequenceAndPayload() {
        var source = new RequestEnvelope(1, 42, new ClientRequest.CastAbility("amelia_r", null,
                new Position(1, 0, 0), new Position(3, 64, 5)));
        assertEquals(source, NetworkWireCodec.decodeRequest(NetworkWireCodec.encodeRequest(source)));
    }

    @Test
    void messageRoundTripKeepsFilteredPositions() {
        UUID visible = UUID.randomUUID();
        var message = new ServerMessage.MinimapDeltaMessage(TeamId.BLUE,
                Map.of(visible, new Position(2, 64, 3)));
        assertEquals(message, NetworkWireCodec.decodeMessage(NetworkWireCodec.encodeMessage(message)));
    }

    @Test
    void unknownMessageKindsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> NetworkWireCodec.decodeRequest("{\"kind\":\"java.lang.Runtime\",\"payload\":{}}"));
        assertThrows(IllegalArgumentException.class,
                () -> NetworkWireCodec.decodeMessage("{\"kind\":\"unknown\",\"payload\":{}}"));
    }

    @Test
    void snapshotReplacementRemovesHiddenEnemyAndResyncsSequence() {
        var state = new ClientReplicatedState();
        UUID owner = UUID.randomUUID(), enemy = UUID.randomUUID(), match = UUID.randomUUID();
        var hero = new ServerMessage.HeroDelta(owner, "jason", TeamId.BLUE, true, 100, 100,
                0, 0, 1, 0, 500, 1, 0, 0, 0, Map.of("jason_q", 0), Map.of(),
                List.of(), null, null, null, 0, Map.of(), Map.of());
        var matchState = new ServerMessage.MatchStateEvent(MatchState.PLAYING, 25, null);
        var scoreboard = new ServerMessage.ScoreboardMessage(0, 0, List.of());
        state.apply(new ServerMessage.Snapshot(match, 12, hero, matchState, List.of(),
                Set.of(owner, enemy), Map.of(owner, new Position(0, 0, 0),
                enemy, new Position(4, 0, 4)), scoreboard));
        assertEquals(12, state.nextSequence());
        state.apply(new ServerMessage.VisionDeltaMessage(TeamId.BLUE, Set.of(), Set.of(enemy)));
        assertFalse(state.minimapPositions().containsKey(enemy));
        state.apply(new ServerMessage.Snapshot(match, 13, hero, matchState, List.of(),
                Set.of(owner), Map.of(owner, new Position(0, 0, 0)), scoreboard));
        assertEquals(13, state.nextSequence());
        assertFalse(state.visibleIds().contains(enemy));
        assertFalse(state.minimapPositions().containsKey(enemy));
    }

    @Test
    void lobbySequenceFloorPreventsReusingOldRequestIds() {
        var state = new ClientReplicatedState();
        state.apply(NetworkWireCodec.decodeMessage(NetworkWireCodec.encodeMessage(
                new ServerMessage.SequenceFloor(37))));
        assertEquals(37, state.nextSequence());
    }

    @Test
    void lobbyStateSurvivesSameMatchLoadingSnapshotUntilPlaying() {
        UUID match = UUID.randomUUID(), owner = UUID.randomUUID();
        var lobby = new LobbySnapshot(match, MatchState.LOADING, true, 4, 5, 0,
                List.of(new LobbyPlayer(owner, "owner", TeamId.BLUE, "jason",
                        SummonerSpell.FLASH, false, true)));
        var state = new ClientReplicatedState();
        state.apply(NetworkWireCodec.decodeMessage(NetworkWireCodec.encodeMessage(
                new ServerMessage.LobbyStateMessage(lobby, 4))));
        assertEquals(lobby, state.lobby());
        state.apply(new ServerMessage.Snapshot(match, 5, null,
                new ServerMessage.MatchStateEvent(MatchState.LOADING, 0, null), List.of(),
                Set.of(), Map.of(), new ServerMessage.ScoreboardMessage(0, 0, List.of())));
        assertEquals(lobby, state.lobby());
        state.apply(new ServerMessage.MatchStateEvent(MatchState.PLAYING, 0, null));
        assertNull(state.lobby());
    }
}
