package com.zeravorn.map;

import com.zeravorn.jungle.JungleCamp;
import com.zeravorn.jungle.JungleConfigLoader;
import com.zeravorn.jungle.JungleService;
import com.zeravorn.buff.BuffService;
import com.zeravorn.economy.ExperienceService;
import com.zeravorn.economy.GoldService;
import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.hero.AmeliaAbilityDefinitions;
import com.zeravorn.ability.AbilitySlot;
import com.zeravorn.network.ClientRequest;
import com.zeravorn.minion.Lane;
import com.zeravorn.minion.LaneMinion;
import com.zeravorn.minion.WaveService;
import com.zeravorn.team.TeamId;
import com.zeravorn.network.RequestError;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/** Server-owned presentation actors, driven only by the match clock. */
public final class ArenaActorRuntime {
    private final MapDefinition map;
    private final WaveService waves = new WaveService();
    private final JungleService jungle;
    private final Map<UUID, Long> nextHeroAttack = new HashMap<>();
    private final List<LaneActor> laneActors = new ArrayList<>();
    private final List<CampActor> campActors = new ArrayList<>();
    private final List<PendingAmelia> pendingAmelia = new ArrayList<>();
    private final ArenaProjectileRuntime projectiles = new ArenaProjectileRuntime();
    private final ArenaStructureRuntime structures;
    private final Map<UUID, Aggression> aggressors = new HashMap<>();
    private int lastWave;

    public ArenaActorRuntime(MapDefinition map, BuffService buffs) {
        this(map, buffs, null);
    }

    public ArenaActorRuntime(MapDefinition map, BuffService buffs, com.zeravorn.match.MatchSession match) {
        this.map = map;
        this.structures = new ArenaStructureRuntime(map, match, this);
        this.jungle = new JungleService(new GoldService(), new ExperienceService(), buffs);
        var definitions = JungleConfigLoader.loadDefaultDefinitions();
        for (var point : map.jungleCamps()) {
            var definition = definitions.get(point.mobDefinitionId());
            if (definition != null) campActors.add(new CampActor(new JungleCamp(point.id(), definition,
                    point.position())));
        }
    }

    public ArenaStructureRuntime structures() { return structures; }
    public ArenaProjectileRuntime projectiles() { return projectiles; }
    List<LaneActor> laneUnits() { return List.copyOf(laneActors); }
    public void recordAggression(UUID attacker, TeamId victimTeam, long tick) { aggressors.put(attacker, new Aggression(victimTeam, tick + 60)); }
    boolean isHeroAggressor(UUID id, TeamId defendingTeam, long tick) { var a=aggressors.get(id); return a!=null&&a.team==defendingTeam&&a.until>=tick; }
    private record Aggression(TeamId team,long until) { }
    public void clear() {
        laneActors.forEach(a -> a.entity.discard()); laneActors.clear();
        campActors.forEach(a -> { if (a.entity != null) a.entity.discard(); });
        pendingAmelia.clear(); projectiles.clear(); structures.clear();
    }

