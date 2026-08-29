package com.zeravorn.ability;

import java.util.Objects;

public record AbilityDefinition(String id, AbilitySlot slot, int maxRank, int unlockLevel) {
    public AbilityDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(slot, "slot");
        if (id.isBlank() || maxRank < 1 || unlockLevel < 1) {
            throw new IllegalArgumentException("Invalid ability definition");
        }
    }
}
