package com.zeravorn.map;

import com.zeravorn.team.TeamId;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;

/** Idempotent scenery pass over our blockout. Never replaces unknown player-placed blocks. */
final class ArenaVisualUpgrade {
    private static final int BATCH = 512;
    private final MapDefinition map;
    private final int floorY;
    private final BlockPos marker;
    private int index;

    ArenaVisualUpgrade(MapDefinition map, int floorY) {
        this.map = map;
        this.floorY = floorY;
        this.marker = new BlockPos(0, floorY - 1, 0);
    }

    boolean step(ServerLevel level) {
        if (level.getBlockState(marker).is(Blocks.CRYING_OBSIDIAN)) return true;
        int minX = (int) map.playableBoundary().minX();
        int minZ = (int) map.playableBoundary().minZ();
        int width = (int) (map.playableBoundary().maxX() - minX) + 1;
        int depth = (int) (map.playableBoundary().maxZ() - minZ) + 1;
        int end = Math.min(width * depth, index + BATCH);
        for (; index < end; index++) decorateCell(level, minX + index % width, minZ + index / width);
        if (index < width * depth) return false;
        // The marker is below the generated floor, invisible to players and outside movement/LOS.
        if (level.getBlockState(marker).isAir())
            level.setBlock(marker, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
        return true;
    }

    private void decorateCell(ServerLevel level, int x, int z) {
        if (x <= map.playableBoundary().minX() + 2 || x >= map.playableBoundary().maxX() - 2
                || z <= map.playableBoundary().minZ() + 2 || z >= map.playableBoundary().maxZ() - 2) return;
        double lane = laneDistance(x, z);
        for (TeamId team : TeamId.values()) {
            Position throne = map.thrones().get(team);
            if (distance(x, z, throne) <= 4) {
                structure(level, x, z, throne, team == TeamId.BLUE ? Blocks.DIAMOND_BLOCK : Blocks.REDSTONE_BLOCK,
                        true);
                return;
            }
            Position spawn = map.spawns().get(team).position();
            double base = distance(x, z, spawn);
            if (base <= 11) {
                if (base > 7 && base < 10.5) floor(level, x, z, Blocks.POLISHED_ANDESITE);
                else if (base <= 7 && (Math.abs(x - spawn.x()) == 6 || Math.abs(z - spawn.z()) == 6))
                    floor(level, x, z, Blocks.SMOOTH_STONE);
                return;
            }
        }
        for (var tower : map.towers()) {
            if (distance(x, z, tower.position()) <= 3) {
                structure(level, x, z, tower.position(), tower.team() == TeamId.BLUE
                        ? Blocks.CONCRETE.blue() : Blocks.CONCRETE.red(), false);
                return;
            }
        }
        for (var camp : map.jungleCamps()) {
            double d = distance(x, z, camp.position());
            if (d <= 6) {
                if (d >= 4 && d < 5.5) floor(level, x, z, Blocks.MOSSY_STONE_BRICKS);
                else if (d > 1 && d < 4 && hash(x, z) % 3 == 0) floor(level, x, z, Blocks.COARSE_DIRT);
                return;
            }
        }
        for (var bush : map.bushZones()) {
            if (distance(x, z, bush.center()) <= bush.radius()) {
                if (lane > 2.5 && hash(x, z) % 3 != 0)
                    replaceAirOrOldBush(level, x, floorY + 1, z,
                            hash(x, z) % 5 == 0 ? Blocks.FERN : Blocks.SHORT_GRASS);
                else if ((int) bush.center().x() == x && (int) bush.center().z() == z)
                    replaceAirOrOldBush(level, x, floorY + 1, z, Blocks.SHORT_GRASS);
                if (hash(x, z) % 4 == 0) floor(level, x, z, Blocks.MOSS_BLOCK);
                return;
            }
        }
        if (lane < 5 || nearImportant(x, z, 9)) return;
        if (hash(x, z) % 5 == 0) floor(level, x, z, Blocks.PODZOL);
        else if (hash(x, z) % 7 == 0) floor(level, x, z, Blocks.MOSS_BLOCK);

        int tx = Math.round(x / 11f) * 11, tz = Math.round(z / 11f) * 11;
        if (Math.abs(x - tx) <= 2 && Math.abs(z - tz) <= 2
                && hash(tx, tz) % 3 != 0 && laneDistance(tx, tz) >= 10
                && !nearImportant(tx, tz, 10)) {
            if (x == tx && z == tz) {
                for (int y = floorY + 1; y <= floorY + 4; y++) airOnly(level, x, y, z, Blocks.SPRUCE_LOG);
            } else if (Math.abs(x - tx) <= 2 && Math.abs(z - tz) <= 2) {
                leaves(level, x, floorY + 4, z);
                if (Math.abs(x - tx) <= 1 && Math.abs(z - tz) <= 1)
                    leaves(level, x, floorY + 5, z);
            }
            return;
        }
        // Sparse paired rock walls create real LOS/Flash decisions without sealing jungle entrances.
        int wx = Math.round(x / 23f) * 23, wz = Math.round(z / 23f) * 23;
        if (Math.abs(x - wx) <= 2 && z == wz && hash(wx, wz) % 3 == 0
                && laneDistance(wx, wz) >= 10 && !nearImportant(wx, wz, 10)) {
            airOnly(level, x, floorY + 1, z, Blocks.MOSSY_COBBLESTONE);
            if (Math.abs(x - wx) <= 1) airOnly(level, x, floorY + 2, z, Blocks.MOSSY_COBBLESTONE);
        } else if (hash(x, z) % 11 == 0) {
            airOnly(level, x, floorY + 1, z, Blocks.FERN);
        }
    }

    private void structure(ServerLevel level, int x, int z, Position center, Block teamBlock, boolean throne) {
        int dx = Math.abs(x - (int) center.x()), dz = Math.abs(z - (int) center.z());
        if (dx == 0 && dz == 0) {
            airOnly(level, x, floorY + (throne ? 6 : 5), z, Blocks.SEA_LANTERN);
            return;
        }
        if (dx <= 1 && dz <= 1) {
            floor(level, x, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
            airOnly(level, x, floorY + 1, z, throne ? Blocks.CHISELED_POLISHED_BLACKSTONE : teamBlock);
            if (throne) airOnly(level, x, floorY + 5, z, teamBlock);
            else airOnly(level, x, floorY + 4, z, teamBlock);
        } else if (dx <= (throne ? 3 : 2) && dz <= (throne ? 3 : 2)
                && (dx == (throne ? 3 : 2) || dz == (throne ? 3 : 2))) {
            floor(level, x, z, throne ? Blocks.GILDED_BLACKSTONE : Blocks.POLISHED_ANDESITE);
        }
    }

    private boolean nearImportant(int x, int z, double radius) {
        for (var spawn : map.spawns().values()) if (distance(x, z, spawn.position()) < radius + 3) return true;
        for (var throne : map.thrones().values()) if (distance(x, z, throne) < radius) return true;
        for (var tower : map.towers()) if (distance(x, z, tower.position()) < radius) return true;
        for (var camp : map.jungleCamps()) if (distance(x, z, camp.position()) < radius) return true;
        for (var bush : map.bushZones()) if (distance(x, z, bush.center()) < radius) return true;
        return false;
    }

    private double laneDistance(double x, double z) {
        double best = Double.POSITIVE_INFINITY;
        for (var byTeam : map.lanePaths().values()) {
            var path = byTeam.get(TeamId.BLUE);
            for (int i = 1; i < path.size(); i++) {
                Position a = path.get(i - 1), b = path.get(i);
                double vx = b.x() - a.x(), vz = b.z() - a.z();
                double t = Math.max(0, Math.min(1, ((x - a.x()) * vx + (z - a.z()) * vz)
                        / Math.max(0.001, vx * vx + vz * vz)));
                double px = a.x() + t * vx - x, pz = a.z() + t * vz - z;
                best = Math.min(best, Math.sqrt(px * px + pz * pz));
            }
        }
        return best;
    }

    private static double distance(double x, double z, Position p) {
        return Math.hypot(x - p.x(), z - p.z());
    }

    private static int hash(int x, int z) {
        int h = Math.abs(x) * 73428767 ^ Math.abs(z) * 912931;
        return (h ^ h >>> 16) & Integer.MAX_VALUE;
    }

    private void floor(ServerLevel level, int x, int z, Block replacement) {
        BlockPos pos = new BlockPos(x, floorY, z);
        var current = level.getBlockState(pos);
        if (current.is(Blocks.GRASS_BLOCK) || current.is(Blocks.STONE_BRICKS)
                || current.is(Blocks.CONCRETE.blue()) || current.is(Blocks.CONCRETE.red())
                || current.is(Blocks.PODZOL) || current.is(Blocks.MOSS_BLOCK)
                || current.is(Blocks.COARSE_DIRT) || current.is(Blocks.POLISHED_ANDESITE)
                || current.is(Blocks.SMOOTH_STONE) || current.is(Blocks.MOSSY_STONE_BRICKS)
                || current.is(Blocks.POLISHED_BLACKSTONE_BRICKS) || current.is(Blocks.GILDED_BLACKSTONE))
            level.setBlock(pos, replacement.defaultBlockState(), 2);
    }

    private static void airOnly(ServerLevel level, int x, int y, int z, Block block) {
        BlockPos pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos).isAir()) level.setBlock(pos, block.defaultBlockState(), 2);
    }

    private static void leaves(ServerLevel level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos).isAir())
            level.setBlock(pos, Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true), 2);
    }

    private static void replaceAirOrOldBush(ServerLevel level, int x, int y, int z, Block block) {
        BlockPos pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos).isAir() || level.getBlockState(pos).is(Blocks.OAK_LEAVES))
            level.setBlock(pos, block.defaultBlockState(), 2);
    }
}