    /** Adds actor damage after the authoritative Amelia ability service accepts a cast. */
    public void ameliaAbility(ServerPlayer player, HeroRuntime caster, ClientRequest.CastAbility request, long tick) {
        var eye = player.getEyePosition();
        var look = player.getLookAngle().normalize();
        String id = request.abilityId();
        if ("zeravorn:amelia_q".equals(id)) {
            var config = AmeliaAbilityDefinitions.q(caster.abilityRank(AbilitySlot.Q));
            int damage = config.damagePerTick()
                    + (int) Math.floor(caster.effectiveStats().abilityPower() * config.apRatio());
            pendingAmelia.add(new PendingAmelia(caster.owner(), new Position(eye.x, eye.y, eye.z),
                    new Position(look.x, look.y, look.z), config.range(), damage, tick + 1,
                    config.ticks()));
        } else if ("zeravorn:amelia_e".equals(id)) {
            var config = AmeliaAbilityDefinitions.e(caster.abilityRank(AbilitySlot.E));
            int damage = config.damage()
                    + (int) Math.floor(caster.effectiveStats().abilityPower() * config.apRatio());
            damageArea(caster, new Position(player.getX(), player.getY(), player.getZ()),
                    null, config.radius(), damage, tick);
        } else if ("zeravorn:amelia_r".equals(id)) {
            var config = AmeliaAbilityDefinitions.r(caster.abilityRank(AbilitySlot.R));
            Position center = request.groundPoint();
            Position casterPosition = new Position(player.getX(), player.getY(), player.getZ());
            if (center == null || casterPosition.distanceSquared(center) > config.range() * config.range())
                center = new Position(player.getX() + look.x * config.range(), player.getY(),
                        player.getZ() + look.z * config.range());
            int damage = config.damage()
                    + (int) Math.floor(caster.effectiveStats().abilityPower() * config.apRatio());
            pendingAmelia.add(new PendingAmelia(caster.owner(), center, null, config.radius(),
                    damage, tick + config.delayTicks(), 1));
        }
    }

    private void damageArea(HeroRuntime caster, Position center, Position direction,
                            double radius, int damage, long tick) {
        for (LaneActor actor : laneActors) {
            if (actor.minion.dead() || actor.minion.team() == caster.team()
                    || !withinArea(actor.entity, center, direction, radius)) continue;
            actor.minion.receiveDamage(damage);
            actor.entity.setAnimation(actor.minion.dead() ? "death" : "hit", true);
            if (actor.minion.dead()) {
                caster.addGold(actor.minion.definition().gold());
                caster.addExperience(actor.minion.definition().experience());
            }
        }
        for (CampActor actor : campActors) {
            if (!actor.camp.alive() || actor.entity == null
                    || !withinArea(actor.entity, center, direction, radius)) continue;
            actor.camp.damage(damage, tick);
            actor.entity.setAnimation(actor.camp.alive() ? "hit" : "death", true);
            actor.entity.setHealthDisplay(actor.camp.health(), actor.camp.maxHealth());
            if (!actor.camp.alive()) jungle.kill(caster, actor.camp, tick, 120L * 20L, 1.10, 1.15, 0.10);
        }
    }

    private static boolean withinArea(ArenaActorEntity actor, Position center, Position direction,
                                      double radius) {
        double dx = actor.getX() - center.x(), dz = actor.getZ() - center.z();
        if (dx * dx + dz * dz > radius * radius) return false;
        if (direction == null) return true;
        double horizontalLength = Math.hypot(direction.x(), direction.z());
        if (horizontalLength < 0.001) return false;
        double dot = (dx * direction.x() + dz * direction.z()) / horizontalLength;
        return dot > 0 && dot * dot >= (dx * dx + dz * dz) * 0.25;
    }

    /** Applies an arena basic attack to a spawned minion or jungle camp. */
    public RequestError attack(ServerPlayer player, HeroRuntime hero, ArenaActorEntity target,
                               long elapsedTicks, double range) {
        if (!hero.alive()) return RequestError.DEAD;
        if (target.isRemoved()) return RequestError.INVALID_TARGET;
        var structure = structures.node(target);
        double reach = range + (structure == null ? 0 : structure.radius);
        if (player.distanceToSqr(target) > reach * reach) return RequestError.OUT_OF_RANGE;
        if (elapsedTicks < nextHeroAttack.getOrDefault(player.getUUID(), 0L)) return RequestError.COOLDOWN;
        if (structure != null) {
            if (!canSeeStructure(player, structure)) return RequestError.INVALID_TARGET;
            RequestError error = structures.attack(hero, structure, elapsedTicks);
            if (error == null) nextHeroAttack.put(player.getUUID(), elapsedTicks + Math.max(1L, Math.round(hero.effectiveStats().attackInterval() * 20)));
            return error;
        }
        int damage = hero.effectiveStats().attack();
        for (LaneActor actor : laneActors) if (actor.entity == target) {
            if (actor.minion.team() == hero.team() || actor.minion.dead()) return RequestError.INVALID_TARGET;
            actor.minion.receiveDamage(damage);
            target.setAnimation("hit", true);
            if (actor.minion.dead()) {
                hero.addGold(actor.minion.definition().gold());
                hero.addExperience(actor.minion.definition().experience());
                target.setAnimation("death", true);
            }
            nextHeroAttack.put(player.getUUID(), elapsedTicks
                    + Math.max(1L, Math.round(hero.effectiveStats().attackInterval() * 20)));
            return null;
        }
        for (CampActor actor : campActors) if (actor.entity == target && actor.camp.alive()) {
            actor.camp.damage(damage, elapsedTicks);
            target.setAnimation("hit", true);
            target.setHealthDisplay(actor.camp.health(), actor.camp.maxHealth());
            if (!actor.camp.alive()) {
                jungle.kill(hero, actor.camp, elapsedTicks, 120L * 20L, 1.10, 1.15, 0.10);
                target.setAnimation("death", true);
            }
            nextHeroAttack.put(player.getUUID(), elapsedTicks
                    + Math.max(1L, Math.round(hero.effectiveStats().attackInterval() * 20)));
            return null;
        }
        return RequestError.INVALID_TARGET;
    }

