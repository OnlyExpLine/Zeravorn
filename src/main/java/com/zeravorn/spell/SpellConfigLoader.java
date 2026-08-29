package com.zeravorn.spell;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

public final class SpellConfigLoader {
    private SpellConfigLoader() { }
    public static Map<SummonerSpell, SummonerSpellDefinition> loadDefaults() {
        var stream = SpellConfigLoader.class.getResourceAsStream("/config_defaults/spells.json");
        if (stream == null) throw new IllegalStateException("Missing spell config");
        var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        Map<SummonerSpell, SummonerSpellDefinition> result = new EnumMap<>(SummonerSpell.class);
        for (JsonElement element : root.getAsJsonArray("spells")) { var spell = element.getAsJsonObject(); SummonerSpell id = SummonerSpell.valueOf(spell.get("id").getAsString()); result.put(id, new SummonerSpellDefinition(id, spell.get("cooldownSeconds").getAsInt(), spell.get("rangeOrPercent").getAsDouble())); }
        if (result.size() != SummonerSpell.values().length) throw new IllegalArgumentException("Incomplete summoner spell config");
        return Map.copyOf(result);
    }
}
