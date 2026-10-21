package com.zeravorn.lobby;

import com.zeravorn.match.MatchSession;
import com.zeravorn.match.MatchState;
import com.zeravorn.network.ClientRequest;
import com.zeravorn.network.FabricMobaNetworking;
import com.zeravorn.network.RequestResult;
import com.zeravorn.network.ServerMessage;
import com.zeravorn.map.ArenaActorEntity;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

import java.util.UUID;

/** Fabric-facing lobby owner; one match per server until match rotation is introduced. */
public final class FabricLobbyController {
    private static LobbyService lobby;
    private static java.util.function.Function<MatchSession, ArenaEntry> arenaEntryFactory = ConfiguredArenaEntry::new;
    private static ArenaEntry arenaEntry;
    private static int countdownTicks;
    private static boolean rosterDirty;
    private static boolean arenaEntryFailed;
    private static int finishTicks = -1;
    private static LobbyRoom room;
    private static final CountdownLock COUNTDOWN_LOCK = new CountdownLock();

    private FabricLobbyController() { }

    public static void initialize() {
        room = LobbyWorld.room();
        FabricMobaNetworking.setLobbyHandler((player, envelope) -> {
            LobbyService current = ensure();
            var error = current.apply(player.getUUID(), (ClientRequest.Lobby) envelope.payload());
            if (error == null) broadcast(player.level().getServer());
            return error == null ? RequestResult.accepted(envelope.clientSequenceId())
                    : RequestResult.rejected(envelope.clientSequenceId(), error);
        });
        FabricMobaNetworking.setLobbyLifecycle(FabricLobbyController::resync,
                FabricLobbyController::disconnect);
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (countdownLocked(player) || inTestArena(player)) return InteractionResult.FAIL;
            if (inLobbyRoom(player) && (lobby == null
                    || lobby.match().state() != MatchState.HERO_SELECT)) return InteractionResult.FAIL;
            if (!(player instanceof ServerPlayer serverPlayer) || room == null || lobby == null
                    || lobby.match().state() != MatchState.HERO_SELECT) return InteractionResult.PASS;
            var pos = hit.getBlockPos();
            if (!room.dimensionId().equals(level.dimension().identifier().toString()))
                return InteractionResult.PASS;
            if (LobbyWorld.isSelectionTerminal(room.dimensionId(), pos))
                return InteractionResult.SUCCESS;
            var pedestal = room.pedestalAt(level.dimension().identifier().toString(),
                    pos.getX(), pos.getY(), pos.getZ());
            if (pedestal.isPresent()) {
                selectPedestal(serverPlayer, pedestal.get().heroId());
                return InteractionResult.SUCCESS;
            }
            if (sameBlock(room.bluePresentation(), pos)) {
                applyPhysical(serverPlayer, new ClientRequest.Lobby(
                        ClientRequest.LobbyAction.SELECT_TEAM, com.zeravorn.team.TeamId.BLUE,
                        null, null, null, null));
                return InteractionResult.SUCCESS;
            }
            if (sameBlock(room.redPresentation(), pos)) {
                applyPhysical(serverPlayer, new ClientRequest.Lobby(
                        ClientRequest.LobbyAction.SELECT_TEAM, com.zeravorn.team.TeamId.RED,
                        null, null, null, null));
                return InteractionResult.SUCCESS;
            }
            if (sameBlock(room.readyArea(), pos)) {
                boolean ready = lobby.snapshot().players().stream()
                        .filter(candidate -> candidate.playerId().equals(player.getUUID()))
                        .findFirst().map(LobbyPlayer::ready).orElse(false);
                applyPhysical(serverPlayer, new ClientRequest.Lobby(
                        ClientRequest.LobbyAction.SET_READY, null, null, null, !ready, null));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        });
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (countdownLocked(player) || inTestArena(player)) return InteractionResult.FAIL;
            if (inLobbyRoom(player) && (lobby == null
                    || lobby.match().state() != MatchState.HERO_SELECT)) return InteractionResult.FAIL;
            if (!(player instanceof ServerPlayer serverPlayer) || room == null || lobby == null
                    || !entity.entityTags().contains("zeravorn_lobby_preview")
                    || lobby.match().state() != MatchState.HERO_SELECT) return inLobbyRoom(player)
                            ? InteractionResult.FAIL : InteractionResult.PASS;
            var pedestal = room.pedestalAt(level.dimension().identifier().toString(),
                    (int) Math.floor(entity.getX()), (int) Math.floor(entity.getY()) - 1,
                    (int) Math.floor(entity.getZ()));
            if (pedestal.isEmpty()) return inLobbyRoom(player)
                    ? InteractionResult.FAIL : InteractionResult.PASS;
            selectPedestal(serverPlayer, pedestal.get().heroId());
            return InteractionResult.SUCCESS;
        });
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (entity instanceof ArenaActorEntity actor && player instanceof ServerPlayer serverPlayer
                    && lobby != null && lobby.match().state() == MatchState.PLAYING)
                return arenaEntry.attackActor(serverPlayer, actor)
                        ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            return countdownLocked(player) || inLobbyRoom(player) || inTestArena(player)
                    ? InteractionResult.FAIL : InteractionResult.PASS;
        });
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
                countdownLocked(player) || inLobbyRoom(player) || inTestArena(player)
                        ? InteractionResult.FAIL : InteractionResult.PASS);
        UseItemCallback.EVENT.register((player, level, hand) ->
                countdownLocked(player) || inLobbyRoom(player) || inTestArena(player)
                        ? InteractionResult.FAIL : InteractionResult.PASS);
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) ->
                !inLobbyRoom(player) && !inTestArena(player));
        ServerTickEvents.END_SERVER_TICK.register(FabricLobbyController::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            lobby = null;
            countdownTicks = 0;
            rosterDirty = false;
            arenaEntryFailed = false;
            finishTicks = -1;
            COUNTDOWN_LOCK.clear();
            arenaEntry = null;
        });
    }

    /** M13 installs a real arena loader/transfer; false keeps players safely in LOADING. */
    public static void setArenaEntry(ArenaEntry entry) {
        arenaEntry = java.util.Objects.requireNonNull(entry);
    }

    /** M13 replaces the test-only arena for every newly created match. */
    public static void setArenaEntryFactory(java.util.function.Function<MatchSession, ArenaEntry> factory) {
        arenaEntryFactory = java.util.Objects.requireNonNull(factory);
    }

    private static void selectPedestal(ServerPlayer player, String heroId) {
        applyPhysical(player, new ClientRequest.Lobby(
                ClientRequest.LobbyAction.SELECT_HERO, null, heroId, null, null, null));
    }

    private static void applyPhysical(ServerPlayer player, ClientRequest.Lobby intent) {
        var error = lobby.apply(player.getUUID(), intent);
        if (error == null) broadcast(player.level().getServer());
        else FabricMobaNetworking.sendLobby(player, new ServerMessage.RequestResultMessage(
                RequestResult.rejected(-1, error)));
    }

    private static boolean sameBlock(com.zeravorn.map.Position position, net.minecraft.core.BlockPos block) {
        return position.x() == block.getX() && position.y() == block.getY()
                && position.z() == block.getZ();
    }

    private static boolean countdownLocked(net.minecraft.world.entity.player.Player player) {
        return player instanceof ServerPlayer && lobby != null
                && lobby.match().state() == MatchState.COUNTDOWN
                && COUNTDOWN_LOCK.contains(player.getUUID());
    }

    private static boolean inLobbyRoom(net.minecraft.world.entity.player.Player player) {
        return player instanceof ServerPlayer && room != null
                && room.dimensionId().equals(player.level().dimension().identifier().toString());
    }

    private static boolean inTestArena(net.minecraft.world.entity.player.Player player) {
        return player instanceof ServerPlayer
                && ("zeravorn:test_arena".equals(player.level().dimension().identifier().toString())
                || "zeravorn:arena_v1".equals(player.level().dimension().identifier().toString()));
    }

    private static LobbyService ensure() {
        if (lobby == null) {
            lobby = new LobbyService(new MatchSession(UUID.randomUUID()));
            if (arenaEntry == null) arenaEntry = arenaEntryFactory.apply(lobby.match());
        }
        return lobby;
    }

    private static void resync(ServerPlayer player) {
        LobbyService current = ensure();
        if (current.snapshot().locked()) {
            if (!current.join(player.getUUID(), player.getName().getString())) return;
            if (!arenaEntry.reconnect(player.level().getServer(), current.snapshot(), player)) {
                com.zeravorn.Zeravorn.LOGGER.warn("Could not restore locked match for {}", player.getUUID());
            }
            broadcast(player.level().getServer());
            return;
        }
        if (!LobbyWorld.enter(player.level().getServer(), player)) return;
        if (!current.join(player.getUUID(), player.getName().getString())) return;
        broadcast(player.level().getServer());
    }

    private static void disconnect(UUID playerId) {
        if (lobby == null) return;
        lobby.disconnect(playerId);
        rosterDirty = true;
    }

    private static void tick(MinecraftServer server) {
        if (lobby == null) return;
        // Single owner of MatchSession elapsed time; arena adapter must not tick it again.
        if (lobby.match().state() == MatchState.PLAYING) {
            lobby.match().tick();
            arenaEntry.tick(server, lobby.match().elapsedTicks());
        }
        if (lobby.match().state() == MatchState.FINISHED) {
            if (finishTicks < 0) {
                finishTicks = 100;
                arenaEntry.finish();
                COUNTDOWN_LOCK.capture(server, lobby.snapshot());
                publishMatchState();
            }
            COUNTDOWN_LOCK.enforce(server);
            if (--finishTicks <= 0) {
                lobby.match().transitionTo(MatchState.POST_GAME);
                publishMatchState();
            }
        }
        if (lobby.match().state() == MatchState.POST_GAME) {
            returnToLobby(server);
            return;
        }
        if (rosterDirty) {
            rosterDirty = false;
            broadcast(server);
        }
        if (lobby.match().state() == MatchState.LOADING && !arenaEntryFailed) {
            try {
                if (arenaEntry.loadAndTransfer(server, lobby.snapshot())
                        && allAtArena(server, lobby.snapshot())) {
                    COUNTDOWN_LOCK.capture(server, lobby.snapshot());
                    lobby.match().transitionTo(MatchState.COUNTDOWN);
                    countdownTicks = 100;
                    publishMatchState();
                    broadcast(server);
                }
            } catch (RuntimeException exception) {
                arenaEntryFailed = true;
                com.zeravorn.Zeravorn.LOGGER.error("Arena entry failed; keeping match in LOADING", exception);
            }
        } else if (lobby.match().state() == MatchState.COUNTDOWN && --countdownTicks <= 0) {
            lobby.match().transitionTo(MatchState.PLAYING);
            COUNTDOWN_LOCK.clear();
            publishMatchState();
            broadcast(server);
        } else if (lobby.match().state() == MatchState.COUNTDOWN) {
            COUNTDOWN_LOCK.enforce(server);
            if (countdownTicks % 20 == 0) broadcast(server);
        }
    }

    private static void returnToLobby(MinecraftServer server) {
        // Keep the old session until every online participant has transferred successfully.
        for (LobbyPlayer participant : lobby.snapshot().players()) {
            ServerPlayer player = server.getPlayerList().getPlayer(participant.playerId());
            if (player != null && !inLobbyRoom(player) && !LobbyWorld.enter(server, player)) return;
        }
        for (LobbyPlayer participant : lobby.snapshot().players()) FabricMobaNetworking.unbind(participant.playerId());
        arenaEntry.close();
        COUNTDOWN_LOCK.clear();
        lobby = new LobbyService(new MatchSession(UUID.randomUUID()));
        arenaEntry = arenaEntryFactory.apply(lobby.match());
        countdownTicks = 0; finishTicks = -1; arenaEntryFailed = false; rosterDirty = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            FabricMobaNetworking.unbind(player.getUUID());
            lobby.join(player.getUUID(), player.getName().getString());
        }
        broadcast(server);
    }

    private static void publishMatchState() {
        var event = new ServerMessage.MatchStateEvent(lobby.match().state(),
                lobby.match().elapsedTicks(), lobby.match().winner());
        for (LobbyPlayer participant : lobby.snapshot().players())
            FabricMobaNetworking.sendMatchState(participant.playerId(), event);
    }

    private static boolean allAtArena(MinecraftServer server, LobbySnapshot snapshot) {
        String dimension = null;
        for (LobbyPlayer participant : snapshot.players()) {
            if (lobby.match().teams().roster(participant.team()).participants().stream()
                    .anyMatch(slot -> slot.playerId().equals(participant.playerId()) && slot.bot())) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(participant.playerId());
            if (player == null || !FabricMobaNetworking.hasActiveGateway(participant.playerId()))
                return false;
            String current = player.level().dimension().identifier().toString();
            if (current.equals(room.dimensionId())) return false;
            if (arenaEntry.destinationDimensionId() != null
                    && !arenaEntry.destinationDimensionId().equals(current)) return false;
            if (dimension == null) dimension = current;
            else if (!dimension.equals(current)) return false;
        }
        return dimension != null;
    }

    private static void broadcast(MinecraftServer server) {
        if (server == null || lobby == null) return;
        var state = lobby.snapshot();
        var snapshot = new LobbySnapshot(state.matchId(), state.phase(), state.locked(),
                state.blueVacantSlots(), state.redVacantSlots(), countdownTicks, state.players());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            FabricMobaNetworking.sendLobby(player, new ServerMessage.LobbyStateMessage(snapshot,
                    FabricMobaNetworking.lobbySequence(player.getUUID())));
        }
    }

    @FunctionalInterface
    public interface ArenaEntry {
        /** True only after the arena is ready, heroes/gateways are attached, and players reach fountains. */
        boolean loadAndTransfer(MinecraftServer server, LobbySnapshot lockedSelection);

        /** Rebind a previously locked participant without changing team, hero or spell. */
        default boolean reconnect(MinecraftServer server, LobbySnapshot selection, ServerPlayer player) {
            return false;
        }

        /** Optional exact world ID; a real arena adapter should provide it. */
        default String destinationDimensionId() { return null; }

        /** Drive arena actors from the match clock after the arena has loaded. */
        default void tick(MinecraftServer server, long elapsedTicks) { }

        default boolean attackActor(ServerPlayer player, ArenaActorEntity entity) { return false; }
        default void finish() { }
        default void close() { }
    }
}
