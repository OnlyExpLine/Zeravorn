package com.zeravorn.client.mixin;

import com.zeravorn.client.ClientMobaNetworking;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevent vanilla hotbar scrolling while a server-confirmed MOBA match plays. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void zeravorn$blockHotbarScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (ClientMobaNetworking.state().playing()) ci.cancel();
    }
}
