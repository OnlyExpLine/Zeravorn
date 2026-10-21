package com.zeravorn.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zeravorn.Zeravorn;
import com.zeravorn.map.ArenaActorEntity;
import com.zeravorn.map.ArenaVisualAsset;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.math.Axis;
import org.joml.Vector3f;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Draws the authored Blockbench cuboids on the networked arena hitboxes. */
public final class ArenaActorRenderer extends ArmorStandRenderer {
    private static final Map<String, Visual> VISUALS = new HashMap<>();

    public ArenaActorRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override public ArmorStandRenderState createRenderState() { return new ActorState(); }

    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(net.minecraft.world.entity.decoration.ArmorStand entity) {
        return super.getBoundingBoxForCulling(entity).inflate(5, 8, 5);
    }

    @Override public void extractRenderState(net.minecraft.world.entity.decoration.ArmorStand entity,
                                             ArmorStandRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        ActorState actor = (ActorState) state;
        String[] parts = entity.getCustomName() == null ? new String[0]
                : entity.getCustomName().getString().split("\\|", 5);
        actor.visual = parts.length > 0 ? parts[0] : "";
        actor.clip = parts.length > 1 ? parts[1] : "idle";
        try { actor.startTick = parts.length > 2 ? Integer.parseInt(parts[2]) : 0; }
        catch (NumberFormatException ignored) { actor.startTick = 0; }
        actor.yaw = entity.getYRot();
        actor.pitch = entity.getXRot();
        actor.aimYaw = entity instanceof ArenaActorEntity e ? e.aimYaw() : actor.yaw;
        actor.protectedStructure = entity instanceof ArenaActorEntity e && e.structureProtected();
        actor.nameTag = null;
        if (parts.length >= 5) {
            try {
                int health = Integer.parseInt(parts[3]);
                int maximum = Math.max(1, Integer.parseInt(parts[4]));
                int filled = Math.max(0, Math.min(10, (int) Math.ceil(health * 10.0 / maximum)));
                actor.nameTag = net.minecraft.network.chat.Component.literal(
                        "[" + "|".repeat(filled) + ".".repeat(10 - filled) + "] "
                                + health + "/" + maximum + (actor.protectedStructure ? " PROTECTED" : ""));
                actor.nameTagAttachment = new net.minecraft.world.phys.Vec3(0,
                        actor.visual.startsWith("tower_t3") ? 7.1 : actor.visual.startsWith("tower_t2") ? 6 : actor.visual.startsWith("tower_t1") ? 5.1 : actor.visual.startsWith("throne_") ? 5.5 : 2.15, 0);
            } catch (NumberFormatException ignored) { }
        }
    }

