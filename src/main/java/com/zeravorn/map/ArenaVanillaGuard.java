package com.zeravorn.map;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;

/** Keeps Minecraft survival mechanics out of the server-authoritative MOBA arena. */
public final class ArenaVanillaGuard {
    private ArenaVanillaGuard() { }

    public static void initialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !(entity instanceof ServerPlayer player && player.level().dimension().equals(ArenaWorld.DIMENSION)));
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) ->
                !(entity instanceof ServerPlayer player && player.level().dimension().equals(ArenaWorld.DIMENSION)));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!player.level().dimension().equals(ArenaWorld.DIMENSION)) continue;
                if (player.getHealth() < player.getMaxHealth()) player.setHealth(player.getMaxHealth());
                var food = player.getFoodData();
                if (food.getFoodLevel() != 20) food.setFoodLevel(20);
                if (food.getSaturationLevel() < 20) food.setSaturation(20);
            }
        });
    }
}
