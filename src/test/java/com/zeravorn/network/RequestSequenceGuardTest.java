package com.zeravorn.network;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RequestSequenceGuardTest {
    private static RequestEnvelope request(long sequence) {
        return new RequestEnvelope(1, sequence, new ClientRequest.BuyItem("iron_blade"));
    }

    @Test
    void rejectsDuplicateAndOlderRequestsWithoutReexecution() {
        var guard = new RequestSequenceGuard();
        var player = UUID.randomUUID();
        assertTrue(guard.reserve(player, request(7)).success());
        assertEquals(RequestError.DUPLICATE_SEQUENCE, guard.reserve(player, request(7)).errorCode());
        assertEquals(RequestError.DUPLICATE_SEQUENCE, guard.reserve(player, request(6)).errorCode());
        assertTrue(guard.reserve(player, request(8)).success());
    }

    @Test
    void tracksPlayersIndependentlyAndPreservesSequenceAcrossReconnect() {
        var guard = new RequestSequenceGuard();
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        assertTrue(guard.reserve(first, request(2)).success());
        assertTrue(guard.reserve(second, request(2)).success());
        assertEquals(RequestError.DUPLICATE_SEQUENCE, guard.reserve(first, request(2)).errorCode());
        guard.clearPlayer(second);
        assertTrue(guard.reserve(second, request(2)).success());
        guard.clearMatch();
        assertTrue(guard.reserve(first, request(2)).success());
    }

    @Test
    void invalidProtocolDoesNotReserveSequence() {
        var guard = new RequestSequenceGuard();
        var player = UUID.randomUUID();
        var wrong = new RequestEnvelope(99, 1, new ClientRequest.UpgradeAbility("jason_q"));
        assertEquals(RequestError.UNSUPPORTED_PROTOCOL, guard.reserve(player, wrong).errorCode());
        assertTrue(guard.reserve(player, request(1)).success());
    }

    @Test
    void requestResultEnforcesSuccessErrorInvariant() {
        assertThrows(IllegalArgumentException.class, () -> new RequestResult(1, true, RequestError.DEAD));
        assertThrows(IllegalArgumentException.class, () -> new RequestResult(1, false, null));
    }
}
