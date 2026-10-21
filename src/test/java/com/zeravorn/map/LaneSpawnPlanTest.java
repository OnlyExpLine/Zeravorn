package com.zeravorn.map;

import com.zeravorn.minion.Lane;
import com.zeravorn.team.TeamId;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class LaneSpawnPlanTest {
    @Test void allSixFormationsClearBaseModelsAndEachOther() {
        var map = MapLoader.loadDefault();
        var starts = new ArrayList<Position>();
        for (var team : TeamId.values()) for (var lane : Lane.values()) for (int slot=0;slot<6;slot++) {
            var path = map.lanePaths().get(lane).get(team);
            var route = LaneSpawnPlan.route(path,slot);
            var start = route.getFirst();
            assertTrue(map.playableBoundary().contains(start));
            assertTrue(start.distanceSquared(map.fountains().get(team).center()) > 12*12);
            for (var throne : map.thrones().values()) assertTrue(start.distanceSquared(throne)>5*5, team+" "+lane+" "+slot);
            for (var previous : starts) assertTrue(start.distanceSquared(previous)>=1.4*1.4,"Overlapping formations");
            starts.add(start);
            assertEquals(path.getLast(),route.getLast());
            assertFalse(route.contains(path.getFirst()),"Must never turn back to the shared spawn");
            // Follow every metre of the base exit; no segment crosses either throne core.
            var a=start;var b=route.get(1);
            for(int i=0;i<=100;i++){
                var sample=new Position(a.x()+(b.x()-a.x())*i/100,a.y(),a.z()+(b.z()-a.z())*i/100);
                for(var throne:map.thrones().values()) assertTrue(sample.distanceSquared(throne)>4*4);
                for(var tower:map.towers()) assertTrue(sample.distanceSquared(tower.position())>2*2);
            }
        }
        assertEquals(36,starts.size());
    }
}
