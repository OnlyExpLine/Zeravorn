package com.zeravorn.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.zeravorn.map.Position;
import com.zeravorn.ability.AbilitySlot;
import com.zeravorn.hero.HeroDefinitions;
import com.zeravorn.network.ClientRequest;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;

import java.util.LinkedHashMap;
import java.util.Map;

/** MOBA keys are active only with a server-confirmed PLAYING snapshot. */
public final class ClientInputRouter {
    static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(com.zeravorn.Zeravorn.id("moba"));
    private static final KeyMapping BASIC = key("basic", InputConstants.Type.MOUSE, InputConstants.MOUSE_BUTTON_LEFT);
    private static final KeyMapping Q = key("q", InputConstants.Type.KEYSYM, InputConstants.KEY_Q);
    private static final KeyMapping E = key("e", InputConstants.Type.KEYSYM, InputConstants.KEY_E);
    private static final KeyMapping F = key("f", InputConstants.Type.KEYSYM, InputConstants.KEY_F);
    private static final KeyMapping R = key("r", InputConstants.Type.KEYSYM, InputConstants.KEY_R);
    private static final KeyMapping SPELL = key("spell", InputConstants.Type.KEYSYM, InputConstants.KEY_G);
    private static final KeyMapping SHOP = key("shop", InputConstants.Type.KEYSYM, InputConstants.KEY_B);
    private static final KeyMapping STATS = key("stats", InputConstants.Type.KEYSYM, InputConstants.KEY_C);
    private static final KeyMapping RECALL = key("recall", InputConstants.Type.KEYSYM, InputConstants.KEY_V);
    private static final KeyMapping SCOREBOARD = key("scoreboard", InputConstants.Type.KEYSYM, InputConstants.KEY_TAB);
    private static final Map<KeyMapping, InputConstants.Key> VANILLA_BINDINGS = new LinkedHashMap<>();
    private static boolean active;
    private static boolean shopOpen;
    private static boolean statsOpen;
    private static CameraType cameraBeforeDeath;

    private ClientInputRouter() { }

    private static KeyMapping key(String name, InputConstants.Type type, int code) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.zeravorn." + name, type, code, CATEGORY));
    }

    public static void initialize() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            var hero = ClientMobaNetworking.state().ownHero();
            if (client.player != null && ClientMobaNetworking.state().playing()
                    && hero != null && !hero.alive()) freezeMovement(client);
        });
        ClientTickEvents.END_CLIENT_TICK.register(ClientInputRouter::tick);
    }

    public static boolean statsHeld() { return active && STATS.isDown(); }
    public static boolean scoreboardHeld() { return active && SCOREBOARD.isDown(); }
    public static void shopClosed() { shopOpen = false; }
    public static void statsClosed() { statsOpen = false; }

    private static void tick(Minecraft client) {
        boolean shouldBeActive = client.player != null && ClientMobaNetworking.state().playing();
        if (shouldBeActive != active) {
            active = shouldBeActive;
            if (active) suppressVanilla(client);
            else {
                restoreVanilla();
                shopOpen = false;
                statsOpen = false;
            }
        }
        var currentHero = ClientMobaNetworking.state().ownHero();
        if (active && currentHero != null && !currentHero.alive()) {
            if (cameraBeforeDeath == null) cameraBeforeDeath = client.options.getCameraType();
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            freezeMovement(client);
        } else if (cameraBeforeDeath != null) {
            client.options.setCameraType(cameraBeforeDeath);
            cameraBeforeDeath = null;
        }
        if (!active || shopOpen || statsOpen) return;
        var hero = ClientMobaNetworking.state().ownHero();
        if (hero == null) return;
        if (SHOP.consumeClick()) {
            shopOpen = true;
            client.setScreenAndShow(new ShopScreen());
            return;
        }
        if (STATS.consumeClick()) {
            statsOpen = true;
            client.setScreenAndShow(new StatsScreen());
            return;
        }
        if (RECALL.consumeClick() && hero.alive()) {
            ClientMobaNetworking.send(new ClientRequest.Recall(ClientRequest.RecallAction.START));
        }
        if (!hero.alive()) return;
        while (BASIC.consumeClick()) {
            ClientMobaNetworking.send(new ClientRequest.BasicAttack(aim(client), targetHint(client)));
            if ("amelia".equals(hero.heroId())) AmeliaMeshRenderer.play("basic");
        }
        cast(client, Q, AbilitySlot.Q, hero.heroId());
        cast(client, E, AbilitySlot.E, hero.heroId());
        cast(client, R, AbilitySlot.R, hero.heroId());
        if ("shelianer".equals(hero.heroId())) cast(client, F, AbilitySlot.F, hero.heroId());
        else while (F.consumeClick()) { /* F is absent for other heroes. */ }
        while (SPELL.consumeClick()) {
            if (hero.selectedSpell() != null) {
                ClientMobaNetworking.send(new ClientRequest.UseSpell(hero.selectedSpell(),
                        targetHint(client), aim(client), groundPoint(client)));
            }
        }
    }

    private static void freezeMovement(Minecraft client) {
        client.options.keyUp.setDown(false);
        client.options.keyDown.setDown(false);
        client.options.keyLeft.setDown(false);
        client.options.keyRight.setDown(false);
        client.options.keyJump.setDown(false);
        client.options.keySprint.setDown(false);
        client.options.keyShift.setDown(false);
        client.player.setDeltaMovement(0, 0, 0);
    }

    private static void cast(Minecraft client, KeyMapping key, AbilitySlot slot, String heroId) {
        while (key.consumeClick()) {
            var ability = HeroDefinitions.byId(heroId).ability(slot);
            if (ability != null) ClientMobaNetworking.send(new ClientRequest.CastAbility(ability.id(),
                    targetHint(client), aim(client), groundPoint(client)));
            if (ability != null && "amelia".equals(heroId)
                    && ClientMobaNetworking.state().ownHero().abilityRanks().getOrDefault(ability.id(), 0) > 0)
                AmeliaMeshRenderer.play(switch (slot) {
                case Q -> "q_start";
                case E -> "e";
                case R -> "r";
                default -> "idle";
            });
        }
    }

    private static Position aim(Minecraft client) {
        var look = client.player.getLookAngle();
        return new Position(look.x, look.y, look.z);
    }

    private static java.util.UUID targetHint(Minecraft client) {
        return client.crosshairPickEntity == null ? null : client.crosshairPickEntity.getUUID();
    }

    private static Position groundPoint(Minecraft client) {
        if (client.hitResult == null) return null;
        var point = client.hitResult.getLocation();
        return new Position(point.x, point.y, point.z);
    }

    private static void suppressVanilla(Minecraft client) {
        remember(client.options.keyAttack);
        remember(client.options.keyDrop);
        remember(client.options.keyInventory);
        remember(client.options.keySwapOffhand);
        remember(client.options.keyPlayerList);
        for (KeyMapping hotbar : client.options.keyHotbarSlots) remember(hotbar);
        KeyMapping.resetMapping();
    }

    private static void remember(KeyMapping mapping) {
        VANILLA_BINDINGS.put(mapping, KeyMappingHelper.getBoundKeyOf(mapping));
        mapping.setKey(InputConstants.UNKNOWN);
        mapping.setDown(false);
    }

    private static void restoreVanilla() {
        for (var entry : VANILLA_BINDINGS.entrySet()) entry.getKey().setKey(entry.getValue());
        VANILLA_BINDINGS.clear();
        KeyMapping.resetMapping();
    }
}
