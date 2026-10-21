package com.zeravorn.lobby;

import com.zeravorn.hero.HeroDefinitions;
import com.zeravorn.bot.BotRosterPlanner;
import com.zeravorn.match.MatchSession;
import com.zeravorn.match.MatchState;
import com.zeravorn.network.ClientRequest;
import com.zeravorn.network.RequestError;
import com.zeravorn.spell.SummonerSpell;
import com.zeravorn.team.MatchParticipant;
import com.zeravorn.team.TeamId;
import com.zeravorn.team.TeamRoster;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** All mutating methods run on the server thread. No client selection is authoritative. */
public final class LobbyService {
    /** Temporary testing allowance requested by the owner; turn off for final Ready rules. */
    public static final boolean ALLOW_READY_WITHOUT_HERO_FOR_TESTING = true;
    private final MatchSession match;
    private final Map<UUID, LobbyPlayer> players = new LinkedHashMap<>();
    private boolean locked;
    private java.util.List<BotRosterPlanner.Slot> botSlots = java.util.List.of();

    public LobbyService(MatchSession match) {
        this.match = Objects.requireNonNull(match, "match");
    }

    public MatchSession match() { return match; }
    public java.util.List<BotRosterPlanner.Slot> botSlots() { return botSlots; }

    public boolean join(UUID playerId, String nickname) {
        // Reconnect to a locked selection is read-only and preserves the original slot.
        if (players.containsKey(playerId)) return true;
        if (locked || match.state() != MatchState.LOBBY && match.state() != MatchState.HERO_SELECT)
            return false;
        players.put(playerId, new LobbyPlayer(playerId, nickname, null, null, null, false, false));
        if (match.state() == MatchState.LOBBY) match.transitionTo(MatchState.HERO_SELECT);
        return true;
    }

    public void disconnect(UUID playerId) {
        if (locked) return; // M14 reconnect retains the locked match participant.
        players.remove(playerId);
        match.teams().remove(playerId);
    }

    /** Returns null on success; stable error code otherwise. */
    public RequestError apply(UUID playerId, ClientRequest.Lobby request) {
        LobbyPlayer current = players.get(playerId);
        if (current == null || locked || match.state() != MatchState.HERO_SELECT)
            return RequestError.WRONG_STATE;
        if (request == null || request.action() == null) return RequestError.INVALID_TARGET;
        return switch (request.action()) {
            case SELECT_TEAM -> selectTeam(current, request.team());
            case SELECT_HERO -> selectHero(current, request.heroId());
            case SELECT_SPELL -> selectSpell(current, request.spell());
            case SET_JUNGLER -> setJungler(current, request.jungler());
            case SET_READY -> setReady(current, request.ready());
        };
    }

    private RequestError selectTeam(LobbyPlayer player, TeamId team) {
        if (team == null) return RequestError.INVALID_TARGET;
        if (team == player.team()) return null;
        if (match.teams().roster(team).isFull()) return RequestError.TEAM_FULL;
        // Capacity has been checked before removing the previous slot.
        match.teams().remove(player.playerId());
        if (!match.teams().add(team, new MatchParticipant(player.playerId(), player.nickname(), false)))
            throw new IllegalStateException("Checked team capacity changed on the server thread");
        players.put(player.playerId(), new LobbyPlayer(player.playerId(), player.nickname(), team,
                null, null, false, false));
        return null;
    }

    private RequestError selectHero(LobbyPlayer player, String heroId) {
        if (player.team() == null || heroId == null || HeroDefinitions.all().stream()
                .noneMatch(hero -> hero.id().equals(heroId))) return RequestError.INVALID_TARGET;
        if (players.values().stream().anyMatch(other -> !other.playerId().equals(player.playerId())
                && other.team() == player.team() && heroId.equals(other.heroId())))
            return RequestError.UNAVAILABLE_FOR_TEAM;
        players.put(player.playerId(), new LobbyPlayer(player.playerId(), player.nickname(), player.team(),
                heroId, player.spell(), player.jungler(), false));
        return null;
    }

