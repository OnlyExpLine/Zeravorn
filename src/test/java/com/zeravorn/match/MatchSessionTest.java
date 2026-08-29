package com.zeravorn.match;

import com.zeravorn.team.MatchParticipant;
import com.zeravorn.team.TeamId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchSessionTest {
	@Test
	void followsTheServerMatchLifecycle() {
		MatchSession session = new MatchSession(UUID.randomUUID());

		session.transitionTo(MatchState.HERO_SELECT);
		session.transitionTo(MatchState.LOADING);
		session.transitionTo(MatchState.COUNTDOWN);
		session.transitionTo(MatchState.PLAYING);
		session.tick();
		session.tick();

		assertEquals(2, session.elapsedTicks());
		assertTrue(session.finish(TeamId.BLUE, MatchFinishReason.THRONE_DESTROYED));
		assertEquals(MatchState.FINISHED, session.state());
		assertEquals(TeamId.BLUE, session.winner());

		session.transitionTo(MatchState.POST_GAME);
		session.reset();
		assertEquals(MatchState.LOBBY, session.state());
		assertEquals(0, session.elapsedTicks());
		assertEquals(null, session.winner());
	}

	@Test
	void rejectsInvalidTransitions() {
		MatchSession session = new MatchSession(UUID.randomUUID());

		assertThrows(IllegalStateException.class, () -> session.transitionTo(MatchState.PLAYING));
		assertThrows(IllegalStateException.class, () -> session.finish(TeamId.RED, MatchFinishReason.ADMIN_STOP));
	}

	@Test
	void cannotFinishWithoutAuthoritativeWinnerAndResetClearsRosters() {
		MatchSession session = playingSession();
		assertThrows(IllegalStateException.class, () -> session.transitionTo(MatchState.FINISHED));
		assertTrue(session.teams().add(TeamId.BLUE, new MatchParticipant(UUID.randomUUID(), "Blue", false)));
		session.finish(TeamId.BLUE, MatchFinishReason.THRONE_DESTROYED);
		session.transitionTo(MatchState.POST_GAME);
		session.reset();
		assertEquals(0, session.teams().roster(TeamId.BLUE).size());
	}

	@Test
	void finishingIsIdempotent() {
		MatchSession session = playingSession();

		assertTrue(session.finish(TeamId.RED, MatchFinishReason.THRONE_DESTROYED));
		assertFalse(session.finish(TeamId.BLUE, MatchFinishReason.ADMIN_STOP));
		assertEquals(TeamId.RED, session.winner());
		assertEquals(MatchFinishReason.THRONE_DESTROYED, session.finishReason());
	}

	@Test
	void teamRosterHasFiveSlotsAndUniquePlayersAcrossTeams() {
		MatchSession session = new MatchSession(UUID.randomUUID());
		for (int i = 0; i < 5; i++) {
			assertTrue(session.teams().add(TeamId.BLUE,
					new MatchParticipant(UUID.randomUUID(), "Blue" + i, false)));
		}
		assertFalse(session.teams().add(TeamId.BLUE,
				new MatchParticipant(UUID.randomUUID(), "Overflow", false)));

		UUID existingPlayer = session.teams().roster(TeamId.BLUE).participants().getFirst().playerId();
		assertFalse(session.teams().add(TeamId.RED,
				new MatchParticipant(existingPlayer, "Duplicate", false)));
		assertEquals(5, session.teams().roster(TeamId.BLUE).size());
	}

	private static MatchSession playingSession() {
		MatchSession session = new MatchSession(UUID.randomUUID());
		session.transitionTo(MatchState.HERO_SELECT);
		session.transitionTo(MatchState.LOADING);
		session.transitionTo(MatchState.COUNTDOWN);
		session.transitionTo(MatchState.PLAYING);
		return session;
	}
}
