package com.zeravorn.ability;

import java.util.Objects;

public record AbilityRuntime(AbilityDefinition definition, int rank) {
    public AbilityRuntime {
        Objects.requireNonNull(definition, "definition");
        if (rank < 0 || rank > definition.maxRank()) throw new IllegalArgumentException("Invalid ability rank");
        if (rank > 0 && definition.unlockLevel() < 1) throw new IllegalArgumentException("Invalid unlock level");
    }
    public boolean learned() { return rank > 0; }
}
