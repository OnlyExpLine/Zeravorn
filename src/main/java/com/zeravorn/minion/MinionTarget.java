package com.zeravorn.minion;

import com.zeravorn.structure.StructureType;
import com.zeravorn.team.TeamId;
import java.util.UUID;

/** Authoritative nearby target snapshot; lane minions deliberately never receive hero targets. */
public record MinionTarget(UUID id, TeamId team, Kind kind, boolean alive, double distance) {
    public enum Kind { LANE_MINION, TOWER, THRONE }
    public MinionTarget { if (id == null || team == null || kind == null || distance < 0) throw new IllegalArgumentException("Invalid minion target"); }
}
