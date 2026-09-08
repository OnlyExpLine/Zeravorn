package com.zeravorn.hero;

import com.zeravorn.ability.AbilityCastValidator;
import com.zeravorn.ability.AbilityContext;
import com.zeravorn.ability.AbilityEvent;
import com.zeravorn.ability.AbilitySlot;
import com.zeravorn.combat.CooldownService;
import com.zeravorn.combat.CrowdControlEffect;
import com.zeravorn.combat.CrowdControlService;
import com.zeravorn.combat.CrowdControlType;
import com.zeravorn.combat.DamageInstance;
import com.zeravorn.combat.DamageService;
import com.zeravorn.combat.DamageType;
import com.zeravorn.combat.ManaService;
import com.zeravorn.combat.TargetSnapshot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.zeravorn.buff.BuffService;

/** Server-authoritative Amelia ability resolution. All damage remains independent from client animation. */
public final class AmeliaAbilityService {
    private final CooldownService cooldowns; private final ManaService mana = new ManaService(); private final DamageService damage;
    private final CrowdControlService crowdControl; private final AbilityCastValidator validator; private final List<AbilityEvent> events = new ArrayList<>(); private final List<FrostBreath> frostBreaths = new ArrayList<>(); private final List<IceSpikes> iceSpikes = new ArrayList<>(); private final BuffService buffs;
    public AmeliaAbilityService(CooldownService cooldowns, DamageService damage, CrowdControlService crowdControl) { this(cooldowns, damage, crowdControl, null); }
    public AmeliaAbilityService(CooldownService cooldowns, DamageService damage, CrowdControlService crowdControl, BuffService buffs) { this.cooldowns = cooldowns; this.damage = damage; this.crowdControl = crowdControl; this.buffs = buffs; this.validator = new AbilityCastValidator(crowdControl); }
    public AmeliaAbilityResult castQ(HeroRuntime caster, List<TargetSnapshot> snapshots, Map<UUID, HeroRuntime> targets, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.Q, context); if (!error.isEmpty()) return AmeliaAbilityResult.rejected(error);
        var config = AmeliaAbilityDefinitions.q(caster.abilityRank(AbilitySlot.Q)); if (!pay(caster, "amelia_q", config.cooldownTicks(), config.mana(), context.serverTick())) return rejected(caster, "amelia_q", context.serverTick());
        Set<UUID> targetIds = new HashSet<>();
        for (TargetSnapshot snapshot : snapshots) { HeroRuntime target = targets.get(snapshot.id()); if (enemy(caster, target, snapshot, config.range())) targetIds.add(target.owner()); }
        frostBreaths.add(new FrostBreath(caster.owner(), Set.copyOf(targetIds), context.serverTick(), config.ticks(), config.damagePerTick() + scaledAp(caster, config.apRatio()), config.slow()));
        events.add(new AbilityEvent(caster.owner(), AbilitySlot.Q, AbilityEvent.Phase.START, context.serverTick()));
        return new AmeliaAbilityResult(true, "FROST_BREATH", 0);
    }
    public AmeliaAbilityResult castE(HeroRuntime caster, List<TargetSnapshot> snapshots, Map<UUID, HeroRuntime> targets, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.E, context); if (!error.isEmpty()) return AmeliaAbilityResult.rejected(error);
        var config = AmeliaAbilityDefinitions.e(caster.abilityRank(AbilitySlot.E)); if (!pay(caster, "amelia_e", config.cooldownTicks(), config.mana(), context.serverTick())) return rejected(caster, "amelia_e", context.serverTick());
        int total = 0, amount = config.damage() + scaledAp(caster, config.apRatio()); events.add(new AbilityEvent(caster.owner(), AbilitySlot.E, AbilityEvent.Phase.START, context.serverTick()));
        for (TargetSnapshot snapshot : snapshots) { HeroRuntime target = targets.get(snapshot.id()); if (!enemy(caster, target, snapshot, config.radius())) continue; total += hit(caster, target, AbilitySlot.E, amount, context.serverTick()).damage(); crowdControl.apply(target.owner(), new CrowdControlEffect(caster.owner(), CrowdControlType.KNOCKBACK, config.knockback(), context.serverTick() + 7, false)); }
        return new AmeliaAbilityResult(true, "BLIZZARD_BURST", total);
    }
    /** Resolves the documented 0.55-second ground-target delay after the server accepts the target area. */
    public AmeliaAbilityResult castR(HeroRuntime caster, List<TargetSnapshot> snapshots, Map<UUID, HeroRuntime> targets, AbilityContext context) {
        String error = validator.validate(caster, AbilitySlot.R, context); if (!error.isEmpty()) return AmeliaAbilityResult.rejected(error);
        var config = AmeliaAbilityDefinitions.r(caster.abilityRank(AbilitySlot.R)); if (!pay(caster, "amelia_r", config.cooldownTicks(), config.mana(), context.serverTick())) return rejected(caster, "amelia_r", context.serverTick());
        Set<UUID> targetIds = new HashSet<>();
        for (TargetSnapshot snapshot : snapshots) { HeroRuntime target = targets.get(snapshot.id()); if (enemy(caster, target, snapshot, config.radius())) targetIds.add(target.owner()); }
        iceSpikes.add(new IceSpikes(caster.owner(), Set.copyOf(targetIds), context.serverTick() + config.delayTicks(), config.damage() + scaledAp(caster, config.apRatio())));
        events.add(new AbilityEvent(caster.owner(), AbilitySlot.R, AbilityEvent.Phase.START, context.serverTick()));
        return new AmeliaAbilityResult(true, "ICE_SPIKES", 0);
    }
    /** Resolves channel and delayed ground effects only while the match is active. */
    public void tick(long serverTick, boolean matchPlaying, Map<UUID, HeroRuntime> heroes) {
        frostBreaths.removeIf(effect -> {
            HeroRuntime caster = heroes.get(effect.caster());
            if (!matchPlaying || caster == null || !caster.alive()) return true;
            if (serverTick < effect.nextTick()) return false;
            for (UUID targetId : effect.targetIds()) {
                HeroRuntime target = heroes.get(targetId);
                if (target != null && target.alive() && target.team() != caster.team()) {
                    hit(caster, target, AbilitySlot.Q, effect.damagePerTick(), serverTick);
                    crowdControl.apply(target.owner(), new CrowdControlEffect(caster.owner(), CrowdControlType.SLOW, effect.slow(), serverTick + 10L, true));
                }
            }
            effect.advance(); return effect.ticksRemaining() == 0;
        });
        iceSpikes.removeIf(effect -> {
            if (!matchPlaying || serverTick < effect.hitTick()) return !matchPlaying;
            HeroRuntime caster = heroes.get(effect.caster());
            if (caster != null && caster.alive()) for (UUID targetId : effect.targetIds()) {
                HeroRuntime target = heroes.get(targetId);
                if (target != null && target.alive() && target.team() != caster.team()) hit(caster, target, AbilitySlot.R, effect.damage(), serverTick);
            }
            return true;
        });
    }
    public List<AbilityEvent> events() { return List.copyOf(events); }
    private boolean pay(HeroRuntime caster, String action, long cooldown, int manaCost, long tick) { if (!cooldowns.ready(caster.owner(), action, tick) || !mana.trySpend(caster, manaCost)) return false; cooldowns.tryStart(caster.owner(), action, tick, buffs == null ? cooldown : buffs.heroAbilityCooldownTicks(caster.owner(), cooldown, tick)); return true; }
    private AmeliaAbilityResult rejected(HeroRuntime caster, String action, long tick) { return AmeliaAbilityResult.rejected(cooldowns.ready(caster.owner(), action, tick) ? "NO_MANA" : "COOLDOWN"); }
    private AmeliaAbilityResult hit(HeroRuntime caster, HeroRuntime target, AbilitySlot slot, int amount, long tick) { var result = damage.applyAbility(caster, target, new DamageInstance(UUID.randomUUID(), caster.owner(), target.owner(), DamageType.MAGICAL, amount, tick)); if (!result.applied()) return AmeliaAbilityResult.rejected(result.reason()); events.add(new AbilityEvent(caster.owner(), slot, AbilityEvent.Phase.HIT, tick)); return new AmeliaAbilityResult(true, "", result.amount()); }
    private static int scaledAp(HeroRuntime hero, double ratio) { return (int) Math.floor(hero.effectiveStats().abilityPower() * ratio); }
    private static boolean enemy(HeroRuntime caster, HeroRuntime target, TargetSnapshot snapshot, double range) { return target != null && snapshot != null && target.owner().equals(snapshot.id()) && target.alive() && snapshot.alive() && caster.team() != target.team() && snapshot.distance() <= range; }
    private static final class FrostBreath {
        private final UUID caster; private final Set<UUID> targetIds; private long nextTick; private int ticksRemaining; private final int damagePerTick; private final double slow;
        private FrostBreath(UUID caster, Set<UUID> targetIds, long nextTick, int ticksRemaining, int damagePerTick, double slow) { this.caster = caster; this.targetIds = targetIds; this.nextTick = nextTick; this.ticksRemaining = ticksRemaining; this.damagePerTick = damagePerTick; this.slow = slow; }
        UUID caster() { return caster; } Set<UUID> targetIds() { return targetIds; } long nextTick() { return nextTick; } int ticksRemaining() { return ticksRemaining; } int damagePerTick() { return damagePerTick; } double slow() { return slow; }
        void advance() { nextTick += 10L; ticksRemaining--; }
    }
    private record IceSpikes(UUID caster, Set<UUID> targetIds, long hitTick, int damage) { }
}
