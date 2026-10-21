package com.zeravorn.network;

import com.zeravorn.map.Position;
import com.zeravorn.match.MatchState;
import com.zeravorn.spell.SummonerSpell;

import java.util.Objects;

/**
 * Admission checks shared by all server request handlers. Passing this check
 * does not authorize a hit, spend, cooldown or teleport: the authoritative
 * gameplay service must still validate its own live world state.
 */
public final class GameplayRequestValidator {
    public RequestError validate(RequestValidationContext context, ClientRequest request) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(request, "request");
        if (request instanceof ClientRequest.Lobby lobby) return validateLobby(context, lobby);
        if (!context.match().teams().contains(context.senderId())) return RequestError.INVALID_TARGET;
        if (context.match().state() != MatchState.PLAYING) return RequestError.WRONG_STATE;
        if (context.hero() == null || !context.senderId().equals(context.hero().owner())) {
            return RequestError.INVALID_TARGET;
        }
        if (!context.match().teams().roster(context.hero().team()).contains(context.senderId())) {
            return RequestError.INVALID_TARGET;
        }
        if (request instanceof ClientRequest.UpgradeAbility upgrade) {
            if (blank(upgrade.abilityId())) return RequestError.INVALID_ABILITY;
            return context.hero().definition().abilityById(upgrade.abilityId()) == null
                    ? RequestError.ABILITY_NOT_AVAILABLE : null;
        }
        if (request instanceof ClientRequest.BuyItem buy) {
            return blank(buy.itemId()) ? RequestError.INVALID_TARGET : null;
        }
        if (request instanceof ClientRequest.SellItem sell) {
            return sell.slot() < 0 ? RequestError.WRONG_SLOT : null;
        }
        if (!context.hero().alive()) return RequestError.DEAD;
        if (request instanceof ClientRequest.BasicAttack basic) {
            return validDirection(basic.aimDirection()) ? null : RequestError.INVALID_TARGET;
        }
        if (request instanceof ClientRequest.CastAbility cast) {
            if (blank(cast.abilityId())) return RequestError.INVALID_ABILITY;
            var ability = context.hero().definition().abilityById(cast.abilityId());
            if (ability == null) return RequestError.ABILITY_NOT_AVAILABLE;
            if (context.hero().abilityRank(cast.abilityId()) == 0) return RequestError.ABILITY_NOT_AVAILABLE;
            return validOptionalDirection(cast.aimDirection()) && validOptionalPoint(cast.groundPoint())
                    ? null : RequestError.INVALID_TARGET;
        }
        if (request instanceof ClientRequest.UseSpell spell) {
            if (spell.spell() == null || spell.spell() != context.selectedSpell()) return RequestError.INVALID_TARGET;
            if (spell.spell() == SummonerSpell.RETRIBUTION && !context.assignedJungler()) {
                return RequestError.INVALID_TARGET;
            }
            return validOptionalDirection(spell.aimDirection()) && validOptionalPoint(spell.groundPoint())
                    ? null : RequestError.INVALID_TARGET;
        }
        if (request instanceof ClientRequest.Recall recall) {
            return recall.action() == null ? RequestError.INVALID_TARGET : null;
        }
        throw new IllegalArgumentException("Unhandled client request: " + request.getClass().getName());
    }

    private RequestError validateLobby(RequestValidationContext context, ClientRequest.Lobby lobby) {
        var state = context.match().state();
        if (state != MatchState.LOBBY && state != MatchState.HERO_SELECT) return RequestError.WRONG_STATE;
        if (lobby.action() == null) return RequestError.INVALID_TARGET;
        return switch (lobby.action()) {
            case SELECT_TEAM -> lobby.team() == null ? RequestError.INVALID_TARGET : null;
            case SELECT_HERO -> blank(lobby.heroId()) ? RequestError.INVALID_TARGET : null;
            case SELECT_SPELL -> lobby.spell() == null ? RequestError.INVALID_TARGET : null;
            case SET_READY -> lobby.ready() == null ? RequestError.INVALID_TARGET : null;
            case SET_JUNGLER -> lobby.jungler() == null ? RequestError.INVALID_TARGET : null;
        };
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean validOptionalPoint(Position value) {
        return value == null || finite(value);
    }

    private static boolean validOptionalDirection(Position value) {
        return value == null || validDirection(value);
    }

    private static boolean validDirection(Position value) {
        return value != null && finite(value)
                && (value.x() != 0 || value.y() != 0 || value.z() != 0);
    }

    private static boolean finite(Position value) {
        return Double.isFinite(value.x()) && Double.isFinite(value.y()) && Double.isFinite(value.z());
    }
}
