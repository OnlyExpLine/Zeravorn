package com.zeravorn.network;

import com.zeravorn.Zeravorn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundMobaPayload(String json) implements CustomPacketPayload {
    public static final Type<ClientboundMobaPayload> TYPE = new Type<>(Zeravorn.id("moba_s2c"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundMobaPayload> CODEC =
            CustomPacketPayload.codec((payload, buffer) -> buffer.writeUtf(payload.json(), 32767),
                    buffer -> new ClientboundMobaPayload(buffer.readUtf(32767)));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
