package com.zeravorn.hero;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class LokiAbilityDefinitions {
    public record Q(int damage, double attackRatio, long cooldownTicks, int mana, double range, long pullTicks) { }
    public record E(long cooldownTicks, int mana, long durationTicks, double moveBonus) { }
    public record R(int damage, double attackRatio, long cooldownTicks, int mana, double radius, long rootTicks) { }
    private static final List<Q> Q; private static final List<E> E; private static final List<R> R;
    static { try (var stream = LokiAbilityDefinitions.class.getResourceAsStream("/config_defaults/loki_abilities.json")) { if (stream == null) throw new IllegalStateException("Missing Loki ability config"); JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject(); Q = q(root.getAsJsonArray("q")); E = e(root.getAsJsonArray("e")); R = r(root.getAsJsonArray("r")); } catch (Exception exception) { throw new IllegalStateException("Unable to load Loki ability config", exception); } }
    private static List<Q> q(JsonArray rows) { List<Q> values = new ArrayList<>(); for (var row : rows) { var v = row.getAsJsonObject(); values.add(new Q(v.get("damage").getAsInt(), v.get("attackRatio").getAsDouble(), v.get("cooldownTicks").getAsLong(), v.get("mana").getAsInt(), v.get("range").getAsDouble(), v.get("pullTicks").getAsLong())); } return List.copyOf(values); }
    private static List<E> e(JsonArray rows) { List<E> values = new ArrayList<>(); for (var row : rows) { var v = row.getAsJsonObject(); values.add(new E(v.get("cooldownTicks").getAsLong(), v.get("mana").getAsInt(), v.get("durationTicks").getAsLong(), v.get("moveBonus").getAsDouble())); } return List.copyOf(values); }
    private static List<R> r(JsonArray rows) { List<R> values = new ArrayList<>(); for (var row : rows) { var v = row.getAsJsonObject(); values.add(new R(v.get("damage").getAsInt(), v.get("attackRatio").getAsDouble(), v.get("cooldownTicks").getAsLong(), v.get("mana").getAsInt(), v.get("radius").getAsDouble(), v.get("rootTicks").getAsLong())); } return List.copyOf(values); }
    private LokiAbilityDefinitions() { }
    public static Q q(int rank) { return Q.get(rank - 1); } public static E e(int rank) { return E.get(rank - 1); } public static R r(int rank) { return R.get(rank - 1); }
}
