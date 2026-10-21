package com.zeravorn.map;

import com.zeravorn.minion.Lane;
import com.zeravorn.structure.TowerOrder;
import com.zeravorn.team.TeamId;
import com.zeravorn.vision.BushZone;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable logical arena data. Actual collision/LOS always comes from the server world. */
public record MapDefinition(String id, Boundary playableBoundary, Map<TeamId, Spawn> spawns,
                            Map<TeamId, Zone> fountains, Map<TeamId, Zone> fountainDangerZones,
                            Map<TeamId, Position> thrones,
                            List<TowerPoint> towers, Map<Lane, Map<TeamId, List<Position>>> lanePaths,
                            List<CampPoint> jungleCamps, List<BushZone> bushZones) {
    public MapDefinition {
        Objects.requireNonNull(id);
        Objects.requireNonNull(playableBoundary);
        spawns = Map.copyOf(spawns);
        fountains = Map.copyOf(fountains);
        fountainDangerZones = Map.copyOf(fountainDangerZones);
        thrones = Map.copyOf(thrones);
        towers = List.copyOf(towers);
        lanePaths = lanePaths.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> entry.getValue().entrySet().stream().collect(
                        java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey,
                                path -> List.copyOf(path.getValue())))));
        jungleCamps = List.copyOf(jungleCamps);
        bushZones = List.copyOf(bushZones);
    }

    public record Boundary(double minX, double maxX, double minZ, double maxZ) {
        public boolean contains(Position p) {
            return p != null && Double.isFinite(p.x()) && Double.isFinite(p.y()) && Double.isFinite(p.z())
                    && p.x() >= minX && p.x() <= maxX && p.z() >= minZ && p.z() <= maxZ;
        }
    }
    public record Spawn(Position position, float yaw, double safeRadius) { }
    public record Zone(Position center, double radius) {
        public boolean contains(Position p) { return center.distanceSquared(p) <= radius * radius; }
    }
    public record TowerPoint(String id, TeamId team, Lane lane, TowerOrder order, Position position,
                             double hitboxRadius, double attackRadius, double visionRadius) { }
    public record CampPoint(String id, Position position, double leashRadius,
                            String mobDefinitionId, String teamAffinity) { }

    public com.zeravorn.bot.BotRouteProfile botRoute(TeamId team, Lane lane) {
        return new com.zeravorn.bot.BotRouteProfile(lane, lanePaths.get(lane).get(team));
    }
}
