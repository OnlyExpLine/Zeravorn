package com.zeravorn.map;

import net.minecraft.server.level.ServerLevel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Server-owned homing shots. Damage is committed once, on arrival at a still-live target. */
public final class ArenaProjectileRuntime {
    private final List<Shot> shots = new ArrayList<>();

    public void launch(ServerLevel level, String visual, Position origin, Supplier<Position> target,
                       BooleanSupplier alive, Runnable impact) {
        ArenaActorEntity entity = new ArenaActorEntity(ArenaActorEntity.TYPE, level);
        entity.setPresentationOnly(true);
        entity.setVisual(visual);
        entity.setAnimation("flight", true);
        entity.setPos(origin.x(), origin.y(), origin.z());
        level.addFreshEntity(entity);
        shots.add(new Shot(entity, target, alive, impact));
    }

    public void tick() {
        for (var it = shots.iterator(); it.hasNext();) {
            Shot shot = it.next();
            if (shot.entity.isRemoved() || !shot.alive.getAsBoolean() || ++shot.age > 100) {
                shot.entity.discard(); it.remove(); continue;
            }
            Position p = shot.target.get();
            if (p == null) { shot.entity.discard(); it.remove(); continue; }
            double dx = p.x() - shot.entity.getX(), dy = p.y() - shot.entity.getY(), dz = p.z() - shot.entity.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance <= .85) {
                shot.impact.run(); shot.entity.discard(); it.remove(); continue;
            }
            shot.entity.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
            shot.entity.setXRot((float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))));
            shot.entity.teleportTo(shot.entity.getX() + dx / distance * .85,
                    shot.entity.getY() + dy / distance * .85, shot.entity.getZ() + dz / distance * .85);
        }
    }

    public void clear() { shots.forEach(s -> s.entity.discard()); shots.clear(); }
    private static final class Shot {
        final ArenaActorEntity entity;
        final Supplier<Position> target;
        final BooleanSupplier alive;
        final Runnable impact;
        int age;
        Shot(ArenaActorEntity entity, Supplier<Position> target, BooleanSupplier alive, Runnable impact) {
            this.entity = entity; this.target = target; this.alive = alive; this.impact = impact;
        }
    }
}
