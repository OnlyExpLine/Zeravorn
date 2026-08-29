package com.zeravorn.vision;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Mutable server state owned by one team; no client-side authority. */
public final class VisionState {
    private final Set<UUID> visible = new HashSet<>();
    public Set<UUID> visibleIds() { return Set.copyOf(visible); }
    void replaceVisible(Set<UUID> ids) { visible.clear(); visible.addAll(ids); }
}
