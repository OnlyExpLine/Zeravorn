package com.zeravorn.lobby;

import com.zeravorn.hero.HeroDefinitions;
import com.zeravorn.map.Position;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class LobbyRoomTest {
    @Test void pedestalInteractionIsDimensionAndBlockScoped() {
        var pedestals = new ArrayList<LobbyRoom.HeroPedestal>();
        for (int i = 0; i < HeroDefinitions.all().size(); i++)
            pedestals.add(new LobbyRoom.HeroPedestal(HeroDefinitions.all().get(i).id(),
                    new Position(i * 3, 65, 4)));
        var room = new LobbyRoom("zeravorn:lobby", new Position(0, 65, 0),
                new Position(-7, 65, 0), new Position(7, 65, 0),
                new Position(0, 65, -5), pedestals);
        assertEquals(HeroDefinitions.all().getFirst().id(),
                room.pedestalAt("zeravorn:lobby", 0, 65, 4).orElseThrow().heroId());
        assertTrue(room.pedestalAt("zeravorn:arena", 0, 65, 4).isEmpty());
        assertTrue(room.pedestalAt("zeravorn:lobby", 0, 65, 5).isEmpty());
    }
}
