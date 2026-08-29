package com.zeravorn.jungle;

import com.zeravorn.map.Position;
import java.util.Objects;

/** Engine-independent server domain state for a single configured jungle camp. */
public final class JungleCamp {
    private final String id;
    private final JungleMobType definition;
    private final Position spawn;
    private JungleCampState state = JungleCampState.WAITING;
    private int health;
    private long respawnAtTick;
    private boolean deathRewardClaimed;

    public JungleCamp(String id, JungleMobType definition, Position spawn) {
        this.id = Objects.requireNonNull(id); this.definition = Objects.requireNonNull(definition); this.spawn = Objects.requireNonNull(spawn);
    }
    public String id() { return id; }
    public JungleMobType definition() { return definition; }
    public Position spawnPoint() { return spawn; }
    public JungleCampState state() { return state; }
    public int health() { return health; }
    public boolean alive() { return state == JungleCampState.ALIVE || state == JungleCampState.LEASH_RETURN; }
    /** Balance scaling: +10% HP and +6% damage every three minutes, capped at 18:00. */
    public int scaledMaxHealth(long matchElapsedTicks) { return scaled(definition.health(), 0.10, matchElapsedTicks); }
    public int scaledDamage(long matchElapsedTicks) { return scaled(definition.damage(), 0.06, matchElapsedTicks); }
    public void spawn() { state = JungleCampState.ALIVE; health = definition.health(); respawnAtTick = 0; deathRewardClaimed = false; }
    public boolean damage(int amount, long serverTick) {
        if (amount < 0 || !alive()) return false;
        health = Math.max(0, health - amount);
        if (health == 0) { state = JungleCampState.DEAD_WAIT_RESPAWN; respawnAtTick = serverTick + definition.respawnSeconds() * 20L; }
        return true;
    }
    public void beginLeashReturn() { if (alive()) state = JungleCampState.LEASH_RETURN; }
    public void completeLeashReturn() { if (state == JungleCampState.LEASH_RETURN) { state = JungleCampState.ALIVE; health = definition.health(); } }
    /** Atomically claims this death's reward, preventing duplicate rewards from repeated kill events. */
    public boolean claimDeathReward() {
        if (state != JungleCampState.DEAD_WAIT_RESPAWN || health != 0 || deathRewardClaimed) return false;
        deathRewardClaimed = true;
        return true;
    }
    public void tick(long serverTick, boolean matchPlaying, long firstSpawnTick) {
        if (!matchPlaying) return;
        if (state == JungleCampState.WAITING && serverTick >= firstSpawnTick) spawn();
        if (state == JungleCampState.DEAD_WAIT_RESPAWN && serverTick >= respawnAtTick) spawn();
    }
    private static int scaled(int base, double perStage, long elapsedTicks) {
        long stages = Math.min(6, Math.max(0, elapsedTicks) / (180L * 20L));
        return (int) Math.floor(base * (1.0 + perStage * stages));
    }
}
