package com.zeravorn.lobby;

import com.zeravorn.ability.AbilityLevelService;
import com.zeravorn.buff.BuffService;
import com.zeravorn.combat.CooldownService;
import com.zeravorn.combat.CrowdControlService;
import com.zeravorn.hero.HeroDefinitions;
import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.item.ItemCatalog;
import com.zeravorn.item.ShopService;
import com.zeravorn.map.Position;
import com.zeravorn.match.MatchSession;
import com.zeravorn.network.FabricMobaNetworking;
import com.zeravorn.network.GameplayRequestValidator;
import com.zeravorn.network.RequestError;
import com.zeravorn.network.RequestSequenceGuard;
import com.zeravorn.network.ServerMatchGateway;
import com.zeravorn.network.ServerMessage;
import com.zeravorn.network.ServerRequestProcessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Test-only M11 entry adapter. M13 must replace this with real map and world queries. */
final class TestArenaEntry implements FabricLobbyController.ArenaEntry {
    private final MatchSession match;
    private final Map<UUID, HeroRuntime> heroes = new HashMap<>();
    private ServerMatchGateway gateway;

    TestArenaEntry(MatchSession match) { this.match = match; }

    @Override public String destinationDimensionId() { return "zeravorn:test_arena"; }

    @Override public boolean loadAndTransfer(MinecraftServer server, LobbySnapshot selection) {
        if (!selection.matchId().equals(match.matchId())) throw new IllegalArgumentException("Wrong match");
        for (LobbyPlayer participant : selection.players())
            if (!isBot(participant) && server.getPlayerList().getPlayer(participant.playerId()) == null) return false;
        ServerLevel level = TestArenaWorld.prepare(server);
        if (gateway == null) gateway = gateway(server);
        for (LobbyPlayer participant : selection.players()) {
            if (isBot(participant)) {
                HeroRuntime botHero = heroes.computeIfAbsent(participant.playerId(), ignored -> new HeroRuntime(
                        participant.playerId(), participant.team(), HeroDefinitions.byId(participant.heroId())));
                gateway.registerBot(botHero);
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(participant.playerId());
            if (!TestArenaWorld.enter(level, player, participant.team())) return false;
            attach(player, participant);
        }
        return true;
    }

    private boolean isBot(LobbyPlayer participant) {
        return match.teams().roster(participant.team()).participants().stream()
                .anyMatch(slot -> slot.playerId().equals(participant.playerId()) && slot.bot());
    }

    @Override public boolean reconnect(MinecraftServer server, LobbySnapshot selection, ServerPlayer player) {
        if (gateway == null) return false;
        LobbyPlayer participant = selection.players().stream()
                .filter(entry -> entry.playerId().equals(player.getUUID())).findFirst().orElse(null);
        if (participant == null) return false;
        ServerLevel level = TestArenaWorld.prepare(server);
        if (!TestArenaWorld.enter(level, player, participant.team())) return false;
        attach(player, participant);
        return true;
    }

    private void attach(ServerPlayer player, LobbyPlayer participant) {
        HeroRuntime hero = heroes.computeIfAbsent(participant.playerId(), ignored ->
                new HeroRuntime(participant.playerId(), participant.team(),
                        HeroDefinitions.byId(participant.heroId())));
        FabricMobaNetworking.attach(player, gateway, hero, participant.spell(), participant.jungler());
    }

    private ServerMatchGateway gateway(MinecraftServer server) {
        var sequences = new RequestSequenceGuard();
        var cooldowns = new CooldownService();
        var buffs = new BuffService();
        var crowdControl = new CrowdControlService();
        var processor = new ServerRequestProcessor(sequences, new GameplayRequestValidator(),
                new AbilityLevelService(), new ShopService(new ItemCatalog()),
                (context, request) -> RequestError.TEST_ARENA_NO_GAMEPLAY);
        ServerMatchGateway.VisibilitySnapshotSource visibility = new ServerMatchGateway.VisibilitySnapshotSource() {
            @Override public Set<UUID> visibleIds(HeroRuntime viewer) {
                return Set.of(viewer.owner());
            }

            @Override public Map<UUID, Position> minimapPositions(HeroRuntime viewer) {
                ServerPlayer player = server.getPlayerList().getPlayer(viewer.owner());
                return player == null ? Map.of() : Map.of(viewer.owner(),
                        new Position(player.getX(), player.getY(), player.getZ()));
            }

            @Override public List<ServerMessage.StructureState> structures(HeroRuntime viewer) {
                return List.of();
            }
        };
        return new ServerMatchGateway(match, sequences, processor, cooldowns, buffs,
                crowdControl, visibility);
    }
}
