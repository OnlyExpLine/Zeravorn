package com.zeravorn.ability;

import java.util.UUID;

public record AbilityEvent(UUID caster, AbilitySlot slot, Phase phase, long serverTick) {
    public enum Phase { START, HIT, CANCEL }
    public AbilityEvent { if (caster == null || slot == null || phase == null || serverTick < 0) throw new IllegalArgumentException("Invalid ability event"); }
}
