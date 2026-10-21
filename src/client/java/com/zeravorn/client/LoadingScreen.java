package com.zeravorn.client;

import com.zeravorn.lobby.LobbyPlayer;
import com.zeravorn.team.TeamId;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Read-only loading roster. World readiness is server-owned, never a client progress guess. */
public final class LoadingScreen extends Screen {
    public LoadingScreen() { super(Component.literal("Zeravorn Loading")); }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        var lobby = ClientMobaNetworking.state().lobby();
        if (lobby == null) return;
        int x = width / 2 - 178, y = height / 2 - 112;
        g.fill(x, y, x + 356, y + 224, 0xF0101721);
        g.outline(x, y, 356, 224, 0xFF8BA8BE);
        g.centeredText(font, "ZERAVORN - LOADING ARENA", width / 2, y + 12, 0xFFFFD68B);
        g.text(font, "BLUE", x + 14, y + 38, 0xFF87B8FA);
        g.text(font, "RED", x + 185, y + 38, 0xFFFF8888);
        drawTeam(g, lobby.players(), TeamId.BLUE, x + 14, y + 54);
        drawTeam(g, lobby.players(), TeamId.RED, x + 185, y + 54);
        g.centeredText(font, "Server is validating and building the arena",
                width / 2, y + 195, 0xFFB6C9D7);
        g.centeredText(font, "Countdown starts only when every player is ready in-world",
                width / 2, y + 207, 0xFF94AABB);
    }

    private void drawTeam(GuiGraphicsExtractor g, java.util.List<LobbyPlayer> players,
                          TeamId team, int x, int y) {
        int row = 0;
        for (LobbyPlayer player : players) {
            if (player.team() != team || row >= 5) continue;
            int ry = y + row * 26;
            g.fill(x, ry, x + 20, ry + 20, team == TeamId.BLUE ? 0xFF395D87 : 0xFF8A4449);
            String initial = player.heroId() == null ? "?"
                    : player.heroId().substring(0, 1).toUpperCase();
            g.text(font, initial, x + 7, ry + 6, 0xFFFFFFFF);
            g.text(font, player.nickname(), x + 26, ry, 0xFFFFFFFF);
            String detail = (player.heroId() == null ? "-" : player.heroId())
                    + " / " + (player.spell() == null ? "-" : player.spell());
            g.text(font, detail, x + 26, ry + 11, 0xFFB6C9D7);
            row++;
        }
        for (; row < 5; row++)
            g.text(font, "Empty slot", x + 26, y + row * 26 + 5, 0xFF728493);
    }
}
