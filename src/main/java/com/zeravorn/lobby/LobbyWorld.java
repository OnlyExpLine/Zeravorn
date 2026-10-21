package com.zeravorn.lobby;

import com.zeravorn.Zeravorn;
import com.zeravorn.hero.HeroDefinitions;
import com.zeravorn.map.Position;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Minimal M11-only lobby room. M13 owns the real arena, not this dimension. */
public final class LobbyWorld {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,
            Zeravorn.id("lobby"));
    private static final BlockPos MARKER = new BlockPos(0, 64, 0);
    private static final BlockPos SELECTION_TERMINAL = new BlockPos(0, 65, -4);
    private static final LobbyRoom ROOM = layout();

    private LobbyWorld() { }

    public static LobbyRoom room() { return ROOM; }

    public static boolean isSelectionTerminal(String dimension, BlockPos pos) {
        return ROOM.dimensionId().equals(dimension) && SELECTION_TERMINAL.equals(pos);
    }

    /** Returns false without touching the current world if the dimension is absent or occupied. */
    public static boolean enter(MinecraftServer server, ServerPlayer player) {
        ServerLevel level = server.getLevel(DIMENSION);
        if (level == null) {
            Zeravorn.LOGGER.error("Missing zeravorn:lobby dimension; cannot enter lobby");
            return false;
        }
        if (!prepare(level)) return false;
        Position spawn = ROOM.safeSpawn();
        return player.teleportTo(level, spawn.x() + 0.5, spawn.y(), spawn.z() + 0.5,
                Set.<Relative>of(), 0, 0, true);
    }

    private static boolean prepare(ServerLevel level) {
        if (level.getBlockState(MARKER).is(Blocks.LODESTONE)) {
            // Existing M11 rooms predate the terminal; add it without rebuilding the room.
            if (level.getBlockState(SELECTION_TERMINAL).isAir())
                level.setBlock(SELECTION_TERMINAL, Blocks.EMERALD_BLOCK.defaultBlockState(), 3);
            for (ArmorStand preview : level.getEntitiesOfClass(ArmorStand.class,
                    new AABB(-10, 65, 2, 10, 69, 6),
                    entity -> entity.entityTags().contains("zeravorn_lobby_preview"))) dress(preview);
            return true;
        }
        // Preserve an existing hand-built room instead of overwriting it.
        for (int x = -12; x <= 12; x++) for (int z = -10; z <= 10; z++)
            for (int y = 64; y <= 69; y++)
                if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) {
                    Zeravorn.LOGGER.error("Lobby build area is occupied at {},{},{}; leaving it untouched", x, y, z);
                    return false;
                }
        for (int x = -12; x <= 12; x++) for (int z = -10; z <= 10; z++)
            level.setBlock(new BlockPos(x, 64, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        for (int y = 65; y <= 68; y++) {
            for (int x = -12; x <= 12; x++) {
                level.setBlock(new BlockPos(x, y, -10), Blocks.GLASS.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, y, 10), Blocks.GLASS.defaultBlockState(), 3);
            }
            for (int z = -9; z <= 9; z++) {
                level.setBlock(new BlockPos(-12, y, z), Blocks.GLASS.defaultBlockState(), 3);
                level.setBlock(new BlockPos(12, y, z), Blocks.GLASS.defaultBlockState(), 3);
            }
        }
        level.setBlock(new BlockPos(-8, 65, 0), Blocks.CONCRETE.blue().defaultBlockState(), 3);
        level.setBlock(new BlockPos(8, 65, 0), Blocks.CONCRETE.red().defaultBlockState(), 3);
        level.setBlock(new BlockPos(0, 65, -2), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        level.setBlock(SELECTION_TERMINAL, Blocks.EMERALD_BLOCK.defaultBlockState(), 3);
        label(level, -7.5, 67, 0.5, "BLUE TEAM: right click block");
        label(level, 8.5, 67, 0.5, "RED TEAM: right click block");
        label(level, 0.5, 67, -1.5, "READY: right click block");
        label(level, 0.5, 67, -3.5, "SELECTION: right click emerald block (or L)");
        for (LobbyRoom.HeroPedestal pedestal : ROOM.pedestals()) {
            int x = (int) pedestal.position().x(), z = (int) pedestal.position().z();
            level.setBlock(new BlockPos(x, 65, z), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 3);
            ArmorStand preview = new ArmorStand(level, x + 0.5, 66, z + 0.5);
            preview.setNoGravity(true);
            preview.setInvulnerable(true);
            dress(preview);
            preview.addTag("zeravorn_lobby_preview");
            preview.setCustomName(Component.literal(pedestal.heroId().toUpperCase() + "  "
                    + HeroDefinitions.byId(pedestal.heroId()).heroClass()));
            preview.setCustomNameVisible(true);
            level.addFreshEntity(preview);
        }
        // Marker is written last so an interrupted build is never mistaken for a finished room.
        level.setBlock(MARKER, Blocks.LODESTONE.defaultBlockState(), 3);
        return true;
    }

    private static void label(ServerLevel level, double x, double y, double z, String text) {
        ArmorStand label = new ArmorStand(level, x, y, z);
        label.setInvisible(true);
        label.setNoGravity(true);
        label.setInvulnerable(true);
        label.setCustomName(Component.literal(text));
        label.setCustomNameVisible(true);
        level.addFreshEntity(label);
    }

    private static void dress(ArmorStand preview) {
        preview.setShowArms(true);
        preview.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        preview.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        preview.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
        preview.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
    }

    private static LobbyRoom layout() {
        var pedestals = new ArrayList<LobbyRoom.HeroPedestal>();
        for (int i = 0; i < HeroDefinitions.all().size(); i++)
            pedestals.add(new LobbyRoom.HeroPedestal(HeroDefinitions.all().get(i).id(),
                    new Position(-8 + i * 4, 65, 4)));
        return new LobbyRoom("zeravorn:lobby", new Position(0, 65, -6),
                new Position(-8, 65, 0), new Position(8, 65, 0),
                new Position(0, 65, -2), List.copyOf(pedestals));
    }
}
