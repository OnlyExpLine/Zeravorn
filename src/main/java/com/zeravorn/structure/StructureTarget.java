package com.zeravorn.structure;

import com.zeravorn.team.TeamId;
import java.util.UUID;

/** Server-side target snapshot for a tower or throne. */
public record StructureTarget(UUID id, TeamId team, Kind kind, boolean alive, boolean damagedAlliedHero, double distance) {
    public enum Kind { MINION, HERO }
    public StructureTarget { if (id == null || team == null || kind == null || distance < 0) throw new IllegalArgumentException("Invalid structure target"); }
}
