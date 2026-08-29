package com.zeravorn.combat;

import com.zeravorn.team.TeamId;
import java.util.UUID;

public record TargetSnapshot(UUID id, TeamId team, boolean alive, double distance) {
    public TargetSnapshot {
        if (id == null || team == null || distance < 0) throw new IllegalArgumentException("Invalid target");
    }
}
