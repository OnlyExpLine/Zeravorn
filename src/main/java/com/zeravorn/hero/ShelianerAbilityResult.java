package com.zeravorn.hero;

public record ShelianerAbilityResult(boolean executed, String reason, int damage) {
    public static ShelianerAbilityResult rejected(String reason) { return new ShelianerAbilityResult(false, reason, 0); }
}
