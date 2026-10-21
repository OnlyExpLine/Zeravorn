package com.zeravorn.network;

import com.zeravorn.combat.CooldownService;
import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.match.MatchSession;
import com.zeravorn.spell.SummonerSpell;
import com.zeravorn.team.TeamId;
import com.zeravorn.ability.AbilityEvent;
import com.zeravorn.combat.CombatEvent;
import com.zeravorn.Zeravorn;
import com.zeravorn.buff.BuffService;
import com.zeravorn.combat.CrowdControlService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

/** One live match's network boundary; bind() is called by match entry (M11). */
public final class ServerMatchGateway {
    private final MatchSession match;
    private final RequestSequenceGuard sequences;
    private final ServerRequestProcessor processor;
    private final CooldownService cooldowns;
    private final BuffService buffs;
    private final CrowdControlService crowdControl;
    private final Map<UUID, Binding> bindings = new HashMap<>();
    private final Map<UUID, HeroRuntime> heroes = new HashMap<>();
    private final Map<UUID, Score> scores = new HashMap<>();
    private final VisibilitySnapshotSource visibility;
    private final Map<UUID, Set<UUID>> lastVisible = new HashMap<>();
    private long transportTicks;

    public ServerMatchGateway(MatchSession match, RequestSequenceGuard sequences,
                              ServerRequestProcessor processor, CooldownService cooldowns,
                              BuffService buffs, CrowdControlService crowdControl,
                              VisibilitySnapshotSource visibility) {
        this.match = Objects.requireNonNull(match);
        this.sequences = Objects.requireNonNull(sequences);
        this.processor = Objects.requireNonNull(processor);
        this.cooldowns = Objects.requireNonNull(cooldowns);
        this.buffs = Objects.requireNonNull(buffs);
        this.crowdControl = Objects.requireNonNull(crowdControl);
        this.visibility = Objects.requireNonNull(visibility);
    }

