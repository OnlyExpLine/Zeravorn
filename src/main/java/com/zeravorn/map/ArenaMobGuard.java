package com.zeravorn.map;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Mob;

/** Vanilla wildlife and monsters are not MOBA jungle camps. */
public final class ArenaMobGuard {
    private ArenaMobGuard() { }

    public static void initialize() {
        ServerEntityEvents.ALLOW_LOAD.register((entity, level, reason, fromDisk) ->
                !level.dimension().equals(ArenaWorld.DIMENSION)
                        || !(entity instanceof Mob)
                        || BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals("zeravorn"));
    }
}
