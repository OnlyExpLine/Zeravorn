package com.zeravorn.client;

import com.zeravorn.item.ItemCatalog;
import com.zeravorn.item.ItemCategory;
import com.zeravorn.item.ItemDefinition;
import com.zeravorn.network.ClientRequest;
import com.zeravorn.hero.HeroDefinitions;
import com.zeravorn.hero.DamageType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Double-click buy/sell. The server alone sets gold and inventory. */
public final class ShopScreen extends Screen {
    private static final ItemCatalog CATALOG = new ItemCatalog();
    private ItemCategory category = ItemCategory.PHYSICAL;
    private String lastSelection;
    private long lastClickAt;

    public ShopScreen() { super(Component.literal("Zeravorn Shop")); }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void onClose() {
        ClientInputRouter.shopClosed();
        super.onClose();
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int x = width / 2 - 178;
        int y = height / 2 - 102;
        g.fill(x, y, x + 356, y + 204, 0xE5151E2B);
        g.outline(x, y, 356, 204, 0xFF92A8BB);
        var hero = ClientMobaNetworking.state().ownHero();
        g.text(font, "SHOP  Gold: " + (hero == null ? "-" : hero.gold()), x + 10, y + 8, 0xFFFFD577);
        var categories = ItemCategory.values();
        for (int i = 0; i < categories.length; i++) {
            int tx = x + 10 + i * 82;
            g.fill(tx, y + 25, tx + 78, y + 40,
                    categories[i] == category ? 0xFF3A658B : 0xFF273440);
            g.text(font, categories[i].name(), tx + 3, y + 28, 0xFFFFFFFF);
        }
        List<ItemDefinition> items = items();
        for (int i = 0; i < items.size() && i < 8; i++) {
            var item = items.get(i);
            int rowY = y + 47 + i * 17;
            g.fill(x + 10, rowY, x + 201, rowY + 15,
                    ("buy:" + item.id()).equals(lastSelection) ? 0xFF49627A : 0xAA293747);
            String reason = disabledReason(item, hero);
            g.text(font, item.id().replace('_', ' ') + "  " + item.price()
                    + (reason == null ? "" : "  [" + reason + "]"), x + 14, rowY + 3,
                    reason == null ? 0xFFFFFFFF : 0xFFAAAAAA);
        }
        g.text(font, "OWNED (double-click to sell)", x + 214, y + 48, 0xFFFFE0B6);
        if (hero != null) {
            for (int i = 0; i < hero.normalItems().size(); i++) {
                g.text(font, (i + 1) + ". " + hero.normalItems().get(i), x + 214,
                        y + 67 + i * 17, 0xFFFFFFFF);
            }
            g.text(font, "B. " + (hero.boots() == null ? "-" : hero.boots()), x + 214, y + 164, 0xFFFFFFFF);
        }
        ItemDefinition selected = CATALOG.find(lastSelection != null && lastSelection.startsWith("buy:")
                ? lastSelection.substring(4) : "");
        if (selected != null) {
            var s = selected.stats();
            g.text(font, "ATK " + s.attack() + " AP " + s.abilityPower() + " HP " + s.maxHealth()
                    + " MP " + s.maxMana() + " AS " + s.attackSpeed(),
                    x + 10, y + 165, 0xFFB8D7EE);
            g.text(font, "LS " + s.lifesteal() + " SV " + s.spellVamp() + " MS " + s.moveSpeed()
                    + " MPregen " + s.manaRegen(), x + 10, y + 178, 0xFFB8D7EE);
        } else {
            g.text(font, "Double-click item to buy or sell. Esc closes.", x + 10, y + 186, 0xFFB8C5D3);
        }
        var result = ClientMobaNetworking.state().recentRequestFailure();
        if (result != null) g.text(font, result.errorCode().name(), x + 214, y + 186, 0xFFFF7777);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() != 0) return super.mouseClicked(event, doubled);
        int x = width / 2 - 178;
        int y = height / 2 - 102;
        int mx = (int) event.x(), my = (int) event.y();
        if (my >= y + 25 && my < y + 40) {
            int tab = (mx - x - 10) / 82;
            if (mx >= x + 10 && tab >= 0 && tab < ItemCategory.values().length) {
                category = ItemCategory.values()[tab];
                lastSelection = null;
                return true;
            }
        }
        if (mx >= x + 10 && mx < x + 201 && my >= y + 47 && my < y + 183) {
            int row = (my - y - 47) / 17;
            var items = items();
            if (row >= 0 && row < items.size() && row < 8) {
                String id = items.get(row).id();
                if (doubleClick("buy:" + id)) ClientMobaNetworking.send(new ClientRequest.BuyItem(id));
                return true;
            }
        }
        var hero = ClientMobaNetworking.state().ownHero();
        if (hero != null && mx >= x + 214 && mx < x + 350) {
            int row = (my - y - 67) / 17;
            if (my >= y + 67 && row >= 0 && row < hero.normalItems().size()) {
                if (doubleClick("sell:" + row)) ClientMobaNetworking.send(new ClientRequest.SellItem(row));
                return true;
            }
            if (my >= y + 164 && my < y + 179 && hero.boots() != null) {
                if (doubleClick("sell:boots")) ClientMobaNetworking.send(new ClientRequest.SellItem(5));
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    private boolean doubleClick(String selection) {
        long now = System.currentTimeMillis();
        boolean confirmed = selection.equals(lastSelection) && now - lastClickAt <= 350;
        lastSelection = selection;
        lastClickAt = now;
        return confirmed;
    }

    private List<ItemDefinition> items() {
        return CATALOG.all().stream().filter(item -> item.category() == category).toList();
    }

    private String disabledReason(ItemDefinition item, com.zeravorn.network.ServerMessage.HeroDelta hero) {
        if (hero == null) return "NO_MATCH";
        if (hero.normalItems().contains(item.id()) || item.id().equals(hero.boots())) return "OWNED";
        var damage = HeroDefinitions.byId(hero.heroId()).damageType();
        if (item.category() == ItemCategory.PHYSICAL && damage == DamageType.MAGICAL
                || item.category() == ItemCategory.MAGICAL && damage == DamageType.PHYSICAL) {
            return "CLASS_RESTRICTED";
        }
        if (hero.gold() < item.price()) return "NO_GOLD";
        return null;
    }
}
