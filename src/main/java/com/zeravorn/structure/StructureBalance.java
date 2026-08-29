package com.zeravorn.structure;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Read-only structure balance used by server controllers. */
public final class StructureBalance {
    public record Values(int health, int damage, double attackRange, double attackIntervalSeconds) { }
    private static final JsonObject ROOT = load();
    private StructureBalance() { }
    private static JsonObject load() {
        try (var stream = StructureBalance.class.getResourceAsStream("/config_defaults/structures.json")) {
            if (stream == null) throw new IllegalStateException("Missing structure config");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception exception) { throw new IllegalStateException("Unable to load structure config", exception); }
    }
    public static Values tower(TowerOrder order) { return values(ROOT.getAsJsonObject(order.name().toLowerCase())); }
    public static Values throne() { return values(ROOT.getAsJsonObject("throne")); }
    private static Values values(JsonObject value) { return new Values(value.get("health").getAsInt(), value.get("damage").getAsInt(), value.get("attackRange").getAsDouble(), value.get("attackIntervalSeconds").getAsDouble()); }
}
