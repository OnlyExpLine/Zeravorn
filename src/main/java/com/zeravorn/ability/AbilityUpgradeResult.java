package com.zeravorn.ability;

public record AbilityUpgradeResult(boolean upgraded, String reason, int newRank) {
    public static AbilityUpgradeResult success(int rank) { return new AbilityUpgradeResult(true, "", rank); }
    public static AbilityUpgradeResult rejected(String reason, int rank) { return new AbilityUpgradeResult(false, reason, rank); }
}
