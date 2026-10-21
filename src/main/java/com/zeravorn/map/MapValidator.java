package com.zeravorn.map;

import com.zeravorn.minion.Lane;
import com.zeravorn.jungle.JungleConfigLoader;
import com.zeravorn.structure.TowerOrder;
import com.zeravorn.team.TeamId;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Fails closed before any player is teleported or world blocks are generated. */
public final class MapValidator {
    private MapValidator() { }

    public static void validate(MapDefinition map) {
        Objects.requireNonNull(map);
        if (map.id().isBlank()) fail("map id");
        var box = map.playableBoundary();
        if (!Double.isFinite(box.minX()) || !Double.isFinite(box.maxX())
                || !Double.isFinite(box.minZ()) || !Double.isFinite(box.maxZ())
                || box.minX() >= box.maxX() || box.minZ() >= box.maxZ()
                || box.maxX() - box.minX() < 100 || box.maxZ() - box.minZ() < 100)
            fail("playable boundary");
        for (TeamId team : TeamId.values()) {
            var spawn = map.spawns().get(team);
            var fountain = map.fountains().get(team);
            var danger = map.fountainDangerZones().get(team);
            Position throne = map.thrones().get(team);
            if (spawn == null || fountain == null || danger == null || throne == null) fail(team + " base missing");
            inside(map, spawn.position(), team + " spawn");
            inside(map, fountain.center(), team + " fountain");
            inside(map, danger.center(), team + " danger zone");
            inside(map, throne, team + " throne");
            if (!Float.isFinite(spawn.yaw()) || spawn.safeRadius() < 4
                    || !Double.isFinite(spawn.safeRadius()) || fountain.radius() < spawn.safeRadius()
                    || !Double.isFinite(fountain.radius())
                    || !fountain.contains(spawn.position())) fail(team + " unsafe spawn/fountain");
            if (!Double.isFinite(danger.radius()) || danger.radius() < fountain.radius()
                    || !danger.contains(fountain.center())) fail(team + " invalid danger zone");
            circleInside(map, fountain.center(), fountain.radius(), team + " fountain");
            circleInside(map, danger.center(), danger.radius(), team + " danger zone");
            if (map.fountains().get(team == TeamId.BLUE ? TeamId.RED : TeamId.BLUE) != null
                    && map.fountains().get(team == TeamId.BLUE ? TeamId.RED : TeamId.BLUE).contains(throne))
                fail(team + " throne in enemy fountain");
        }
        if (map.spawns().get(TeamId.BLUE).position().distanceSquared(
                map.spawns().get(TeamId.RED).position()) < 10000) fail("bases too close");
        if (map.towers().size() != 18) fail("expected 18 towers");
        Set<String> ids = new HashSet<>();
        for (var tower : map.towers()) {
            if (!ids.add(tower.id())) fail("duplicate tower id " + tower.id());
            if (!tower.id().equals(tower.team() + "_" + tower.lane() + "_" + tower.order()))
                fail("tower id mismatch " + tower.id());
            inside(map, tower.position(), tower.id());
            if (!Double.isFinite(tower.hitboxRadius()) || tower.hitboxRadius() <= 0
                    || !Double.isFinite(tower.attackRadius()) || tower.attackRadius() <= tower.hitboxRadius()
                    || !Double.isFinite(tower.visionRadius()) || tower.visionRadius() < tower.attackRadius())
                fail("invalid tower ranges " + tower.id());
            circleInside(map, tower.position(), tower.attackRadius(), tower.id() + " attack radius");
        }
        for (Lane lane : Lane.values()) for (TeamId team : TeamId.values()) {
            for (TowerOrder order : TowerOrder.values()) {
                int count = 0;
                for (var tower : map.towers()) if (tower.team() == team && tower.lane() == lane
                        && tower.order() == order) count++;
                if (count != 1) fail("missing or duplicate " + team + " " + lane + " " + order);
            }
            var byTeam = map.lanePaths().get(lane);
            List<Position> path = byTeam == null ? null : byTeam.get(team);
            if (path == null || path.size() < 4) fail(team + " " + lane + " path too short");
            for (int index = 0; index < path.size(); index++) {
                Position point = path.get(index);
                inside(map, point, team + " " + lane + " waypoint");
                for (var tower : map.towers())
                    if (point.distanceSquared(tower.position())
                            <= (tower.hitboxRadius() + 0.5) * (tower.hitboxRadius() + 0.5))
                        fail(team + " " + lane + " waypoint intersects " + tower.id());
                // The last point may target the enemy throne; all other waypoints must be walkable.
                if (index < path.size() - 1) {
                    for (var throne : map.thrones().entrySet())
                        if (sameBlock(point, throne.getValue()))
                            fail(team + " " + lane + " waypoint intersects " + throne.getKey() + " throne");
                    for (var bush : map.bushZones())
                        if (sameBlock(point, bush.center()))
                            fail(team + " " + lane + " waypoint intersects bush " + bush.id());
                }
            }
            for (int i = 1; i < path.size(); i++)
                if (path.get(i - 1).distanceSquared(path.get(i)) > 12.01 * 12.01)
                    fail(team + " " + lane + " waypoint gap exceeds 12 blocks");
            if (path.getFirst().distanceSquared(map.spawns().get(team).position()) > 900)
                fail(team + " " + lane + " path does not begin near spawn");
            TeamId enemy = team == TeamId.BLUE ? TeamId.RED : TeamId.BLUE;
            if (path.getLast().distanceSquared(map.thrones().get(enemy)) > 900)
                fail(team + " " + lane + " path does not reach enemy base");
            // Structures must be encountered T3 -> T2 -> T1 when leaving own base.
            int previous = -1;
            for (TowerOrder order : new TowerOrder[]{TowerOrder.T3, TowerOrder.T2, TowerOrder.T1}) {
                var tower = map.towers().stream().filter(t -> t.team() == team && t.lane() == lane
                        && t.order() == order).findFirst().orElseThrow();
                int nearest = nearest(path, tower.position());
                if (nearest <= previous || path.get(nearest).distanceSquared(tower.position()) > 225)
                    fail(team + " " + lane + " tower order/path " + order);
                previous = nearest;
            }
        }
        Set<String> requiredCamps = Set.of("BLUE_RED", "BLUE_BLUE", "BLUE_GREEN_A", "BLUE_GREEN_B",
                "RED_RED", "RED_BLUE", "RED_GREEN_A", "RED_GREEN_B", "CONTESTED_A", "CONTESTED_B");
        Set<String> camps = new HashSet<>();
        var mobDefinitions = JungleConfigLoader.loadDefaultDefinitions();
        for (var camp : map.jungleCamps()) {
            if (!camps.add(camp.id())) fail("duplicate camp " + camp.id());
            inside(map, camp.position(), camp.id());
            if (!Double.isFinite(camp.leashRadius()) || camp.leashRadius() <= 0
                    || camp.mobDefinitionId() == null || camp.mobDefinitionId().isBlank()
                    || !Set.of("BLUE", "RED", "NEUTRAL").contains(camp.teamAffinity()))
                fail("invalid camp " + camp.id());
            if (!mobDefinitions.containsKey(camp.mobDefinitionId()))
                fail("unknown camp mob " + camp.id() + ": " + camp.mobDefinitionId());
            if (camp.id().startsWith("BLUE_") && !camp.teamAffinity().equals("BLUE")
                    || camp.id().startsWith("RED_") && !camp.teamAffinity().equals("RED")
                    || camp.id().startsWith("CONTESTED_") && !camp.teamAffinity().equals("NEUTRAL"))
                fail("camp affinity mismatch " + camp.id());
            if (!box.contains(new Position(camp.position().x() - camp.leashRadius(),
                    camp.position().y(), camp.position().z() - camp.leashRadius()))
                    || !box.contains(new Position(camp.position().x() + camp.leashRadius(),
                    camp.position().y(), camp.position().z() + camp.leashRadius())))
                fail("camp leash outside boundary " + camp.id());
        }
        if (!camps.equals(requiredCamps)) fail("camp completeness; missing " + difference(requiredCamps, camps));
        Set<String> bushes = new HashSet<>();
        for (var bush : map.bushZones()) {
            if (!bushes.add(bush.id())) fail("duplicate bush " + bush.id());
            inside(map, bush.center(), bush.id());
            if (!box.contains(new Position(bush.center().x() - bush.radius(), bush.center().y(),
                    bush.center().z() - bush.radius())) || !box.contains(new Position(
                    bush.center().x() + bush.radius(), bush.center().y(), bush.center().z() + bush.radius())))
                fail("bush outside boundary " + bush.id());
        }
        if (bushes.isEmpty()) fail("missing bush zones");
        if (ids.stream().anyMatch(MapValidator::epicObjective)
                || camps.stream().anyMatch(MapValidator::epicObjective))
            fail("epic boss objective is not allowed");
    }

