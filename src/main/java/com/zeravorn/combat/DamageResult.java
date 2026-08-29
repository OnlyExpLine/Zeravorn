package com.zeravorn.combat;

public record DamageResult(boolean applied, int amount, int targetHealthAfter, boolean killed, String reason) {
    public static DamageResult rejected(String reason, int health) { return new DamageResult(false, 0, health, false, reason); }
}
