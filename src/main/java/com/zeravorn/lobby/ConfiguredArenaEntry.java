package com.zeravorn.lobby;

import com.zeravorn.ability.AbilityLevelService;
import com.zeravorn.buff.BuffService;
import com.zeravorn.combat.CooldownService;
import com.zeravorn.combat.CrowdControlService;
import com.zeravorn.combat.DamageService;
import com.zeravorn.combat.BasicAttackService;
import com.zeravorn.combat.TargetingService;
import com.zeravorn.combat.AttackSpeedService;
import com.zeravorn.hero.HeroDefinitions;
import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.hero.JasonAbilityService;
import com.zeravorn.hero.ShelianerAbilityService;
import com.zeravorn.hero.EsakiAbilityService;
import com.zeravorn.hero.AmeliaAbilityService;
import com.zeravorn.hero.LokiAbilityService;
import com.zeravorn.item.ItemCatalog;
import com.zeravorn.item.ShopService;
import com.zeravorn.map.ArenaWorld;
import com.zeravorn.map.ArenaActorRuntime;
import com.zeravorn.map.ArenaActorEntity;
import com.zeravorn.map.ArenaGeometry;
import com.zeravorn.map.ArenaWorldQueries;
import com.zeravorn.map.MapDefinition;
import com.zeravorn.map.MapLoader;
import com.zeravorn.map.Position;
import com.zeravorn.match.MatchSession;
import com.zeravorn.network.FabricMobaNetworking;
import com.zeravorn.network.GameplayRequestValidator;
import com.zeravorn.network.RequestError;
import com.zeravorn.network.RequestSequenceGuard;
import com.zeravorn.network.ServerMatchGateway;
import com.zeravorn.network.ServerMessage;
import com.zeravorn.network.ServerRequestProcessor;
import com.zeravorn.network.DomainGameplayActionAdapter;
import com.zeravorn.spell.SpellConfigLoader;
import com.zeravorn.spell.SpellService;
import com.zeravorn.projectile.ProjectileService;
import com.zeravorn.vision.VisionService;
import com.zeravorn.vision.VisionSource;
import com.zeravorn.vision.VisionSourceType;
import com.zeravorn.vision.VisionTarget;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/** M13 map entry and live player-world targeting; non-player actors remain separate producers. */
final class ConfiguredArenaEntry implements FabricLobbyController.ArenaEntry {
    private final MatchSession match;
    private final MapDefinition map = MapLoader.loadDefault();
    private final ArenaWorld world = new ArenaWorld(map);
    private final BuffService buffs = new BuffService();
    private final ArenaActorRuntime actors;
    private final com.zeravorn.map.ArenaScenery scenery = new com.zeravorn.map.ArenaScenery(map);
    private final Map<UUID, HeroRuntime> heroes = new HashMap<>();
    private final Map<UUID, Position> deathPositions = new HashMap<>();
    private static final int[] RESPAWN_SECONDS = {0, 5, 6, 7, 9, 11, 13, 16, 19, 22, 25};
    private final VisionService vision = new VisionService();
    private AmeliaAbilityService ameliaAbilities;
    private ServerMatchGateway gateway;
    private CrowdControlService crowdControl;

    ConfiguredArenaEntry(MatchSession match) { this.match = match; actors = new ArenaActorRuntime(map, buffs, match); }

    @Override public String destinationDimensionId() { return world.dimensionId(); }

