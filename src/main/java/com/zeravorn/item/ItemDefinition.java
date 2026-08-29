package com.zeravorn.item;
import java.util.Objects;
public record ItemDefinition(String id, String name, ItemCategory category, int price, SlotType slotType, ItemStats stats) {
    public ItemDefinition { Objects.requireNonNull(id); Objects.requireNonNull(name); Objects.requireNonNull(category); Objects.requireNonNull(slotType); Objects.requireNonNull(stats); if (id.isBlank() || price <= 0) throw new IllegalArgumentException("Invalid item"); }
}
