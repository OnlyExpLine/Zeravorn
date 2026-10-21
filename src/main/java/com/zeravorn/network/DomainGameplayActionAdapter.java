package com.zeravorn.network;

import com.zeravorn.ability.AbilityContext;
import com.zeravorn.combat.BasicAttackService;
import com.zeravorn.combat.TargetSnapshot;
import com.zeravorn.hero.AmeliaAbilityService;
import com.zeravorn.hero.EsakiAbilityService;
import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.hero.JasonAbilityService;
import com.zeravorn.hero.LokiAbilityService;
import com.zeravorn.hero.ShelianerAbilityService;
import com.zeravorn.jungle.JungleCamp;
import com.zeravorn.map.Position;
import com.zeravorn.spell.FlashEndpointValidator;
import com.zeravorn.spell.SpellService;
import com.zeravorn.spell.SummonerSpell;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * M10 dispatch into M3-M8 services. WorldQueries must derive every target,
 * distance and safe endpoint from server world state; client hints are not facts.
 */
public final class DomainGameplayActionAdapter implements GameplayActionAdapter {
    private final BasicAttackService basic;
    private final SpellService spells;
    private final JasonAbilityService jason;
    private final ShelianerAbilityService shelianer;
    private final EsakiAbilityService esaki;
    private final AmeliaAbilityService amelia;
    private final LokiAbilityService loki;
    private final WorldQueries world;

    public DomainGameplayActionAdapter(BasicAttackService basic, SpellService spells,
                                       JasonAbilityService jason, ShelianerAbilityService shelianer,
                                       EsakiAbilityService esaki, AmeliaAbilityService amelia,
                                       LokiAbilityService loki, WorldQueries world) {
        this.basic = Objects.requireNonNull(basic);
        this.spells = Objects.requireNonNull(spells);
        this.jason = Objects.requireNonNull(jason);
        this.shelianer = Objects.requireNonNull(shelianer);
        this.esaki = Objects.requireNonNull(esaki);
        this.amelia = Objects.requireNonNull(amelia);
        this.loki = Objects.requireNonNull(loki);
        this.world = Objects.requireNonNull(world);
    }

    @Override public RequestError execute(RequestValidationContext context, ClientRequest request) {
        if (request instanceof ClientRequest.BasicAttack attack) return basic(context, attack);
        if (request instanceof ClientRequest.CastAbility cast) return cast(context, cast);
        if (request instanceof ClientRequest.UseSpell spell) return spell(context, spell);
        if (request instanceof ClientRequest.Recall recall) return world.recall(context, recall.action());
        if (request instanceof ClientRequest.Lobby lobby) return world.lobby(context, lobby);
        return RequestError.INVALID_TARGET;
    }

    private RequestError basic(RequestValidationContext context, ClientRequest.BasicAttack request) {
        HeroRuntime caster = context.hero();
        if (world.handlesBasicActor(caster, request))
            return world.basicActor(caster, request, context.match().elapsedTicks());
        HeroRuntime target = world.basicTarget(caster, request);
        if (target == null) return RequestError.INVALID_TARGET;
        TargetSnapshot snapshot = world.snapshot(caster, target);
        if (snapshot == null) return RequestError.INVALID_TARGET;
        var result = basic.attack(caster, target, snapshot, world.basicRange(caster), 1.0,
                context.match().elapsedTicks(), 20);
        if (result.applied()) world.heroDamaged(caster, target, context.match().elapsedTicks());
        return result.applied() ? null : error(result.reason());
    }

