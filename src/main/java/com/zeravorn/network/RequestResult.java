package com.zeravorn.network;

import java.util.Objects;

/** Correlates one server decision with its C2S sequence. */
public record RequestResult(long clientSequenceId, boolean success, RequestError errorCode) {
    public RequestResult {
        if (success == (errorCode != null)) {
            throw new IllegalArgumentException("Success requires no error; failure requires an error");
        }
    }

    public static RequestResult accepted(long sequenceId) {
        return new RequestResult(sequenceId, true, null);
    }

    public static RequestResult rejected(long sequenceId, RequestError error) {
        return new RequestResult(sequenceId, false, Objects.requireNonNull(error, "error"));
    }
}
