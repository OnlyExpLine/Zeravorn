package com.zeravorn.hero;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Config-backed rank values for Esaki's physical earth abilities. */
public final class EsakiAbilityDefinitions {
    public record Q(int damage, double attackRatio, long cooldownTicks, int mana, double range, double projectileSpeed) { }
    public record E(int damage, double attackRatio, long cooldownTicks, int mana, double radius, double knockback) { }
    public record R(int damagePerPulse, double attackRatio, long cooldownTicks, int mana, double radius, int pulses) { }
    private static final List<Q> Q; private static final List<E> E; private static final List<R> R;
    static {
        try (var stream = EsakiAbilityDefinitions.class.getResourceAsStream("/config_defaults/esaki_abilities.json")) {
            if (stream == null) throw new IllegalStateException("Missing Esaki ability config");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Q = q(root.getAsJsonArray("q")); E = e(root.getAsJsonArray("e")); R = r(root.getAsJsonArray("r"));
        } catch (Exception exception) { throw new IllegalStateException("Unable to load Esaki ability config", exception); }
    }
    private static List<Q> q(JsonArray rows) { List<Q> values = new ArrayList<>(); for (var row : rows) { var v = row.getAsJsonObject(); values.add(new Q(v.get("damage").getAsInt(), v.get("attackRatio").getAsDouble(), v.get("cooldownTicks").getAsLong(), v.get("mana").getAsInt(), v.get("range").getAsDouble(), v.get("projectileSpeed").getAsDouble())); } return List.copyOf(values); }
    private static List<E> e(JsonArray rows) { List<E> values = new ArrayList<>(); for (var row : rows) { var v = row.getAsJsonObject(); values.add(new E(v.get("damage").getAsInt(), v.get("attackRatio").getAsDouble(), v.get("cooldownTicks").getAsLong(), v.get("mana").getAsInt(), v.get("radius").getAsDouble(), v.get("knockback").getAsDouble())); } return List.copyOf(values); }
    private static List<R> r(JsonArray rows) { List<R> values = new ArrayList<>(); for (var row : rows) { var v = row.getAsJsonObject(); values.add(new R(v.get("damagePerPulse").getAsInt(), v.get("attackRatio").getAsDouble(), v.get("cooldownTicks").getAsLong(), v.get("mana").getAsInt(), v.get("radius").getAsDouble(), v.get("pulses").getAsInt())); } return List.copyOf(values); }
    private EsakiAbilityDefinitions() { }
    public static Q q(int rank) { return Q.get(rank - 1); } public static E e(int rank) { return E.get(rank - 1); } public static R r(int rank) { return R.get(rank - 1); }
}
