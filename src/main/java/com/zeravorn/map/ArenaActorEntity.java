package com.zeravorn.map;

import com.zeravorn.Zeravorn;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;

/** Networked, non-persistent hitbox for a match-controlled Blockbench actor. */
public final class ArenaActorEntity extends ArmorStand {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> AIM_YAW = net.minecraft.network.syncher.SynchedEntityData.defineId(ArenaActorEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> PROTECTED = net.minecraft.network.syncher.SynchedEntityData.defineId(ArenaActorEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    public static final EntityType<ArenaActorEntity> TYPE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            Zeravorn.id("arena_actor"), EntityType.Builder.of(ArenaActorEntity::new, MobCategory.MISC)
                    .sized(0.8f, 1.9f).clientTrackingRange(10).updateInterval(1).noSave()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Zeravorn.id("arena_actor"))));

    public ArenaActorEntity(EntityType<? extends ArenaActorEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setNoBasePlate(true);
        setInvisible(true);
    }

    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); builder.define(AIM_YAW, 0f); builder.define(PROTECTED, false);
    }
    public void setAimYaw(float yaw) { getEntityData().set(AIM_YAW, yaw); }
    public float aimYaw() { return getEntityData().get(AIM_YAW); }
    public void setStructureProtected(boolean value) { getEntityData().set(PROTECTED, value); }
    public boolean structureProtected() { return getEntityData().get(PROTECTED); }

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(TYPE, ArmorStand.createAttributes());
    }

    public void setVisual(String visual) {
        setCustomName(Component.literal(visual + "|spawn|" + tickCount));
        setCustomNameVisible(false);
    }

    public void setAnimation(String clip, boolean restart) {
        String[] parts = getCustomName() == null ? new String[0] : getCustomName().getString().split("\\|", 5);
        if (parts.length == 0 || !restart && parts.length > 1 && parts[1].equals(clip)) return;
        setCustomName(Component.literal(parts[0] + "|" + clip + "|" + tickCount
                + (parts.length >= 5 ? "|" + parts[3] + "|" + parts[4] : "")));
    }

    public void setHealthDisplay(int health, int maximum) {
        String[] parts = getCustomName() == null ? new String[0] : getCustomName().getString().split("\\|", 5);
        if (parts.length < 3) return;
        if (parts.length >= 5 && parts[3].equals(Integer.toString(health))
                && parts[4].equals(Integer.toString(maximum))) return;
        setCustomName(Component.literal(parts[0] + "|" + parts[1] + "|" + parts[2]
                + "|" + health + "|" + maximum));
    }

    public String animationName() {
        String[] parts = getCustomName() == null ? new String[0] : getCustomName().getString().split("\\|", 3);
        return parts.length > 1 ? parts[1] : "idle";
    }

    public int animationAge() {
        String[] parts = getCustomName() == null ? new String[0] : getCustomName().getString().split("\\|", 5);
        if (parts.length < 3) return Integer.MAX_VALUE;
        try { return tickCount - Integer.parseInt(parts[2]); }
        catch (NumberFormatException ignored) { return Integer.MAX_VALUE; }
    }

    public void setPresentationOnly(boolean marker) {
        byte flags = getEntityData().get(DATA_CLIENT_FLAGS);
        getEntityData().set(DATA_CLIENT_FLAGS, (byte)(marker ? flags | CLIENT_FLAG_MARKER : flags & ~CLIENT_FLAG_MARKER));
        refreshDimensions();
    }

    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 160 * 160; }
}