    private RequestError selectSpell(LobbyPlayer player, SummonerSpell spell) {
        if (player.team() == null || spell == null) return RequestError.INVALID_TARGET;
        if (spell == SummonerSpell.RETRIBUTION && !player.jungler()) return RequestError.JUNGLER_ONLY;
        players.put(player.playerId(), new LobbyPlayer(player.playerId(), player.nickname(), player.team(),
                player.heroId(), spell, player.jungler(), false));
        return null;
    }

    private RequestError setJungler(LobbyPlayer player, Boolean jungler) {
        if (player.team() == null || jungler == null) return RequestError.INVALID_TARGET;
        if (jungler && players.values().stream().anyMatch(other -> !other.playerId().equals(player.playerId())
                && other.team() == player.team() && other.jungler())) return RequestError.JUNGLER_TAKEN;
        SummonerSpell spell = !jungler && player.spell() == SummonerSpell.RETRIBUTION ? null : player.spell();
        players.put(player.playerId(), new LobbyPlayer(player.playerId(), player.nickname(), player.team(),
                player.heroId(), spell, jungler, false));
        return null;
    }

    private RequestError setReady(LobbyPlayer player, Boolean ready) {
        if (ready == null) return RequestError.INVALID_TARGET;
        if (ready && (player.team() == null || player.spell() == null
                || !ALLOW_READY_WITHOUT_HERO_FOR_TESTING && player.heroId() == null))
            return RequestError.INCOMPLETE_SELECTION;
        players.put(player.playerId(), new LobbyPlayer(player.playerId(), player.nickname(), player.team(),
                player.heroId(), player.spell(), player.jungler(), ready));
        if (ready && !players.isEmpty() && players.values().stream().allMatch(LobbyPlayer::ready)) lock();
        return null;
    }

    private void lock() {
        // Assign free heroes deterministically, first by join order, then hero definition order.
        for (LobbyPlayer player : new ArrayList<>(players.values())) {
            if (player.heroId() != null) continue;
            String hero = HeroDefinitions.all().stream().map(def -> def.id())
                    .filter(id -> players.values().stream().noneMatch(other ->
                            other.team() == player.team() && id.equals(other.heroId())))
                    .findFirst().orElseThrow(() -> new IllegalStateException("No free hero for team"));
            players.put(player.playerId(), new LobbyPlayer(player.playerId(), player.nickname(),
                    player.team(), hero, player.spell(), player.jungler(), true));
        }
        var planner = new BotRosterPlanner();
        var planned = new ArrayList<BotRosterPlanner.Slot>();
        for (TeamId team : TeamId.values()) {
            var humans = players.values().stream().filter(player -> player.team() == team).toList();
            for (var bot : planner.fill(match.matchId(), team,
                    humans.stream().map(LobbyPlayer::heroId).toList(), humans.size())) {
                if (!match.teams().add(team, new MatchParticipant(bot.id(), bot.name(), true)))
                    throw new IllegalStateException("Bot slot allocation failed");
                planned.add(bot);
            }
        }
        botSlots = java.util.List.copyOf(planned);
        locked = true;
        match.transitionTo(MatchState.LOADING);
    }

    public LobbySnapshot snapshot() {
        var lines = new ArrayList<>(players.values().stream()
                .sorted(Comparator.comparing(LobbyPlayer::nickname)).toList());
        for (var bot : botSlots) lines.add(new LobbyPlayer(bot.id(), bot.name(), bot.team(),
                bot.heroId(), SummonerSpell.FLASH, false, true));
        return new LobbySnapshot(match.matchId(), match.state(), locked,
                TeamRoster.MAX_SLOTS - match.teams().roster(TeamId.BLUE).size(),
                TeamRoster.MAX_SLOTS - match.teams().roster(TeamId.RED).size(), 0, lines);
    }
}
