package com.zeravorn.bot;

import com.zeravorn.map.Position;
import com.zeravorn.minion.Lane;

import java.util.List;
import java.util.Objects;

/** Map-supplied safe waypoints; M13 binds these to validated arena paths. */
public record BotRouteProfile(Lane lane, List<Position> waypoints) {
    public BotRouteProfile {
        Objects.requireNonNull(lane);
        waypoints = List.copyOf(waypoints);
        if (waypoints.isEmpty()) throw new IllegalArgumentException("Empty bot route");
        if (waypoints.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("Null waypoint");
    }
}
