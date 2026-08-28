package com.zeravorn.team;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class TeamRoster {
	public static final int MAX_SLOTS = 5;

	private final TeamId team;
	private final List<MatchParticipant> participants = new ArrayList<>(MAX_SLOTS);

	public TeamRoster(TeamId team) {
		this.team = Objects.requireNonNull(team, "team");
	}

	public TeamId team() {
		return team;
	}

	public boolean add(MatchParticipant participant) {
		Objects.requireNonNull(participant, "participant");
		if (isFull() || contains(participant.playerId())) {
			return false;
		}
		participants.add(participant);
		return true;
	}

	public boolean remove(UUID playerId) {
		return participants.removeIf(participant -> participant.playerId().equals(playerId));
	}

	public boolean contains(UUID playerId) {
		return participants.stream().anyMatch(participant -> participant.playerId().equals(playerId));
	}

	public boolean isFull() {
		return participants.size() == MAX_SLOTS;
	}

	public int size() {
		return participants.size();
	}

	public List<MatchParticipant> participants() {
		return List.copyOf(participants);
	}
}
