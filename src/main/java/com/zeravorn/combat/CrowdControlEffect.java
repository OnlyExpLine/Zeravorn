package com.zeravorn.combat;

import java.util.UUID;

public record CrowdControlEffect(UUID source, CrowdControlType type, double strength, long expiresAtTick, boolean cleanseable) {
    public CrowdControlEffect {
        if (strength < 0 || expiresAtTick < 0) throw new IllegalArgumentException("Invalid CC effect");
    }
}
