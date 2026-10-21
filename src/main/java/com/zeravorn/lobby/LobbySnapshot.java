package com.zeravorn.lobby;

import com.zeravorn.match.MatchState;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Full public lobby state, safe to send to every participant. */
public record LobbySnapshot(UUID matchId, MatchState phase, boolean locked,
                            int blueVacantSlots, int redVacantSlots,
                            int countdownTicks, List<LobbyPlayer> players) {
    public LobbySnapshot {
        Objects.requireNonNull(matchId, "matchId");
        Objects.requireNonNull(phase, "phase");
        players = List.copyOf(players);
        if (blueVacantSlots < 0 || redVacantSlots < 0 || countdownTicks < 0)
            throw new IllegalArgumentException("Negative lobby counter");
    }
}
