package com.zeravorn.combat;

import java.util.Objects;
import java.util.UUID;

public record DamageInstance(UUID id, UUID source, UUID target, DamageType type, int amount, long serverTick) {
    public DamageInstance {
        Objects.requireNonNull(id); Objects.requireNonNull(source); Objects.requireNonNull(target); Objects.requireNonNull(type);
        if (amount < 0 || serverTick < 0) throw new IllegalArgumentException("Invalid damage instance");
    }
}
