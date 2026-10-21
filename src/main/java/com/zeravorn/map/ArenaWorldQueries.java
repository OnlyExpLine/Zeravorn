package com.zeravorn.map;

import com.zeravorn.ability.AbilityContext;
import com.zeravorn.combat.TargetSnapshot;
import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.hero.JasonAbilityService;
import com.zeravorn.jungle.JungleCamp;
import com.zeravorn.network.ClientRequest;
import com.zeravorn.network.DomainGameplayActionAdapter;
import com.zeravorn.network.RequestError;
import com.zeravorn.network.RequestValidationContext;
import com.zeravorn.spell.FlashEndpointValidator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Resolves player hero requests from live server entities, never client coordinates. */
public final class ArenaWorldQueries implements DomainGameplayActionAdapter.WorldQueries {
    private final MinecraftServer server;
    private final MapDefinition map;
    private final Map<UUID, HeroRuntime> heroes;
    private final ArenaActorRuntime actors;

    public ArenaWorldQueries(MinecraftServer server, MapDefinition map, Map<UUID, HeroRuntime> heroes) {
        this(server, map, heroes, null);
    }

    public ArenaWorldQueries(MinecraftServer server, MapDefinition map, Map<UUID, HeroRuntime> heroes,
                             ArenaActorRuntime actors) {
        this.server = server;
        this.map = map;
        this.heroes = heroes;
        this.actors = actors;
    }

    private ServerLevel level() { return server.getLevel(ArenaWorld.DIMENSION); }
    private ArenaGeometry geometry() { return new ArenaGeometry(map, level()); }
    private ServerPlayer player(HeroRuntime hero) {
        if (hero == null) return null;
        ServerPlayer player = server.getPlayerList().getPlayer(hero.owner());
        return player != null && player.level() == level() ? player : null;
    }

    private boolean targetable(HeroRuntime caster, HeroRuntime target) {
        if (caster == null || target == null || !target.alive()) return false;
        Position from = position(caster), to = position(target);
        if (from == null || to == null || !geometry().hasLineOfSight(from, to)) return false;
        String bush = geometry().bushAt(to);
        return target.team() == caster.team() || bush == null || bush.equals(geometry().bushAt(from));
    }

    @Override public HeroRuntime basicTarget(HeroRuntime caster, ClientRequest.BasicAttack request) {
        HeroRuntime target = heroes.get(request.targetHint());
        return targetable(caster, target) && target.team() != caster.team() ? target : null;
    }

    @Override public boolean handlesBasicActor(HeroRuntime caster, ClientRequest.BasicAttack request) {
        ServerPlayer player = player(caster);
        return player != null && actors != null && actors.findTarget(player, caster.team(), request.targetHint(),
                request.aimDirection(), basicRange(caster)) != null;
    }

    @Override public RequestError basicActor(HeroRuntime caster, ClientRequest.BasicAttack request, long tick) {
        ServerPlayer player = player(caster);
        if (player == null || actors == null) return RequestError.INVALID_TARGET;
        ArenaActorEntity target = actors.findTarget(player, caster.team(), request.targetHint(), request.aimDirection(),
                basicRange(caster));
        if (target == null) return RequestError.INVALID_TARGET;
        return actors.attack(player, caster, target, tick, basicRange(caster));
    }

    @Override public void ameliaActors(HeroRuntime caster, ClientRequest.CastAbility request, long tick) {
        ServerPlayer player = player(caster);
        if (player != null && actors != null) actors.ameliaAbility(player, caster, request, tick);
    }

    @Override public void heroDamaged(HeroRuntime caster, HeroRuntime target, long tick) {
        if (actors != null && caster.team() != target.team()) actors.recordAggression(caster.owner(), target.team(), tick);
    }

    @Override public double basicRange(HeroRuntime caster) {
        return caster.definition().heroClass() == com.zeravorn.hero.HeroClass.MARKSMAN
                || caster.definition().heroClass() == com.zeravorn.hero.HeroClass.MAGE ? 7 : 3;
    }

    @Override public HeroRuntime abilityTarget(HeroRuntime caster, ClientRequest.CastAbility request) {
        HeroRuntime target = heroes.get(request.targetId());
        return targetable(caster, target) ? target : null;
    }

    @Override public TargetSnapshot snapshot(HeroRuntime caster, HeroRuntime target) {
        Position from = position(caster), to = position(target);
        if (!targetable(caster, target) || from == null || to == null) return null;
        return new TargetSnapshot(target.owner(), target.team(), target.alive(),
                Math.sqrt(from.distanceSquared(to)));
    }

    @Override public List<TargetSnapshot> areaTargets(HeroRuntime caster, ClientRequest.CastAbility request) {
        List<TargetSnapshot> result = new ArrayList<>();
        Position from = position(caster);
        if (from == null) return result;
        for (HeroRuntime target : heroes.values()) {
            if (target == caster) continue;
            Position to = position(target);
            if (to != null && from.distanceSquared(to) <= 100) {
                TargetSnapshot snapshot = snapshot(caster, target);
                if (snapshot != null) result.add(snapshot);
            }
        }
        return List.copyOf(result);
    }

    @Override public Map<UUID, HeroRuntime> heroes() { return Map.copyOf(heroes); }

    @Override public RequestError jasonDash(RequestValidationContext context, ClientRequest.CastAbility request,
                                            JasonAbilityService service, AbilityContext abilityContext) {
        Position from = position(context.hero()), direction = request.aimDirection();
        if (from == null || direction == null) return RequestError.INVALID_TARGET;
        double length = Math.sqrt(direction.distanceSquared(new Position(0, 0, 0)));
        if (!Double.isFinite(length) || length == 0) return RequestError.INVALID_TARGET;
        Position destination = new Position(from.x() + 2 * direction.x() / length, from.y(),
                from.z() + 2 * direction.z() / length);
        if (!geometry().safeEndpoint(destination) || !geometry().hasLineOfSight(from, destination))
            return RequestError.OUTSIDE_BOUNDARY;
        var result = service.beginE(context.hero(), abilityContext);
        if (!result.executed()) {
            try { return RequestError.valueOf(result.reason()); }
            catch (IllegalArgumentException ignored) { return RequestError.SERVER_ERROR; }
        }
        return moveHero(context.hero(), destination) ? null : RequestError.SERVER_ERROR;
    }

    @Override public Position position(HeroRuntime hero) {
        ServerPlayer player = player(hero);
        return player == null ? null : new Position(player.getX(), player.getY(), player.getZ());
    }

    @Override public Position flashDestination(HeroRuntime hero, ClientRequest.UseSpell request) {
        return request.groundPoint();
    }

    @Override public FlashEndpointValidator flashEndpointValidator(HeroRuntime hero) {
        return endpoint -> geometry().safeEndpoint(endpoint);
    }

    @Override public boolean moveHero(HeroRuntime hero, Position destination) {
        ServerPlayer player = player(hero);
        return player != null && geometry().safeEndpoint(destination)
                && player.teleportTo(level(), destination.x(), destination.y(), destination.z(),
                Set.<Relative>of(), player.getYRot(), player.getXRot(), true);
    }

    @Override public JungleCamp retributionCamp(HeroRuntime hero, UUID targetHint) { return null; }
    @Override public RequestError recall(RequestValidationContext context, ClientRequest.RecallAction action) {
        return RequestError.WRONG_STATE; // M14 owns interrupted recall and fountain channel.
    }
    @Override public RequestError lobby(RequestValidationContext context, ClientRequest.Lobby request) {
        return RequestError.WRONG_STATE;
    }
}
