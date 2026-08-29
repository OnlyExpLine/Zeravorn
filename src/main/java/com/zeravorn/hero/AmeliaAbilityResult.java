package com.zeravorn.hero;

public record AmeliaAbilityResult(boolean executed, String reason, int damage) {
    public static AmeliaAbilityResult rejected(String reason) { return new AmeliaAbilityResult(false, reason, 0); }
}
