package com.zeravorn.vision;

import com.zeravorn.map.Position;

/** Config/map adapter shape; M13 will load actual zones from MapDefinition. */
public record BushZone(String id, Position center, double radius) {
    public BushZone { if (id == null || id.isBlank() || center == null || radius <= 0) throw new IllegalArgumentException("Invalid bush zone"); }
    public boolean contains(Position position) { return center.distanceSquared(position) <= radius * radius; }
}