    @Override public void tick(MinecraftServer server, long elapsedTicks) {
        var level = server.getLevel(ArenaWorld.DIMENSION);
        if (level != null) {
            if (elapsedTicks % 20 == 0) world.keepDaylightAndClearWeather(level);
            actors.tick(level, elapsedTicks, heroes);
        }
        if (match.state() != com.zeravorn.match.MatchState.PLAYING) return;
        if (ameliaAbilities != null) ameliaAbilities.tick(elapsedTicks, true, heroes);
        for (HeroRuntime hero : heroes.values()) {
            hero.tickRegeneration(buffs.effectiveMaxMana(hero, elapsedTicks));
            if (hero.alive()) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(hero.owner());
            if (hero.respawnAtTick() == 0) {
                hero.scheduleRespawn(elapsedTicks + RESPAWN_SECONDS[Math.min(10, hero.level())] * 20L);
                if (gateway != null) gateway.recordKill(null, hero.owner());
                if (player != null && player.level() == level)
                    deathPositions.put(hero.owner(), new Position(player.getX(), player.getY(), player.getZ()));
            }
            if (elapsedTicks >= hero.respawnAtTick()) {
                if (player != null && !world.enter(server, player, hero.team())) continue;
                hero.respawn();
                deathPositions.remove(hero.owner());
                if (gateway != null) gateway.broadcastScoreboard();
                continue;
            }
            if (player != null && player.level() == level) {
                Position anchor = deathPositions.get(hero.owner());
                player.setDeltaMovement(0, 0, 0);
                if (anchor != null && (Math.abs(player.getX() - anchor.x()) > 0.02
                        || Math.abs(player.getY() - anchor.y()) > 0.02
                        || Math.abs(player.getZ() - anchor.z()) > 0.02))
                    player.teleportTo(anchor.x(), anchor.y(), anchor.z());
            }
        }
    }

    @Override public boolean attackActor(ServerPlayer player, ArenaActorEntity entity) {
        HeroRuntime hero = heroes.get(player.getUUID());
        return hero != null && (crowdControl == null || !crowdControl.blocksBasicOrAbility(hero.owner())) && player.level().dimension().equals(ArenaWorld.DIMENSION)
                && actors.attack(player, hero, entity, match.elapsedTicks(),
                hero.definition().heroClass() == com.zeravorn.hero.HeroClass.MAGE || hero.definition().heroClass() == com.zeravorn.hero.HeroClass.MARKSMAN ? 7 : 3) == null;
    }

    @Override public void finish() { actors.projectiles().clear(); }
    @Override public void close() { actors.clear(); scenery.clear(); world.releaseMatchChunks(); heroes.clear(); deathPositions.clear(); }

