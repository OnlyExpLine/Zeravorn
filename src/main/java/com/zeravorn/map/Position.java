package com.zeravorn.map;

/** Lightweight, engine-independent map position used by gameplay contracts. */
public record Position(double x, double y, double z) {
    public double distanceSquared(Position other) {
        double dx = x - other.x, dy = y - other.y, dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }
}
