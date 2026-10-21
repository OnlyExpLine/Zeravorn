package com.zeravorn.map;

import java.util.ArrayList;
import java.util.List;

/** Formation placed along each lane beyond the fountain, never behind the shared base waypoint. */
public final class LaneSpawnPlan {
    public static final int MAX_UNITS_PER_LANE_TEAM = 60;
    private LaneSpawnPlan() { }

    public static List<Position> route(List<Position> path, int slot) {
        double remaining = 24 - (slot / 2) * 1.8;
        for (int i = 1; i < path.size(); i++) {
            Position a = path.get(i - 1), b = path.get(i);
            double length = Math.hypot(b.x() - a.x(), b.z() - a.z());
            if (length < .001) continue;
            if (remaining >= length) { remaining -= length; continue; }
            double dx = (b.x() - a.x()) / length, dz = (b.z() - a.z()) / length;
            double side = slot % 2 == 0 ? -.8 : .8;
            var route = new ArrayList<Position>();
            route.add(new Position(a.x() + dx * remaining - dz * side, a.y(),
                    a.z() + dz * remaining + dx * side));
            route.addAll(path.subList(i, path.size()));
            return List.copyOf(route);
        }
        throw new IllegalArgumentException("Lane is too short for the spawn formation");
    }
}
