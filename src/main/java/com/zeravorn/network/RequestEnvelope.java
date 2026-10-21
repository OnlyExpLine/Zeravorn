package com.zeravorn.network;

import java.util.Objects;

/** Common header for every state-changing C2S request. */
public record RequestEnvelope(int protocolVersion, long clientSequenceId, ClientRequest payload) {
    public static final int CURRENT_VERSION = 1;

    public RequestEnvelope {
        Objects.requireNonNull(payload, "payload");
    }
}
