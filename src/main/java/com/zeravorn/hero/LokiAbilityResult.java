package com.zeravorn.hero;

public record LokiAbilityResult(boolean executed, String reason, int damage) {
    public static LokiAbilityResult rejected(String reason) { return new LokiAbilityResult(false, reason, 0); }
}
