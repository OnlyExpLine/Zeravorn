package com.zeravorn.hero;

public record HeroStats(int health, double healthRegen, int maxMana, double manaRegen, int attack) {
    public HeroStats {
        if (health <= 0 || healthRegen < 0 || maxMana < 0 || manaRegen < 0 || attack < 0) {
            throw new IllegalArgumentException("Hero stats must be non-negative and health must be positive");
        }
    }
}