    private static int nearest(List<Position> path, Position point) {
        int index = 0;
        for (int i = 1; i < path.size(); i++)
            if (path.get(i).distanceSquared(point) < path.get(index).distanceSquared(point)) index = i;
        return index;
    }
    private static boolean sameBlock(Position a, Position b) {
        return (int) Math.floor(a.x()) == (int) Math.floor(b.x())
                && (int) Math.floor(a.z()) == (int) Math.floor(b.z());
    }
    private static Set<String> difference(Set<String> expected, Set<String> actual) {
        var missing = new HashSet<>(expected);
        missing.removeAll(actual);
        return missing;
    }
    private static boolean epicObjective(String id) {
        String upper = id.toUpperCase(java.util.Locale.ROOT);
        return upper.contains("BOSS") || upper.contains("DRAGON") || upper.contains("BARON")
                || upper.contains("LORD") || upper.contains("TURTLE");
    }
    private static void inside(MapDefinition map, Position point, String label) {
        if (!map.playableBoundary().contains(point)) fail(label + " outside boundary");
    }
    private static void circleInside(MapDefinition map, Position center, double radius, String label) {
        if (!map.playableBoundary().contains(new Position(center.x() - radius, center.y(), center.z() - radius))
                || !map.playableBoundary().contains(new Position(center.x() + radius, center.y(),
                center.z() + radius))) fail(label + " outside boundary");
    }
    private static void fail(String message) { throw new IllegalArgumentException(message); }
}
