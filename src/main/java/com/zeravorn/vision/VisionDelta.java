package com.zeravorn.vision;

import com.zeravorn.team.TeamId;
import java.util.Set;
import java.util.UUID;

/** Safe S2C delta: only IDs approved by server visibility are emitted. */
public record VisionDelta(TeamId team, Set<UUID> showIds, Set<UUID> hideIds) { }