    private static boolean canSeeStructure(ServerPlayer player, ArenaStructureRuntime.Node node) {
        var eye=player.getEyePosition();
        double dx=node.entity.getX()-eye.x,dy=node.entity.getY()+1.5-eye.y,dz=node.entity.getZ()-eye.z;
        double length=Math.sqrt(dx*dx+dy*dy+dz*dz),end=Math.max(0,length-node.radius-.2);
        for(double d=.25;d<end;d+=.25){
            var p=net.minecraft.core.BlockPos.containing(eye.x+dx*d/length,eye.y+dy*d/length,eye.z+dz*d/length);
            if(!player.level().getBlockState(p).getCollisionShape(player.level(),p).isEmpty())return false;
        }
        return true;
    }

    /** Server-side aim test; a client UUID is only a hint and cannot extend range or see through the aim cone. */
    public ArenaActorEntity findTarget(ServerPlayer player, TeamId team, UUID hint,
                                       Position ignoredClientAim, double range) {
        var eye = player.getEyePosition();
        var look = player.getLookAngle().normalize();
        ArenaActorEntity closest = null;
        double best = Double.POSITIVE_INFINITY;
        for (LaneActor actor : laneActors) {
            if (actor.minion.dead() || actor.minion.team() == team || actor.entity.isRemoved()) continue;
            double distance = aimedDistance(eye.x, eye.y, eye.z, look.x, look.y, look.z,
                    actor.entity, range);
            if (distance >= 0 && (actor.entity.getUUID().equals(hint) || distance < best)) {
                closest = actor.entity;
                best = distance;
                if (actor.entity.getUUID().equals(hint)) return closest;
            }
        }
        for (CampActor actor : campActors) {
            if (!actor.camp.alive() || actor.entity == null || actor.entity.isRemoved()) continue;
            double distance = aimedDistance(eye.x, eye.y, eye.z, look.x, look.y, look.z,
                    actor.entity, range);
            if (distance >= 0 && (actor.entity.getUUID().equals(hint) || distance < best)) {
                closest = actor.entity;
                best = distance;
                if (actor.entity.getUUID().equals(hint)) return closest;
            }
        }
        for (var node : structures.nodes()) {
            if (node.entity == null || node.health() <= 0 || node.team == team) continue;
            // Intersect the logical structure volume, not the narrow presentation stand.
            var box = new net.minecraft.world.phys.AABB(node.entity.getX()-node.radius, node.entity.getY(), node.entity.getZ()-node.radius,
                    node.entity.getX()+node.radius,node.entity.getY()+4,node.entity.getZ()+node.radius);
            var hit = box.clip(eye, eye.add(look.scale(range)));
            if (hit.isPresent() && eye.distanceTo(hit.get()) < best && canSeeStructure(player,node)) {
                best = eye.distanceTo(hit.get()); closest = node.entity;
            }
        }
        return closest;
    }

