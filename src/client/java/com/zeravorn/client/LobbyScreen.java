package com.zeravorn.client;

import com.zeravorn.hero.HeroDefinitions;
import com.zeravorn.lobby.LobbyPlayer;
import com.zeravorn.lobby.LobbyService;
import com.zeravorn.network.ClientRequest;
import com.zeravorn.spell.SummonerSpell;
import com.zeravorn.team.TeamId;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/** Server-confirmed lobby selection, opened from the in-world terminal or L. */
public final class LobbyScreen extends Screen {
    private static final int W = 560, H = 374;
    private static final int GOLD = 0xFFF1C876, MUTED = 0xFF9BABBA;
    private static final int[] HERO_COLORS = {
            0xFFB46D50, 0xFF6F9DC6, 0xFFC29556, 0xFFB07DBF, 0xFF77A17C
    };

    public LobbyScreen() { super(Component.literal("Zeravorn Lobby")); }
    @Override public boolean isPauseScreen() { return false; }
    private int panelWidth() { return Math.min(W, width - 16); }
    private int left() { return (width - panelWidth()) / 2; }
    private int top() { return (height - Math.min(H, height - 16)) / 2; }
    private boolean compact() { return height < 390; }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        var snapshot = ClientMobaNetworking.state().lobby();
        if (snapshot == null) return;
        LobbyPlayer self = self();
        int x = left(), y = top(), w = panelWidth(), h = Math.min(H, height - 16);
        boolean compact = compact();
        int teamY = compact ? 48 : 68, heroY = compact ? 91 : 125;
        int cardH = compact ? 65 : 100, spellY = compact ? 177 : 255;
        g.fill(0, 0, width, height, 0xA9000710);
        g.fill(x - 3, y - 3, x + w + 3, y + h + 3, 0xFF5B7185);
        g.fill(x, y, x + w, y + h, 0xFF101B29);
        g.fill(x, y, x + w, y + (compact ? 30 : 43), 0xFF1D2F42);
        g.fill(x, y, x + 4, y + (compact ? 30 : 43), GOLD);
        g.text(font, "ZERAVORN  /  LOBBY", x + 14, y + (compact ? 10 : 11), 0xFFFFFFFF);
        if (!compact) g.text(font, "CHOOSE YOUR SIDE  -  PREPARE FOR BATTLE", x + 14, y + 25, MUTED);
        g.text(font, "ESC  RETURN TO ROOM", x + w - 124, y + 10, MUTED);

        g.text(font, "01  TEAM", x + 12, y + (compact ? 34 : 52), GOLD);
        int half = (w - 32) / 2;
        teamButton(g, x + 12, y + teamY, half, "BLUE", 5 - snapshot.blueVacantSlots(),
                0xFF62ADFF, self != null && self.team() == TeamId.BLUE, mx, my);
        teamButton(g, x + 20 + half, y + teamY, half, "RED", 5 - snapshot.redVacantSlots(),
                0xFFFF7777, self != null && self.team() == TeamId.RED, mx, my);

