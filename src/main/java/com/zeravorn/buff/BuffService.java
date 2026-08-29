package com.zeravorn.buff;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Keeps buff authority on the server and deliberately has no Minecraft client dependencies. */
public final class BuffService {
    private final Map<UUID, Map<BuffType, TimedBuff>> active = new HashMap<>();

    public void apply(UUID owner, TimedBuff buff) {
        active.computeIfAbsent(owner, ignored -> new HashMap<>()).put(buff.type(), buff);
    }
    public TimedBuff get(UUID owner, BuffType type, long serverTick) {
        expire(serverTick);
        return active.getOrDefault(owner, Map.of()).get(type);
    }
    public boolean has(UUID owner, BuffType type, long serverTick) { return get(owner, type, serverTick) != null; }
    public void onDeath(UUID owner) { active.remove(owner); }
    public void tick(long serverTick) { expire(serverTick); }
    public double outgoingHeroDamageMultiplier(UUID owner, long serverTick) {
        TimedBuff red = get(owner, BuffType.RED, serverTick);
        return red == null ? 1.0 : red.outgoingDamageMultiplier();
    }
    public double cooldownReduction(UUID owner, long serverTick) {
        TimedBuff blue = get(owner, BuffType.BLUE, serverTick);
        return blue == null ? 0.0 : blue.cooldownReduction();
    }
    public double maxManaMultiplier(UUID owner, long serverTick) {
        TimedBuff blue = get(owner, BuffType.BLUE, serverTick);
        return blue == null ? 1.0 : blue.maxManaMultiplier();
    }
    private void expire(long serverTick) {
        active.values().forEach(buffs -> buffs.values().removeIf(buff -> buff.expiresAtTick() <= serverTick));
        active.values().removeIf(Map::isEmpty);
    }
}
