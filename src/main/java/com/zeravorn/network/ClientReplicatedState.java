package com.zeravorn.network;

import com.zeravorn.map.Position;
import com.zeravorn.match.MatchState;
import com.zeravorn.lobby.LobbySnapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Client read model. Only S2C messages mutate it; C2S input never does. */
public final class ClientReplicatedState {
    private UUID matchId;
    private long nextClientSequenceId;
    private ServerMessage.HeroDelta ownHero;
    private ServerMessage.MatchStateEvent match;
    private final Map<String, ServerMessage.StructureState> structures = new HashMap<>();
    private final Set<UUID> visibleIds = new HashSet<>();
    private final Map<UUID, Position> minimapPositions = new HashMap<>();
    private RequestResult lastRequestResult;
    private long lastRequestResultAtMillis;
    private ServerMessage.ScoreboardMessage scoreboard;
    private LobbySnapshot lobby;
    private long startedAtMillis;
    private final ArrayDeque<ServerMessage> presentationEvents = new ArrayDeque<>();

    public void apply(ServerMessage message) {
        if (message instanceof ServerMessage.Snapshot snapshot) {
            if (!snapshot.matchId().equals(matchId)) {
                nextClientSequenceId = 0;
                lobby = null;
            }
            matchId = snapshot.matchId();
            nextClientSequenceId = Math.max(nextClientSequenceId, snapshot.nextClientSequenceId());
            ownHero = snapshot.ownHero();
            if (snapshot.match().state() == MatchState.PLAYING) lobby = null;
            match = snapshot.match();
            scoreboard = snapshot.scoreboard();
            presentationEvents.clear();
            structures.clear();
            for (var structure : snapshot.structures()) structures.put(structure.structureId(), structure);
            visibleIds.clear();
            visibleIds.addAll(snapshot.visibleIds());
            minimapPositions.clear();
            for (var entry : snapshot.minimapPositions().entrySet()) {
                if (visibleIds.contains(entry.getKey())) minimapPositions.put(entry.getKey(), entry.getValue());
            }
        } else if (message instanceof ServerMessage.HeroDelta delta) {
            ownHero = delta;
        } else if (message instanceof ServerMessage.MatchStateEvent event) {
            if (event.state() == MatchState.PLAYING && (match == null
                    || match.state() != MatchState.PLAYING)) startedAtMillis = System.currentTimeMillis();
            match = event;
            if (event.state() == MatchState.PLAYING) lobby = null;
        } else if (message instanceof ServerMessage.AbilityRankChanged rank && ownHero != null
                && rank.heroId().equals(ownHero.entityId())) {
            var ranks = new HashMap<>(ownHero.abilityRanks());
            ranks.put(rank.abilityId(), rank.newRank());
            ownHero = new ServerMessage.HeroDelta(ownHero.entityId(), ownHero.heroId(), ownHero.team(),
                    ownHero.alive(), ownHero.health(), ownHero.maxHealth(), ownHero.mana(), ownHero.maxMana(),
                    ownHero.level(), ownHero.experience(), ownHero.gold(), rank.remainingSkillPoints(),
                    ownHero.kills(), ownHero.deaths(), ownHero.assists(), ranks, ownHero.cooldownTicks(),
                    ownHero.normalItems(), ownHero.boots(), ownHero.selectedSpell(), ownHero.stats(),
                    ownHero.cooldownReduction(), ownHero.crowdControlTicks(), ownHero.buffTicks());
        } else if (message instanceof ServerMessage.StructureState structure) {
            structures.put(structure.structureId(), structure);
        } else if (message instanceof ServerMessage.VisionDeltaMessage vision) {
            if (ownHero == null || vision.team() != ownHero.team()) return;
            visibleIds.addAll(vision.showIds());
            visibleIds.removeAll(vision.hideIds());
            for (UUID hidden : vision.hideIds()) minimapPositions.remove(hidden);
        } else if (message instanceof ServerMessage.MinimapDeltaMessage minimap) {
            if (ownHero == null || minimap.team() != ownHero.team()) return;
            minimapPositions.clear();
            for (var entry : minimap.visiblePositions().entrySet()) {
                if (visibleIds.contains(entry.getKey())) minimapPositions.put(entry.getKey(), entry.getValue());
            }
        } else if (message instanceof ServerMessage.RequestResultMessage result) {
            lastRequestResult = result.result();
            lastRequestResultAtMillis = System.currentTimeMillis();
        } else if (message instanceof ServerMessage.SequenceFloor floor) {
            nextClientSequenceId = Math.max(nextClientSequenceId, floor.nextClientSequenceId());
        } else if (message instanceof ServerMessage.LobbyStateMessage state) {
            if (!state.snapshot().matchId().equals(matchId)) {
                nextClientSequenceId = 0;
                ownHero = null;
                match = null;
                scoreboard = null;
                structures.clear();
                visibleIds.clear();
                minimapPositions.clear();
            }
            matchId = state.snapshot().matchId();
            lobby = state.snapshot();
            nextClientSequenceId = Math.max(nextClientSequenceId, state.nextClientSequenceId());
        } else if (message instanceof ServerMessage.ScoreboardMessage board) {
            scoreboard = board;
        } else if (message instanceof ServerMessage.AbilityEventMessage
                || message instanceof ServerMessage.ProjectileEvent
                || message instanceof ServerMessage.CombatEventMessage) {
            if (presentationEvents.size() == 64) presentationEvents.removeFirst();
            presentationEvents.addLast(message);
        }
    }

    public long nextSequence() { return nextClientSequenceId++; }
    public boolean playing() { return match != null && match.state() == MatchState.PLAYING; }
    public UUID matchId() { return matchId; }
    public ServerMessage.HeroDelta ownHero() { return ownHero; }
    public ServerMessage.MatchStateEvent match() { return match; }
    public Map<String, ServerMessage.StructureState> structures() { return Map.copyOf(structures); }
    public Set<UUID> visibleIds() { return Set.copyOf(visibleIds); }
    public Map<UUID, Position> minimapPositions() { return Map.copyOf(minimapPositions); }
    public RequestResult lastRequestResult() { return lastRequestResult; }
    public RequestResult recentRequestFailure() {
        return lastRequestResult != null && !lastRequestResult.success()
                && System.currentTimeMillis() - lastRequestResultAtMillis < 2500
                ? lastRequestResult : null;
    }
    public ServerMessage.ScoreboardMessage scoreboard() { return scoreboard; }
    public LobbySnapshot lobby() { return lobby; }
    public boolean showStart() {
        return match != null && match.state() == MatchState.PLAYING
                && System.currentTimeMillis() - startedAtMillis < 1200;
    }
    public List<ServerMessage> drainPresentationEvents() {
        var events = new ArrayList<>(presentationEvents);
        presentationEvents.clear();
        return events;
    }
    public void clear() {
        matchId = null;
        ownHero = null;
        match = null;
        structures.clear();
        visibleIds.clear();
        minimapPositions.clear();
        lastRequestResult = null;
        lastRequestResultAtMillis = 0;
        scoreboard = null;
        lobby = null;
        startedAtMillis = 0;
        presentationEvents.clear();
    }
}