    @Override public boolean loadAndTransfer(MinecraftServer server, LobbySnapshot selection) {
        if (!selection.matchId().equals(match.matchId())) throw new IllegalArgumentException("Wrong match");
        for (LobbyPlayer participant : selection.players())
            if (!isBot(participant) && server.getPlayerList().getPlayer(participant.playerId()) == null) return false;
        if (!world.prepareStep(server)) return false;
        if (!scenery.prepare(server.getLevel(ArenaWorld.DIMENSION))) return false;
        actors.structures().prepare(server.getLevel(ArenaWorld.DIMENSION), heroes);
        if (gateway == null) gateway = gateway(server);
        for (LobbyPlayer participant : selection.players()) {
            HeroRuntime hero = heroes.computeIfAbsent(participant.playerId(), ignored -> new HeroRuntime(
                    participant.playerId(), participant.team(), HeroDefinitions.byId(participant.heroId())));
            if (isBot(participant)) {
                gateway.registerBot(hero);
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(participant.playerId());
            if (!world.enter(server, player, participant.team())) return false;
            FabricMobaNetworking.attach(player, gateway, hero, participant.spell(), participant.jungler());
        }
        return true;
    }

    @Override public boolean reconnect(MinecraftServer server, LobbySnapshot selection, ServerPlayer player) {
        if (gateway == null || !world.prepareStep(server)) return false;
        LobbyPlayer participant = selection.players().stream()
                .filter(entry -> entry.playerId().equals(player.getUUID()) && !isBot(entry))
                .findFirst().orElse(null);
        if (participant == null || !world.enter(server, player, participant.team())) return false;
        HeroRuntime hero = heroes.get(participant.playerId());
        if (hero == null) return false;
        FabricMobaNetworking.attach(player, gateway, hero, participant.spell(), participant.jungler());
        return true;
    }

    private boolean isBot(LobbyPlayer participant) {
        return match.teams().roster(participant.team()).participants().stream()
                .anyMatch(slot -> slot.playerId().equals(participant.playerId()) && slot.bot());
    }

    private ServerMatchGateway gateway(MinecraftServer server) {
        var sequences = new RequestSequenceGuard();
        var cooldowns = new CooldownService();
        crowdControl = new CrowdControlService();
        var damage = new DamageService(buffs);
        var projectiles = new ProjectileService();
        var worldQueries = new ArenaWorldQueries(server, map, heroes, actors);
        ameliaAbilities = new AmeliaAbilityService(cooldowns, damage, crowdControl, buffs);
        var actions = new DomainGameplayActionAdapter(
                new BasicAttackService(cooldowns, new TargetingService(), damage,
                        new AttackSpeedService(), crowdControl),
                new SpellService(SpellConfigLoader.loadDefaults(), cooldowns, crowdControl),
                new JasonAbilityService(cooldowns, damage, crowdControl, buffs),
                new ShelianerAbilityService(cooldowns, damage, crowdControl, buffs, projectiles),
                new EsakiAbilityService(cooldowns, damage, crowdControl, buffs, projectiles),
                ameliaAbilities,
                new LokiAbilityService(cooldowns, damage, crowdControl, buffs, projectiles),
                worldQueries);
        var processor = new ServerRequestProcessor(sequences, new GameplayRequestValidator(),
                new AbilityLevelService(), new ShopService(new ItemCatalog()), actions);
        ServerMatchGateway.VisibilitySnapshotSource visibility = new ServerMatchGateway.VisibilitySnapshotSource() {
            @Override public Set<UUID> visibleIds(HeroRuntime viewer) {
                var level = server.getLevel(ArenaWorld.DIMENSION);
                if (level == null) return Set.of(viewer.owner());
                var geometry = new ArenaGeometry(map, level);
                List<VisionSource> sources = new ArrayList<>();
                List<VisionTarget> targets = new ArrayList<>();
                for (HeroRuntime hero : heroes.values()) {
                    ServerPlayer actor = server.getPlayerList().getPlayer(hero.owner());
                    if (actor == null || actor.level() != level) continue;
                    Position position = new Position(actor.getX(), actor.getY(), actor.getZ());
                    String bush = geometry.bushAt(position);
                    targets.add(new VisionTarget(hero.owner(), hero.team(), position, hero.alive(), bush));
                    if (hero.alive()) sources.add(new VisionSource(hero.owner(), hero.team(), position,
                            VisionSourceType.HERO, vision.radius(VisionSourceType.HERO), bush));
                }
                for (var node : actors.structures().nodes()) if (node.health() > 0) sources.add(new VisionSource(
                        UUID.nameUUIDFromBytes(node.id.getBytes(StandardCharsets.UTF_8)), node.team, node.position,
                        node.order == null ? VisionSourceType.THRONE : VisionSourceType.TOWER,
                        vision.radius(node.order == null ? VisionSourceType.THRONE : VisionSourceType.TOWER), null));
                for (var team : com.zeravorn.team.TeamId.values()) sources.add(new VisionSource(
                        UUID.nameUUIDFromBytes((team + "_FOUNTAIN").getBytes(StandardCharsets.UTF_8)),
                        team, map.fountains().get(team).center(), VisionSourceType.FOUNTAIN,
                        vision.radius(VisionSourceType.FOUNTAIN), null));
                vision.recompute(viewer.team(), sources, targets, geometry, match.elapsedTicks());
                Set<UUID> visible = new HashSet<>();
                visible.add(viewer.owner());
                for (HeroRuntime hero : heroes.values())
                    if (hero.team() == viewer.team() || vision.visibleTo(viewer.team(), hero.owner()))
                        visible.add(hero.owner());
                return visible;
            }

            @Override public Map<UUID, Position> minimapPositions(HeroRuntime viewer) {
                Map<UUID, Position> positions = new HashMap<>();
                for (UUID id : visibleIds(viewer)) {
                    ServerPlayer player = server.getPlayerList().getPlayer(id);
                    if (player != null) positions.put(id, new Position(player.getX(), player.getY(), player.getZ()));
                }
                return positions;
            }

            @Override public List<ServerMessage.StructureState> structures(HeroRuntime viewer) {
                return actors.structures().snapshots();
            }
        };
        return new ServerMatchGateway(match, sequences, processor, cooldowns, buffs,
                crowdControl, visibility);
    }
}