    private RequestError cast(RequestValidationContext context, ClientRequest.CastAbility request) {
        HeroRuntime caster = context.hero();
        String id = request.abilityId();
        var abilityContext = new AbilityContext(context.match().elapsedTicks(), true);
        HeroRuntime target = world.abilityTarget(caster, request);
        TargetSnapshot targetSnapshot = target == null ? null : world.snapshot(caster, target);
        List<TargetSnapshot> targets = world.areaTargets(caster, request);
        Map<UUID, HeroRuntime> heroes = world.heroes();
        Map<UUID, Integer> before = new java.util.HashMap<>();
        heroes.forEach((uuid, hero) -> before.put(uuid, hero.health()));
        RequestError result = switch (id) {
            case "zeravorn:jason_q" -> checkJasonQ(caster, target, targetSnapshot, abilityContext);
            case "zeravorn:jason_e" -> world.jasonDash(context, request, jason, abilityContext);
            case "zeravorn:jason_r" -> checkJasonR(caster, targets, heroes, abilityContext);
            case "zeravorn:shelianer_q" -> checkShelianerQ(caster, target, targetSnapshot, abilityContext);
            case "zeravorn:shelianer_e" -> checkShelianerE(caster, target, targetSnapshot, abilityContext);
            case "zeravorn:shelianer_f" -> checkShelianerF(caster, abilityContext);
            case "zeravorn:shelianer_r" -> checkShelianerR(caster, target, targetSnapshot, abilityContext);
            case "zeravorn:esaki_q" -> checkEsakiQ(caster, target, targetSnapshot, abilityContext);
            case "zeravorn:esaki_e" -> checkEsakiE(caster, targets, heroes, abilityContext);
            case "zeravorn:esaki_r" -> checkEsakiR(caster, targets, heroes, abilityContext);
            case "zeravorn:amelia_q" -> checkAmeliaQ(caster, targets, heroes, abilityContext);
            case "zeravorn:amelia_e" -> checkAmeliaE(caster, targets, heroes, abilityContext);
            case "zeravorn:amelia_r" -> checkAmeliaR(caster, targets, heroes, abilityContext);
            case "zeravorn:loki_q" -> checkLokiQ(caster, target, targetSnapshot, abilityContext);
            case "zeravorn:loki_e" -> checkLokiE(caster, abilityContext);
            case "zeravorn:loki_r" -> checkLokiR(caster, targets, heroes, abilityContext);
            default -> RequestError.INVALID_ABILITY;
        };
        if (result == null && id.startsWith("zeravorn:amelia_"))
            world.ameliaActors(caster, request, context.match().elapsedTicks());
        if (result == null) heroes.forEach((uuid, hero) -> {
            if (hero.health() < before.getOrDefault(uuid, hero.health())) world.heroDamaged(caster, hero, context.match().elapsedTicks());
        });
        return result;
    }

    private RequestError checkJasonR(HeroRuntime c, List<TargetSnapshot> t, Map<UUID, HeroRuntime> h, AbilityContext a) {
        var r = jason.castR(c, t, h, a); return check(r.executed(), r.reason());
    }
    private RequestError checkJasonQ(HeroRuntime c, HeroRuntime t, TargetSnapshot s, AbilityContext a) {
        var r = jason.castQ(c, t, s, a); return check(r.executed(), r.reason());
    }
    private RequestError checkShelianerQ(HeroRuntime c, HeroRuntime t, TargetSnapshot s, AbilityContext a) {
        var r = shelianer.castQ(c, t, s, a); return check(r.executed(), r.reason());
    }
    private RequestError checkShelianerE(HeroRuntime c, HeroRuntime t, TargetSnapshot s, AbilityContext a) {
        var r = shelianer.castE(c, t, s, a); return check(r.executed(), r.reason());
    }
    private RequestError checkShelianerF(HeroRuntime c, AbilityContext a) {
        var r = shelianer.castF(c, a); return check(r.executed(), r.reason());
    }
    private RequestError checkShelianerR(HeroRuntime c, HeroRuntime t, TargetSnapshot s, AbilityContext a) {
        var r = shelianer.castR(c, t, s, a); return check(r.executed(), r.reason());
    }
    private RequestError checkEsakiQ(HeroRuntime c, HeroRuntime t, TargetSnapshot s, AbilityContext a) {
        var r = esaki.castQ(c, t, s, a); return check(r.executed(), r.reason());
    }
    private RequestError checkEsakiE(HeroRuntime c, List<TargetSnapshot> t, Map<UUID, HeroRuntime> h, AbilityContext a) {
        var r = esaki.castE(c, t, h, a); return check(r.executed(), r.reason());
    }
    private RequestError checkEsakiR(HeroRuntime c, List<TargetSnapshot> t, Map<UUID, HeroRuntime> h, AbilityContext a) {
        var r = esaki.castR(c, t, h, a); return check(r.executed(), r.reason());
    }
    private RequestError checkAmeliaQ(HeroRuntime c, List<TargetSnapshot> t, Map<UUID, HeroRuntime> h, AbilityContext a) {
        var r = amelia.castQ(c, t, h, a); return check(r.executed(), r.reason());
    }
    private RequestError checkAmeliaE(HeroRuntime c, List<TargetSnapshot> t, Map<UUID, HeroRuntime> h, AbilityContext a) {
        var r = amelia.castE(c, t, h, a); return check(r.executed(), r.reason());
    }
    private RequestError checkAmeliaR(HeroRuntime c, List<TargetSnapshot> t, Map<UUID, HeroRuntime> h, AbilityContext a) {
        var r = amelia.castR(c, t, h, a); return check(r.executed(), r.reason());
    }
    private RequestError checkLokiQ(HeroRuntime c, HeroRuntime t, TargetSnapshot s, AbilityContext a) {
        var r = loki.castQ(c, t, s, a); return check(r.executed(), r.reason());
    }
    private RequestError checkLokiE(HeroRuntime c, AbilityContext a) {
        var r = loki.castE(c, a); return check(r.executed(), r.reason());
    }
    private RequestError checkLokiR(HeroRuntime c, List<TargetSnapshot> t, Map<UUID, HeroRuntime> h, AbilityContext a) {
        var r = loki.castR(c, t, h, a); return check(r.executed(), r.reason());
    }

