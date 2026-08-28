package com.zeravorn.match;

import com.zeravorn.team.TeamId;
import com.zeravorn.team.TeamService;

import java.util.Objects;
import java.util.UUID;

public final class MatchSession {
	private final UUID matchId;
	private final TeamService teams;
	private MatchState state = MatchState.LOBBY;
	private long elapsedTicks;
	private TeamId winner;
	private MatchFinishReason finishReason;

	public MatchSession(UUID matchId) {
		this.matchId = Objects.requireNonNull(matchId, "matchId");
		this.teams = new TeamService();
	}

	public UUID matchId() {
		return matchId;
	}

	public MatchState state() {
		return state;
	}

	public long elapsedTicks() {
		return elapsedTicks;
	}

	public TeamId winner() {
		return winner;
	}

	public MatchFinishReason finishReason() {
		return finishReason;
	}

	public TeamService teams() {
		return teams;
	}

	public void transitionTo(MatchState nextState) {
		Objects.requireNonNull(nextState, "nextState");
		if (!isAllowedTransition(state, nextState)) {
			throw new IllegalStateException("Invalid match transition: " + state + " -> " + nextState);
		}
		state = nextState;
	}

	public boolean finish(TeamId winningTeam, MatchFinishReason reason) {
		Objects.requireNonNull(winningTeam, "winningTeam");
		Objects.requireNonNull(reason, "reason");
		if (state == MatchState.FINISHED || state == MatchState.POST_GAME) {
			return false;
		}
		if (state != MatchState.PLAYING) {
			throw new IllegalStateException("Match can finish only while PLAYING");
		}
		winner = winningTeam;
		finishReason = reason;
		state = MatchState.FINISHED;
		return true;
	}

	public void tick() {
		if (state == MatchState.PLAYING) {
			elapsedTicks++;
		}
	}

	public void reset() {
		if (state != MatchState.POST_GAME) {
			throw new IllegalStateException("Match can reset only from POST_GAME");
		}
		state = MatchState.LOBBY;
		elapsedTicks = 0;
		winner = null;
		finishReason = null;
	}

	private static boolean isAllowedTransition(MatchState current, MatchState next) {
		return switch (current) {
			case LOBBY -> next == MatchState.HERO_SELECT;
			case HERO_SELECT -> next == MatchState.LOADING;
			case LOADING -> next == MatchState.COUNTDOWN;
			case COUNTDOWN -> next == MatchState.PLAYING;
			case PLAYING -> next == MatchState.FINISHED;
			case FINISHED -> next == MatchState.POST_GAME;
			case POST_GAME -> next == MatchState.LOBBY;
		};
	}
}
