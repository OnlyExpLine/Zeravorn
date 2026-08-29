package com.zeravorn.item;
public record ItemStats(int attack, int abilityPower, int maxHealth, int maxMana, double manaRegen,
                        double attackSpeed, double lifesteal, double spellVamp, double moveSpeed) {
    public ItemStats { if (attack < 0 || abilityPower < 0 || maxHealth < 0 || maxMana < 0 || manaRegen < 0 || attackSpeed < 0 || lifesteal < 0 || spellVamp < 0 || moveSpeed < 0) throw new IllegalArgumentException("Negative item stat"); }
    public static final ItemStats NONE = new ItemStats(0,0,0,0,0,0,0,0,0);
    public ItemStats plus(ItemStats o) { return new ItemStats(attack+o.attack,abilityPower+o.abilityPower,maxHealth+o.maxHealth,maxMana+o.maxMana,manaRegen+o.manaRegen,attackSpeed+o.attackSpeed,lifesteal+o.lifesteal,spellVamp+o.spellVamp,moveSpeed+o.moveSpeed); }
}
