package com.zeravorn.hero;

public record JasonAbilityResult(boolean executed, String reason, int damage, int healing) {
    public static JasonAbilityResult rejected(String reason) { return new JasonAbilityResult(false, reason, 0, 0); }
}
