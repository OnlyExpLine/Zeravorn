package com.zeravorn.minion;

/** Balance-backed immutable definition. */
public record LaneMinionDefinition(MinionType type, int maxHealth, int damage,
                                    double attackIntervalSeconds, double range, int gold, int experience) {
    public LaneMinionDefinition {
        if (maxHealth <= 0 || damage <= 0 || attackIntervalSeconds <= 0 || range <= 0 || gold < 0 || experience < 0)
            throw new IllegalArgumentException("Invalid minion definition");
    }
    public static LaneMinionDefinition defaults(MinionType type) { return LaneBalance.definition(type); }
}
