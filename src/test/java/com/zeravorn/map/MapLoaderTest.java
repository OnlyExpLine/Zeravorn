package com.zeravorn.map;

import com.google.gson.JsonParser;
import com.zeravorn.minion.Lane;
import com.zeravorn.team.TeamId;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MapLoaderTest {
    @Test void defaultMapHasBothBasesAndAllThreePlayableRoutes() {
        var map = MapLoader.loadDefault();
        assertEquals(18, map.towers().size());
        assertEquals(10, map.jungleCamps().size());
        for (Lane lane : Lane.values()) for (TeamId team : TeamId.values())
            assertEquals(map.lanePaths().get(lane).get(team), map.botRoute(team, lane).waypoints());
        assertTrue(map.playableBoundary().contains(map.spawns().get(TeamId.BLUE).position()));
        assertFalse(map.playableBoundary().contains(new Position(200, 65, 0)));
    }

    @Test void validatorRejectsMissingTowerAndOutOfBoundsWaypoint() {
        try (var stream = MapLoaderTest.class.getResourceAsStream("/config_defaults/arena_v1.json")) {
            assertNotNull(stream);
            var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            root.getAsJsonArray("towers").remove(0);
            assertThrows(IllegalArgumentException.class, () -> MapLoader.parse(root));
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
        try (var stream = MapLoaderTest.class.getResourceAsStream("/config_defaults/arena_v1.json")) {
            var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            root.getAsJsonObject("lanePaths").getAsJsonObject("MID")
                    .getAsJsonArray("BLUE").get(4).getAsJsonArray().set(0,
                            new com.google.gson.JsonPrimitive(999));
            assertThrows(IllegalArgumentException.class, () -> MapLoader.parse(root));
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }

    @Test void validatorRejectsRouteThroughThronePillar() {
        try (var stream = MapLoaderTest.class.getResourceAsStream("/config_defaults/arena_v1.json")) {
            assertNotNull(stream);
            var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            var redMid = root.getAsJsonObject("lanePaths").getAsJsonObject("MID").getAsJsonArray("RED");
            redMid.remove(1);
            redMid.remove(1);
            var error = assertThrows(IllegalArgumentException.class, () -> MapLoader.parse(root));
            assertTrue(error.getMessage().contains("throne"));
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
