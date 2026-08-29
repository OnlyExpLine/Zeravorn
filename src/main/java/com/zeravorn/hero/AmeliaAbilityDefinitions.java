package com.zeravorn.hero;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Config-backed rank values for Amelia's magical ice abilities. */
public final class AmeliaAbilityDefinitions {
    public record Q(int damagePerTick, double apRatio, long cooldownTicks, int mana, double range, double slow, int ticks) { }
    public record E(int damage, double apRatio, long cooldownTicks, int mana, double radius, double knockback) { }
    public record R(int damage, double apRatio, long cooldownTicks, int mana, double range, double radius, long delayTicks) { }
    private static final List<Q> Q; private static final List<E> E; private static final List<R> R;
    static { try (var stream = AmeliaAbilityDefinitions.class.getResourceAsStream("/config_defaults/amelia_abilities.json")) { if (stream == null) throw new IllegalStateException("Missing Amelia ability config"); JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject(); Q = q(root.getAsJsonArray("q")); E = e(root.getAsJsonArray("e")); R = r(root.getAsJsonArray("r")); } catch (Exception exception) { throw new IllegalStateException("Unable to load Amelia ability config", exception); } }
    private static List<Q> q(JsonArray rows) { List<Q> values = new ArrayList<>(); for (var row : rows) { var v = row.getAsJsonObject(); values.add(new Q(v.get("damagePerTick").getAsInt(), v.get("apRatio").getAsDouble(), v.get("cooldownTicks").getAsLong(), v.get("mana").getAsInt(), v.get("range").getAsDouble(), v.get("slow").getAsDouble(), v.get("ticks").getAsInt())); } return List.copyOf(values); }
    private static List<E> e(JsonArray rows) { List<E> values = new ArrayList<>(); for (var row : rows) { var v = row.getAsJsonObject(); values.add(new E(v.get("damage").getAsInt(), v.get("apRatio").getAsDouble(), v.get("cooldownTicks").getAsLong(), v.get("mana").getAsInt(), v.get("radius").getAsDouble(), v.get("knockback").getAsDouble())); } return List.copyOf(values); }
    private static List<R> r(JsonArray rows) { List<R> values = new ArrayList<>(); for (var row : rows) { var v = row.getAsJsonObject(); values.add(new R(v.get("damage").getAsInt(), v.get("apRatio").getAsDouble(), v.get("cooldownTicks").getAsLong(), v.get("mana").getAsInt(), v.get("range").getAsDouble(), v.get("radius").getAsDouble(), v.get("delayTicks").getAsLong())); } return List.copyOf(values); }
    private AmeliaAbilityDefinitions() { }
    public static Q q(int rank) { return Q.get(rank - 1); } public static E e(int rank) { return E.get(rank - 1); } public static R r(int rank) { return R.get(rank - 1); }
}
