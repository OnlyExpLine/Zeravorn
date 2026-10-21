package com.zeravorn.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/** Hides Minecraft survival HUD only while the player is inside Zeravorn's arena. */
public final class ArenaHudGuard {
    private ArenaHudGuard() { }

    public static void initialize() {
        for (Identifier id : new Identifier[] {
                VanillaHudElements.HOTBAR, VanillaHudElements.ARMOR_BAR,
                VanillaHudElements.HEALTH_BAR, VanillaHudElements.FOOD_BAR,
                VanillaHudElements.AIR_BAR, VanillaHudElements.MOUNT_HEALTH,
                VanillaHudElements.INFO_BAR, VanillaHudElements.EXPERIENCE_LEVEL,
                VanillaHudElements.HELD_ITEM_TOOLTIP, VanillaHudElements.SCOREBOARD,
                VanillaHudElements.PLAYER_LIST
        }) {
            HudElementRegistry.replaceElement(id, original -> (graphics, tickCounter) -> {
                if (!inArena()) original.extractRenderState(graphics, tickCounter);
            });
        }
    }

    private static boolean inArena() {
        var player = Minecraft.getInstance().player;
        return player != null && "zeravorn:arena_v1".equals(
                player.level().dimension().identifier().toString());
    }
}
