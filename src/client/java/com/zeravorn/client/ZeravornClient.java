package com.zeravorn.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import com.zeravorn.map.ArenaActorEntity;

public class ZeravornClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ArenaActorEntity.TYPE, ArenaActorRenderer::new);
		ClientMobaNetworking.initialize();
		ClientInputRouter.initialize();
		ArenaHudGuard.initialize();
		GameplayHud.initialize();
		ArenaDebugOverlay.initialize();
		LobbyUiController.initialize();
	}
}
