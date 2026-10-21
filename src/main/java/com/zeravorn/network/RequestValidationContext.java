package com.zeravorn.network;

import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.match.MatchSession;
import com.zeravorn.spell.SummonerSpell;

import java.util.Objects;
import java.util.UUID;

/** Server-owned facts only; never deserialize these from a C2S packet. */
public record RequestValidationContext(MatchSession match, UUID senderId, HeroRuntime hero,
                                       SummonerSpell selectedSpell, boolean assignedJungler) {
    public RequestValidationContext {
        Objects.requireNonNull(match, "match");
        Objects.requireNonNull(senderId, "senderId");
    }
}
