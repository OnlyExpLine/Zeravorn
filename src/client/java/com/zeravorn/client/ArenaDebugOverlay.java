package com.zeravorn.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.zeravorn.Zeravorn;
import com.zeravorn.map.MapDefinition;
import com.zeravorn.map.MapLoader;
import com.zeravorn.map.Position;
import com.zeravorn.minion.Lane;
import com.zeravorn.team.TeamId;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** F9 shows the packaged logical map; it does not reveal live hidden enemies. */
public final class ArenaDebugOverlay {
    private static final MapDefinition MAP = MapLoader.loadDefault();
    private static final double HERO_VISION_RADIUS = new com.zeravorn.vision.VisionService()
            .radius(com.zeravorn.vision.VisionSourceType.HERO);
    private static final KeyMapping TOGGLE = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.zeravorn.arena_debug", InputConstants.Type.KEYSYM, InputConstants.KEY_F9,
            ClientInputRouter.CATEGORY));
    private static boolean visible;

    private ArenaDebugOverlay() { }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE.consumeClick()) visible = !visible;
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Zeravorn.id("arena_debug"),
                (graphics, ignored) -> render(graphics));
    }

    private static void render(GuiGraphicsExtractor g) {
        var client = Minecraft.getInstance();
        if (!visible || client.player == null || !"zeravorn:arena_v1".equals(
                client.player.level().dimension().identifier().toString())) return;
        int size = 150, x = g.guiWidth() - size - 9, y = g.guiHeight() - size - 20;
        g.fill(x - 4, y - 15, x + size + 4, y + size + 4, 0xE6141E2A);
        g.outline(x, y, size, size, 0xFFFFD377);
        g.text(client.font, "MAP DEBUG  F9", x, y - 12, 0xFFFFD377);
        for (Lane lane : Lane.values()) {
            var path = MAP.lanePaths().get(lane).get(TeamId.BLUE);
            for (Position point : path) dot(g, x, y, point, 0xFFD6D6D6, 2);
        }
        for (var tower : MAP.towers()) {
            int px = px(x, tower.position()), py = py(y, tower.position());
            int color = tower.team() == TeamId.BLUE ? 0xFF58A8FF : 0xFFFF6666;
            g.fill(px - 2, py - 2, px + 3, py + 3, color);
            g.outline(px - 5, py - 5, 10, 10, 0x888ED8FF); // tower-radius marker
        }
        for (var camp : MAP.jungleCamps()) {
            dot(g, x, y, camp.position(), 0xFFFFBD62, 2);
            int px = px(x, camp.position()), py = py(y, camp.position());
            int r = Math.max(2, (int) Math.round(camp.leashRadius() * size / 220));
            g.outline(px - r, py - r, 2 * r, 2 * r, 0x99FFBD62);
        }
        for (var bush : MAP.bushZones()) {
            int px = px(x, bush.center()), py = py(y, bush.center());
            g.fill(px - 2, py - 2, px + 3, py + 3, 0xFF6FC786);
        }
        for (TeamId team : TeamId.values()) {
            dot(g, x, y, MAP.spawns().get(team).position(),
                    team == TeamId.BLUE ? 0xFF58A8FF : 0xFFFF6666, 4);
            dot(g, x, y, MAP.thrones().get(team), 0xFFFFFFFF, 3);
            var danger = MAP.fountainDangerZones().get(team);
            int fx = px(x, danger.center()), fy = py(y, danger.center());
            int fr = Math.max(2, (int) Math.round(danger.radius() * size / 220));
            g.outline(fx - fr, fy - fr, fr * 2, fr * 2, 0x99FF5555);
        }
        Position player = new Position(client.player.getX(), client.player.getY(), client.player.getZ());
        dot(g, x, y, player, 0xFFFFFF55, 2);
        int vr = Math.max(2, (int) Math.round(HERO_VISION_RADIUS * size / 220));
        g.outline(px(x, player) - vr, py(y, player) - vr, vr * 2, vr * 2, 0x99FFFF55);
        g.text(client.font, "lane  tower  camp  bush", x, y + size + 7, 0xFFE6E6E6);
    }

    private static void dot(GuiGraphicsExtractor g, int x, int y, Position p, int color, int radius) {
        int px = px(x, p), py = py(y, p);
        g.fill(px - radius, py - radius, px + radius + 1, py + radius + 1, color);
    }
    private static int px(int x, Position p) {
        return x + (int) Math.round((p.x() - MAP.playableBoundary().minX()) * 150
                / (MAP.playableBoundary().maxX() - MAP.playableBoundary().minX()));
    }
    private static int py(int y, Position p) {
        return y + (int) Math.round((p.z() - MAP.playableBoundary().minZ()) * 150
                / (MAP.playableBoundary().maxZ() - MAP.playableBoundary().minZ()));
    }
}
