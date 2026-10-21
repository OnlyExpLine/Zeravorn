package com.zeravorn.lobby;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Relative;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

/** Captures arena fountain positions and enforces the prep freeze until START. */
final class CountdownLock {
    private final Map<UUID, Anchor> anchors = new HashMap<>();

    void capture(MinecraftServer server, LobbySnapshot snapshot) {
        anchors.clear();
        for (LobbyPlayer participant : snapshot.players()) {
            ServerPlayer player = server.getPlayerList().getPlayer(participant.playerId());
            if (player != null) anchors.put(player.getUUID(),
                    new Anchor(player.level().dimension(),
                            player.getX(), player.getY(), player.getZ()));
        }
    }

    boolean contains(UUID playerId) { return anchors.containsKey(playerId); }

    void enforce(MinecraftServer server) {
        for (var entry : anchors.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Anchor anchor = entry.getValue();
            if (player == null) continue;
            if (!anchor.dimension().equals(player.level().dimension())) {
                var level = server.getLevel(anchor.dimension());
                if (level != null) player.teleportTo(level, anchor.x(), anchor.y(), anchor.z(),
                        Set.<Relative>of(), player.getYRot(), player.getXRot(), true);
                continue;
            }
            player.setDeltaMovement(0, 0, 0);
            if (Math.abs(player.getX() - anchor.x()) > 0.02
                    || Math.abs(player.getY() - anchor.y()) > 0.02
                    || Math.abs(player.getZ() - anchor.z()) > 0.02)
                player.teleportTo(anchor.x(), anchor.y(), anchor.z());
        }
    }

    void clear() { anchors.clear(); }

    private record Anchor(ResourceKey<Level> dimension, double x, double y, double z) { }
}
