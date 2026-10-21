package com.zeravorn.network;

import com.zeravorn.Zeravorn;
import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.spell.SummonerSpell;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Fabric transport. Gameplay ownership is supplied by active match gateways. */
public final class FabricMobaNetworking {
    private static final Map<UUID, ServerMatchGateway> GATEWAYS = new HashMap<>();
    private static final RequestSequenceGuard LOBBY_SEQUENCES = new RequestSequenceGuard();
    private static LobbyRequestHandler lobbyHandler = (player, request) ->
            RequestResult.rejected(request.clientSequenceId(), RequestError.WRONG_STATE);
    private static java.util.function.Consumer<ServerPlayer> lobbyResync = player -> { };

    private FabricMobaNetworking() { }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(ServerboundMobaPayload.TYPE, ServerboundMobaPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClientboundMobaPayload.TYPE, ClientboundMobaPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ServerboundMobaPayload.TYPE, (payload, context) ->
                context.server().execute(() -> receive(context.player(), payload.json())));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                server.execute(() -> {
                    UUID playerId = handler.player.getUUID();
                    var gateway = GATEWAYS.get(playerId);
                    if (gateway != null) gateway.disconnect(playerId);
                    lobbyDisconnect.accept(playerId);
                    LOBBY_SEQUENCES.clearPlayer(playerId);
                }));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerMatchGateway gateway : java.util.Set.copyOf(GATEWAYS.values())) gateway.tick();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            GATEWAYS.clear();
            LOBBY_SEQUENCES.clearMatch();
            // Callbacks are mod registrations, not per-world state; keep them for the next server.
        });
    }

    public static void bind(UUID playerId, ServerMatchGateway gateway) {
        GATEWAYS.put(playerId, gateway);
    }

    /** Match-entry/reconnect call: register transport before sending the snapshot. */
    public static void attach(ServerPlayer player, ServerMatchGateway gateway, HeroRuntime hero,
                              SummonerSpell selectedSpell, boolean assignedJungler) {
        bind(player.getUUID(), gateway);
        gateway.bind(player, hero, selectedSpell, assignedJungler);
    }

    public static void unbind(UUID playerId) {
        GATEWAYS.remove(playerId);
    }

    /** Match entry must attach a gameplay gateway before countdown can begin. */
    public static boolean hasActiveGateway(UUID playerId) {
        var gateway = GATEWAYS.get(playerId);
        return gateway != null && gateway.contains(playerId);
    }

    public static void sendMatchState(UUID playerId, ServerMessage.MatchStateEvent event) {
        var gateway = GATEWAYS.get(playerId);
        if (gateway != null && gateway.contains(playerId)) gateway.sendTo(playerId, event);
    }

    /** M11 supplies the server-authoritative lobby state machine here. */
    public static void setLobbyHandler(LobbyRequestHandler handler) {
        lobbyHandler = java.util.Objects.requireNonNull(handler);
    }

    private static java.util.function.Consumer<UUID> lobbyDisconnect = id -> { };

    public static void setLobbyLifecycle(java.util.function.Consumer<ServerPlayer> resync,
                                         java.util.function.Consumer<UUID> disconnect) {
        lobbyResync = java.util.Objects.requireNonNull(resync);
        lobbyDisconnect = java.util.Objects.requireNonNull(disconnect);
    }

    public static long lobbySequence(UUID playerId) { return LOBBY_SEQUENCES.nextSequence(playerId); }

    public static void sendLobby(ServerPlayer player, ServerMessage message) { send(player, message); }

    private static void receive(ServerPlayer player, String json) {
        UUID playerId = player.getUUID();
        var gateway = GATEWAYS.get(playerId);
        if ("resync".equals(json)) {
            if (gateway != null && gateway.contains(playerId)) gateway.sendSnapshot(playerId);
            else lobbyResync.accept(player);
            return;
        }
        try {
            RequestEnvelope request = NetworkWireCodec.decodeRequest(json);
            if (gateway != null) {
                gateway.handle(playerId, request);
            } else {
                RequestResult reserved = LOBBY_SEQUENCES.reserve(playerId, request);
                RequestResult result = !reserved.success() ? reserved
                        : request.payload() instanceof ClientRequest.Lobby
                        ? lobbyHandler.handle(player, request)
                        : RequestResult.rejected(request.clientSequenceId(), RequestError.WRONG_STATE);
                send(player, new ServerMessage.RequestResultMessage(result));
            }
        } catch (RuntimeException exception) {
            Zeravorn.LOGGER.warn("Rejected malformed Zeravorn packet from {}", playerId, exception);
        }
    }

    private static void send(ServerPlayer player, ServerMessage message) {
        if (ServerPlayNetworking.canSend(player, ClientboundMobaPayload.TYPE)) {
            ServerPlayNetworking.send(player, new ClientboundMobaPayload(NetworkWireCodec.encodeMessage(message)));
        }
    }

    @FunctionalInterface
    public interface LobbyRequestHandler {
        RequestResult handle(ServerPlayer player, RequestEnvelope request);
    }

}
