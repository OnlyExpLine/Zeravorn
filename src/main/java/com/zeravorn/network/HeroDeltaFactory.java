package com.zeravorn.network;

import com.zeravorn.combat.CooldownService;
import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.spell.SummonerSpell;
import com.zeravorn.buff.BuffService;
import com.zeravorn.buff.BuffType;
import com.zeravorn.combat.CrowdControlService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Builds the owner's authoritative HUD state; never uses client values. */
public final class HeroDeltaFactory {
    private HeroDeltaFactory() { }

    public static ServerMessage.HeroDelta ownHero(HeroRuntime hero, CooldownService cooldowns,
                                                   long serverTick, int kills, int deaths, int assists,
                                                   SummonerSpell selectedSpell, BuffService buffs,
                                                   CrowdControlService crowdControl) {
        Objects.requireNonNull(hero);
        Objects.requireNonNull(cooldowns);
        Objects.requireNonNull(buffs);
        Objects.requireNonNull(crowdControl);
        var cooldown = new LinkedHashMap<String, Long>();
        for (var ability : hero.definition().abilities().values()) {
            String runtimeKey = ability.id().substring(ability.id().lastIndexOf(':') + 1);
            cooldown.put(ability.id(), cooldowns.remaining(hero.owner(), runtimeKey, serverTick));
        }
        cooldown.put("basic_attack", cooldowns.remaining(hero.owner(), "basic_attack", serverTick));
        if (selectedSpell != null) {
            cooldown.put(selectedSpell.name(), cooldowns.remaining(hero.owner(), selectedSpell.name(), serverTick));
        }
        var normalItems = hero.inventory().normalItems().stream().map(item -> item.id()).toList();
        var boots = hero.inventory().boots();
        var stats = hero.effectiveStats();
        var ccTicks = new LinkedHashMap<String, Long>();
        for (var effect : crowdControl.effects(hero.owner())) {
            long remaining = Math.max(0, effect.expiresAtTick() - serverTick);
            if (remaining > 0) ccTicks.merge(effect.type().name(), remaining, Math::max);
        }
        var buffTicks = new LinkedHashMap<String, Long>();
        for (BuffType type : BuffType.values()) {
            var active = buffs.get(hero.owner(), type, serverTick);
            if (active != null) buffTicks.put(type.name(), Math.max(0, active.expiresAtTick() - serverTick));
        }
        return new ServerMessage.HeroDelta(hero.owner(), hero.definition().id(), hero.team(), hero.alive(),
                hero.health(), stats.maxHealth(), hero.mana(), buffs.effectiveMaxMana(hero, serverTick),
                hero.level(), hero.experience(), hero.gold(), hero.availableSkillPoints(),
                kills, deaths, assists, hero.abilityRanks(), cooldown, normalItems,
                boots == null ? null : boots.id(), selectedSpell, stats,
                buffs.cooldownReduction(hero.owner(), serverTick), ccTicks, buffTicks);
    }
}
