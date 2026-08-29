package com.zeravorn.minion;

import com.zeravorn.map.Position;
import com.zeravorn.team.TeamId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.Comparator;

public final class LaneMinion {
    public enum State { MOVE_PATH, FIGHT_MINION, ATTACK_STRUCTURE, RECOVER_PATH }
    private final UUID id;
    private final TeamId team;
    private final Lane lane;
    private final int waveNumber;
    private final LaneMinionDefinition definition;
    private final List<Position> waypoints;
    private int waypointIndex;
    private int health;
    private State state = State.MOVE_PATH;
    private UUID targetId;

    public LaneMinion(UUID id, TeamId team, Lane lane, int waveNumber, LaneMinionDefinition definition, List<Position> waypoints) {
        this.id = Objects.requireNonNull(id); this.team = Objects.requireNonNull(team); this.lane = Objects.requireNonNull(lane);
        if (waveNumber < 1) throw new IllegalArgumentException("waveNumber must be positive");
        this.waveNumber = waveNumber; this.definition = Objects.requireNonNull(definition);
        this.waypoints = List.copyOf(waypoints); if (this.waypoints.isEmpty()) throw new IllegalArgumentException("waypoints required");
        this.health = scaledHealth();
    }
    public UUID id() { return id; } public TeamId team() { return team; } public Lane lane() { return lane; }
    public int waveNumber() { return waveNumber; } public LaneMinionDefinition definition() { return definition; }
    public List<Position> waypoints() { return waypoints; } public int waypointIndex() { return waypointIndex; }
    public int health() { return health; } public int maxHealth() { return scaledHealth(); }
    public int damage() { return (int)Math.round(definition.damage() * scalingFactor(0.05)); }
    public State state() { return state; } public UUID targetId() { return targetId; }
    public void setState(State state) { this.state = Objects.requireNonNull(state); }
    public void setTarget(UUID targetId) { this.targetId = targetId; }
    /** Target priority required by the GDD: enemy lane minions, then a tower, then throne. */
    public MinionTarget chooseTarget(List<MinionTarget> nearby) {
        MinionTarget selected = nearby.stream().filter(target -> target.alive() && target.team() != team)
                .min(Comparator.comparingInt((MinionTarget target) -> switch (target.kind()) { case LANE_MINION -> 0; case TOWER -> 1; case THRONE -> 2; })
                        .thenComparingDouble(MinionTarget::distance)).orElse(null);
        targetId = selected == null ? null : selected.id();
        state = selected == null ? State.MOVE_PATH : selected.kind() == MinionTarget.Kind.LANE_MINION ? State.FIGHT_MINION : State.ATTACK_STRUCTURE;
        return selected;
    }
    public boolean advanceWaypoint() { if (waypointIndex + 1 >= waypoints.size()) return false; waypointIndex++; return true; }
    public void receiveDamage(int amount) { if (amount < 0) throw new IllegalArgumentException("negative damage"); health = Math.max(0, health - amount); }
    public boolean dead() { return health == 0; }
    private int scaledHealth() { return (int)Math.round(definition.maxHealth() * scalingFactor(0.08)); }
    private double scalingFactor(double rate) { return 1.0 + rate * Math.min(10, Math.max(0, (waveNumber - 1) / 4)); }
}
