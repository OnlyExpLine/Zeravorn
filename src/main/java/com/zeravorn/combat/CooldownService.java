package com.zeravorn.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CooldownService {
    private final Map<Key, Long> readyAt = new HashMap<>();

    public boolean ready(UUID owner, String action, long serverTick) { return serverTick >= readyAt.getOrDefault(new Key(owner, action), 0L); }
    public boolean tryStart(UUID owner, String action, long serverTick, long durationTicks) {
        if (durationTicks < 0 || serverTick < 0 || !ready(owner, action, serverTick)) return false;
        readyAt.put(new Key(owner, action), serverTick + durationTicks);
        return true;
    }
    public long remaining(UUID owner, String action, long serverTick) { return Math.max(0, readyAt.getOrDefault(new Key(owner, action), 0L) - serverTick); }
    private record Key(UUID owner, String action) { }
}
