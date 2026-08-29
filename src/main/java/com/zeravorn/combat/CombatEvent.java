package com.zeravorn.combat;

import java.util.UUID;

public record CombatEvent(Type type, UUID source, UUID target, int amount, long serverTick) {
    public enum Type { DAMAGE, HEAL, DEATH }
    public CombatEvent {
        if (type == null || source == null || target == null || amount < 0 || serverTick < 0) {
            throw new IllegalArgumentException("Invalid combat event");
        }
    }
}
