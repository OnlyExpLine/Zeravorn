package com.zeravorn.bot;

import com.zeravorn.map.Position;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** A server-filtered view. Callers must supply only targets visible to this bot's team. */
public record BotPerception(boolean alive, int health, int maxHealth, Position position,
                            boolean atFountain, boolean recallAvailable, boolean alliedWaveAtTower,
                            List<Target> visibleTargets) {
    public BotPerception {
        Objects.requireNonNull(position);
        visibleTargets = List.copyOf(visibleTargets);
        if (maxHealth <= 0 || health < 0 || health > maxHealth) throw new IllegalArgumentException("health");
    }

    public record Target(UUID id, Kind kind, Position position, boolean legal) {
        public Target {
            Objects.requireNonNull(id);
            Objects.requireNonNull(kind);
            Objects.requireNonNull(position);
        }
    }

    public enum Kind { HERO, MINION, TOWER, THRONE }
}
