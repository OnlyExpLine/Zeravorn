package com.zeravorn.lobby;

import com.zeravorn.spell.SummonerSpell;
import com.zeravorn.team.TeamId;

import java.util.Objects;
import java.util.UUID;

/** Server-confirmed selection; nullable team/hero/spell mean not selected. */
public record LobbyPlayer(UUID playerId, String nickname, TeamId team, String heroId,
                          SummonerSpell spell, boolean jungler, boolean ready) {
    public LobbyPlayer {
        Objects.requireNonNull(playerId, "playerId");
        if (nickname == null || nickname.isBlank()) throw new IllegalArgumentException("nickname");
    }
}
