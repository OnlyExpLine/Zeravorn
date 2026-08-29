package com.zeravorn.team;

import java.util.Objects;
import java.util.UUID;

public final class TeamService {
	private final TeamRoster blue = new TeamRoster(TeamId.BLUE);
	private final TeamRoster red = new TeamRoster(TeamId.RED);

	public TeamRoster roster(TeamId team) {
		return team == TeamId.BLUE ? blue : red;
	}

	public boolean add(TeamId team, MatchParticipant participant) {
		Objects.requireNonNull(team, "team");
		Objects.requireNonNull(participant, "participant");
		if (contains(participant.playerId())) {
			return false;
		}
		return roster(team).add(participant);
	}

	public boolean remove(UUID playerId) {
		return blue.remove(playerId) || red.remove(playerId);
	}

	public boolean contains(UUID playerId) {
		return blue.contains(playerId) || red.contains(playerId);
	}

	public void reset() {
		blue.clear();
		red.clear();
	}
}