    private static double aimedDistance(double ex, double ey, double ez, double lx, double ly,
                                        double lz, ArenaActorEntity entity, double range) {
        double dx = entity.getX() - ex, dy = entity.getY() + 0.9 - ey, dz = entity.getZ() - ez;
        double along = dx * lx + dy * ly + dz * lz;
        if (along < 0 || along > range) return -1;
        double off = dx * dx + dy * dy + dz * dz - along * along;
        return off <= 1.7 * 1.7 ? along : -1;
    }

    public void tick(ServerLevel level, long elapsedTicks, Map<UUID, HeroRuntime> heroes) {
        structures.prepare(level, heroes);
        projectiles.tick();
        if (!structures.active()) return;
        for (Iterator<PendingAmelia> it = pendingAmelia.iterator(); it.hasNext();) {
            PendingAmelia effect = it.next();
            HeroRuntime caster = heroes.get(effect.caster);
            if (caster == null || !caster.alive()) { it.remove(); continue; }
            if (elapsedTicks < effect.nextTick) continue;
            damageArea(caster, effect.center, effect.direction, effect.radius, effect.damage, elapsedTicks);
            if(effect.direction!=null){
                Position end=new Position(effect.center.x()+effect.direction.x()*effect.radius,effect.center.y()+effect.direction.y()*effect.radius,effect.center.z()+effect.direction.z()*effect.radius);
                projectiles.launch(level,"projectile_ice_blue",effect.center,()->end,caster::alive,()->{});
            }
            effect.remaining--;
            effect.nextTick += 10;
            if (effect.remaining <= 0) it.remove();
        }
        int wave = waves.waveNumberAt(elapsedTicks);
        // A clock jump must not materialize every missed wave in one tick.
        if (lastWave < wave) { lastWave = wave; spawnWave(level, wave); }
        for (Iterator<LaneActor> it = laneActors.iterator(); it.hasNext();) {
            LaneActor actor = it.next();
            if (actor.minion.dead() && !actor.entity.isRemoved()) {
                if (!"death".equals(actor.entity.animationName())) actor.entity.setAnimation("death", true);
                if (actor.entity.animationAge() < 25) continue;
            }
            if (actor.entity.isRemoved() || actor.minion.dead() || !actor.tick(laneActors, elapsedTicks, structures, projectiles, level)) {
                actor.entity.discard();
                it.remove();
            }
            if (!structures.active()) return;
        }
        structures.tick(level, elapsedTicks, heroes);
        for (CampActor actor : campActors) {
            boolean wasAlive = actor.camp.alive();
            if (wasAlive && actor.entity != null && actor.entity.isRemoved())
                actor.camp.damage(actor.camp.health(), elapsedTicks);
            actor.camp.tick(elapsedTicks, true, JungleService.FIRST_SPAWN_TICK);
            if (actor.camp.alive() && (actor.entity == null || actor.entity.isRemoved())) {
                actor.entity = spawn(level, actor.camp.spawnPoint(), campVisual(actor.camp.definition().id()));
                actor.entity.setHealthDisplay(actor.camp.health(), actor.camp.maxHealth());
            } else if (!actor.camp.alive() && actor.entity != null) {
                if (!"death".equals(actor.entity.animationName())) actor.entity.setAnimation("death", true);
                if (actor.entity.animationAge() >= 40) {
                    actor.entity.discard();
                    actor.entity = null;
                }
            } else if (actor.camp.alive() && actor.entity != null) {
                actor.tick(level, heroes, elapsedTicks);
                actor.entity.setHealthDisplay(actor.camp.health(), actor.camp.maxHealth());
            }
        }
    }

