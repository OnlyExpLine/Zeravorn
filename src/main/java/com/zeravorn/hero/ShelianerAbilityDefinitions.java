package com.zeravorn.hero;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Rank-aware Shelianer balance definitions, loaded from packaged config. */
public final class ShelianerAbilityDefinitions {
    public record Q(int damage, double attackRatio, long cooldownTicks, int mana, double range) { }
    public record E(int damagePerTick, double attackRatio, long cooldownTicks, int mana, double slow, long durationTicks) { }
    public record F(long cooldownTicks, int mana, double dashDistance) { }
    public record R(int damagePerHit, double attackRatio, long cooldownTicks, int mana, double range, long stunTicks, int hits) { }
    private static final List<Q> Q; private static final List<E> E; private static final List<F> F; private static final List<R> R;
    static {
        try (var stream = ShelianerAbilityDefinitions.class.getResourceAsStream("/config_defaults/shelianer_abilities.json")) {
            if (stream == null) throw new IllegalStateException("Missing Shelianer ability config");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Q = q(root.getAsJsonArray("q")); E = e(root.getAsJsonArray("e")); F = f(root.getAsJsonArray("f")); R = r(root.getAsJsonArray("r"));
        } catch (Exception exception) { throw new IllegalStateException("Unable to load Shelianer ability config", exception); }
    }
    private static List<Q> q(JsonArray rows) { List<Q> values = new ArrayList<>(); for (var row : rows) { var v = row.getAsJsonObject(); values.add(new Q(v.get("damage").getAsInt(), v.get("attackRatio").getAsDouble(), v.get("cooldownTicks").getAsLong(), v.get("mana").getAsInt(), v.get("range").getAsDouble())); } return List.copyOf(values); }
    private static List<E> e(JsonArray rows) { List<E> values = new ArrayList<>(); for (var row : rows) { var v = row.getAsJsonObject(); values.add(new E(v.get("damagePerTick").getAsInt(), v.get("attackRatio").getAsDouble(), v.get("cooldownTicks").getAsLong(), v.get("mana").getAsInt(), v.get("slow").getAsDouble(), v.get("durationTicks").getAsLong())); } return List.copyOf(values); }
    private static List<F> f(JsonArray rows) { List<F> values = new ArrayList<>(); for (var row : rows) { var v = row.getAsJsonObject(); values.add(new F(v.get("cooldownTicks").getAsLong(), v.get("mana").getAsInt(), v.get("dashDistance").getAsDouble())); } return List.copyOf(values); }
    private static List<R> r(JsonArray rows) { List<R> values = new ArrayList<>(); for (var row : rows) { var v = row.getAsJsonObject(); values.add(new R(v.get("damagePerHit").getAsInt(), v.get("attackRatio").getAsDouble(), v.get("cooldownTicks").getAsLong(), v.get("mana").getAsInt(), v.get("range").getAsDouble(), v.get("stunTicks").getAsLong(), v.get("hits").getAsInt())); } return List.copyOf(values); }
    private ShelianerAbilityDefinitions() { }
    public static Q q(int rank) { return Q.get(rank - 1); } public static E e(int rank) { return E.get(rank - 1); }
    public static F f(int rank) { return F.get(rank - 1); } public static R r(int rank) { return R.get(rank - 1); }
}
