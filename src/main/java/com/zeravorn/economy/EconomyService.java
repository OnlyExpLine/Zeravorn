package com.zeravorn.economy;

import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.minion.LaneMinionDefinition;
import java.util.Collection;
import java.util.Objects;

/** Server-owned reward rules from Balance v1.1. Callers provide only authoritative participants. */
public final class EconomyService {
    public static final int HERO_KILL_GOLD = 300;
    public static final int HERO_KILL_XP_BASE = 160;
    public static final int HERO_KILL_XP_PER_VICTIM_LEVEL = 20;
    public static final int ASSIST_GOLD = 100;
    public static final int TOWER_TEAM_GOLD = 150;
    public static final int TOWER_TEAM_XP = 100;
    public static final int TOWER_LAST_HIT_GOLD = 100;
    private final GoldService gold = new GoldService();
    private final ExperienceService experience = new ExperienceService();

    public void heroKill(HeroRuntime killer, HeroRuntime victim) {
        Objects.requireNonNull(killer); Objects.requireNonNull(victim);
        gold.grant(killer, HERO_KILL_GOLD, GoldReason.KILL);
        experience.grant(killer, HERO_KILL_XP_BASE + victim.level() * HERO_KILL_XP_PER_VICTIM_LEVEL, ExperienceReason.KILL);
    }
    public void heroAssist(HeroRuntime assistant, HeroRuntime victim) {
        Objects.requireNonNull(assistant); Objects.requireNonNull(victim);
        gold.grant(assistant, ASSIST_GOLD, GoldReason.ASSIST);
        experience.grant(assistant, (HERO_KILL_XP_BASE + victim.level() * HERO_KILL_XP_PER_VICTIM_LEVEL) / 2, ExperienceReason.ASSIST);
    }
    public void towerDestroyed(Collection<HeroRuntime> team, HeroRuntime lastHitter) {
        for (HeroRuntime hero : team) { gold.grant(hero, TOWER_TEAM_GOLD, GoldReason.TOWER); experience.grant(hero, TOWER_TEAM_XP, ExperienceReason.LANE); }
        if (lastHitter != null) gold.grant(lastHitter, TOWER_LAST_HIT_GOLD, GoldReason.TOWER);
    }
    public void laneMinionKilled(LaneMinionDefinition minion, HeroRuntime lastHitter, Collection<HeroRuntime> heroesInXpRange) {
        Objects.requireNonNull(minion);
        if (lastHitter != null) gold.grant(lastHitter, minion.gold(), GoldReason.LANE_LAST_HIT);
        int count = heroesInXpRange.size();
        if (count == 0) return;
        double share = count == 1 ? 1.0 : count == 2 ? .80 : .60;
        for (HeroRuntime hero : heroesInXpRange) experience.grant(hero, (int) Math.floor(minion.experience() * share), ExperienceReason.LANE);
    }
    public void laneProximityGold(LaneMinionDefinition minion, Collection<HeroRuntime> nearbyAllies, HeroRuntime lastHitter) {
        for (HeroRuntime hero : nearbyAllies) if (hero != lastHitter) gold.grant(hero, minion.gold() / 4, GoldReason.LANE_PROXIMITY);
    }
}
