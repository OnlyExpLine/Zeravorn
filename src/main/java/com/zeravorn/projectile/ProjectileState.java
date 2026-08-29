package com.zeravorn.projectile;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ProjectileState {
    private final UUID id;
    private final UUID caster;
    private final ProjectileDefinition definition;
    private final Set<UUID> hitTargets = new HashSet<>();
    private double travelled;
    private int remainingPierces;
    private boolean done;

    public ProjectileState(UUID id, UUID caster, ProjectileDefinition definition) { this.id = id; this.caster = caster; this.definition = definition; this.remainingPierces = definition.pierceCount(); }
    public UUID id() { return id; }
    public UUID caster() { return caster; }
    public ProjectileDefinition definition() { return definition; }
    public boolean done() { return done; }
    public boolean canHit(UUID target) { return !done && !hitTargets.contains(target); }
    public void advance(double distance) { if (distance < 0) throw new IllegalArgumentException("Distance cannot be negative"); travelled += distance; if (travelled >= definition.maxRange()) done = true; }
    public boolean registerHit(UUID target) { if (!canHit(target)) return false; hitTargets.add(target); if (remainingPierces == 0) done = true; else remainingPierces--; return true; }
}
