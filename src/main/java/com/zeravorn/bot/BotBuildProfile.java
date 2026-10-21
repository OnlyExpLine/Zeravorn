package com.zeravorn.bot;

import com.zeravorn.hero.HeroClass;

import java.util.List;
import java.util.Objects;

/** Fixed non-adaptive item order; purchases still go through ShopService. */
public record BotBuildProfile(List<String> itemIds) {
    public BotBuildProfile {
        itemIds = List.copyOf(itemIds);
        if (itemIds.isEmpty() || itemIds.stream().anyMatch(id -> id == null || id.isBlank()))
            throw new IllegalArgumentException("Empty bot build");
    }

    public static BotBuildProfile forClass(HeroClass heroClass) {
        Objects.requireNonNull(heroClass);
        return switch (heroClass) {
            case FIGHTER -> new BotBuildProfile(List.of("traveler_boots", "iron_blade", "titan_armor"));
            case ASSASSIN -> new BotBuildProfile(List.of("traveler_boots", "berserker_blade", "blooddrinker"));
            case MARKSMAN -> new BotBuildProfile(List.of("combat_boots", "berserker_blade", "crusher"));
            case MAGE -> new BotBuildProfile(List.of("traveler_boots", "frost_crystal", "wisdom_staff"));
            case TANK -> new BotBuildProfile(List.of("traveler_boots", "titan_armor", "giant_heart"));
        };
    }
}
