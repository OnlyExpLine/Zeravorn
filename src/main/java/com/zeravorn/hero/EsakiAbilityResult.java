package com.zeravorn.hero;

public record EsakiAbilityResult(boolean executed, String reason, int damage) {
    public static EsakiAbilityResult rejected(String reason) { return new EsakiAbilityResult(false, reason, 0); }
}
