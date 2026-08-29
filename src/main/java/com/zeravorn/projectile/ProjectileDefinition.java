package com.zeravorn.projectile;

public record ProjectileDefinition(double speed, double maxRange, double collisionRadius, int pierceCount) {
    public ProjectileDefinition {
        if (speed <= 0 || maxRange <= 0 || collisionRadius <= 0 || pierceCount < 0) throw new IllegalArgumentException("Invalid projectile definition");
    }
}
