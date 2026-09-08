package com.zeravorn.vision;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Validated M9 balance values used by the authoritative vision service. */
public record VisionConfig(double heroRadius, double laneMinionRadius, double towerRadius,
                           double throneRadius, double fountainRadius, long bushAttackRevealTicks,
                           long alliedUnitAttackRevealTicks) {
    public VisionConfig {
        if (heroRadius <= 0 || laneMinionRadius <= 0 || towerRadius <= 0 || throneRadius <= 0
                || fountainRadius <= 0 || bushAttackRevealTicks <= 0 || alliedUnitAttackRevealTicks <= 0) {
            throw new IllegalArgumentException("Invalid vision config");
        }
    }

    public static VisionConfig loadDefault() {
        try (var stream = VisionConfig.class.getResourceAsStream("/config_defaults/vision.json")) {
            if (stream == null) throw new IllegalStateException("Missing vision config");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            return new VisionConfig(positive(root, "heroRadius"), positive(root, "laneMinionRadius"),
                    positive(root, "towerRadius"), positive(root, "throneRadius"), positive(root, "fountainRadius"),
                    secondsToTicks(positive(root, "bushAttackRevealSeconds")),
                    secondsToTicks(positive(root, "alliedUnitAttackRevealSeconds")));
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load vision config", exception);
        }
    }

    private static double positive(JsonObject root, String key) {
        if (!root.has(key) || root.get(key).getAsDouble() <= 0) throw new IllegalArgumentException("Invalid vision field: " + key);
        return root.get(key).getAsDouble();
    }
    private static long secondsToTicks(double seconds) { return Math.max(1, Math.round(seconds * 20.0)); }
}
