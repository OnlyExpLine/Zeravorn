package com.zeravorn.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable, validated item catalogue loaded from packaged balance data. */
public final class ItemCatalog {
    private final Map<String, ItemDefinition> items = new LinkedHashMap<>();

    public ItemCatalog() {
        try (var stream = ItemCatalog.class.getResourceAsStream("/config_defaults/items.json")) {
            if (stream == null) throw new IllegalStateException("Missing item balance config");
            JsonArray definitions = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("items");
            for (var element : definitions) add(parse(element.getAsJsonObject()));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load item balance config", exception);
        }
        if (items.size() != 20) throw new IllegalStateException("Expected exactly 20 starter items");
    }

    private static ItemDefinition parse(JsonObject item) {
        JsonObject stats = item.getAsJsonObject("stats");
        return new ItemDefinition(item.get("id").getAsString(), item.get("name").getAsString(),
                ItemCategory.valueOf(item.get("category").getAsString()), item.get("price").getAsInt(),
                SlotType.valueOf(item.get("slotType").getAsString()), new ItemStats(
                value(stats, "attack"), value(stats, "abilityPower"), value(stats, "maxHealth"), value(stats, "maxMana"),
                decimal(stats, "manaRegen"), decimal(stats, "attackSpeed"), decimal(stats, "lifesteal"),
                decimal(stats, "spellVamp"), decimal(stats, "moveSpeed")));
    }
    private static int value(JsonObject object, String field) { return object.has(field) ? object.get(field).getAsInt() : 0; }
    private static double decimal(JsonObject object, String field) { return object.has(field) ? object.get(field).getAsDouble() : 0; }
    private void add(ItemDefinition item) { if (items.putIfAbsent(item.id(), item) != null) throw new IllegalStateException("Duplicate item id: " + item.id()); }
    public ItemDefinition find(String id) { return items.get(id); }
    public Collection<ItemDefinition> all() { return Collections.unmodifiableCollection(items.values()); }
}