    private void spawnWave(ServerLevel level, int wave) {
        for (Lane lane : Lane.values()) for (TeamId team : TeamId.values()) {
            var path = map.lanePaths().get(lane).get(team);
            long population = laneActors.stream().filter(a -> !a.minion.dead()
                    && a.minion.team() == team && a.minion.lane() == lane).count();
            if (population + 6 > LaneSpawnPlan.MAX_UNITS_PER_LANE_TEAM) continue;
            int slot = 0;
            for (LaneMinion minion : waves.spawnWave(wave, team, lane, path)) {
                String visual = switch (minion.definition().type()) {
                    case MELEE -> "lane_warrior";
                    case RANGED -> "lane_ranged";
                    case SIEGE -> "lane_siege";
                };
                var route = LaneSpawnPlan.route(path, slot++);
                var start = route.getFirst();
                if (!freeSpawn(level, start)) continue;
                minion = new LaneMinion(minion.id(), team, lane, wave, minion.definition(), route);
                minion.advanceWaypoint();
                var entity = spawn(level, start, visual + "_" + team.name().toLowerCase());
                laneActors.add(new LaneActor(minion, entity, start.x(), start.z()));
            }
        }
    }

    private boolean freeSpawn(ServerLevel level, Position p) {
        for (var actor : laneActors) if (!actor.minion.dead()
                && Math.hypot(p.x() - actor.x, p.z() - actor.z) < 1.4) return false;
        return level.noCollision(new net.minecraft.world.phys.AABB(p.x()+.1,p.y(),p.z()+.1,
                p.x()+.9,p.y()+1.9,p.z()+.9));
    }

    private static ArenaActorEntity spawn(ServerLevel level, Position position, String visual) {
        var entity = new ArenaActorEntity(ArenaActorEntity.TYPE, level);
        entity.setPos(position.x() + 0.5, position.y(), position.z() + 0.5);
        entity.setVisual(visual);
        level.addFreshEntity(entity);
        return entity;
    }

    private static String campVisual(String definition) {
        return switch (definition) {
            case "RED" -> "jungle_red_buff";
            case "BLUE" -> "jungle_blue_buff";
            case "GREEN_A" -> "jungle_green_1";
            case "GREEN_B" -> "jungle_green_2";
            // The two contested camps have no dedicated art yet; use green variants.
            case "CONTESTED_A" -> "jungle_green_1";
            default -> "jungle_green_2";
        };
    }

    private static final class CampActor {
        private final JungleCamp camp;
        private ArenaActorEntity entity;
        private long nextAttackTick;
        private CampActor(JungleCamp camp) { this.camp = camp; }

        private void tick(ServerLevel level, Map<UUID, HeroRuntime> heroes, long elapsedTicks) {
            ServerPlayer nearest = null;
            HeroRuntime targetHero = null;
            double nearestDistance = Double.POSITIVE_INFINITY;
            for (HeroRuntime hero : heroes.values()) {
                if (!hero.alive()) continue;
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(hero.owner());
                if (player == null || player.level() != level) continue;
                double fromSpawn = Math.hypot(player.getX() - camp.spawnPoint().x(),
                        player.getZ() - camp.spawnPoint().z());
                double toMob = Math.hypot(player.getX() - entity.getX(), player.getZ() - entity.getZ());
                if (fromSpawn <= camp.definition().leashRadius() && toMob <= 7 && toMob < nearestDistance) {
                    nearest = player;
                    targetHero = hero;
                    nearestDistance = toMob;
                }
            }
            if (nearest != null && targetHero != null) {
                if (nearestDistance > 2.2) {
                    moveToward(nearest.getX(), nearest.getZ(), 0.065);
                    if (entity.animationAge() > 8 || !"hit".equals(entity.animationName()))
                        entity.setAnimation("walk", false);
                } else if (elapsedTicks >= nextAttackTick) {
                    targetHero.receiveDamage(camp.scaledDamage(elapsedTicks));
                    entity.setAnimation("attack", true);
                    nextAttackTick = elapsedTicks + Math.max(1,
                            Math.round(camp.definition().attackInterval() * 20));
                } else if (entity.animationAge() > 18) entity.setAnimation("idle", false);
                return;
            }
            double homeX = camp.spawnPoint().x() + 0.5, homeZ = camp.spawnPoint().z() + 0.5;
            if (Math.hypot(homeX - entity.getX(), homeZ - entity.getZ()) > 0.12) {
                if (camp.state() != com.zeravorn.jungle.JungleCampState.LEASH_RETURN)
                    camp.beginLeashReturn(elapsedTicks);
                moveToward(homeX, homeZ, 0.09);
                entity.setAnimation("leash_return", false);
            } else if (entity.animationAge() > 10 || !"hit".equals(entity.animationName())) {
                entity.setAnimation("idle", false);
            }
        }

