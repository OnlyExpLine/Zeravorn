package com.zeravorn.hero;

import java.util.List;

public final class JasonAbilityDefinitions {
    public record Q(int baseDamage, double healRatio, long cooldownTicks) { }
    public record E(int baseDamage, double attackRatio, long cooldownTicks, long windowTicks, long stunTicks) { }
    public record R(int baseDamage, double attackRatio, long cooldownTicks, double radius, long stunTicks) { }
    private static final List<Q> Q = List.of(new Q(60, .30, 160), new Q(70, .33, 160), new Q(80, .36, 160), new Q(90, .40, 160));
    private static final List<E> E = List.of(new E(40, .60, 180, 100, 14), new E(50, .60, 180, 100, 16), new E(60, .60, 170, 100, 18), new E(70, .60, 160, 100, 20));
    private static final List<R> R = List.of(new R(110, .80, 480, 3, 25), new R(140, .80, 400, 3, 30));
    private JasonAbilityDefinitions() { }
    public static Q q(int rank) { return Q.get(rank - 1); }
    public static E e(int rank) { return E.get(rank - 1); }
    public static R r(int rank) { return R.get(rank - 1); }
}
