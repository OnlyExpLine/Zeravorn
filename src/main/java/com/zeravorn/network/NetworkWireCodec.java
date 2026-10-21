package com.zeravorn.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Objects;

/** Explicit discriminators: the peer never chooses an arbitrary Java class. */
public final class NetworkWireCodec {
    private static final Gson GSON = new Gson();
    private NetworkWireCodec() { }

    public static String encodeRequest(RequestEnvelope request) {
        JsonObject root = new JsonObject();
        root.addProperty("protocolVersion", request.protocolVersion());
        root.addProperty("clientSequenceId", request.clientSequenceId());
        root.addProperty("kind", requestKind(request.payload()));
        root.add("payload", GSON.toJsonTree(request.payload()));
        return root.toString();
    }

    public static RequestEnvelope decodeRequest(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        String kind = root.get("kind").getAsString();
        var payload = root.get("payload");
        ClientRequest request = switch (kind) {
            case "basic_attack" -> GSON.fromJson(payload, ClientRequest.BasicAttack.class);
            case "cast_ability" -> GSON.fromJson(payload, ClientRequest.CastAbility.class);
            case "upgrade_ability" -> GSON.fromJson(payload, ClientRequest.UpgradeAbility.class);
            case "use_spell" -> GSON.fromJson(payload, ClientRequest.UseSpell.class);
            case "buy_item" -> GSON.fromJson(payload, ClientRequest.BuyItem.class);
            case "sell_item" -> GSON.fromJson(payload, ClientRequest.SellItem.class);
            case "recall" -> GSON.fromJson(payload, ClientRequest.Recall.class);
            case "lobby" -> GSON.fromJson(payload, ClientRequest.Lobby.class);
            default -> throw new IllegalArgumentException("Unknown C2S kind: " + kind);
        };
        return new RequestEnvelope(root.get("protocolVersion").getAsInt(),
                root.get("clientSequenceId").getAsLong(), Objects.requireNonNull(request));
    }

    public static String encodeMessage(ServerMessage message) {
        JsonObject root = new JsonObject();
        root.addProperty("kind", messageKind(message));
        root.add("payload", GSON.toJsonTree(message));
        return root.toString();
    }

    public static ServerMessage decodeMessage(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        String kind = root.get("kind").getAsString();
        var payload = root.get("payload");
        return switch (kind) {
            case "snapshot" -> GSON.fromJson(payload, ServerMessage.Snapshot.class);
            case "hero_delta" -> GSON.fromJson(payload, ServerMessage.HeroDelta.class);
            case "ability_rank_changed" -> GSON.fromJson(payload, ServerMessage.AbilityRankChanged.class);
            case "ability_event" -> GSON.fromJson(payload, ServerMessage.AbilityEventMessage.class);
            case "projectile_event" -> GSON.fromJson(payload, ServerMessage.ProjectileEvent.class);
            case "match_state" -> GSON.fromJson(payload, ServerMessage.MatchStateEvent.class);
            case "vision_delta" -> GSON.fromJson(payload, ServerMessage.VisionDeltaMessage.class);
            case "structure_state" -> GSON.fromJson(payload, ServerMessage.StructureState.class);
            case "minimap_delta" -> GSON.fromJson(payload, ServerMessage.MinimapDeltaMessage.class);
            case "combat_event" -> GSON.fromJson(payload, ServerMessage.CombatEventMessage.class);
            case "request_result" -> GSON.fromJson(payload, ServerMessage.RequestResultMessage.class);
            case "scoreboard" -> GSON.fromJson(payload, ServerMessage.ScoreboardMessage.class);
            case "sequence_floor" -> GSON.fromJson(payload, ServerMessage.SequenceFloor.class);
            case "lobby_state" -> GSON.fromJson(payload, ServerMessage.LobbyStateMessage.class);
            default -> throw new IllegalArgumentException("Unknown S2C kind: " + kind);
        };
    }

    private static String requestKind(ClientRequest request) {
        return switch (request) {
            case ClientRequest.BasicAttack ignored -> "basic_attack";
            case ClientRequest.CastAbility ignored -> "cast_ability";
            case ClientRequest.UpgradeAbility ignored -> "upgrade_ability";
            case ClientRequest.UseSpell ignored -> "use_spell";
            case ClientRequest.BuyItem ignored -> "buy_item";
            case ClientRequest.SellItem ignored -> "sell_item";
            case ClientRequest.Recall ignored -> "recall";
            case ClientRequest.Lobby ignored -> "lobby";
        };
    }

    private static String messageKind(ServerMessage message) {
        return switch (message) {
            case ServerMessage.Snapshot ignored -> "snapshot";
            case ServerMessage.HeroDelta ignored -> "hero_delta";
            case ServerMessage.AbilityRankChanged ignored -> "ability_rank_changed";
            case ServerMessage.AbilityEventMessage ignored -> "ability_event";
            case ServerMessage.ProjectileEvent ignored -> "projectile_event";
            case ServerMessage.MatchStateEvent ignored -> "match_state";
            case ServerMessage.VisionDeltaMessage ignored -> "vision_delta";
            case ServerMessage.StructureState ignored -> "structure_state";
            case ServerMessage.MinimapDeltaMessage ignored -> "minimap_delta";
            case ServerMessage.CombatEventMessage ignored -> "combat_event";
            case ServerMessage.RequestResultMessage ignored -> "request_result";
            case ServerMessage.ScoreboardMessage ignored -> "scoreboard";
            case ServerMessage.SequenceFloor ignored -> "sequence_floor";
            case ServerMessage.LobbyStateMessage ignored -> "lobby_state";
        };
    }
}
