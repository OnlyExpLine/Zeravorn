package com.zeravorn.network;

import com.zeravorn.ability.AbilityEvent;
import com.zeravorn.combat.CombatEvent;
import com.zeravorn.map.Position;
import com.zeravorn.match.MatchState;
import com.zeravorn.hero.EffectiveHeroStats;
import com.zeravorn.spell.SummonerSpell;
import com.zeravorn.team.TeamId;
import com.zeravorn.lobby.LobbySnapshot;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Only server-filtered, presentation-safe values may enter an S2C message. */
public sealed interface ServerMessage permits ServerMessage.Snapshot, ServerMessage.HeroDelta,
        ServerMessage.AbilityRankChanged, ServerMessage.AbilityEventMessage,
        ServerMessage.ProjectileEvent, ServerMessage.MatchStateEvent,
        ServerMessage.VisionDeltaMessage, ServerMessage.StructureState,
        ServerMessage.MinimapDeltaMessage, ServerMessage.CombatEventMessage,
        ServerMessage.RequestResultMessage, ServerMessage.ScoreboardMessage,
        ServerMessage.SequenceFloor, ServerMessage.LobbyStateMessage {

    record Snapshot(UUID matchId, long nextClientSequenceId, HeroDelta ownHero,
                    MatchStateEvent match, List<StructureState> structures,
                    Set<UUID> visibleIds, Map<UUID, Position> minimapPositions,
                    ScoreboardMessage scoreboard) implements ServerMessage {
        public Snapshot {
            structures = List.copyOf(structures);
            visibleIds = Set.copyOf(visibleIds);
            minimapPositions = Map.copyOf(minimapPositions);
        }
    }

    record HeroDelta(UUID entityId, String heroId, TeamId team, boolean alive,
                     int health, int maxHealth, int mana, int maxMana,
                     int level, int experience, int gold, int availableSkillPoints,
                     int kills, int deaths, int assists, Map<String, Integer> abilityRanks,
                     Map<String, Long> cooldownTicks, List<String> normalItems,
                     String boots, SummonerSpell selectedSpell, EffectiveHeroStats stats,
                     double cooldownReduction,
                     Map<String, Long> crowdControlTicks,
                     Map<String, Long> buffTicks) implements ServerMessage {
        public HeroDelta {
            abilityRanks = Map.copyOf(abilityRanks);
            cooldownTicks = Map.copyOf(cooldownTicks);
            normalItems = List.copyOf(normalItems);
            crowdControlTicks = Map.copyOf(crowdControlTicks);
            buffTicks = Map.copyOf(buffTicks);
        }
    }

    record AbilityRankChanged(UUID heroId, String abilityId, int newRank,
                              int remainingSkillPoints) implements ServerMessage { }

    record AbilityEventMessage(AbilityEvent event) implements ServerMessage { }

    record ProjectileEvent(UUID instanceId, String definitionId, Phase phase,
                           Position visiblePosition) implements ServerMessage {
        public enum Phase { SPAWN, UPDATE, REMOVE }
    }

    record MatchStateEvent(MatchState state, long elapsedTicks, TeamId winner) implements ServerMessage { }

    record VisionDeltaMessage(TeamId team, Set<UUID> showIds, Set<UUID> hideIds) implements ServerMessage {
        public VisionDeltaMessage {
            showIds = Set.copyOf(showIds);
            hideIds = Set.copyOf(hideIds);
        }
    }

    record StructureState(String structureId, int health, int maxHealth,
                          boolean destroyed, UUID currentTarget) implements ServerMessage { }

    record MinimapDeltaMessage(TeamId team, Map<UUID, Position> visiblePositions) implements ServerMessage {
        public MinimapDeltaMessage { visiblePositions = Map.copyOf(visiblePositions); }
    }

    record CombatEventMessage(CombatEvent event) implements ServerMessage { }

    record RequestResultMessage(RequestResult result) implements ServerMessage { }

    /** Non-mutating reconnect handshake before a match has been attached. */
    record SequenceFloor(long nextClientSequenceId) implements ServerMessage { }

    record LobbyStateMessage(LobbySnapshot snapshot, long nextClientSequenceId)
            implements ServerMessage { }

    record ScoreboardMessage(int blueScore, int redScore, List<PlayerLine> players) implements ServerMessage {
        public ScoreboardMessage { players = List.copyOf(players); }
    }

    record PlayerLine(UUID playerId, String nickname, TeamId team, String heroId,
                      int level, int kills, int deaths, int assists, int gold,
                      List<String> items, String boots, long respawnTicks) {
        public PlayerLine { items = List.copyOf(items); }
    }
}
