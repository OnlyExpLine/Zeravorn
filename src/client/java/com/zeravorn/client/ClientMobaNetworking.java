package com.zeravorn.client;

import com.zeravorn.Zeravorn;
import com.zeravorn.network.ClientReplicatedState;
import com.zeravorn.network.ClientRequest;
import com.zeravorn.network.ClientboundMobaPayload;
import com.zeravorn.network.NetworkWireCodec;
import com.zeravorn.network.RequestEnvelope;
import com.zeravorn.network.ServerboundMobaPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientMobaNetworking {
    private static final ClientReplicatedState STATE = new ClientReplicatedState();

    private ClientMobaNetworking() { }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(ClientboundMobaPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    try {
                        STATE.apply(NetworkWireCodec.decodeMessage(payload.json()));
                    } catch (RuntimeException exception) {
                        Zeravorn.LOGGER.warn("Ignored malformed Zeravorn server message", exception);
                    }
                }));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            STATE.clear();
            if (ClientPlayNetworking.canSend(ServerboundMobaPayload.TYPE)) {
                ClientPlayNetworking.send(new ServerboundMobaPayload("resync"));
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> STATE.clear());
    }

    public static ClientReplicatedState state() { return STATE; }

    public static boolean send(ClientRequest request) {
        if (!ClientPlayNetworking.canSend(ServerboundMobaPayload.TYPE)
                || STATE.matchId() == null && !(request instanceof ClientRequest.Lobby)) return false;
        var envelope = new RequestEnvelope(RequestEnvelope.CURRENT_VERSION, STATE.nextSequence(), request);
        ClientPlayNetworking.send(new ServerboundMobaPayload(NetworkWireCodec.encodeRequest(envelope)));
        return true;
    }
}
