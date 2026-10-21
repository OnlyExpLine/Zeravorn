package com.zeravorn.bot;

import com.zeravorn.map.Position;

import java.util.Comparator;

/** Small deterministic policy; no hidden target lookup or future-state prediction. */
public final class BotBrain {
    public BotDecision decide(BotPerception view, BotRouteProfile route, int waypointIndex) {
        if (!view.alive()) return new BotDecision(BotState.RESPAWN_RETURN, null, null);
        if (view.atFountain()) return new BotDecision(BotState.SPAWN_BUY, null, route.waypoints().getFirst());
        if ((long) view.health() * 10 < (long) view.maxHealth() * 3) {
            return new BotDecision(view.recallAvailable() ? BotState.RECALL : BotState.RETREAT,
                    null, route.waypoints().getFirst());
        }
        var enemy = view.visibleTargets().stream()
                .filter(t -> t.legal() && (t.kind() == BotPerception.Kind.HERO
                        || t.kind() == BotPerception.Kind.MINION))
                .filter(t -> view.position().distanceSquared(t.position()) <= 64)
                .min(Comparator.comparingDouble((BotPerception.Target t) ->
                        view.position().distanceSquared(t.position())).thenComparing(BotPerception.Target::id))
                .orElse(null);
        if (enemy != null) return new BotDecision(enemy.kind() == BotPerception.Kind.HERO
                ? BotState.FIGHT : BotState.LANE_FARM, enemy.id(), null);
        if (view.alliedWaveAtTower()) {
            var structure = view.visibleTargets().stream()
                    .filter(t -> t.legal() && (t.kind() == BotPerception.Kind.TOWER
                            || t.kind() == BotPerception.Kind.THRONE))
                    .min(Comparator.comparingDouble((BotPerception.Target t) ->
                            view.position().distanceSquared(t.position())).thenComparing(BotPerception.Target::id))
                    .orElse(null);
            if (structure != null) return new BotDecision(BotState.ATTACK_STRUCTURE, structure.id(), null);
        }
        Position waypoint = route.waypoints().get(Math.floorMod(waypointIndex, route.waypoints().size()));
        return new BotDecision(waypointIndex >= route.waypoints().size() - 1
                ? BotState.PUSH : BotState.MOVE_ROUTE, null, waypoint);
    }
}
