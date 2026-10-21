package com.zeravorn.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.zeravorn.match.MatchState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;

/** Lobby selection opens at the in-world terminal; L is an optional shortcut. */
public final class LobbyUiController {
    private static final KeyMapping OPEN = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.zeravorn.lobby", InputConstants.Type.KEYSYM, InputConstants.KEY_L,
            ClientInputRouter.CATEGORY));

    private LobbyUiController() { }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(LobbyUiController::tick);
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!level.isClientSide() || !com.zeravorn.lobby.LobbyWorld.isSelectionTerminal(
                    level.dimension().identifier().toString(), hit.getBlockPos())) return InteractionResult.PASS;
            var lobby = ClientMobaNetworking.state().lobby();
            if (lobby == null || lobby.locked()) return InteractionResult.PASS;
            Minecraft.getInstance().setScreenAndShow(new LobbyScreen());
            return InteractionResult.SUCCESS;
        });
    }

    private static void tick(Minecraft client) {
        var lobby = ClientMobaNetworking.state().lobby();
        if (client.player == null || lobby == null) {
            return;
        }
        if (lobby.phase() == MatchState.PLAYING || lobby.phase() == MatchState.FINISHED) {
            if (client.gui.screen() instanceof LobbyScreen || client.gui.screen() instanceof LoadingScreen)
                client.setScreenAndShow(null);
            return;
        }
        if (lobby.phase() == MatchState.LOADING) {
            if (!(client.gui.screen() instanceof LoadingScreen)) client.setScreenAndShow(new LoadingScreen());
            return;
        }
        if (lobby.phase() == MatchState.COUNTDOWN) {
            if (client.gui.screen() instanceof LobbyScreen || client.gui.screen() instanceof LoadingScreen)
                client.setScreenAndShow(null);
            return;
        }
        if (OPEN.consumeClick() && client.gui.screen() == null) {
            client.setScreenAndShow(new LobbyScreen());
        }
    }
}
