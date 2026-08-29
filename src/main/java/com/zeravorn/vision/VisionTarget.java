package com.zeravorn.vision;

import com.zeravorn.map.Position;
import com.zeravorn.team.TeamId;
import java.util.UUID;

/** Replicated only when VisionService permits it for the receiving team. */
public record VisionTarget(UUID id, TeamId team, Position position, boolean alive, String bushId) {
    public VisionTarget { if (id == null || team == null || position == null) throw new IllegalArgumentException("Invalid vision target"); }
}
