package com.zeravorn.hero;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class HeroProgressionConfig {
	private static final ProgressionDefinition DEFAULTS = loadDefaults();

    private HeroProgressionConfig() { }

	public static int startingGold() { return DEFAULTS.startingGold(); }
    public static int startingSkillPoints() { return DEFAULTS.startingSkillPoints(); }

	public static int xpToLevel(int level) {
		if (level < 1 || level > HeroDefinition.MAX_LEVEL) throw new IllegalArgumentException("Level must be 1-10");
		return DEFAULTS.xpThresholds().get(level - 1);
    }
	public static int maxXp() { return xpToLevel(HeroDefinition.MAX_LEVEL); }

    private static ProgressionDefinition loadDefaults() {
        try (InputStream stream = HeroProgressionConfig.class.getResourceAsStream("/config_defaults/levels.json")) {
            if (stream == null) throw new IllegalStateException("Missing default progression config");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray thresholds = root.getAsJsonArray("xpThresholds");
            List<Integer> xp = new ArrayList<>();
            for (int i = 0; i < thresholds.size(); i++) xp.add(thresholds.get(i).getAsInt());
            ProgressionDefinition result = new ProgressionDefinition(root.get("startingGold").getAsInt(),
                    root.get("startingSkillPoints").getAsInt(), List.copyOf(xp));
            if (result.xpThresholds().size() != HeroDefinition.MAX_LEVEL || result.xpThresholds().getFirst() != 0
                    || result.startingGold() < 0 || result.startingSkillPoints() < 0) {
                throw new IllegalArgumentException("Invalid progression config");
            }
            return result;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load progression config", exception);
        }
    }

    private record ProgressionDefinition(int startingGold, int startingSkillPoints, List<Integer> xpThresholds) { }
}
