package com.zeravorn.spell;

import com.zeravorn.combat.CooldownService;
import com.zeravorn.combat.CrowdControlService;
import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.jungle.JungleCamp;
import com.zeravorn.map.Position;
import com.zeravorn.match.MatchState;
import java.util.Map;
import java.util.Objects;

/** Validates and applies server-authoritative summoner spell effects. */
public final class SpellService {
    private final Map<SummonerSpell, SummonerSpellDefinition> definitions;
    private final CooldownService cooldowns; private final CrowdControlService crowdControl;
    public SpellService(Map<SummonerSpell, SummonerSpellDefinition> definitions, CooldownService cooldowns, CrowdControlService crowdControl) { this.definitions = Map.copyOf(definitions); this.cooldowns = Objects.requireNonNull(cooldowns); this.crowdControl = Objects.requireNonNull(crowdControl); }
    public SpellResult flash(HeroRuntime hero, MatchState state, Position from, Position destination, FlashEndpointValidator endpointValidator, long tick) {
        SpellResult check = check(hero, state, SummonerSpell.FLASH, tick, true); if (!check.accepted()) return check;
        double range = definitions.get(SummonerSpell.FLASH).rangeOrPercent();
        if (from.distanceSquared(destination) > range * range || !endpointValidator.isSafeEndpoint(destination)) return SpellResult.rejected("OUTSIDE_BOUNDARY");
        start(hero, SummonerSpell.FLASH, tick); return SpellResult.success(destination, 0);
    }
    public SpellResult retribution(HeroRuntime hero, MatchState state, boolean assignedJungler, JungleCamp camp, long tick) {
        SpellResult check = check(hero, state, SummonerSpell.RETRIBUTION, tick, false); if (!check.accepted()) return check;
        if (!assignedJungler || camp == null || !camp.alive()) return SpellResult.rejected("INVALID_TARGET");
        int damage = (int) definitions.get(SummonerSpell.RETRIBUTION).rangeOrPercent() + 50 * hero.level();
        camp.damage(damage, tick); start(hero, SummonerSpell.RETRIBUTION, tick); return SpellResult.success(null, damage);
    }
    public SpellResult cleanse(HeroRuntime hero, MatchState state, long tick) { SpellResult check = check(hero, state, SummonerSpell.CLEANSE, tick, false); if (!check.accepted()) return check; crowdControl.cleanse(hero.owner()); start(hero, SummonerSpell.CLEANSE, tick); return SpellResult.success(null, 0); }
    public SpellResult regeneration(HeroRuntime hero, MatchState state, long tick) { SpellResult check = check(hero, state, SummonerSpell.REGENERATION, tick, false); if (!check.accepted()) return check; int amount = (int) Math.floor(hero.effectiveStats().maxHealth() * definitions.get(SummonerSpell.REGENERATION).rangeOrPercent()); hero.heal(amount); start(hero, SummonerSpell.REGENERATION, tick); return SpellResult.success(null, amount); }
    private SpellResult check(HeroRuntime hero, MatchState state, SummonerSpell spell, long tick, boolean movement) {
        if (state != MatchState.PLAYING) return SpellResult.rejected("WRONG_STATE"); if (!hero.alive()) return SpellResult.rejected("DEAD");
        // Cleanse is specifically the server-authoritative exception to CC input blocking.
        if (spell != SummonerSpell.CLEANSE && crowdControl.blocksBasicOrAbility(hero.owner())) return SpellResult.rejected("STUNNED");
        if (spell != SummonerSpell.CLEANSE && movement && crowdControl.blocksMovement(hero.owner())) return SpellResult.rejected("ROOT_BLOCK");
        return cooldowns.ready(hero.owner(), spell.name(), tick) ? SpellResult.success(null, 0) : SpellResult.rejected("COOLDOWN");
    }
    private void start(HeroRuntime hero, SummonerSpell spell, long tick) { cooldowns.tryStart(hero.owner(), spell.name(), tick, definitions.get(spell).cooldownSeconds() * 20L); }
}
