package com.zeravorn.hero;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class JasonAbilityDefinitions {
    public record Q(int baseDamage, double healRatio, long cooldownTicks) { }
    public record E(int baseDamage, double attackRatio, long cooldownTicks, long windowTicks, long stunTicks) { }
    public record R(int baseDamage, double attackRatio, long cooldownTicks, double radius, long stunTicks) { }
    private static final List<Q> Q;
    private static final List<E> E;
    private static final List<R> R;
    static {
        try (var stream = JasonAbilityDefinitions.class.getResourceAsStream("/config_defaults/jason_abilities.json")) {
            if (stream == null) throw new IllegalStateException("Missing Jason ability config");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Q = q(root.getAsJsonArray("q"));
            E = e(root.getAsJsonArray("e"));
            R = r(root.getAsJsonArray("r"));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load Jason ability config", exception);
        }
    }
    private static List<Q> q(JsonArray rows) { List<Q> values = new ArrayList<>(); for (var row : rows) { var v = row.getAsJsonObject(); values.add(new Q(v.get("baseDamage").getAsInt(), v.get("healRatio").getAsDouble(), v.get("cooldownTicks").getAsLong())); } return List.copyOf(values); }
    private static List<E> e(JsonArray rows) { List<E> values = new ArrayList<>(); for (var row : rows) { var v = row.getAsJsonObject(); values.add(new E(v.get("baseDamage").getAsInt(), v.get("attackRatio").getAsDouble(), v.get("cooldownTicks").getAsLong(), v.get("windowTicks").getAsLong(), v.get("stunTicks").getAsLong())); } return List.copyOf(values); }
    private static List<R> r(JsonArray rows) { List<R> values = new ArrayList<>(); for (var row : rows) { var v = row.getAsJsonObject(); values.add(new R(v.get("baseDamage").getAsInt(), v.get("attackRatio").getAsDouble(), v.get("cooldownTicks").getAsLong(), v.get("radius").getAsDouble(), v.get("stunTicks").getAsLong())); } return List.copyOf(values); }
    private JasonAbilityDefinitions() { }
    public static Q q(int rank) { return Q.get(rank - 1); }
    public static E e(int rank) { return E.get(rank - 1); }
    public static R r(int rank) { return R.get(rank - 1); }
}
