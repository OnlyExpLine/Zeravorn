package com.zeravorn.map;

import com.zeravorn.economy.EconomyService;
import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.match.MatchSession;
import com.zeravorn.match.MatchState;
import com.zeravorn.minion.Lane;
import com.zeravorn.network.RequestError;
import com.zeravorn.network.ServerMessage;
import com.zeravorn.structure.*;
import com.zeravorn.team.TeamId;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Connects map positions, structure rules, actor models and server-authoritative attacks. */
public final class ArenaStructureRuntime {
    private final MapDefinition map;
    private final MatchSession match;
    private final StructureService rules;
    private final ArenaActorRuntime actors;
    private final EconomyService economy = new EconomyService();
    private final FountainService fountainRules = new FountainService();
    private final List<Node> nodes = new ArrayList<>();
    private final Map<TeamId, ArenaActorEntity> fountains = new EnumMap<>(TeamId.class);
    private Map<UUID, HeroRuntime> heroes = Map.of();
    private boolean prepared;

    public ArenaStructureRuntime(MapDefinition map, MatchSession match, ArenaActorRuntime actors) {
        this.map = map; this.match = match; this.actors = actors; rules = new StructureService(match);
        for (var point : map.towers()) nodes.add(new Node(point.id(), point.team(), point.lane(), point.order(), point.position(), point.hitboxRadius()));
        for (TeamId team : TeamId.values()) nodes.add(new Node(team + "_THRONE", team, null, null, map.thrones().get(team), 2.5));
    }

    public void prepare(ServerLevel level, Map<UUID, HeroRuntime> heroes) {
        this.heroes = heroes;
        if (prepared) return;
        prepared = true;
        for (Node n : nodes) {
            n.entity = spawn(level, n.position, n.order == null ? "throne_" + color(n.team)
                    : "tower_" + n.order.name().toLowerCase(Locale.ROOT) + "_" + color(n.team), false);
            n.entity.setYRot(n.team == TeamId.BLUE ? -45 : 135);
            n.entity.setAimYaw(n.entity.getYRot());
            n.entity.setStructureProtected(!n.vulnerable());
            n.entity.setHealthDisplay(n.health(), n.maximum());
            n.entity.setAnimation(n.order == null && !n.vulnerable() ? "protected" : "idle", true);
        }
        for (TeamId team : TeamId.values()) fountains.put(team, spawn(level, map.fountains().get(team).center(), "fountain_" + color(team), true));
    }

    static ArenaActorEntity spawn(ServerLevel level, Position p, String visual, boolean marker) {
        var e = new ArenaActorEntity(ArenaActorEntity.TYPE, level);
        e.setPresentationOnly(marker); e.setVisual(visual); e.setAnimation("idle", true);
        e.setPos(p.x() + .5, p.y(), p.z() + .5); level.addFreshEntity(e); return e;
    }
    private static String color(TeamId t) { return t.name().toLowerCase(Locale.ROOT); }
    public Node node(ArenaActorEntity entity) { return nodes.stream().filter(n -> n.entity == entity).findFirst().orElse(null); }
    public boolean active() { return match == null || match.state() == MatchState.PLAYING; }
    public Node nextTarget(TeamId attacker, Lane lane, Position from) {
        return nodes.stream().filter(n -> n.team != attacker && (n.lane == lane || n.order == null)
                && n.health() > 0 && n.vulnerable() && horizontal(from, n.position) <= 9)
                .min(Comparator.comparingDouble(n -> horizontal(from, n.position))).orElse(null);
    }
    public List<Node> nodes() { return List.copyOf(nodes); }
    public List<ServerMessage.StructureState> snapshots() {
        return nodes.stream().map(n -> new ServerMessage.StructureState(n.id, n.health(), n.maximum(), n.health() == 0, n.target)).toList();
    }

