package com.zeravorn.map;

import com.zeravorn.vision.LineOfSight;
import com.zeravorn.team.TeamId;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;

/** Real block-based boundary, collision and LOS checks used by M13 world adapters. */
public final class ArenaGeometry implements LineOfSight {
    private final MapDefinition map;
    private final ServerLevel level;

    public ArenaGeometry(MapDefinition map, ServerLevel level) {
        this.map = Objects.requireNonNull(map);
        this.level = Objects.requireNonNull(level);
    }

    public boolean inside(Position position) { return map.playableBoundary().contains(position); }

    public boolean inEnemyFountainDanger(TeamId actorTeam, Position position) {
        TeamId enemy = actorTeam == TeamId.BLUE ? TeamId.RED : TeamId.BLUE;
        return map.fountainDangerZones().get(enemy).contains(position);
    }

    /** Flash/dash endpoint: feet/head clear, solid support, inside playable box. */
    public boolean safeEndpoint(Position position) {
        if (!inside(position)) return false;
        int x = (int) Math.floor(position.x()), y = (int) Math.floor(position.y());
        int z = (int) Math.floor(position.z());
        return level.getBlockState(new BlockPos(x, y, z)).isAir()
                && level.getBlockState(new BlockPos(x, y + 1, z)).isAir()
                && !level.getBlockState(new BlockPos(x, y - 1, z)).isAir();
    }

    @Override public boolean hasLineOfSight(Position from, Position to) {
        if (!inside(from) || !inside(to)) return false;
        double dx = to.x() - from.x(), dy = to.y() - from.y(), dz = to.z() - from.z();
        int samples = Math.max(1, (int) Math.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz) * 4));
        for (int i = 1; i < samples; i++) {
            double t = (double) i / samples;
            int x = (int) Math.floor(from.x() + dx * t);
            int y = (int) Math.floor(from.y() + dy * t + 1.5);
            int z = (int) Math.floor(from.z() + dz * t);
            if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) return false;
        }
        return true;
    }

    public String bushAt(Position point) {
        for (var bush : map.bushZones()) if (bush.contains(point)) return bush.id();
        return null;
    }
}
