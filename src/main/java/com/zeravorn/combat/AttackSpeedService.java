package com.zeravorn.combat;

public final class AttackSpeedService {
    public long intervalTicks(double baseIntervalSeconds, double attackSpeedMultiplier, int ticksPerSecond) {
        if (baseIntervalSeconds <= 0 || attackSpeedMultiplier <= 0 || ticksPerSecond <= 0) throw new IllegalArgumentException("Invalid attack speed");
        return Math.max(1, Math.round(baseIntervalSeconds * ticksPerSecond / attackSpeedMultiplier));
    }
}
