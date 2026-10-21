package com.zeravorn.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Per-player, per-match high-water mark. A sequence is reserved before a
 * gameplay handler runs, so a retransmission cannot execute a mutation twice.
 * Reset only when the match/connection identity is deliberately replaced.
 */
public final class RequestSequenceGuard {
    private final Map<UUID, Long> lastSequenceByPlayer = new HashMap<>();

    public synchronized RequestResult reserve(UUID playerId, RequestEnvelope request) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(request, "request");
        long sequence = request.clientSequenceId();
        if (request.protocolVersion() != RequestEnvelope.CURRENT_VERSION) {
            return RequestResult.rejected(sequence, RequestError.UNSUPPORTED_PROTOCOL);
        }
        if (sequence < 0 || sequence <= lastSequenceByPlayer.getOrDefault(playerId, -1L)) {
            return RequestResult.rejected(sequence, RequestError.DUPLICATE_SEQUENCE);
        }
        lastSequenceByPlayer.put(playerId, sequence);
        return RequestResult.accepted(sequence);
    }

    public synchronized void clearMatch() {
        lastSequenceByPlayer.clear();
    }

    /** For pre-match connection scopes only; never clear a live match on disconnect. */
    public synchronized void clearPlayer(UUID playerId) {
        lastSequenceByPlayer.remove(Objects.requireNonNull(playerId));
    }

    public synchronized long nextSequence(UUID playerId) {
        long last = lastSequenceByPlayer.getOrDefault(Objects.requireNonNull(playerId), -1L);
        if (last == Long.MAX_VALUE) throw new IllegalStateException("Client sequence exhausted");
        return last + 1;
    }
}
