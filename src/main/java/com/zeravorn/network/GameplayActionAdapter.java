package com.zeravorn.network;

/**
 * Boundary to world-dependent combat/spell/recall services. A null return means
 * the authoritative action was actually applied; otherwise it was rejected.
 */
@FunctionalInterface
public interface GameplayActionAdapter {
    RequestError execute(RequestValidationContext context, ClientRequest request);

    static GameplayActionAdapter unavailable() {
        return (context, request) -> RequestError.WRONG_STATE;
    }
}
