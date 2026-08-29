package com.zeravorn.minion;
import com.zeravorn.map.Position; import com.zeravorn.team.TeamId; import java.util.List; import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*;
class LaneMinionTest {
 @Test void wavesAndCadence(){var s=new WaveService(); assertEquals(0,s.waveNumberAt(599)); assertEquals(1,s.waveNumberAt(600)); assertEquals(new WaveComposition(3,2,0),s.composition(1)); assertEquals(1,s.composition(3).siege()); assertEquals(5,s.spawnWave(1,TeamId.BLUE,Lane.MID,List.of(new Position(0,0,0))).size());}
 @Test void scalingAndPath(){var m=new WaveService().spawnWave(5,TeamId.RED,Lane.TOP,List.of(new Position(0,0,0),new Position(1,0,0))).get(0); assertTrue(m.maxHealth()>300); assertTrue(m.damage()>20); assertTrue(m.advanceWaypoint()); assertFalse(m.advanceWaypoint());}
}
