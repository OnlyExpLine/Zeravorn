package com.zeravorn.jungle;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.zeravorn.buff.BuffType;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Loads M7 balance values from the packaged jungle definition. */
public final class JungleConfigLoader {
    private JungleConfigLoader() { }
    public static Map<String, JungleMobType> loadDefaultDefinitions() {
        var stream = JungleConfigLoader.class.getResourceAsStream("/config_defaults/jungle.json");
        if (stream == null) throw new IllegalStateException("Missing jungle config");
        var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        Map<String, JungleMobType> result = new LinkedHashMap<>();
        for (JsonElement element : root.getAsJsonArray("mobs")) {
            var mob = element.getAsJsonObject(); String buff = mob.get("rewardBuff").getAsString();
            JungleMobType type = new JungleMobType(mob.get("id").getAsString(), mob.get("health").getAsInt(), mob.get("damage").getAsInt(),
                    mob.get("attackInterval").getAsDouble(), mob.get("gold").getAsInt(), mob.get("experience").getAsInt(),
                    mob.get("respawnSeconds").getAsInt(), mob.get("leashRadius").getAsDouble(), buff.isBlank() ? null : BuffType.valueOf(buff), mob.get("specialCrowdControl").getAsString());
            if (result.put(type.id(), type) != null) throw new IllegalArgumentException("Duplicate jungle mob id: " + type.id());
        }
        return Map.copyOf(result);
    }
}
