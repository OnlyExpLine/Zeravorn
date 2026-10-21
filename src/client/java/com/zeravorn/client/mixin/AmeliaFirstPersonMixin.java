package com.zeravorn.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zeravorn.client.AmeliaFirstPersonRenderer;
import com.zeravorn.client.ClientMobaNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class AmeliaFirstPersonMixin {
    @Inject(method = "submitHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
            at = @At("HEAD"), cancellable = true)
    private void zeravorn$submitAmeliaHands(float partialTick, PoseStack pose,
                                           SubmitNodeCollector collector, LocalPlayer player,
                                           int light, CallbackInfo ci) {
        var hero = ClientMobaNetworking.state().ownHero();
        if (player != Minecraft.getInstance().player || hero == null
                || !"amelia".equals(hero.heroId())) return;
        if (AmeliaFirstPersonRenderer.submit(player, partialTick, pose, collector, light)) ci.cancel();
    }
}