        g.text(font, "02  HERO", x + 12, y + (compact ? 78 : 109), GOLD);
        if (!compact) g.text(font, "Right-click a pedestal or choose here", x + w - 218, y + 109, MUTED);
        int gap = 6, cardW = (w - 24 - gap * 4) / 5;
        for (int i = 0; i < HeroDefinitions.all().size(); i++) {
            var hero = HeroDefinitions.all().get(i);
            boolean selected = self != null && hero.id().equals(self.heroId());
            boolean taken = self != null && self.team() != null && snapshot.players().stream()
                    .anyMatch(other -> !other.playerId().equals(self.playerId())
                            && other.team() == self.team() && hero.id().equals(other.heroId()));
            int cx = x + 12 + i * (cardW + gap), cy = y + heroY;
            panel(g, cx, cy, cardW, cardH, selected ? 0xFF294458 : 0xFF1A2B3B,
                    selected ? GOLD : taken ? 0xFF66474C : 0xFF40566B,
                    !taken && inside(mx, my, cx, cy, cardW, cardH));
            g.fill(cx + 2, cy + 2, cx + cardW - 2, cy + (compact ? 28 : 61), 0xFF182435);
            g.fill(cx + 2, cy + (compact ? 25 : 57), cx + cardW - 2, cy + (compact ? 28 : 61), HERO_COLORS[i % HERO_COLORS.length]);
            // Heraldic silhouette until the in-game hero renderer is available.
            g.fill(cx + cardW / 2 - (compact ? 5 : 10), cy + (compact ? 5 : 11),
                    cx + cardW / 2 + (compact ? 5 : 10), cy + (compact ? 15 : 31),
                    HERO_COLORS[i % HERO_COLORS.length]);
            g.fill(cx + cardW / 2 - (compact ? 12 : 19), cy + (compact ? 16 : 33),
                    cx + cardW / 2 + (compact ? 12 : 19), cy + (compact ? 24 : 55),
                    HERO_COLORS[i % HERO_COLORS.length]);
            g.centeredText(font, hero.id().toUpperCase(), cx + cardW / 2, cy + (compact ? 32 : 65),
                    taken ? 0xFF999999 : 0xFFFFFFFF);
            g.centeredText(font, selected ? "SELECTED" : taken ? "TAKEN" : hero.heroClass().name(),
                    cx + cardW / 2, cy + (compact ? 46 : 79), selected ? GOLD : MUTED);
        }

        g.text(font, "03  SUMMONER SPELL", x + 12, y + (compact ? 162 : 238), GOLD);
        int spellW = (w - 24 - gap * 3) / 4;
        for (int i = 0; i < SummonerSpell.values().length; i++) {
            SummonerSpell spell = SummonerSpell.values()[i];
            boolean disabled = spell == SummonerSpell.RETRIBUTION && (self == null || !self.jungler());
            choice(g, x + 12 + i * (spellW + gap), y + spellY, spellW, compact ? 20 : 25, spell.name(),
                    self != null && self.spell() == spell, disabled, mx, my);
        }
        if (!compact) g.text(font, self != null && self.jungler() ? "Jungler role active"
                : "Retribution requires the jungler role", x + 12, y + 286, MUTED);
        choice(g, x + w - (compact ? 240 : 111), y + (compact ? 200 : 282), 99, 19,
                self != null && self.jungler() ? "JUNGLER  ON" : "JUNGLER  OFF",
                self != null && self.jungler(), self == null || self.team() == null, mx, my);

