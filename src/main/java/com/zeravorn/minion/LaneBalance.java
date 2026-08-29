package com.zeravorn.minion;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Validated lane/minion balance read from config rather than gameplay classes. */
public final class LaneBalance {
    private static final JsonObject ROOT = load();
    private LaneBalance() { }
    private static JsonObject load() {
        try (var stream = LaneBalance.class.getResourceAsStream("/config_defaults/lane_minions.json")) {
            if (stream == null) throw new IllegalStateException("Missing lane minion config");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception exception) { throw new IllegalStateException("Unable to load lane minion config", exception); }
    }
    public static long firstWaveTicks() { return ROOT.get("firstWaveSeconds").getAsLong() * 20; }
    public static long intervalTicks() { return ROOT.get("waveIntervalSeconds").getAsLong() * 20; }
    public static LaneMinionDefinition definition(MinionType type) {
        JsonObject value = ROOT.getAsJsonObject("minions").getAsJsonObject(type.name().toLowerCase());
        return new LaneMinionDefinition(type, value.get("health").getAsInt(), value.get("damage").getAsInt(),
                value.get("attackIntervalSeconds").getAsDouble(), value.get("range").getAsDouble(),
                value.get("gold").getAsInt(), value.get("experience").getAsInt());
    }
}
