package com.zeravorn.hero;

public record JasonMovementResult(boolean moved, String reason, double distance) {
    public static JasonMovementResult rejected(String reason) { return new JasonMovementResult(false, reason, 0); }
}
