package com.zeravorn.hero;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zeravorn.ability.AbilityDefinition;
import com.zeravorn.ability.AbilitySlot;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Loads immutable M2 hero definitions from packaged balance resources. */
public final class HeroConfigLoader {
    private static final String HERO_RESOURCE = "/config_defaults/heroes.json";

    private HeroConfigLoader() {
    }

    public static List<HeroDefinition> loadDefaultHeroes() {
        try (InputStream stream = HeroConfigLoader.class.getResourceAsStream(HERO_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing default hero config: " + HERO_RESOURCE);
            }
            JsonArray heroes = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("heroes");
            List<HeroDefinition> definitions = new ArrayList<>();
            for (JsonElement element : heroes) {
                definitions.add(parseHero(element.getAsJsonObject()));
            }
            HeroConfigValidator.validateHeroes(definitions);
            return List.copyOf(definitions);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load default hero config", exception);
        }
    }

    private static HeroDefinition parseHero(JsonObject hero) {
        List<HeroStats> stats = new ArrayList<>();
        for (JsonElement statElement : hero.getAsJsonArray("statsByLevel")) {
            JsonObject stat = statElement.getAsJsonObject();
            stats.add(new HeroStats(stat.get("health").getAsInt(), stat.get("healthRegen").getAsDouble(),
                    stat.get("maxMana").getAsInt(), stat.get("manaRegen").getAsDouble(), stat.get("attack").getAsInt()));
        }
        EnumMap<AbilitySlot, AbilityDefinition> abilities = new EnumMap<>(AbilitySlot.class);
        for (JsonElement abilityElement : hero.getAsJsonArray("abilities")) {
            JsonObject ability = abilityElement.getAsJsonObject();
            AbilitySlot slot = AbilitySlot.valueOf(ability.get("slot").getAsString());
            abilities.put(slot, new AbilityDefinition(ability.get("id").getAsString(), slot,
                    ability.get("maxRank").getAsInt(), ability.get("unlockLevel").getAsInt()));
        }
        JsonObject hitbox = hero.getAsJsonObject("hitbox");
        return new HeroDefinition(hero.get("id").getAsString(),
                HeroClass.valueOf(hero.get("heroClass").getAsString()),
                DamageType.valueOf(hero.get("damageType").getAsString()), stats,
                hitbox.get("width").getAsDouble(), hitbox.get("height").getAsDouble(),
                hero.get("moveSpeed").getAsDouble(), hero.get("baseAttackInterval").getAsDouble(), abilities);
    }
}
