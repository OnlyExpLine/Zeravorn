package com.zeravorn.combat;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CrowdControlService {
    private final Map<UUID, List<CrowdControlEffect>> active = new java.util.HashMap<>();

    public void apply(UUID target, CrowdControlEffect effect) {
        List<CrowdControlEffect> effects = active.computeIfAbsent(target, ignored -> new ArrayList<>());
        if (effect.type() == CrowdControlType.SLOW) {
            CrowdControlEffect strongest = effects.stream()
                    .filter(existing -> existing.type() == CrowdControlType.SLOW)
                    .max(java.util.Comparator.comparingDouble(CrowdControlEffect::strength))
                    .orElse(null);
            effects.removeIf(existing -> existing.type() == CrowdControlType.SLOW);
            if (strongest == null || effect.strength() >= strongest.strength()) strongest = effect;
            effects.add(strongest);
            return;
        }
        effects.add(effect);
    }
    public void tick(long serverTick) { active.values().forEach(effects -> effects.removeIf(effect -> effect.expiresAtTick() <= serverTick)); active.values().removeIf(List::isEmpty); }
    public List<CrowdControlEffect> effects(UUID target) { return List.copyOf(active.getOrDefault(target, List.of())); }
    public boolean blocksMovement(UUID target) { return has(target, CrowdControlType.STUN, CrowdControlType.ROOT, CrowdControlType.KNOCKUP, CrowdControlType.KNOCKBACK, CrowdControlType.PULL); }
    public boolean blocksBasicOrAbility(UUID target) { return has(target, CrowdControlType.STUN, CrowdControlType.KNOCKUP, CrowdControlType.KNOCKBACK, CrowdControlType.PULL); }
    public double movementMultiplier(UUID target) {
        return effects(target).stream().filter(effect -> effect.type() == CrowdControlType.SLOW)
                .mapToDouble(effect -> Math.max(0, 1.0 - effect.strength())).min().orElse(1.0);
    }
    public boolean cleanse(UUID target) { List<CrowdControlEffect> effects = active.get(target); return effects != null && effects.removeIf(CrowdControlEffect::cleanseable); }
    private boolean has(UUID target, CrowdControlType... types) { List<CrowdControlEffect> effects = active.getOrDefault(target, List.of()); for (CrowdControlEffect effect : effects) for (CrowdControlType type : types) if (effect.type() == type) return true; return false; }
}
