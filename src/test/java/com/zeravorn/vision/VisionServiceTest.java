package com.zeravorn.vision;

import com.zeravorn.map.Position;
import com.zeravorn.team.TeamId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class VisionServiceTest {
    @Test void lineOfSightAndRangeDetermineEnemyVisibility() {
        VisionService service = new VisionService(); VisionSource blue = source(TeamId.BLUE, new Position(0, 0, 0), 16, null); VisionTarget red = target(TeamId.RED, new Position(10, 0, 0), null);
        assertTrue(service.recompute(TeamId.BLUE, List.of(blue), List.of(red), (from, to) -> true, 0).showIds().contains(red.id()));
        assertTrue(service.recompute(TeamId.BLUE, List.of(blue), List.of(red), (from, to) -> false, 1).hideIds().contains(red.id()));
    }
    @Test void bushHidesEnemyUnlessFriendlySourceSharesBushOrRevealApplies() {
        VisionService service = new VisionService(); VisionTarget red = target(TeamId.RED, new Position(4, 0, 0), "mid_bush"); VisionSource outside = source(TeamId.BLUE, new Position(0, 0, 0), 16, null);
        assertFalse(service.recompute(TeamId.BLUE, List.of(outside), List.of(red), (from, to) -> true, 0).showIds().contains(red.id()));
        VisionSource inside = source(TeamId.BLUE, new Position(0, 0, 0), 16, "mid_bush"); assertTrue(service.recompute(TeamId.BLUE, List.of(inside), List.of(red), (from, to) -> true, 1).showIds().contains(red.id()));
        service.revealToEnemies(TeamId.RED, red.id(), 2, 20); service.recompute(TeamId.BLUE, List.of(outside), List.of(red), (from, to) -> true, 3); assertTrue(service.visibleTo(TeamId.BLUE, red.id()));
    }
    @Test void minimapContainsOnlyServerVisiblePositionsAndAlliesAreAlwaysVisible() {
        VisionService service = new VisionService(); VisionTarget ally = target(TeamId.BLUE, new Position(1, 0, 0), null); VisionTarget enemy = target(TeamId.RED, new Position(100, 0, 0), null);
        service.recompute(TeamId.BLUE, List.of(), List.of(ally, enemy), (from, to) -> true, 0);
        assertEquals(java.util.Set.of(ally.id()), service.minimapDelta(TeamId.BLUE, List.of(ally, enemy)).visiblePositions().keySet());
    }
    private static VisionSource source(TeamId team, Position position, double radius, String bush) { return new VisionSource(UUID.randomUUID(), team, position, VisionSourceType.HERO, radius, bush); }
    private static VisionTarget target(TeamId team, Position position, String bush) { return new VisionTarget(UUID.randomUUID(), team, position, true, bush); }
}
