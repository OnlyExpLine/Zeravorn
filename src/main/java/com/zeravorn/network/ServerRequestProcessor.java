package com.zeravorn.network;

import com.zeravorn.ability.AbilityLevelService;
import com.zeravorn.item.ShopService;

import java.util.Objects;

/** Runs on the logical server thread. Never reports admission as execution. */
public final class ServerRequestProcessor {
    private final RequestSequenceGuard sequences;
    private final GameplayRequestValidator validator;
    private final AbilityLevelService abilityLevels;
    private final ShopService shop;
    private final GameplayActionAdapter actions;

    public ServerRequestProcessor(RequestSequenceGuard sequences, GameplayRequestValidator validator,
                                  AbilityLevelService abilityLevels, ShopService shop,
                                  GameplayActionAdapter actions) {
        this.sequences = Objects.requireNonNull(sequences);
        this.validator = Objects.requireNonNull(validator);
        this.abilityLevels = Objects.requireNonNull(abilityLevels);
        this.shop = Objects.requireNonNull(shop);
        this.actions = Objects.requireNonNull(actions);
    }

    public RequestResult process(RequestValidationContext context, RequestEnvelope request) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(request);
        RequestResult reserved = sequences.reserve(context.senderId(), request);
        if (!reserved.success()) return reserved;
        RequestError error = validator.validate(context, request.payload());
        if (error != null) return RequestResult.rejected(request.clientSequenceId(), error);
        if (request.payload() instanceof ClientRequest.UpgradeAbility upgrade) {
            var result = abilityLevels.tryUpgrade(context.match(), context.hero(), upgrade.abilityId());
            return fromReason(request.clientSequenceId(), result.upgraded(), result.reason());
        }
        if (request.payload() instanceof ClientRequest.BuyItem buy) {
            var result = shop.buy(context.hero(), buy.itemId());
            return fromReason(request.clientSequenceId(), result.success(), result.errorCode());
        }
        if (request.payload() instanceof ClientRequest.SellItem sell) {
            var result = sell.slot() == 5 ? shop.sellBoots(context.hero())
                    : shop.sellNormal(context.hero(), sell.slot());
            return fromReason(request.clientSequenceId(), result.success(), result.errorCode());
        }
        error = actions.execute(context, request.payload());
        return error == null ? RequestResult.accepted(request.clientSequenceId())
                : RequestResult.rejected(request.clientSequenceId(), error);
    }

    private static RequestResult fromReason(long sequence, boolean success, String reason) {
        if (success) return RequestResult.accepted(sequence);
        try {
            return RequestResult.rejected(sequence, RequestError.valueOf(reason));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Unmapped authoritative error: " + reason, exception);
        }
    }
}
