package com.zeravorn.lobby;

import com.zeravorn.hero.HeroDefinitions;
import com.zeravorn.map.Position;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Engine-independent layout contract; M13 supplies the actual separate Minecraft world. */
public record LobbyRoom(String dimensionId, Position safeSpawn, Position bluePresentation,
                        Position redPresentation, Position readyArea,
                        List<HeroPedestal> pedestals) {
    public LobbyRoom {
        Objects.requireNonNull(dimensionId, "dimensionId");
        Objects.requireNonNull(safeSpawn, "safeSpawn");
        Objects.requireNonNull(bluePresentation, "bluePresentation");
        Objects.requireNonNull(redPresentation, "redPresentation");
        Objects.requireNonNull(readyArea, "readyArea");
        List<HeroPedestal> copy = List.copyOf(pedestals);
        if (dimensionId.isBlank() || copy.size() != HeroDefinitions.all().size()
                || copy.stream().map(HeroPedestal::heroId).distinct().count() != copy.size()
                || HeroDefinitions.all().stream().anyMatch(hero ->
                copy.stream().noneMatch(pedestal -> pedestal.heroId().equals(hero.id())))) {
            throw new IllegalArgumentException("Lobby needs one pedestal per hero");
        }
        pedestals = copy;
    }

    public record HeroPedestal(String heroId, Position position) {
        public HeroPedestal {
            Objects.requireNonNull(heroId, "heroId");
            Objects.requireNonNull(position, "position");
        }
    }

    /** The clickable block is the pedestal's configured block coordinate. */
    public Optional<HeroPedestal> pedestalAt(String dimension, int x, int y, int z) {
        if (!dimensionId.equals(dimension)) return Optional.empty();
        return pedestals.stream().filter(p -> p.position().x() == x
                && p.position().y() == y && p.position().z() == z).findFirst();
    }
}
