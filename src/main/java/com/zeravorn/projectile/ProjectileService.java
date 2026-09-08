package com.zeravorn.projectile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ProjectileService {
    private final Map<UUID, ProjectileState> active = new HashMap<>();
    public ProjectileState spawn(UUID caster, ProjectileDefinition definition) { ProjectileState state = new ProjectileState(UUID.randomUUID(), caster, definition); active.put(state.id(), state); return state; }
    public void advance(UUID id, double distance) { ProjectileState state = require(id); state.advance(distance); if (state.done()) active.remove(id); }
    /** Advances a server-owned projectile from elapsed server ticks and its configured blocks/second speed. */
    public void advanceTicks(UUID id, long elapsedTicks, int ticksPerSecond) {
        if (elapsedTicks < 0 || ticksPerSecond <= 0) throw new IllegalArgumentException("Invalid projectile tick advance");
        ProjectileState state = require(id);
        advance(id, state.definition().speed() * elapsedTicks / ticksPerSecond);
    }
    public boolean confirmHit(UUID projectile, UUID target) { ProjectileState state = require(projectile); boolean hit = state.registerHit(target); if (state.done()) active.remove(projectile); return hit; }
    public boolean active(UUID id) { return active.containsKey(id); }
    private ProjectileState require(UUID id) { ProjectileState state = active.get(id); if (state == null) throw new IllegalArgumentException("Unknown projectile"); return state; }
}
