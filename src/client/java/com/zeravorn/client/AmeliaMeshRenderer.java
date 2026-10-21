package com.zeravorn.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.zeravorn.Zeravorn;
import com.zeravorn.hero.AmeliaAbilityDefinitions;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.Minecraft;
import org.joml.Vector3f;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Renders Amelia's authored rigid mesh in place of the local vanilla avatar. */
public final class AmeliaMeshRenderer {
    private static final BlockbenchAnimation ANIMATION = BlockbenchAnimation.load(
            "/assets/zeravorn/amelia/amelia.mesh.json",
            "/assets/zeravorn/amelia/amelia.animations.json", true);
    private static final List<Triangle> TRIANGLES = load();
    private static String actionClip;
    private static int actionStart;
    private static int lastHealth = -1;

    private AmeliaMeshRenderer() { }

    public static void play(String clip) {
        var player = Minecraft.getInstance().player;
        if (player == null || ANIMATION.length(clip) <= 0) return;
        actionClip = clip;
        actionStart = player.tickCount;
    }

    static String actionClip() { return actionClip; }
    static int actionStart() { return actionStart; }

    public static void submit(AvatarRenderState state, PoseStack pose, SubmitNodeCollector collector) {
        if (TRIANGLES.isEmpty()) return;
        var hero = ClientMobaNetworking.state().ownHero();
        String clip;
        float elapsed;
        if (hero != null && lastHealth >= 0 && hero.health() < lastHealth && hero.alive()) {
            actionClip = "hit";
            actionStart = (int) state.ageInTicks;
        }
        if (hero != null) lastHealth = hero.health();
        if (hero != null && !hero.alive()) {
            clip = "death";
            elapsed = state.ageInTicks / 20f;
        } else if (hero != null && hero.crowdControlTicks().containsKey("KNOCKUP")) {
            clip = "knockup";
            elapsed = state.ageInTicks / 20f;
        } else if (hero != null && hero.crowdControlTicks().containsKey("KNOCKBACK")) {
            clip = "knockback";
            elapsed = state.ageInTicks / 20f;
        } else if (hero != null && hero.crowdControlTicks().containsKey("STUN")) {
            clip = "stun";
            elapsed = state.ageInTicks / 20f;
        } else if (hero != null && hero.crowdControlTicks().containsKey("ROOT")) {
            clip = "root";
            elapsed = state.ageInTicks / 20f;
        } else if ("q_start".equals(actionClip) && hero != null && state.ageInTicks >= actionStart
                && hero.abilityRanks().getOrDefault("zeravorn:amelia_q", 0) > 0) {
            float since = Math.max(0, state.ageInTicks - actionStart) / 20f;
            float channel = AmeliaAbilityDefinitions.q(
                    hero.abilityRanks().get("zeravorn:amelia_q")).ticks() * 0.5f;
            if (since < ANIMATION.length("q_start")) {
                clip = "q_start";
                elapsed = since;
            } else if (since < ANIMATION.length("q_start") + channel) {
                clip = "q_loop";
                elapsed = since - ANIMATION.length("q_start");
            } else if (since < ANIMATION.length("q_start") + channel + ANIMATION.length("q_end")) {
                clip = "q_end";
                elapsed = since - ANIMATION.length("q_start") - channel;
            } else {
                actionClip = null;
                clip = state.walkAnimationSpeed > 0.04f ? "walk" : "idle";
                elapsed = state.ageInTicks / 20f;
            }
        } else if (actionClip != null && state.ageInTicks >= actionStart
                && state.ageInTicks - actionStart < ANIMATION.length(actionClip) * 20f) {
            clip = actionClip;
            elapsed = Math.max(0, state.ageInTicks - actionStart) / 20f;
        } else {
            clip = state.walkAnimationSpeed > 0.6f ? "run"
                    : state.walkAnimationSpeed > 0.04f ? "walk" : "idle";
            elapsed = state.ageInTicks / 20f;
        }
        var frame = ANIMATION.frame(clip, elapsed);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180f - state.bodyRot));
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(Zeravorn.id("amelia/amelia.png")),
                (transform, output) -> {
                    for (Triangle triangle : TRIANGLES) {
                        var a = triangle.a; var b = triangle.b; var c = triangle.c;
                        Vector3f pa = frame.transform(a.bone, a.x, a.y, a.z);
                        Vector3f pb = frame.transform(b.bone, b.x, b.y, b.z);
                        Vector3f pc = frame.transform(c.bone, c.x, c.y, c.z);
                        float ux = pb.x - pa.x, uy = pb.y - pa.y, uz = pb.z - pa.z;
                        float vx = pc.x - pa.x, vy = pc.y - pa.y, vz = pc.z - pa.z;
                        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
                        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                        if (length < 0.00001f) continue;
                        // Entity render types consume quads; repeat the final triangle vertex.
                        for (Point point : new Point[] {a, b, c, c}) {
                            Vector3f moved = frame.transform(point.bone, point.x, point.y, point.z);
                            output.addVertex(transform, moved.x, moved.y, moved.z)
                                    .setColor(255, 255, 255, 255).setUv(point.u, point.v)
                                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords)
                                    .setNormal(transform, nx / length, ny / length, nz / length);
                        }
                    }
                });
        pose.popPose();
    }

    private static List<Triangle> load() {
        try (var stream = AmeliaMeshRenderer.class.getResourceAsStream("/assets/zeravorn/amelia/amelia.mesh.json")) {
            if (stream == null) return List.of();
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            List<Triangle> result = new ArrayList<>();
            for (var element : root.getAsJsonArray("geometry")) {
                JsonObject geometry = element.getAsJsonObject();
                if (!geometry.has("vertices") || !geometry.has("faces")) continue;
                String bone = ANIMATION.geometryBone(geometry.get("uuid").getAsString());
                JsonObject positions = geometry.getAsJsonObject("vertices");
                for (var entry : geometry.getAsJsonObject("faces").entrySet()) {
                    JsonObject face = entry.getValue().getAsJsonObject();
                    var ids = face.getAsJsonArray("vertices");
                    if (ids.size() < 3) continue;
                    JsonObject uv = face.getAsJsonObject("uv");
                    Point first = point(ids.get(0).getAsString(), positions, uv, bone);
                    for (int i = 1; i + 1 < ids.size(); i++)
                        result.add(new Triangle(first, point(ids.get(i).getAsString(), positions, uv, bone),
                                point(ids.get(i + 1).getAsString(), positions, uv, bone)));
                }
            }
            return List.copyOf(result);
        } catch (Exception exception) {
            Zeravorn.LOGGER.warn("Cannot load Amelia mesh", exception);
            return List.of();
        }
    }

    private static Point point(String id, JsonObject positions, JsonObject uv, String bone) {
        var p = positions.getAsJsonArray(id);
        var t = uv.getAsJsonArray(id);
        return new Point(p.get(0).getAsFloat() / 16f, p.get(1).getAsFloat() / 16f,
                p.get(2).getAsFloat() / 16f, t.get(0).getAsFloat() / 256f, t.get(1).getAsFloat() / 256f, bone);
    }

    private record Point(float x, float y, float z, float u, float v, String bone) { }
    private record Triangle(Point a, Point b, Point c) { }
}
