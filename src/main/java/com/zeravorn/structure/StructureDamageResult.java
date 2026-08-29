package com.zeravorn.structure;

public record StructureDamageResult(boolean accepted, int appliedDamage, boolean destroyed, String reason) {
    public static StructureDamageResult rejected(String reason, int hp) { return new StructureDamageResult(false, 0, hp <= 0, reason); }
}
