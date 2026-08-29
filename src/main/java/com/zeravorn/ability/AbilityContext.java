package com.zeravorn.ability;

public record AbilityContext(long serverTick, boolean matchPlaying) {
    public AbilityContext { if (serverTick < 0) throw new IllegalArgumentException("Tick cannot be negative"); }
}