    public RequestError attack(HeroRuntime hero, Node node, long tick) {
        if (!active()) return RequestError.WRONG_STATE;
        if (node.team == hero.team() || node.health() <= 0) return RequestError.INVALID_TARGET;
        if (!node.vulnerable()) return RequestError.STRUCTURE_PROTECTED;
        int amount = hero.effectiveStats().attack();
        damage(node, hero.team(), amount, alliedWave(hero.team(), node), hero);
        return null;
    }
    public void minionAttack(Node node, TeamId team, int amount) { if (active()) damage(node, team, amount, true, null); }
    private boolean alliedWave(TeamId attacker, Node node) {
        return actors.laneUnits().stream().anyMatch(a -> !a.minion.dead() && a.minion.team() == attacker
                && horizontal(a.position(), node.position) <= node.range());
    }
    private void damage(Node n, TeamId attacker, int amount, boolean wave, HeroRuntime lastHit) {
        if (n.health() <= 0) return;
        StructureDamageResult result = n.order == null
                ? rules.damageThrone(attacker, n.team, amount, wave, true)
                : rules.damageTower(attacker, n.team, n.lane, n.order, amount, wave, true);
        if (!result.accepted()) return;
        n.entity.setHealthDisplay(n.health(), n.maximum());
        n.entity.setAnimation(result.destroyed() ? "destroy" : "hit", true);
        if (result.destroyed() && n.order != null)
            economy.towerDestroyed(heroes.values().stream().filter(h -> h.team() == attacker).toList(), lastHit);
        if (result.destroyed()) clearCollision(n);
    }
    private void clearCollision(Node n) {
        if (!(n.entity.level() instanceof ServerLevel level)) return;
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) for (int y = 0; y < 3; y++) {
            var p = new net.minecraft.core.BlockPos((int)n.position.x()+x,(int)n.position.y()+y,(int)n.position.z()+z);
            if (level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.BARRIER))
                level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        }
    }

    public void tick(ServerLevel level, long tick, Map<UUID, HeroRuntime> heroes) {
        prepare(level, heroes);
        if (!active()) return;
        for (Node n : nodes) {
            if (n.health() <= 0) continue;
            var candidates = new ArrayList<StructureTarget>();
            for (var a : actors.laneUnits()) if (!a.minion.dead()) candidates.add(new StructureTarget(a.entity.getUUID(), a.minion.team(), StructureTarget.Kind.MINION, true, false, horizontal(n.position, a.position())));
            for (HeroRuntime h : heroes.values()) {
                ServerPlayer p = level.getServer().getPlayerList().getPlayer(h.owner());
                if (p != null && p.level() == level && h.alive()) candidates.add(new StructureTarget(h.owner(), h.team(), StructureTarget.Kind.HERO, true, actors.isHeroAggressor(h.owner(), n.team, tick), horizontal(n.position, position(p))));
            }
            StructureTarget selected = rules.chooseTarget(n.team, candidates, n.range());
            n.target = selected == null ? null : selected.id();
            n.entity.setStructureProtected(!n.vulnerable());
            if (selected != null) {
                Position aim = targetPosition(level, selected.id());
                if (aim != null) n.entity.setAimYaw((float)Math.toDegrees(Math.atan2(n.entity.getX()-aim.x(),aim.z()-n.entity.getZ())));
            }
            if (selected != null && tick >= n.nextAttack) {
                n.entity.setAnimation("attack", true);
                n.nextAttack = tick + Math.round(n.interval() * 20);
                final UUID targetId = selected.id();
                Position origin = new Position(n.entity.getX(), n.entity.getY() + (n.order == null ? 3.5 : n.order == TowerOrder.T3 ? 5.6 : n.order == TowerOrder.T2 ? 4.7 : 3.9), n.entity.getZ());
                actors.projectiles().launch(level, (n.order == null ? "throne_shot_" : "tower_shot_") + color(n.team), origin,
                        () -> targetPosition(level, targetId), () -> targetAlive(level, targetId), () -> damageTarget(targetId, n.damage()));
            } else if (n.entity.animationAge() > 24) n.entity.setAnimation(n.order == null && !n.vulnerable() ? "protected" : n.health() < n.maximum() / 3 ? "low_hp" : n.order == null ? "vulnerable" : "idle", false);
            n.entity.setHealthDisplay(n.health(), n.maximum());
        }
        for (TeamId team : TeamId.values()) {
            boolean healing = false, danger = false;
            for (HeroRuntime h : heroes.values()) {
                ServerPlayer p = level.getServer().getPlayerList().getPlayer(h.owner());
                if (p == null || p.level() != level || !h.alive()) continue;
                if (h.team() == team && map.fountains().get(team).contains(position(p))) {
                    healing = true;
                    fountainRules.heal(h, tick);
                } else if (h.team() != team && map.fountainDangerZones().get(team).contains(position(p))) {
                    danger = true;
                    if (fountainRules.danger(h, tick) > 0) {
                        // Zone damage is immediate at the configured cadence, not delayed by the visual bolt.
                        Position hit = new Position(p.getX(), p.getY()+1, p.getZ());
                        actors.projectiles().launch(level, "throne_shot_" + color(team), new Position(fountains.get(team).getX(), fountains.get(team).getY()+3, fountains.get(team).getZ()),
                                () -> hit, () -> true, () -> { });
                    }
                }
            }
            fountains.get(team).setAnimation(danger ? "danger" : healing ? "heal" : "idle", false);
        }
    }
    private boolean targetAlive(ServerLevel level, UUID id) {
        HeroRuntime hero = heroes.get(id);
        if (hero != null) { var p=level.getServer().getPlayerList().getPlayer(id); return hero.alive() && p != null && p.level()==level; }
        return actors.laneUnits().stream().anyMatch(a -> a.entity.getUUID().equals(id) && !a.minion.dead() && !a.entity.isRemoved());
    }
    private Position targetPosition(ServerLevel level, UUID id) {
        var p=level.getServer().getPlayerList().getPlayer(id);
        if (p != null && p.level()==level) return new Position(p.getX(),p.getY()+1,p.getZ());
        return actors.laneUnits().stream().filter(a -> a.entity.getUUID().equals(id)).map(a -> new Position(a.entity.getX(),a.entity.getY()+.8,a.entity.getZ())).findFirst().orElse(null);
    }
    private void damageTarget(UUID id,int amount) {
        HeroRuntime h=heroes.get(id); if(h!=null){if(h.alive())h.receiveDamage(rules.structureDamageToHero(h.definition().heroClass(), h.effectiveStats().maxHealth()));return;}
        for(var a:actors.laneUnits())if(a.entity.getUUID().equals(id)&&!a.minion.dead()){a.minion.receiveDamage(amount);a.entity.setAnimation(a.minion.dead()?"death":"hit",true);}
    }
    public void clear(){nodes.forEach(n->{if(n.entity!=null)n.entity.discard();});fountains.values().forEach(ArenaActorEntity::discard);fountains.clear();}
    static Position position(ServerPlayer p){return new Position(p.getX(),p.getY(),p.getZ());}
    static double horizontal(Position a,Position b){return Math.hypot(a.x()-b.x(),a.z()-b.z());}
    public final class Node {
        public final String id; public final TeamId team; public final Lane lane; public final TowerOrder order; public final Position position; public final double radius;
        public ArenaActorEntity entity; private long nextAttack; private UUID target;
        Node(String id,TeamId team,Lane lane,TowerOrder order,Position position,double radius){this.id=id;this.team=team;this.lane=lane;this.order=order;this.position=position;this.radius=radius;}
        public int health(){return order==null?rules.throne(team).health():rules.tower(team,lane,order).health();}
        public int maximum(){return order==null?rules.throne(team).maxHealth():rules.tower(team,lane,order).maxHealth();}
        public boolean vulnerable(){return order==null?rules.throneVulnerable(team):rules.towerVulnerable(team,lane,order);}
        private StructureBalance.Values balance(){return order==null?StructureBalance.throne():StructureBalance.tower(order);}
        public double range(){return balance().attackRange();} public double interval(){return balance().attackIntervalSeconds();} public int damage(){return balance().damage();}
    }
}
