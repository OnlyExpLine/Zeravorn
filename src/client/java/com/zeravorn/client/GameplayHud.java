package com.zeravorn.client;

import com.zeravorn.Zeravorn;
import com.zeravorn.network.ServerMessage;
import com.zeravorn.hero.HeroProgressionConfig;
import com.zeravorn.hero.HeroDefinitions;
import com.zeravorn.ability.AbilitySlot;
import com.zeravorn.match.MatchState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Read-only rendering of confirmed network state. */
public final class GameplayHud {
    private GameplayHud() { }

    public static void initialize() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Zeravorn.id("gameplay_hud"),
                (graphics, deltaTracker) -> render(graphics));
    }

    private static void render(GuiGraphicsExtractor graphics) {
        var state = ClientMobaNetworking.state();
        var hero = state.ownHero();
        var match = state.match();
        if (match != null && (match.state() == MatchState.FINISHED || match.state() == MatchState.POST_GAME)) {
            var font = Minecraft.getInstance().font;
            int x = graphics.guiWidth() / 2, y = graphics.guiHeight() / 2;
            graphics.fill(x - 155, y - 35, x + 155, y + 25, 0xD0101828);
            boolean won = hero != null && hero.team() == match.winner();
            graphics.centeredText(font, won ? "VICTORY" : "DEFEAT", x, y - 20, won ? 0xFF7DFFB1 : 0xFFFF6A7A);
            graphics.centeredText(font, "Returning to lobby...", x, y + 3, 0xFFFFFFFF);
            return;
        }
        if (hero == null || match == null
                || match.state() != MatchState.PLAYING && match.state() != MatchState.COUNTDOWN) return;
        var font = Minecraft.getInstance().font;
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        var board = state.scoreboard();
        String teamScore = board == null ? "-:-" : board.blueScore() + ":" + board.redScore();
        graphics.centeredText(font, "ZERAVORN  " + clock(match.elapsedTicks()) + "  " + teamScore
                + "  " + match.state(), w / 2, 5, 0xFFFFFFFF);
        if (Minecraft.getInstance().player != null && "zeravorn:test_arena".equals(
                Minecraft.getInstance().player.level().dimension().identifier().toString()))
            graphics.centeredText(font, "TEST ARENA: entry flow only, combat disabled",
                    w / 2, 17, 0xFFFFC06A);
        if (match.state() == MatchState.COUNTDOWN) {
            var lobby = state.lobby();
            int seconds = lobby == null ? 5 : Math.max(1, (lobby.countdownTicks() + 19) / 20);
            graphics.centeredText(font, String.valueOf(seconds), w / 2, h / 2 - 24, 0xFFFFD577);
        } else if (state.showStart()) {
            graphics.centeredText(font, "START", w / 2, h / 2 - 24, 0xFF77FF77);
        }
        int baseY = h - 67;
        bar(graphics, 8, baseY, 130, 8, hero.health(), hero.maxHealth(), 0xFFB8333A);
        graphics.text(font, "HP " + hero.health() + "/" + hero.maxHealth(), 10, baseY - 11, 0xFFFFFFFF);
        if (hero.maxMana() > 0) {
            bar(graphics, 8, baseY + 16, 130, 8, hero.mana(), hero.maxMana(), 0xFF367FC2);
            graphics.text(font, "MP " + hero.mana() + "/" + hero.maxMana(), 10, baseY + 25, 0xFFE0EFFF);
        }
        graphics.text(font, "LVL " + hero.level() + "  XP " + hero.experience() + "  Gold " + hero.gold(),
                8, baseY + 39, 0xFFFFD574);
        if (hero.level() < 10) {
            int start = HeroProgressionConfig.xpToLevel(hero.level());
            int next = HeroProgressionConfig.xpToLevel(hero.level() + 1);
            bar(graphics, 8, baseY + 34, 130, 3, hero.experience() - start, next - start, 0xFFFFD574);
        }
        graphics.text(font, "K/D/A " + hero.kills() + "/" + hero.deaths() + "/" + hero.assists(),
                8, baseY + 51, 0xFFFFFFFF);
        drawAbilities(graphics, hero, w / 2 - 88, h - 49);
        if (hero.availableSkillPoints() > 0)
            graphics.centeredText(font, "C: spend skill points before using Q/E/R",
                    w / 2, h - 63, 0xFFB8F6FF);
        drawMinimap(graphics, w - 116, 20);
        var result = state.recentRequestFailure();
        if (result != null && !result.success()) {
            graphics.centeredText(font, result.errorCode().name(), w / 2, h - 98, 0xFFFF7777);
        }
        if (!hero.alive()) {
            long remaining = board == null ? 0 : board.players().stream()
                    .filter(line -> line.playerId().equals(hero.entityId()))
                    .mapToLong(ServerMessage.PlayerLine::respawnTicks).findFirst().orElse(0);
            graphics.fill(w / 2 - 103, h / 2 - 34, w / 2 + 103, h / 2 + 8, 0xC9101520);
            graphics.centeredText(font, "DEAD", w / 2, h / 2 - 26, 0xFFFF5555);
            graphics.centeredText(font, "RESPAWN IN " + (remaining + 19) / 20 + "s",
                    w / 2, h / 2 - 8, 0xFFFFFFFF);
        }
        if (!hero.buffTicks().isEmpty()) {
            graphics.text(font, "Buffs " + hero.buffTicks().keySet(), 8, baseY - 23, 0xFF80D4FF);
        }
        if (ClientInputRouter.scoreboardHeld() && board != null) drawScoreboard(graphics, board);
    }

    private static void drawAbilities(GuiGraphicsExtractor g, ServerMessage.HeroDelta hero, int x, int y) {
        List<String> slots = new ArrayList<>(List.of("Q", "E"));
        if ("shelianer".equals(hero.heroId())) slots.add("F");
        slots.add("R");
        var font = Minecraft.getInstance().font;
        int index = 0;
        for (String slot : slots) {
            var definition = HeroDefinitions.byId(hero.heroId()).ability(AbilitySlot.valueOf(slot));
            if (definition == null) continue;
            String id = definition.id();
            int rank = hero.abilityRanks().getOrDefault(id, 0);
            int maxRank = definition.maxRank();
            long cd = hero.cooldownTicks().getOrDefault(id, 0L);
            int sx = x + index * 34;
            g.fill(sx, y, sx + 30, y + 26, rank == 0 ? 0xCC393939 : 0xCC243A51);
            g.outline(sx, y, 30, 26, 0xFF93ADC7);
            g.text(font, slot + " " + rank + "/" + maxRank, sx + 3, y + 3, 0xFFFFFFFF);
            if (slot.equals("R") && hero.level() < 4) g.text(font, "LVL4", sx + 2, y + 15, 0xFFFFA666);
            else if (rank == 0) g.text(font, "NEW", sx + 2, y + 15, 0xFFFFAAAA);
            else if (cd > 0) {
                g.fill(sx + 1, y + 13, sx + 29, y + 25, 0xD9000000);
                g.text(font, ((cd + 19) / 20) + "s", sx + 7, y + 15, 0xFFFFD386);
            }
            if (hero.availableSkillPoints() > 0 && rank < maxRank
                    && hero.level() >= definition.unlockLevel()) {
                g.text(font, "+", sx + 23, y - 9, 0xFF77FF77);
            }
            int manaCost = AbilityUiCatalog.manaCost(hero.heroId(), slot.toLowerCase(), rank);
            if (manaCost > 0) g.text(font, "MP" + manaCost, sx + 1, y + 28, 0xFF8ABEEE);
            index++;
        }
        if (hero.selectedSpell() != null) {
            int sx = x + index * 34;
            g.fill(sx, y, sx + 48, y + 26, 0xCC445A33);
            g.text(font, hero.selectedSpell().name().substring(0, Math.min(5, hero.selectedSpell().name().length())),
                    sx + 2, y + 3, 0xFFFFFFFF);
            long cd = hero.cooldownTicks().getOrDefault(hero.selectedSpell().name(), 0L);
            if (cd > 0) g.text(font, String.valueOf((cd + 19) / 20), sx + 18, y + 15, 0xFFFFD386);
        }
    }

    private static void drawMinimap(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 104, y + 104, 0xCC17202D);
        g.outline(x, y, 104, 104, 0xFF8AA7C2);
        var positions = ClientMobaNetworking.state().minimapPositions();
        if (positions.isEmpty()) return;
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (var pos : positions.values()) {
            minX = Math.min(minX, pos.x()); maxX = Math.max(maxX, pos.x());
            minZ = Math.min(minZ, pos.z()); maxZ = Math.max(maxZ, pos.z());
        }
        double spanX = Math.max(1, maxX - minX);
        double spanZ = Math.max(1, maxZ - minZ);
        for (var entry : positions.entrySet()) {
            int px = x + 5 + (int) Math.round(94 * (entry.getValue().x() - minX) / spanX);
            int py = y + 5 + (int) Math.round(94 * (entry.getValue().z() - minZ) / spanZ);
            int color = ClientMobaNetworking.state().ownHero().entityId().equals(entry.getKey())
                    ? 0xFF66EE88 : 0xFFFF7777;
            g.fill(px - 2, py - 2, px + 2, py + 2, color);
        }
    }

    private static void bar(GuiGraphicsExtractor g, int x, int y, int width, int height,
                            int current, int maximum, int color) {
        g.fill(x, y, x + width, y + height, 0xAA222222);
        int filled = maximum <= 0 ? 0 : Math.max(0, Math.min(width, current * width / maximum));
        g.fill(x, y, x + filled, y + height, color);
    }

    private static String clock(long ticks) {
        long seconds = ticks / 20;
        return "%02d:%02d".formatted(seconds / 60, seconds % 60);
    }

    private static void drawScoreboard(GuiGraphicsExtractor g, ServerMessage.ScoreboardMessage board) {
        int x = g.guiWidth() / 2 - 150, y = g.guiHeight() / 2 - 93;
        g.fill(x, y, x + 300, y + 186, 0xEE18212F);
        g.outline(x, y, 300, 186, 0xFFB0C6D8);
        var font = Minecraft.getInstance().font;
        g.centeredText(font, "BLUE " + board.blueScore() + "  :  " + board.redScore() + " RED",
                x + 150, y + 8, 0xFFFFFFFF);
        int row = 0;
        for (var player : board.players()) {
            if (row >= 10) break;
            int color = player.team() == com.zeravorn.team.TeamId.BLUE ? 0xFF8BC7FF : 0xFFFF9999;
            String line = player.nickname() + "  " + player.heroId() + "  L" + player.level()
                    + "  " + player.kills() + "/" + player.deaths() + "/" + player.assists()
                    + "  " + player.gold() + "g  " + String.join(",", player.items());
            g.text(font, line, x + 8, y + 27 + row * 15, color);
            row++;
        }
    }
}
