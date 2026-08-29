package com.zeravorn.vision;

import com.zeravorn.map.Position;
import com.zeravorn.team.TeamId;
import java.util.Map;
import java.util.UUID;

/** Positions have already passed VisionService filtering. */
public record MinimapDelta(TeamId team, Map<UUID, Position> visiblePositions) { }
