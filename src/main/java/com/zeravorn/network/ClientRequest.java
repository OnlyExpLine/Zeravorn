package com.zeravorn.network;

import com.zeravorn.map.Position;
import com.zeravorn.spell.SummonerSpell;
import com.zeravorn.team.TeamId;

import java.util.UUID;

/** Intent only: none of these fields is trusted as authoritative game state. */
public sealed interface ClientRequest permits ClientRequest.BasicAttack, ClientRequest.CastAbility,
        ClientRequest.UpgradeAbility, ClientRequest.UseSpell, ClientRequest.BuyItem,
        ClientRequest.SellItem, ClientRequest.Recall, ClientRequest.Lobby {

    record BasicAttack(Position aimDirection, UUID targetHint) implements ClientRequest { }

    record CastAbility(String abilityId, UUID targetId, Position aimDirection,
                       Position groundPoint) implements ClientRequest { }

    record UpgradeAbility(String abilityId) implements ClientRequest { }

    record UseSpell(SummonerSpell spell, UUID targetId, Position aimDirection,
                    Position groundPoint) implements ClientRequest { }

    record BuyItem(String itemId) implements ClientRequest { }

    record SellItem(int slot) implements ClientRequest { }

    record Recall(RecallAction action) implements ClientRequest { }

    enum RecallAction { START, CANCEL }

    record Lobby(LobbyAction action, TeamId team, String heroId,
                 SummonerSpell spell, Boolean ready, Boolean jungler) implements ClientRequest {
        public Lobby(LobbyAction action, TeamId team, String heroId,
                     SummonerSpell spell, Boolean ready) {
            this(action, team, heroId, spell, ready, null);
        }
    }

    enum LobbyAction { SELECT_TEAM, SELECT_HERO, SELECT_SPELL, SET_READY, SET_JUNGLER }
}