    private RequestError spell(RequestValidationContext context, ClientRequest.UseSpell request) {
        HeroRuntime caster = context.hero();
        long tick = context.match().elapsedTicks();
        var result = switch (request.spell()) {
            case FLASH -> {
                Position from = world.position(caster);
                Position destination = world.flashDestination(caster, request);
                FlashEndpointValidator endpoint = world.flashEndpointValidator(caster);
                yield from == null || destination == null || endpoint == null ? null
                        : spells.flash(caster, context.match().state(), from, destination, endpoint, tick);
            }
            case RETRIBUTION -> spells.retribution(caster, context.match().state(),
                    context.assignedJungler(), world.retributionCamp(caster, request.targetId()), tick);
            case CLEANSE -> spells.cleanse(caster, context.match().state(), tick);
            case REGENERATION -> spells.regeneration(caster, context.match().state(), tick);
        };
        if (result != null && result.accepted() && request.spell() == SummonerSpell.FLASH
                && result.destination() != null && !world.moveHero(caster, result.destination()))
            return RequestError.SERVER_ERROR;
        return result == null ? RequestError.INVALID_FLASH_ENDPOINT
                : result.accepted() ? null : error(result.errorCode());
    }

    private static RequestError check(boolean executed, String reason) {
        return executed ? null : error(reason);
    }

    private static RequestError error(String reason) {
        if (reason == null || reason.isBlank()) return RequestError.SERVER_ERROR;
        try { return RequestError.valueOf(reason); }
        catch (IllegalArgumentException ignored) { return RequestError.SERVER_ERROR; }
    }

    public interface WorldQueries {
        default void heroDamaged(HeroRuntime caster, HeroRuntime target, long tick) { }
        default boolean handlesBasicActor(HeroRuntime caster, ClientRequest.BasicAttack request) { return false; }
        default RequestError basicActor(HeroRuntime caster, ClientRequest.BasicAttack request, long tick) {
            return RequestError.INVALID_TARGET;
        }
        default void ameliaActors(HeroRuntime caster, ClientRequest.CastAbility request, long tick) { }
        HeroRuntime basicTarget(HeroRuntime caster, ClientRequest.BasicAttack request);
        double basicRange(HeroRuntime caster);
        HeroRuntime abilityTarget(HeroRuntime caster, ClientRequest.CastAbility request);
        TargetSnapshot snapshot(HeroRuntime caster, HeroRuntime target);
        List<TargetSnapshot> areaTargets(HeroRuntime caster, ClientRequest.CastAbility request);
        Map<UUID, HeroRuntime> heroes();
        RequestError jasonDash(RequestValidationContext context, ClientRequest.CastAbility request,
                               JasonAbilityService service, AbilityContext abilityContext);
        Position position(HeroRuntime hero);
        Position flashDestination(HeroRuntime hero, ClientRequest.UseSpell request);
        FlashEndpointValidator flashEndpointValidator(HeroRuntime hero);
        default boolean moveHero(HeroRuntime hero, Position destination) { return false; }
        JungleCamp retributionCamp(HeroRuntime hero, UUID targetHint);
        RequestError recall(RequestValidationContext context, ClientRequest.RecallAction action);
        RequestError lobby(RequestValidationContext context, ClientRequest.Lobby request);
    }
}
