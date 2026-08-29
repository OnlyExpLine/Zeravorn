package com.zeravorn.jungle;

import com.zeravorn.buff.BuffType;

public record JungleMobType(String id, int health, int damage, double attackInterval, int gold, int experience,
                            int respawnSeconds, double leashRadius, BuffType rewardBuff, String specialCrowdControl) {
    public JungleMobType {
        if (id == null || id.isBlank() || health <= 0 || damage < 0 || attackInterval <= 0 || gold < 0 || experience < 0
                || respawnSeconds < 0 || leashRadius <= 0) throw new IllegalArgumentException("Invalid jungle mob definition");
    }
}
