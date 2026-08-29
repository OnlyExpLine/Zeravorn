package com.zeravorn.minion;

import com.zeravorn.map.Position;
import com.zeravorn.team.TeamId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class WaveService {
    public static final long FIRST_WAVE_TICKS = LaneBalance.firstWaveTicks();
    public static final long WAVE_INTERVAL_TICKS = LaneBalance.intervalTicks();
    public WaveComposition composition(int waveNumber) {
        if (waveNumber < 1) throw new IllegalArgumentException("waveNumber must be positive");
        return new WaveComposition(3, 2, waveNumber % 3 == 0 ? 1 : 0);
    }
    public int waveNumberAt(long elapsedTicks) {
        if (elapsedTicks < FIRST_WAVE_TICKS) return 0;
        return (int)((elapsedTicks - FIRST_WAVE_TICKS) / WAVE_INTERVAL_TICKS) + 1;
    }
    public boolean shouldSpawnWave(long elapsedTicks, long previousElapsedTicks) {
        int before = waveNumberAt(previousElapsedTicks), now = waveNumberAt(elapsedTicks);
        return now > before;
    }
    public List<LaneMinion> spawnWave(int waveNumber, TeamId team, Lane lane, List<Position> path) {
        WaveComposition c = composition(waveNumber); List<LaneMinion> result = new ArrayList<>();
        for (int i=0;i<c.melee();i++) result.add(create(MinionType.MELEE, waveNumber, team, lane, path));
        for (int i=0;i<c.ranged();i++) result.add(create(MinionType.RANGED, waveNumber, team, lane, path));
        for (int i=0;i<c.siege();i++) result.add(create(MinionType.SIEGE, waveNumber, team, lane, path));
        return List.copyOf(result);
    }
    private LaneMinion create(MinionType type, int wave, TeamId team, Lane lane, List<Position> path) {
        return new LaneMinion(UUID.randomUUID(), team, lane, wave, LaneMinionDefinition.defaults(type), path);
    }
}
