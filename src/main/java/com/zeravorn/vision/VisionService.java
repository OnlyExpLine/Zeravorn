package com.zeravorn.vision;

import com.zeravorn.team.TeamId;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Computes team vision on the server and filters every visibility/minimap delta. */
public final class VisionService {
    public static final double HERO_RADIUS = 16.0, LANE_MINION_RADIUS = 10.0, TOWER_RADIUS = 18.0, THRONE_RADIUS = 20.0, FOUNTAIN_RADIUS = 20.0;
    private final Map<TeamId, VisionState> states = new EnumMap<>(TeamId.class);
    private final Map<TeamId, Map<UUID, Long>> revealUntil = new EnumMap<>(TeamId.class);
    public VisionService() { for (TeamId team : TeamId.values()) { states.put(team, new VisionState()); revealUntil.put(team, new HashMap<>()); } }
    public VisionDelta recompute(TeamId receivingTeam, Collection<VisionSource> sources, Collection<VisionTarget> targets, LineOfSight los, long serverTick) {
        VisionState state = states.get(receivingTeam); Set<UUID> before = state.visibleIds(), visible = new HashSet<>();
        Map<UUID, Long> reveals = revealUntil.get(receivingTeam); reveals.entrySet().removeIf(entry -> entry.getValue() <= serverTick);
        for (VisionTarget target : targets) {
            if (!target.alive()) continue;
            if (target.team() == receivingTeam || reveals.getOrDefault(target.id(), 0L) > serverTick || canSee(receivingTeam, target, sources, los)) visible.add(target.id());
        }
        state.replaceVisible(visible); Set<UUID> show = new HashSet<>(visible); show.removeAll(before); Set<UUID> hide = new HashSet<>(before); hide.removeAll(visible);
        return new VisionDelta(receivingTeam, Set.copyOf(show), Set.copyOf(hide));
    }
    /** Attack/ability from a bush and allied creep/tower combat reveal the attacker to enemies. */
    public void revealToEnemies(TeamId owner, UUID target, long serverTick, long durationTicks) {
        if (durationTicks <= 0) throw new IllegalArgumentException("Reveal duration must be positive");
        for (TeamId team : TeamId.values()) if (team != owner) revealUntil.get(team).merge(target, serverTick + durationTicks, Math::max);
    }
    public boolean visibleTo(TeamId team, UUID target) { return states.get(team).visibleIds().contains(target); }
    public MinimapDelta minimapDelta(TeamId receivingTeam, Collection<VisionTarget> targets) {
        Map<UUID, com.zeravorn.map.Position> allowed = new HashMap<>(); for (VisionTarget target : targets) if (visibleTo(receivingTeam, target.id())) allowed.put(target.id(), target.position());
        return new MinimapDelta(receivingTeam, Map.copyOf(allowed));
    }
    private boolean canSee(TeamId team, VisionTarget target, Collection<VisionSource> sources, LineOfSight los) {
        for (VisionSource source : sources) {
            if (source.team() != team || source.position().distanceSquared(target.position()) > source.radius() * source.radius() || !los.hasLineOfSight(source.position(), target.position())) continue;
            // A bush conceals an enemy unless a friendly source shares that same bush. M13 supplies actual membership from MapDefinition.
            if (target.bushId() == null || target.bushId().equals(source.bushId())) return true;
        }
        return false;
    }
}
