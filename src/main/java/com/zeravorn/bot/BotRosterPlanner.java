package com.zeravorn.bot;

import com.zeravorn.hero.HeroDefinition;
import com.zeravorn.hero.HeroDefinitions;
import com.zeravorn.minion.Lane;
import com.zeravorn.team.TeamId;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Deterministic five-slot plan, with hero uniqueness scoped to each team. */
public final class BotRosterPlanner {
    public record Slot(UUID id, TeamId team, String heroId, Lane lane, String name) { }

    public List<Slot> fill(UUID matchId, TeamId team, List<String> humanHeroIds, int humanSlots) {
        Objects.requireNonNull(matchId);
        Objects.requireNonNull(team);
        Objects.requireNonNull(humanHeroIds);
        if (humanSlots < 0 || humanSlots > 5 || humanHeroIds.size() > humanSlots)
            throw new IllegalArgumentException("Invalid human slots");
        Set<String> chosen = new HashSet<>(humanHeroIds);
        if (chosen.size() != humanHeroIds.size()) throw new IllegalArgumentException("Duplicate ally hero");
        List<Slot> bots = new ArrayList<>();
        while (bots.size() + humanSlots < 5) {
            Set<com.zeravorn.hero.HeroClass> covered = EnumSet.noneOf(com.zeravorn.hero.HeroClass.class);
            for (String id : chosen) covered.add(HeroDefinitions.byId(id).heroClass());
            HeroDefinition hero = HeroDefinitions.all().stream().filter(candidate -> !chosen.contains(candidate.id()))
                    .filter(candidate -> !covered.contains(candidate.heroClass())).findFirst()
                    .orElseGet(() -> HeroDefinitions.all().stream()
                            .filter(candidate -> !chosen.contains(candidate.id())).findFirst().orElseThrow());
            int number = bots.size() + 1;
            UUID id = UUID.nameUUIDFromBytes((matchId + ":" + team + ":bot:" + number)
                    .getBytes(StandardCharsets.UTF_8));
            Lane lane = switch (bots.size() % 3) {
                case 0 -> Lane.TOP;
                case 1 -> Lane.MID;
                default -> Lane.BOT;
            };
            bots.add(new Slot(id, team, hero.id(), lane, "Bot " + team + " " + number));
            chosen.add(hero.id());
        }
        return List.copyOf(bots);
    }
}