    public void bind(ServerPlayer player, HeroRuntime hero, SummonerSpell selectedSpell,
                     boolean assignedJungler) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(hero);
        if (!player.getUUID().equals(hero.owner())) throw new IllegalArgumentException("Hero owner mismatch");
        heroes.put(player.getUUID(), hero);
        scores.putIfAbsent(player.getUUID(), new Score());
        bindings.put(player.getUUID(), new Binding(player, hero, selectedSpell, assignedJungler));
        sendSnapshot(player.getUUID());
    }

    /** A bot has match/scoreboard state but no client transport binding. */
    public void registerBot(HeroRuntime hero) {
        Objects.requireNonNull(hero);
        if (match.teams().roster(hero.team()).participants().stream()
                .noneMatch(slot -> slot.playerId().equals(hero.owner()) && slot.bot()))
            throw new IllegalArgumentException("Not a bot roster slot");
        heroes.put(hero.owner(), hero);
        scores.putIfAbsent(hero.owner(), new Score());
    }

    /** Disconnect drops only transport; hero and sequence history remain. */
    public void disconnect(UUID playerId) {
        bindings.remove(playerId);
        lastVisible.remove(playerId);
    }

    public boolean contains(UUID playerId) { return bindings.containsKey(playerId); }

    public void handle(UUID sender, RequestEnvelope request) {
        Binding binding = bindings.get(sender);
        if (binding == null) return;
        var context = new RequestValidationContext(match, sender, binding.hero(),
                binding.selectedSpell(), binding.assignedJungler());
        RequestResult result;
        try {
            result = processor.process(context, request);
        } catch (RuntimeException exception) {
            Zeravorn.LOGGER.error("Zeravorn request failed for {}", sender, exception);
            result = RequestResult.rejected(request.clientSequenceId(), RequestError.SERVER_ERROR);
        }
        send(binding.player(), new ServerMessage.RequestResultMessage(result));
        if (result.success()) {
            if (request.payload() instanceof ClientRequest.UpgradeAbility upgrade) {
                send(binding.player(), new ServerMessage.AbilityRankChanged(binding.hero().owner(),
                        upgrade.abilityId(), binding.hero().abilityRank(upgrade.abilityId()),
                        binding.hero().availableSkillPoints()));
            }
            send(binding.player(), ownHero(binding.hero()));
        }
    }

    public void sendSnapshot(UUID playerId) {
        Binding binding = bindings.get(playerId);
        if (binding == null) return;
        var visible = visibility.visibleIds(binding.hero());
        var positions = visibility.minimapPositions(binding.hero());
        lastVisible.put(playerId, new HashSet<>(visible));
        send(binding.player(), new ServerMessage.Snapshot(match.matchId(), sequences.nextSequence(playerId),
                ownHero(binding.hero()), new ServerMessage.MatchStateEvent(match.state(), match.elapsedTicks(),
                match.winner()), visibility.structures(binding.hero()), visible, filter(positions, visible),
                scoreboard()));
    }

    public void tick() {
        transportTicks++;
        if (transportTicks % 5 != 0) return;
        for (Binding binding : bindings.values()) {
            Set<UUID> current = visibility.visibleIds(binding.hero());
            Set<UUID> previous = lastVisible.getOrDefault(binding.hero().owner(), Set.of());
            Set<UUID> show = new HashSet<>(current);
            show.removeAll(previous);
            Set<UUID> hide = new HashSet<>(previous);
            hide.removeAll(current);
            if (!show.isEmpty() || !hide.isEmpty()) {
                send(binding.player(), new ServerMessage.VisionDeltaMessage(binding.hero().team(), show, hide));
            }
            lastVisible.put(binding.hero().owner(), new HashSet<>(current));
            send(binding.player(), new ServerMessage.MinimapDeltaMessage(binding.hero().team(),
                    filter(visibility.minimapPositions(binding.hero()), current)));
            send(binding.player(), ownHero(binding.hero()));
            if (transportTicks % 20 == 0) {
                send(binding.player(), new ServerMessage.MatchStateEvent(match.state(),
                        match.elapsedTicks(), match.winner()));
                for (var structure : visibility.structures(binding.hero())) send(binding.player(), structure);
                send(binding.player(), scoreboard());
            }
        }
    }

    public void sendTo(UUID playerId, ServerMessage message) {
        Binding binding = bindings.get(playerId);
        if (binding == null) return;
        if (message instanceof ServerMessage.MinimapDeltaMessage minimap) {
            if (minimap.team() != binding.hero().team()) return;
            send(binding.player(), new ServerMessage.MinimapDeltaMessage(minimap.team(),
                    filter(minimap.visiblePositions(), visibility.visibleIds(binding.hero()))));
            return;
        }
        if (message instanceof ServerMessage.VisionDeltaMessage visionEvent) {
            if (visionEvent.team() != binding.hero().team()) return;
            var visible = visibility.visibleIds(binding.hero());
            var safeShow = new HashSet<>(visionEvent.showIds());
            safeShow.retainAll(visible);
            send(binding.player(), new ServerMessage.VisionDeltaMessage(visionEvent.team(),
                    safeShow, visionEvent.hideIds()));
            return;
        }
        if (message instanceof ServerMessage.HeroDelta delta
                && !delta.entityId().equals(binding.hero().owner())) return;
        if (message instanceof ServerMessage.Snapshot) return;
        send(binding.player(), message);
    }

    /** Emits a server-confirmed ability phase only to viewers allowed to see its caster. */
    public void publishAbility(AbilityEvent event) {
        for (Binding binding : bindings.values()) {
            if (binding.hero().owner().equals(event.caster())
                    || visibility.visibleIds(binding.hero()).contains(event.caster())) {
                send(binding.player(), new ServerMessage.AbilityEventMessage(event));
            }
        }
    }

    /** Combat feed does not disclose a hidden fight to unrelated viewers. */
    public void publishCombat(CombatEvent event) {
        for (Binding binding : bindings.values()) {
            Set<UUID> visible = visibility.visibleIds(binding.hero());
            if (binding.hero().owner().equals(event.source())
                    || binding.hero().owner().equals(event.target())
                    || visible.contains(event.source()) && visible.contains(event.target())) {
                send(binding.player(), new ServerMessage.CombatEventMessage(event));
            }
        }
    }

    public void publishStructure(ServerMessage.StructureState structure) {
        for (Binding binding : bindings.values()) send(binding.player(), structure);
    }

    /** Called after a server-confirmed death event; does not invent assist credit. */
    public void recordKill(UUID killer, UUID victim) {
        if (killer != null && !killer.equals(victim)) scores.computeIfAbsent(killer, ignored -> new Score()).kills++;
        if (victim != null) scores.computeIfAbsent(victim, ignored -> new Score()).deaths++;
        broadcastScoreboard();
    }

    public void broadcastScoreboard() {
        var board = scoreboard();
        for (Binding binding : bindings.values()) send(binding.player(), board);
    }

    public void recordAssist(UUID assistant) {
        if (assistant != null) scores.computeIfAbsent(assistant, ignored -> new Score()).assists++;
    }

    private ServerMessage.ScoreboardMessage scoreboard() {
        var lines = new java.util.ArrayList<ServerMessage.PlayerLine>();
        int blue = 0, red = 0;
        for (TeamId team : TeamId.values()) {
            for (var participant : match.teams().roster(team).participants()) {
                HeroRuntime hero = heroes.get(participant.playerId());
                Score score = scores.getOrDefault(participant.playerId(), new Score());
                if (team == TeamId.BLUE) blue += score.kills;
                else red += score.kills;
                var items = hero == null ? List.<String>of()
                        : hero.inventory().normalItems().stream().map(item -> item.id()).toList();
                lines.add(new ServerMessage.PlayerLine(participant.playerId(), participant.nickname(), team,
                        hero == null ? "-" : hero.definition().id(), hero == null ? 1 : hero.level(),
                        score.kills, score.deaths, score.assists, hero == null ? 0 : hero.gold(),
                        items, hero == null || hero.inventory().boots() == null ? null : hero.inventory().boots().id(),
                        hero == null ? 0 : Math.max(0, hero.respawnAtTick() - match.elapsedTicks())));
            }
        }
        return new ServerMessage.ScoreboardMessage(blue, red, lines);
    }

    private ServerMessage.HeroDelta ownHero(HeroRuntime hero) {
        Binding binding = bindings.get(hero.owner());
        SummonerSpell selectedSpell = binding == null ? null : binding.selectedSpell();
        Score score = scores.getOrDefault(hero.owner(), new Score());
        return HeroDeltaFactory.ownHero(hero, cooldowns, match.elapsedTicks(),
                score.kills, score.deaths, score.assists,
                selectedSpell, buffs, crowdControl);
    }

    private static void send(ServerPlayer player, ServerMessage message) {
        if (ServerPlayNetworking.canSend(player, ClientboundMobaPayload.TYPE)) {
            ServerPlayNetworking.send(player, new ClientboundMobaPayload(NetworkWireCodec.encodeMessage(message)));
        }
    }

    private static Map<UUID, com.zeravorn.map.Position> filter(
            Map<UUID, com.zeravorn.map.Position> positions, Set<UUID> visible) {
        var safe = new HashMap<UUID, com.zeravorn.map.Position>();
        for (var entry : positions.entrySet()) {
            if (visible.contains(entry.getKey())) safe.put(entry.getKey(), entry.getValue());
        }
        return safe;
    }

    private record Binding(ServerPlayer player, HeroRuntime hero, SummonerSpell selectedSpell,
                           boolean assignedJungler) { }

    private static final class Score { int kills, deaths, assists; }

    /** Must return only IDs and positions approved by VisionService for this hero's team. */
    public interface VisibilitySnapshotSource {
        Set<UUID> visibleIds(HeroRuntime viewer);
        Map<UUID, com.zeravorn.map.Position> minimapPositions(HeroRuntime viewer);
        List<ServerMessage.StructureState> structures(HeroRuntime viewer);
    }
}
