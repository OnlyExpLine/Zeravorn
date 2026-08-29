package com.zeravorn.jungle;

import com.zeravorn.buff.BuffService;
import com.zeravorn.buff.TimedBuff;
import com.zeravorn.economy.ExperienceReason;
import com.zeravorn.economy.ExperienceService;
import com.zeravorn.economy.GoldReason;
import com.zeravorn.economy.GoldService;
import com.zeravorn.hero.HeroRuntime;
import java.util.Objects;

public final class JungleService {
    public static final long FIRST_SPAWN_TICK = 35L * 20L;
    private final GoldService gold; private final ExperienceService experience; private final BuffService buffs;
    public JungleService(GoldService gold, ExperienceService experience, BuffService buffs) { this.gold = Objects.requireNonNull(gold); this.experience = Objects.requireNonNull(experience); this.buffs = Objects.requireNonNull(buffs); }
    public boolean shouldLeashReset(JungleCamp camp, com.zeravorn.map.Position mobPosition) { return mobPosition.distanceSquared(camp.spawnPoint()) > camp.definition().leashRadius() * camp.definition().leashRadius(); }
    public boolean kill(HeroRuntime killer, JungleCamp camp, long tick, long buffDurationTicks, double redDamageMultiplier, double blueMaxManaMultiplier, double blueCdr) {
        if (!camp.claimDeathReward()) return false;
        gold.grant(killer, camp.definition().gold(), GoldReason.JUNGLE); experience.grant(killer, camp.definition().experience(), ExperienceReason.JUNGLE);
        if (camp.definition().rewardBuff() != null) {
            boolean red = camp.definition().rewardBuff().name().equals("RED");
            buffs.apply(killer.owner(), new TimedBuff(camp.definition().rewardBuff(), tick + buffDurationTicks, red ? redDamageMultiplier : 1.0, red ? 1.0 : blueMaxManaMultiplier, red ? 0 : blueCdr));
        }
        return true;
    }
}