        if (!compact) g.fill(x + 12, y + 310, x + w - 12, y + 311, 0xFF344B60);
        if (self != null) g.text(font,
                (self.team() == null ? "NO TEAM" : self.team().name()) + "  /  "
                        + (self.heroId() == null ? "AUTO HERO" : self.heroId().toUpperCase()) + "  /  "
                        + (self.spell() == null ? "NO SPELL" : self.spell().name()),
                x + 12, y + (compact ? 220 : 320), 0xFFE4EAF0);
        boolean canReady = self != null && self.team() != null && self.spell() != null;
        choice(g, x + w - 135, y + (compact ? 198 : 317), 123, compact ? 22 : 29,
                self != null && self.ready() ? "CANCEL READY" : "READY  >",
                self != null && self.ready(), !canReady, mx, my);
        var error = ClientMobaNetworking.state().recentRequestFailure();
        String footer = error != null && !error.success() ? "SERVER: " + error.errorCode()
                : LobbyService.ALLOW_READY_WITHOUT_HERO_FOR_TESTING
                ? "TEST MODE: a free hero is assigned if none is selected" : "Selection is confirmed by the server";
        g.text(font, footer, x + 12, y + (compact ? 231 : h - 18),
                error != null && !error.success() ? 0xFFFF9292 : MUTED);
    }

    private void teamButton(GuiGraphicsExtractor g, int x, int y, int w, String name, int count,
                            int accent, boolean selected, int mx, int my) {
        panel(g, x, y, w, 31, selected ? 0xFF29445B : 0xFF1A2B3B,
                selected ? accent : 0xFF40566B, inside(mx, my, x, y, w, 31));
        g.fill(x, y, x + 4, y + 31, accent);
        g.text(font, name, x + 13, y + 11, selected ? 0xFFFFFFFF : accent);
        g.text(font, count + "/5", x + w - 28, y + 11, 0xFFD8E5EF);
    }

    private void choice(GuiGraphicsExtractor g, int x, int y, int w, int h, String label,
                        boolean selected, boolean disabled, int mx, int my) {
        panel(g, x, y, w, h, disabled ? 0xFF28303A : selected ? 0xFF315844 : 0xFF254056,
                selected ? 0xFF79D69A : disabled ? 0xFF46505B : 0xFF5D829A,
                !disabled && inside(mx, my, x, y, w, h));
        g.centeredText(font, label, x + w / 2, y + (h - 8) / 2,
                disabled ? 0xFF77818A : 0xFFFFFFFF);
    }

    private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h,
                              int fill, int border, boolean hover) {
        g.fill(x, y, x + w, y + h, hover ? 0xFF36546A : fill);
        g.outline(x, y, w, h, border);
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() != 0) return super.mouseClicked(event, doubled);
        var snapshot = ClientMobaNetworking.state().lobby();
        if (snapshot == null || snapshot.locked()) return true;
        LobbyPlayer self = self();
        int x = left(), y = top(), w = panelWidth();
        int mx = (int) event.x(), my = (int) event.y();
        boolean compact = compact();
        int teamY = compact ? 48 : 68, heroY = compact ? 91 : 125;
        int cardH = compact ? 65 : 100, spellY = compact ? 177 : 255;
        ClientRequest.Lobby request = null;
        int half = (w - 32) / 2;
        if (inside(mx, my, x + 12, y + teamY, half, 31))
            request = intent(ClientRequest.LobbyAction.SELECT_TEAM, TeamId.BLUE, null, null, null, null);
        else if (inside(mx, my, x + 20 + half, y + teamY, half, 31))
            request = intent(ClientRequest.LobbyAction.SELECT_TEAM, TeamId.RED, null, null, null, null);
        int gap = 6, cardW = (w - 24 - gap * 4) / 5;
        for (int i = 0; i < HeroDefinitions.all().size(); i++)
            if (inside(mx, my, x + 12 + i * (cardW + gap), y + heroY, cardW, cardH))
                request = intent(ClientRequest.LobbyAction.SELECT_HERO, null,
                        HeroDefinitions.all().get(i).id(), null, null, null);
        int spellW = (w - 24 - gap * 3) / 4;
        for (int i = 0; i < SummonerSpell.values().length; i++) {
            SummonerSpell spell = SummonerSpell.values()[i];
            if (inside(mx, my, x + 12 + i * (spellW + gap), y + spellY, spellW, compact ? 20 : 25)
                    && (spell != SummonerSpell.RETRIBUTION || self != null && self.jungler()))
                request = intent(ClientRequest.LobbyAction.SELECT_SPELL, null, null, spell, null, null);
        }
        if (inside(mx, my, x + w - (compact ? 240 : 111), y + (compact ? 200 : 282), 99, 19)
                && self != null && self.team() != null)
            request = intent(ClientRequest.LobbyAction.SET_JUNGLER, null, null, null, null, !self.jungler());
        if (inside(mx, my, x + w - 135, y + (compact ? 198 : 317), 123, compact ? 22 : 29)
                && self != null && self.team() != null && self.spell() != null)
            request = intent(ClientRequest.LobbyAction.SET_READY, null, null, null, !self.ready(), null);
        if (request != null) ClientMobaNetworking.send(request);
        return request != null || super.mouseClicked(event, doubled);
    }

    private LobbyPlayer self() {
        var snapshot = ClientMobaNetworking.state().lobby();
        UUID id = minecraft == null || minecraft.player == null ? null : minecraft.player.getUUID();
        return snapshot == null ? null : snapshot.players().stream()
                .filter(player -> player.playerId().equals(id)).findFirst().orElse(null);
    }

    private static ClientRequest.Lobby intent(ClientRequest.LobbyAction action, TeamId team,
                                               String hero, SummonerSpell spell, Boolean ready,
                                               Boolean jungler) {
        return new ClientRequest.Lobby(action, team, hero, spell, ready, jungler);
    }
}
