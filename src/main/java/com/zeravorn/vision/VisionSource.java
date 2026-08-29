package com.zeravorn.vision;

import com.zeravorn.map.Position;
import com.zeravorn.team.TeamId;
import java.util.UUID;

public record VisionSource(UUID id, TeamId team, Position position, VisionSourceType type, double radius, String bushId) {
    public VisionSource { if (id == null || team == null || position == null || type == null || radius <= 0) throw new IllegalArgumentException("Invalid vision source"); }
}
