package com.zeravorn.lobby;

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

/** Disposable M11 entry-test arena. M13 replaces it with a validated playable map. */
final class TestArenaWorld {
    private static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,
            Zeravorn.id("test_arena"));
    private static final BlockPos MARKER = new BlockPos(0, 64, 0);

    private TestArenaWorld() { }

    static ServerLevel prepare(MinecraftServer server) {
        ServerLevel level = server.getLevel(DIMENSION);
        if (level == null) throw new IllegalStateException("Missing zeravorn:test_arena dimension");
        if (level.getBlockState(MARKER).is(Blocks.LODESTONE)) return level;
        for (int x = -28; x <= 28; x++) for (int z = -9; z <= 9; z++)
            for (int y = 64; y <= 70; y++)
                if (!level.getBlockState(new BlockPos(x, y, z)).isAir())
                    throw new IllegalStateException("Test arena area occupied at " + x + "," + y + "," + z);
        for (int x = -28; x <= 28; x++) for (int z = -9; z <= 9; z++)
            level.setBlock(new BlockPos(x, 64, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        for (int y = 65; y <= 69; y++) {
            for (int x = -28; x <= 28; x++) {
                level.setBlock(new BlockPos(x, y, -9), Blocks.GLASS.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, y, 9), Blocks.GLASS.defaultBlockState(), 3);
            }
            for (int z = -8; z <= 8; z++) {
                level.setBlock(new BlockPos(-28, y, z), Blocks.GLASS.defaultBlockState(), 3);
                level.setBlock(new BlockPos(28, y, z), Blocks.GLASS.defaultBlockState(), 3);
                level.setBlock(new BlockPos(0, y, z), Blocks.GLASS.defaultBlockState(), 3);
            }
        }
        for (int x = -20; x <= -16; x++) for (int z = -2; z <= 2; z++)
            level.setBlock(new BlockPos(x, 64, z), Blocks.CONCRETE.blue().defaultBlockState(), 3);
        for (int x = 16; x <= 20; x++) for (int z = -2; z <= 2; z++)
            level.setBlock(new BlockPos(x, 64, z), Blocks.CONCRETE.red().defaultBlockState(), 3);
        level.setBlock(MARKER, Blocks.LODESTONE.defaultBlockState(), 3);
        return level;
    }

    static boolean enter(ServerLevel level, ServerPlayer player, TeamId team) {
        double x = team == TeamId.BLUE ? -18.5 : 18.5;
        float yaw = team == TeamId.BLUE ? -90 : 90;
        return player.teleportTo(level, x, 65, 0.5, Set.<Relative>of(), yaw, 0, true);
    }
}