    @Override public void submit(ArmorStandRenderState state, PoseStack pose,
                                  SubmitNodeCollector collector, CameraRenderState camera) {
        ActorState actor = (ActorState) state;
        Visual visual = VISUALS.computeIfAbsent(actor.visual, ArenaActorRenderer::load);
        if (visual == null) return;
        var frame = visual.animation.frame(actor.clip, Math.max(0, state.ageInTicks - actor.startTick) / 20f);
        final var posed = actor.visual.startsWith("tower_t") || actor.visual.startsWith("throne_") && !visual.projectile
                ? visual.animation.aim(frame, actor.visual.startsWith("tower_t") ? "crystal" : "core_crystal", actor.yaw-actor.aimYaw) : frame;
        pose.pushPose();
        // Blockbench actors face -Z; Minecraft's zero yaw points toward +Z.
        pose.mulPose(Axis.YP.rotationDegrees(180f - actor.yaw));
        if (visual.projectile) pose.mulPose(Axis.XP.rotationDegrees(-actor.pitch));
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(visual.texture),
                (transform, output) -> visual.render(transform, output, state.lightCoords, posed));
        if (visual.emissive != null && !(actor.clip.equals("destroy") && state.ageInTicks - actor.startTick > 50))
            collector.submitCustomGeometry(pose, RenderTypes.entityTranslucentEmissive(visual.emissive),
                    (transform, output) -> visual.render(transform, output, 0xF000F0, posed));
        ArenaEffectsRenderer.submit(pose, collector, actor.visual, actor.clip, Math.max(0, state.ageInTicks - actor.startTick), visual.projectile, actor.protectedStructure);
        pose.popPose();
        if (actor.nameTag != null) submitNameDisplay(state, pose, collector, camera);
    }

    private static Visual load(String id) {
        ArenaVisualAsset asset = ArenaVisualAsset.resolve(id);
        if (asset == null) return null;
        String path = asset.geometryPath();
        try (var stream = ArenaActorRenderer.class.getResourceAsStream(path)) {
            if (stream == null) return null;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            List<Vertex> vertices = new ArrayList<>();
            for (var element : root.getAsJsonArray("cubes")) {
                var cube = element.getAsJsonObject();
                var from = cube.getAsJsonArray("from");
                var to = cube.getAsJsonArray("to");
                var pivot = cube.getAsJsonArray("pivot");
                var rotation = cube.getAsJsonArray("rotation");
                float x0 = from.get(0).getAsFloat(), y0 = from.get(1).getAsFloat(), z0 = from.get(2).getAsFloat();
                float x1 = to.get(0).getAsFloat(), y1 = to.get(1).getAsFloat(), z1 = to.get(2).getAsFloat();
                float px = pivot.get(0).getAsFloat(), py = pivot.get(1).getAsFloat(), pz = pivot.get(2).getAsFloat();
                float rx = (float) Math.toRadians(rotation.get(0).getAsFloat());
                float ry = (float) Math.toRadians(rotation.get(1).getAsFloat());
                float rz = (float) Math.toRadians(rotation.get(2).getAsFloat());
                for (String side : List.of("north", "east", "south", "west", "up", "down")) {
                    if (!cube.getAsJsonObject("faces").has(side)) continue;
                    float[][] points = switch (side) {
                        case "north" -> new float[][] {{x1,y1,z0},{x0,y1,z0},{x0,y0,z0},{x1,y0,z0}};
                        case "south" -> new float[][] {{x0,y1,z1},{x1,y1,z1},{x1,y0,z1},{x0,y0,z1}};
                        case "east" -> new float[][] {{x1,y1,z1},{x1,y1,z0},{x1,y0,z0},{x1,y0,z1}};
                        case "west" -> new float[][] {{x0,y1,z0},{x0,y1,z1},{x0,y0,z1},{x0,y0,z0}};
                        case "up" -> new float[][] {{x0,y1,z0},{x1,y1,z0},{x1,y1,z1},{x0,y1,z1}};
                        default -> new float[][] {{x0,y0,z1},{x1,y0,z1},{x1,y0,z0},{x0,y0,z0}};
                    };
                    var uv = cube.getAsJsonObject("faces").getAsJsonObject(side).getAsJsonArray("uv");
                    float u0 = uv.get(0).getAsFloat() / 128f, v0 = uv.get(1).getAsFloat() / 128f;
                    float u1 = uv.get(2).getAsFloat() / 128f, v1 = uv.get(3).getAsFloat() / 128f;
                    float[][] coords = {{u0,v0},{u1,v0},{u1,v1},{u0,v1}};
                    for (int i = 0; i < 4; i++) {
                        var p = new Vector3f(points[i][0] - px, points[i][1] - py, points[i][2] - pz);
                        p.rotateX(rx).rotateY(ry).rotateZ(rz).add(px, py, pz).div(16f);
                        vertices.add(new Vertex(p.x, p.y, p.z, coords[i][0], coords[i][1],
                                cube.get("bone").getAsString()));
                    }
                }
            }
            var animation = BlockbenchAnimation.load(path, asset.animationPath(), false);
            Identifier emissive = ArenaActorRenderer.class.getResource("/assets/zeravorn/" + asset.emissivePath()) == null ? null : Zeravorn.id(asset.emissivePath());
            return new Visual(Zeravorn.id(asset.texturePath()), emissive, asset.projectile(), List.copyOf(vertices), animation);
        } catch (Exception exception) {
            Zeravorn.LOGGER.warn("Cannot load arena visual {}", id, exception);
            return null;
        }
    }

    private static final class ActorState extends ArmorStandRenderState {
        private String visual = "";
        private String clip = "idle";
        private int startTick;
        private float yaw;
        private float pitch;
        private float aimYaw;
        private boolean protectedStructure;
    }

    private record Vertex(float x, float y, float z, float u, float v, String bone) { }

    private record Visual(Identifier texture, Identifier emissive, boolean projectile, List<Vertex> vertices, BlockbenchAnimation animation) {
        private void render(PoseStack.Pose transform, VertexConsumer output, int light,
                            BlockbenchAnimation.Frame frame) {
            for (int i = 0; i + 3 < vertices.size(); i += 4) {
                Vertex a = vertices.get(i), b = vertices.get(i + 1), c = vertices.get(i + 2);
                Vector3f pa = frame.transform(a.bone, a.x, a.y, a.z);
                Vector3f pb = frame.transform(b.bone, b.x, b.y, b.z);
                Vector3f pc = frame.transform(c.bone, c.x, c.y, c.z);
                var ab = new Vector3f(pb).sub(pa);
                var ac = new Vector3f(pc).sub(pa);
                var normal = ab.cross(ac).normalize();
                for (int j = 0; j < 4; j++) {
                    Vertex point = vertices.get(i + j);
                    Vector3f moved = frame.transform(point.bone, point.x, point.y, point.z);
                    output.addVertex(transform, moved.x, moved.y, moved.z)
                            .setColor(255, 255, 255, 255).setUv(point.u, point.v)
                            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                            .setNormal(transform, normal.x, normal.y, normal.z);
                }
            }
        }
    }
}
