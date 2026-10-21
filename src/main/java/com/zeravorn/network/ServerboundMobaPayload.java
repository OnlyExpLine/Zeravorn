package com.zeravorn.network;

import com.zeravorn.Zeravorn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ServerboundMobaPayload(String json) implements CustomPacketPayload {
    public static final Type<ServerboundMobaPayload> TYPE = new Type<>(Zeravorn.id("moba_c2s"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundMobaPayload> CODEC =
            CustomPacketPayload.codec((payload, buffer) -> buffer.writeUtf(payload.json(), 32767),
                    buffer -> new ServerboundMobaPayload(buffer.readUtf(32767)));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