        private void moveToward(double x, double z, double speed) {
            double dx = x - entity.getX(), dz = z - entity.getZ();
            double distance = Math.hypot(dx, dz);
            if (distance < 0.001) return;
            double step = Math.min(speed, distance);
            entity.teleportTo(entity.getX() + dx / distance * step, camp.spawnPoint().y(),
                    entity.getZ() + dz / distance * step);
            entity.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
        }
    }

    private static final class PendingAmelia {
        private final UUID caster;
        private final Position center;
        private final Position direction;
        private final double radius;
        private final int damage;
        private long nextTick;
        private int remaining;
        private PendingAmelia(UUID caster, Position center, Position direction,
                              double radius, int damage, long nextTick, int remaining) {
            this.caster = caster;
            this.center = center;
            this.direction = direction;
            this.radius = radius;
            this.damage = damage;
            this.nextTick = nextTick;
            this.remaining = remaining;
        }
    }

    static final class LaneActor {
        final LaneMinion minion;
        final ArenaActorEntity entity;
        private double x, z;
        private long nextAttackTick;
        private int stalledTicks;
        private double progressX, progressZ;
        private LaneActor(LaneMinion minion, ArenaActorEntity entity, double x, double z) {
            this.minion = minion;
            this.entity = entity;
            this.x = x;
            this.z = z;
            this.progressX = x;
            this.progressZ = z;
        }
        Position position() { return new Position(entity.getX(),entity.getY(),entity.getZ()); }
        private boolean tick(List<LaneActor> actors, long elapsedTicks, ArenaStructureRuntime structures, ArenaProjectileRuntime shots, ServerLevel level) {
            LaneActor enemy = null;
            double nearest = Double.POSITIVE_INFINITY;
            double range = Math.max(1.4, minion.definition().range());
            for (LaneActor other : actors) {
                if (other == this || other.minion.dead() || other.minion.team() == minion.team()
                        || other.minion.lane() != minion.lane()) continue;
                double distance = (x - other.x) * (x - other.x) + (z - other.z) * (z - other.z);
                if (distance <= range * range && distance < nearest) {
                    enemy = other;
                    nearest = distance;
                }
            }
            if (enemy != null) {
                stalledTicks = 0;
                minion.setState(LaneMinion.State.FIGHT_MINION);
                if (elapsedTicks >= nextAttackTick) {
                    final LaneActor victim = enemy;
                    if (minion.definition().type() == com.zeravorn.minion.MinionType.MELEE) {
                        victim.minion.receiveDamage(minion.damage());
                        victim.entity.setAnimation(victim.minion.dead() ? "death" : "hit", true);
                    } else shots.launch(level, "projectile_basic_basic", new Position(entity.getX(), entity.getY()+1, entity.getZ()),
                            () -> new Position(victim.entity.getX(),victim.entity.getY()+.8,victim.entity.getZ()),
                            () -> !victim.minion.dead()&&!victim.entity.isRemoved(), () -> { victim.minion.receiveDamage(minion.damage());victim.entity.setAnimation(victim.minion.dead()?"death":"hit",true); });
                    double facingX=enemy.entity.getX()-entity.getX(), facingZ=enemy.entity.getZ()-entity.getZ();
                    entity.setYRot((float)Math.toDegrees(Math.atan2(-facingX,facingZ)));
                    entity.setAnimation("attack", true);
                    nextAttackTick = elapsedTicks + Math.max(1,
                            Math.round(minion.definition().attackIntervalSeconds() * 20));
                } else if (entity.animationAge() > 17) entity.setAnimation("idle", false);
                return true;
            }
            var node = structures.nextTarget(minion.team(), minion.lane(), position());
            if (node != null) {
                stalledTicks = 0;
                minion.setState(LaneMinion.State.ATTACK_STRUCTURE);
                double dx=node.entity.getX()-entity.getX(),dz=node.entity.getZ()-entity.getZ();
                entity.setYRot((float)Math.toDegrees(Math.atan2(-dx,dz)));
                if(Math.hypot(dx,dz)>range+node.radius) {
                    moveToward(actors,node.entity.getX()-.5,node.entity.getZ()-.5,entity.getY());
                } else if(elapsedTicks>=nextAttackTick){
                    entity.setAnimation("attack",true);
                    if(minion.definition().type()==com.zeravorn.minion.MinionType.MELEE) structures.minionAttack(node,minion.team(),minion.damage());
                    else shots.launch(level,"projectile_basic_basic",new Position(entity.getX(),entity.getY()+1,entity.getZ()),
                            ()->new Position(node.entity.getX(),node.entity.getY()+1,node.entity.getZ()),()->node.health()>0,
                            ()->structures.minionAttack(node,minion.team(),minion.damage()));
                    nextAttackTick=elapsedTicks+Math.max(1,Math.round(minion.definition().attackIntervalSeconds()*20));
                }else if(entity.animationAge()>20)entity.setAnimation("idle",false);
                return true;
            }
            minion.setState(LaneMinion.State.MOVE_PATH);
            // Only stalled travel expires; fighting units and ordinary queues keep their lifetime.
            if (Math.hypot(x-progressX,z-progressZ) >= 1) {
                progressX=x; progressZ=z; stalledTicks=0;
            } else if (++stalledTicks > 20*30) return false;
            return move(actors);
        }

