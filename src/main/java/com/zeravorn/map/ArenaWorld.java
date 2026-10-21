package com.zeravorn.map;

import com.zeravorn.Zeravorn;
import com.zeravorn.team.TeamId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

/** Conservative, incremental block-out. A hand-built arena is never overwritten. */
public final class ArenaWorld {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,
            Zeravorn.id("arena_v1"));
    private final MapDefinition map;
    private final int floorY;
    private final BlockPos marker;
    private final ArenaVisualUpgrade visuals;
    private int scanIndex;
    private int floorIndex;
    private boolean scanned;
    private boolean finished;
    private static final net.minecraft.server.level.TicketType MATCH_TICKET = new net.minecraft.server.level.TicketType(
            0, net.minecraft.server.level.TicketType.FLAG_LOADING | net.minecraft.server.level.TicketType.FLAG_SIMULATION
                    | net.minecraft.server.level.TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    private ServerLevel heldLevel;
    private final java.util.List<net.minecraft.world.level.ChunkPos> heldChunks = new java.util.ArrayList<>();

    private void holdMatchChunks(ServerLevel level) {
        if (heldLevel != null) return;
        heldLevel = level;
        var boundary = map.playableBoundary();
        for (int x = (int)Math.floor(boundary.minX()) >> 4; x <= ((int)Math.floor(boundary.maxX()) >> 4); x++)
            for (int z = (int)Math.floor(boundary.minZ()) >> 4; z <= ((int)Math.floor(boundary.maxZ()) >> 4); z++) {
                var chunk = new net.minecraft.world.level.ChunkPos(x, z);
                level.getChunkSource().addTicketWithRadius(MATCH_TICKET, chunk, 2);
                heldChunks.add(chunk);
            }
    }

    public void releaseMatchChunks() {
        if (heldLevel == null) return;
        for (var chunk : heldChunks) heldLevel.getChunkSource().removeTicketWithRadius(MATCH_TICKET, chunk, 2);
        heldChunks.clear();
        heldLevel = null;
    }

    public ArenaWorld(MapDefinition map) {
        this.map = map;
        this.floorY = (int) Math.floor(map.spawns().get(TeamId.BLUE).position().y()) - 1;
        this.marker = new BlockPos(0, floorY, 0);
        this.visuals = new ArenaVisualUpgrade(map, floorY);
    }

    public String dimensionId() { return "zeravorn:arena_v1"; }

    /** Returns false while building; throws without writing if an existing build area is occupied. */
    public boolean prepareStep(MinecraftServer server) {
        ServerLevel level = server.getLevel(DIMENSION);
        if (level == null) throw new IllegalStateException("Missing zeravorn:arena_v1 dimension");
        holdMatchChunks(level);
        keepDaylightAndClearWeather(level);
        if (finished || level.getBlockState(marker).is(Blocks.LODESTONE)) {
            finished = true;
            return visuals.step(level);
        }
        // An earlier version could finish decorating, then fail validation before writing the marker.
        // Recognize only our complete blockout; never clear or rebuild an occupied player world.
        if (isUnmarkedBlockout(level)) {
            validateBuilt(level);
            level.setBlock(marker, Blocks.LODESTONE.defaultBlockState(), 3);
            finished = true;
            return visuals.step(level);
        }
        int minX = (int) map.playableBoundary().minX();
        int minZ = (int) map.playableBoundary().minZ();
        int width = (int) (map.playableBoundary().maxX() - map.playableBoundary().minX()) + 1;
        int depth = (int) (map.playableBoundary().maxZ() - map.playableBoundary().minZ()) + 1;
        int cells = width * depth;
        if (!scanned) {
            int end = Math.min(cells, scanIndex + 1024);
            for (; scanIndex < end; scanIndex++) {
                int x = minX + scanIndex % width, z = minZ + scanIndex / width;
                for (int y = floorY; y <= floorY + 8; y++) {
                    if (!level.getBlockState(new BlockPos(x, y, z)).isAir())
                        throw new IllegalStateException("Arena build area occupied at " + x + "," + y + "," + z);
                }
            }
            if (scanIndex < cells) return false;
            scanned = true;
        }
        int end = Math.min(cells, floorIndex + 1024);
        for (; floorIndex < end; floorIndex++) {
            int x = minX + floorIndex % width, z = minZ + floorIndex / width;
            boolean edge = x == minX || x == minX + width - 1 || z == minZ || z == minZ + depth - 1;
            level.setBlock(new BlockPos(x, floorY, z), edge
                    ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            if (edge) for (int y = floorY + 1; y <= floorY + 4; y++)
                level.setBlock(new BlockPos(x, y, z), Blocks.GLASS.defaultBlockState(), 2);
        }
        if (floorIndex < cells) return false;
        decorate(level);
        validateBuilt(level);
        level.setBlock(marker, Blocks.LODESTONE.defaultBlockState(), 3);
        finished = true;
        return visuals.step(level);
    }

    public boolean enter(MinecraftServer server, ServerPlayer player, TeamId team) {
        if (!finished) return false;
        ServerLevel level = server.getLevel(DIMENSION);
        if (level == null) return false;
        var spawn = map.spawns().get(team);
        double offset = team == TeamId.BLUE ? 3 : -3;
        return player.teleportTo(level, spawn.position().x() + 0.5 + offset, spawn.position().y(),
                spawn.position().z() + 0.5 + offset, Set.<Relative>of(), spawn.yaw(), 0, true);
    }

    /** The arena has its own clock; neither its weather nor daylight depends on survival worlds. */
    public void keepDaylightAndClearWeather(ServerLevel level) {
        level.dimensionType().defaultClock().ifPresent(clock -> {
            level.clockManager().setTotalTicks(clock, 6000);
            level.clockManager().setPaused(clock, true);
        });
        var weather = level.getWeatherData();
        weather.setRaining(false);
        weather.setThundering(false);
        weather.setRainTime(0);
        weather.setThunderTime(0);
        weather.setClearWeatherTime(12000);
        level.setRainLevel(0);
        level.setThunderLevel(0);
    }

    private void decorate(ServerLevel level) {
        for (var byTeam : map.lanePaths().values()) {
            var path = byTeam.get(TeamId.BLUE);
            for (int i = 1; i < path.size(); i++) {
                Position a = path.get(i - 1), b = path.get(i);
                int steps = (int) Math.ceil(Math.max(Math.abs(a.x() - b.x()), Math.abs(a.z() - b.z())));
                for (int step = 0; step <= steps; step++) {
                    double t = steps == 0 ? 0 : (double) step / steps;
                    int cx = (int) Math.round(a.x() + (b.x() - a.x()) * t);
                    int cz = (int) Math.round(a.z() + (b.z() - a.z()) * t);
                    for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++)
                        level.setBlock(new BlockPos(cx + dx, floorY, cz + dz),
                                Blocks.STONE_BRICKS.defaultBlockState(), 2);
                }
            }
        }
        for (TeamId team : TeamId.values()) {
            var spawn = map.spawns().get(team);
            int cx = (int) spawn.position().x(), cz = (int) spawn.position().z();
            for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++)
                level.setBlock(new BlockPos(cx + dx, floorY, cz + dz), team == TeamId.BLUE
                        ? Blocks.CONCRETE.blue().defaultBlockState()
                        : Blocks.CONCRETE.red().defaultBlockState(), 2);
            Position throne = map.thrones().get(team);
            pillar(level, throne, team == TeamId.BLUE ? Blocks.DIAMOND_BLOCK.defaultBlockState()
                    : Blocks.REDSTONE_BLOCK.defaultBlockState(), 5);
        }
        for (var tower : map.towers()) pillar(level, tower.position(), tower.team() == TeamId.BLUE
                ? Blocks.CONCRETE.blue().defaultBlockState()
                : Blocks.CONCRETE.red().defaultBlockState(), 4);
        for (var camp : map.jungleCamps())
            level.setBlock(new BlockPos((int) camp.position().x(), floorY, (int) camp.position().z()),
                    Blocks.MOSS_BLOCK.defaultBlockState(), 2);
        for (var bush : map.bushZones())
            level.setBlock(new BlockPos((int) bush.center().x(), floorY + 1, (int) bush.center().z()),
                    Blocks.OAK_LEAVES.defaultBlockState(), 2);
    }

    private void validateBuilt(ServerLevel level) {
        for (TeamId team : TeamId.values()) {
            Position spawn = map.spawns().get(team).position();
            if (!clear(level, spawn)) throw new IllegalStateException(team + " arena spawn is blocked");
        }
        for (var entry : map.lanePaths().entrySet()) for (var pathEntry : entry.getValue().entrySet()) {
            var path = pathEntry.getValue();
            // The final waypoint intentionally reaches the enemy throne's hitbox.
            for (int i = 0; i < path.size() - 1; i++)
                if (!clear(level, path.get(i)))
                    throw new IllegalStateException("Blocked " + pathEntry.getKey() + " "
                            + entry.getKey() + " waypoint " + i);
        }
        for (var camp : map.jungleCamps())
            if (!clear(level, camp.position()))
                throw new IllegalStateException("Blocked jungle camp " + camp.id());
    }

    private static boolean clear(ServerLevel level, Position point) {
        int x = (int) Math.floor(point.x()), y = (int) Math.floor(point.y());
        int z = (int) Math.floor(point.z());
        return level.getBlockState(new BlockPos(x, y, z)).isAir()
                && level.getBlockState(new BlockPos(x, y + 1, z)).isAir();
    }

    private boolean isUnmarkedBlockout(ServerLevel level) {
        int minX = (int) map.playableBoundary().minX();
        int maxX = (int) map.playableBoundary().maxX();
        int minZ = (int) map.playableBoundary().minZ();
        int maxZ = (int) map.playableBoundary().maxZ();
        for (int x : new int[]{minX, maxX}) for (int z : new int[]{minZ, maxZ}) {
            if (!level.getBlockState(new BlockPos(x, floorY, z)).is(Blocks.POLISHED_DEEPSLATE)) return false;
            for (int y = floorY + 1; y <= floorY + 4; y++)
                if (!level.getBlockState(new BlockPos(x, y, z)).is(Blocks.GLASS)) return false;
        }
        for (TeamId team : TeamId.values()) {
            Position throne = map.thrones().get(team);
            var expected = team == TeamId.BLUE ? Blocks.DIAMOND_BLOCK : Blocks.REDSTONE_BLOCK;
            for (int y = floorY + 1; y <= floorY + 5; y++)
                if (!level.getBlockState(new BlockPos((int) throne.x(), y, (int) throne.z())).is(expected))
                    return false;
        }
        for (var tower : map.towers()) {
            var expected = tower.team() == TeamId.BLUE ? Blocks.CONCRETE.blue() : Blocks.CONCRETE.red();
            for (int y = floorY + 1; y <= floorY + 4; y++)
                if (!level.getBlockState(new BlockPos((int) tower.position().x(), y,
                        (int) tower.position().z())).is(expected)) return false;
        }
        for (var camp : map.jungleCamps())
            if (!level.getBlockState(new BlockPos((int) camp.position().x(), floorY,
                    (int) camp.position().z())).is(Blocks.MOSS_BLOCK)) return false;
        for (var bush : map.bushZones())
            if (!level.getBlockState(new BlockPos((int) bush.center().x(), floorY + 1,
                    (int) bush.center().z())).is(Blocks.OAK_LEAVES)) return false;
        return true;
    }

    private void pillar(ServerLevel level, Position point,
                        net.minecraft.world.level.block.state.BlockState block, int height) {
        int x = (int) point.x(), z = (int) point.z();
        for (int y = floorY + 1; y < floorY + 1 + height; y++)
            level.setBlock(new BlockPos(x, y, z), block, 2);
    }
}
