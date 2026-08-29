package com.zeravorn.buff;

/** Server-owned timed jungle buff. */
public record TimedBuff(BuffType type, long expiresAtTick, double outgoingDamageMultiplier,
                        double maxManaMultiplier, double cooldownReduction) {
    public TimedBuff {
        if (expiresAtTick < 0 || outgoingDamageMultiplier < 1.0 || maxManaMultiplier < 1.0
                || cooldownReduction < 0 || cooldownReduction >= 1.0) {
            throw new IllegalArgumentException("Invalid timed buff");
        }
    }
}
