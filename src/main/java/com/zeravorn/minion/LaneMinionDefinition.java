package com.zeravorn.minion;

/** Balance-backed immutable definition; values are the v1.1 defaults. */
public record LaneMinionDefinition(MinionType type, int maxHealth, int damage,
                                    double attackIntervalSeconds, double range, int gold, int experience) {
    public LaneMinionDefinition {
        if (maxHealth <= 0 || damage <= 0 || attackIntervalSeconds <= 0 || range <= 0 || gold < 0 || experience < 0)
            throw new IllegalArgumentException("Invalid minion definition");
    }
    public static LaneMinionDefinition defaults(MinionType type) {
        return switch (type) {
            case MELEE -> new LaneMinionDefinition(type, 300, 20, 1.0, 1.5, 24, 24);
            case RANGED -> new LaneMinionDefinition(type, 200, 25, 1.0, 6.0, 20, 20);
            case SIEGE -> new LaneMinionDefinition(type, 550, 35, 2.0, 7.0, 50, 40);
        };
    }
}
