package com.zeravorn.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** UI hints from the same packaged balance resources; gameplay never reads this class. */
final class AbilityUiCatalog {
    private static final Map<String, JsonObject> HEROES = new HashMap<>();

    private AbilityUiCatalog() { }

    static int manaCost(String heroId, String slot, int rank) {
        JsonObject root = HEROES.computeIfAbsent(heroId, AbilityUiCatalog::load);
        if (root == null || !root.has(slot)) return 0;
        var ranks = root.getAsJsonArray(slot);
        if (ranks.size() == 0) return 0;
        var data = ranks.get(Math.min(Math.max(1, rank), ranks.size()) - 1).getAsJsonObject();
        if (data.has("mana")) return data.get("mana").getAsInt();
        if (data.has("manaCost")) return data.get("manaCost").getAsInt();
        return 0;
    }

    private static JsonObject load(String heroId) {
        String path = "/config_defaults/" + heroId + "_abilities.json";
        try (var stream = AbilityUiCatalog.class.getResourceAsStream(path)) {
            if (stream == null) return null;
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception ignored) {
            return null;
        }
    }
}
