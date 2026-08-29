package com.zeravorn.spell;

public record SummonerSpellDefinition(SummonerSpell spell, int cooldownSeconds, double rangeOrPercent) {
    public SummonerSpellDefinition { if (cooldownSeconds <= 0 || rangeOrPercent <= 0) throw new IllegalArgumentException("Invalid spell definition"); }
}
