package com.zeravorn.client;

import com.zeravorn.network.ClientRequest;
import com.zeravorn.hero.HeroDefinitions;
import com.zeravorn.ability.AbilitySlot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** C stats panel also exposes the documented skill-point plus controls. */
public final class StatsScreen extends Screen {
    private static final String[] SLOTS = {"Q", "E", "F", "R"};

    public StatsScreen() { super(Component.literal("Zeravorn Stats")); }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void onClose() {
        ClientInputRouter.statsClosed();
        super.onClose();
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        var hero = ClientMobaNetworking.state().ownHero();
        if (hero == null || hero.stats() == null) return;
        int x = width / 2 - 122, y = height / 2 - 108;
        g.fill(x, y, x + 244, y + 216, 0xE5151E2B);
        g.outline(x, y, 244, 216, 0xFF92A8BB);
        var stats = hero.stats();
        String[] lines = {
                "HP " + hero.health() + "/" + stats.maxHealth() + "  Regen " + stats.healthRegen(),
                "Mana " + hero.mana() + "/" + stats.maxMana() + "  Regen " + stats.manaRegen(),
                "Attack " + stats.attack() + "   AP " + stats.abilityPower(),
                "AS bonus " + stats.attackSpeedBonus() + "   Interval " + stats.attackInterval(),
                "Move " + stats.moveSpeed() + "   Lifesteal " + stats.lifesteal(),
                "Spell Vamp " + stats.spellVamp() + "   CDR " + hero.cooldownReduction(),
                "LVL " + hero.level() + "  Skill points " + hero.availableSkillPoints()
        };
        for (int i = 0; i < lines.length; i++) g.text(font, lines[i], x + 10, y + 10 + i * 15, 0xFFFFFFFF);
        g.text(font, "Buffs " + hero.buffTicks().keySet() + "  CC " + hero.crowdControlTicks().keySet(),
                x + 10, y + 112, 0xFFB8D7EE);
        int row = 0;
        for (String slot : SLOTS) {
            if (slot.equals("F") && !hero.heroId().equals("shelianer")) continue;
            var definition = HeroDefinitions.byId(hero.heroId()).ability(AbilitySlot.valueOf(slot));
            if (definition == null) continue;
            String id = definition.id();
            int rank = hero.abilityRanks().getOrDefault(id, 0);
            int max = definition.maxRank();
            int sy = y + 127 + row * 18;
            String label = slot.equals("R") && hero.level() < 4 ? "R LOCKED - LVL 4"
                    : rank == 0 ? slot + " NOT LEARNED (0/" + max + ")"
                    : slot + "  rank " + rank + "/" + max;
            g.text(font, label,
                    x + 14, sy, 0xFFFFFFFF);
            if (hero.availableSkillPoints() > 0 && rank < max && (!slot.equals("R") || hero.level() >= 4)) {
                g.fill(x + 192, sy - 2, x + 210, sy + 13, 0xFF346E4D);
                g.text(font, "+", x + 197, sy, 0xFFFFFFFF);
            }
            row++;
        }
        var result = ClientMobaNetworking.state().recentRequestFailure();
        if (result != null) g.text(font, result.errorCode().name(), x + 10, y + 201, 0xFFFF7777);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        var hero = ClientMobaNetworking.state().ownHero();
        if (event.button() != 0 || hero == null || hero.availableSkillPoints() <= 0) {
            return super.mouseClicked(event, doubled);
        }
        int x = width / 2 - 122, y = height / 2 - 108;
        if (event.x() < x + 192 || event.x() >= x + 210) return super.mouseClicked(event, doubled);
        if (event.y() < y + 125) return super.mouseClicked(event, doubled);
        int row = (int) (event.y() - y - 125) / 18;
        String[] shown = hero.heroId().equals("shelianer") ? SLOTS : new String[]{"Q", "E", "R"};
        if (row < 0 || row >= shown.length) return super.mouseClicked(event, doubled);
        String slot = shown[row];
        var definition = HeroDefinitions.byId(hero.heroId()).ability(AbilitySlot.valueOf(slot));
        if (definition == null) return true;
        String id = definition.id();
        int rank = hero.abilityRanks().getOrDefault(id, 0);
        int max = definition.maxRank();
        if (rank >= max || slot.equals("R") && hero.level() < 4) return true;
        ClientMobaNetworking.send(new ClientRequest.UpgradeAbility(id));
        return true;
    }
}
