package com.zeravorn.team;

import java.util.Objects;
import java.util.UUID;

public record MatchParticipant(UUID playerId, String nickname, boolean bot) {
	public MatchParticipant {
		Objects.requireNonNull(playerId, "playerId");
		if (nickname == null || nickname.isBlank()) {
			throw new IllegalArgumentException("nickname must not be blank");
		}
	}
}
