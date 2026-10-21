package com.zeravorn.map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zeravorn.minion.Lane;
import com.zeravorn.structure.TowerOrder;
import com.zeravorn.team.TeamId;
import com.zeravorn.vision.BushZone;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class MapLoader {
    private MapLoader() { }

    public static MapDefinition loadDefault() {
        try (var stream = MapLoader.class.getResourceAsStream("/config_defaults/arena_v1.json")) {
            if (stream == null) throw new IllegalStateException("Missing arena_v1.json");
            return parse(JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid arena_v1.json: " + exception.getMessage(), exception);
        }
    }

    public static MapDefinition parse(JsonObject root) {
        try {
            var box = root.getAsJsonObject("playableBoundary");
            var boundary = new MapDefinition.Boundary(number(box, "minX"), number(box, "maxX"),
                    number(box, "minZ"), number(box, "maxZ"));
            Map<TeamId, MapDefinition.Spawn> spawns = new EnumMap<>(TeamId.class);
            Map<TeamId, MapDefinition.Zone> fountains = new EnumMap<>(TeamId.class);
            Map<TeamId, MapDefinition.Zone> danger = new EnumMap<>(TeamId.class);
            Map<TeamId, Position> thrones = new EnumMap<>(TeamId.class);
            for (TeamId team : TeamId.values()) {
                String prefix = team.name().toLowerCase();
                JsonObject spawn = root.getAsJsonObject(prefix + "Spawn");
                JsonObject fountain = root.getAsJsonObject(prefix + "Fountain");
                spawns.put(team, new MapDefinition.Spawn(pos(spawn.getAsJsonArray("pos")),
                        spawn.get("yaw").getAsFloat(), number(spawn, "safeRadius")));
                fountains.put(team, new MapDefinition.Zone(pos(fountain.getAsJsonArray("pos")),
                        number(fountain, "radius")));
                var dangerZone = root.getAsJsonObject("fountainDangerZones").getAsJsonObject(team.name());
                danger.put(team, new MapDefinition.Zone(pos(dangerZone.getAsJsonArray("pos")),
                        number(dangerZone, "radius")));
                thrones.put(team, pos(root.getAsJsonObject(prefix + "Throne").getAsJsonArray("pos")));
            }
            var towerDefaults = root.getAsJsonObject("towerDefaults");
            List<MapDefinition.TowerPoint> towers = new ArrayList<>();
            for (var element : root.getAsJsonArray("towers")) {
                var tower = element.getAsJsonObject();
                towers.add(new MapDefinition.TowerPoint(string(tower, "id"),
                        TeamId.valueOf(string(tower, "team")), Lane.valueOf(string(tower, "lane")),
                        TowerOrder.valueOf(string(tower, "order")), pos(tower.getAsJsonArray("pos")),
                        number(towerDefaults, "hitboxRadius"), number(towerDefaults, "attackRadius"),
                        number(towerDefaults, "visionRadius")));
            }
            Map<Lane, Map<TeamId, List<Position>>> paths = new EnumMap<>(Lane.class);
            var lanes = root.getAsJsonObject("lanePaths");
            for (Lane lane : Lane.values()) {
                var byTeam = lanes.getAsJsonObject(lane.name());
                Map<TeamId, List<Position>> teamPaths = new EnumMap<>(TeamId.class);
                for (TeamId team : TeamId.values()) {
                    List<Position> points = new ArrayList<>();
                    for (var entry : byTeam.getAsJsonArray(team.name())) points.add(pos(entry.getAsJsonArray()));
                    teamPaths.put(team, interpolate(points));
                }
                paths.put(lane, teamPaths);
            }
            List<MapDefinition.CampPoint> camps = new ArrayList<>();
            for (var element : root.getAsJsonArray("jungleCamps")) {
                var camp = element.getAsJsonObject();
                camps.add(new MapDefinition.CampPoint(string(camp, "id"), pos(camp.getAsJsonArray("pos")),
                        number(camp, "leashRadius"), string(camp, "mobDefinitionId"),
                        string(camp, "teamAffinity")));
            }
            List<BushZone> bushes = new ArrayList<>();
            for (var element : root.getAsJsonArray("bushZones")) {
                var bush = element.getAsJsonObject();
                bushes.add(new BushZone(string(bush, "id"), pos(bush.getAsJsonArray("pos")),
                        number(bush, "radius")));
            }
            var definition = new MapDefinition(string(root, "id"), boundary, spawns, fountains, danger,
                    thrones, towers, paths, camps, bushes);
            MapValidator.validate(definition);
            return definition;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid map config: " + exception.getMessage(), exception);
        }
    }

    private static Position pos(JsonArray array) {
        if (array == null || array.size() != 3) throw new IllegalArgumentException("Position requires 3 numbers");
        return new Position(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
    }
    /** JSON stores turn/control points; runtime paths use at most 12-block hops. */
    private static List<Position> interpolate(List<Position> controls) {
        if (controls.isEmpty()) return List.of();
        List<Position> path = new ArrayList<>();
        path.add(controls.getFirst());
        for (int i = 1; i < controls.size(); i++) {
            Position from = controls.get(i - 1), to = controls.get(i);
            int steps = Math.max(1, (int) Math.ceil(Math.sqrt(from.distanceSquared(to)) / 12));
            for (int step = 1; step <= steps; step++) {
                double t = (double) step / steps;
                path.add(new Position(from.x() + (to.x() - from.x()) * t,
                        from.y() + (to.y() - from.y()) * t,
                        from.z() + (to.z() - from.z()) * t));
            }
        }
        return path;
    }
    private static String string(JsonObject object, String name) { return object.get(name).getAsString(); }
    private static double number(JsonObject object, String name) { return object.get(name).getAsDouble(); }
}
