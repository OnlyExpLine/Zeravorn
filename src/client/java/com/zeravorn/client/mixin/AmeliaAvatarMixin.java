package com.zeravorn.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zeravorn.client.AmeliaMeshRenderer;
import com.zeravorn.client.ClientMobaNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class AmeliaAvatarMixin {
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"), cancellable = true)
    private void zeravorn$submitAmelia(LivingEntityRenderState state, PoseStack pose,
                                       SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (!(state instanceof AvatarRenderState avatar)) return;
        var client = Minecraft.getInstance();
        var hero = ClientMobaNetworking.state().ownHero();
        if (client.player == null || avatar.id != client.player.getId() || hero == null
                || !"amelia".equals(hero.heroId())) return;
        AmeliaMeshRenderer.submit(avatar, pose, collector);
        ci.cancel();
    }
}