        private boolean move(List<LaneActor> actors) {
            var path = minion.waypoints();
            var target = path.get(minion.waypointIndex());
            double dx = target.x() - x, dz = target.z() - z;
            double distance = Math.hypot(dx, dz);
            if (distance < 1.4) {
                if (!minion.advanceWaypoint()) return false;
                target = path.get(minion.waypointIndex());
                dx = target.x() - x;
                dz = target.z() - z;
                distance = Math.hypot(dx, dz);
            }
            return moveToward(actors,target.x(),target.z(),target.y());
        }

        private boolean moveToward(List<LaneActor> actors,double tx,double tz,double y) {
            double dx=tx-x,dz=tz-z,distance=Math.hypot(dx,dz);
            if (distance > 0.001) {
                double step = Math.min(distance, 0.11);
                double nextX = x + dx / distance * step;
                double nextZ = z + dz / distance * step;
                // Teleport-based motion bypasses Minecraft collision, so block overlap here.
                if (blocked(actors,nextX,nextZ)) {
                    boolean found=false;
                    for(int side:new int[]{1,-1}){
                        double sx=x+(dx/distance*.35-dz/distance*side)*step,sz=z+(dz/distance*.35+dx/distance*side)*step;
                        if(!blocked(actors,sx,sz)){nextX=sx;nextZ=sz;found=true;break;}
                    }
                    if(!found){entity.setAnimation("idle",false);return true;}
                }
                x = nextX;
                z = nextZ;
                entity.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
                entity.setAnimation(minion.definition().type()==com.zeravorn.minion.MinionType.SIEGE?"move":"walk",false);
                entity.teleportTo(x + 0.5, y, z + 0.5);
            }
            return true;
        }
        private boolean blocked(List<LaneActor> actors,double nx,double nz){
            for(var other:actors)if(other!=this&&!other.minion.dead()&&Math.hypot(nx-other.x,nz-other.z)<1.2)return true;
            var feet=net.minecraft.core.BlockPos.containing(nx+.5,entity.getY(),nz+.5);
            if(!entity.level().getBlockState(feet).getCollisionShape(entity.level(),feet).isEmpty())return true;
            return false;
        }
    }
}
