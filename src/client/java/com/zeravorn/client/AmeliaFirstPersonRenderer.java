package com.zeravorn.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zeravorn.Zeravorn;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Vector3f;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Camera-space Amelia hands, staff, and authored first-person animation clips. */
public final class AmeliaFirstPersonRenderer {
    private static final String MESH = "/assets/zeravorn/amelia/amelia_first_person.mesh.json";
    private static final BlockbenchAnimation ANIMATION = BlockbenchAnimation.load(MESH,
            "/assets/zeravorn/amelia/amelia_first_person.animations.json", true);
    private static final List<Triangle> TRIANGLES = load();

    private AmeliaFirstPersonRenderer() { }

    public static boolean submit(LocalPlayer player, float partialTick, PoseStack pose,
                                 SubmitNodeCollector collector, int light) {
        if (TRIANGLES.isEmpty()) return false;
        float age = player.tickCount + partialTick;
        String action = AmeliaMeshRenderer.actionClip();
        float since = (age - AmeliaMeshRenderer.actionStart()) / 20f;
        String clip = "idle";
        float elapsed = age / 20f;
        if (action != null && since >= 0) {
            if ("q_start".equals(action)) {
                float start = ANIMATION.length("q_start");
                float loop = ANIMATION.length("q_loop");
                float end = ANIMATION.length("q_end");
                if (since < start) { clip = "q_start"; elapsed = since; }
                else if (since < start + loop) { clip = "q_loop"; elapsed = since - start; }
                else if (since < start + loop + end) { clip = "q_end"; elapsed = since - start - loop; }
            } else if (since < ANIMATION.length(action)) {
                clip = action;
                elapsed = since;
            }
        }
        if ("idle".equals(clip) && player.getDeltaMovement().horizontalDistanceSqr() > 0.0001) {
            clip = player.isSprinting() ? "run_sway" : "walk_sway";
        }
        var frame = ANIMATION.frame(clip, elapsed);
        pose.pushPose();
        // The authored mesh uses player-height coordinates; place its shoulders below the camera.
        pose.translate(0, -1.55, -0.75);
        collector.submitCustomGeometry(pose,
                RenderTypes.entityCutout(Zeravorn.id("amelia/amelia_first_person.png")),
                (transform, output) -> {
                    for (Triangle triangle : TRIANGLES) {
                        Vector3f a = frame.transform(triangle.a.bone, triangle.a.x, triangle.a.y, triangle.a.z);
                        Vector3f b = frame.transform(triangle.b.bone, triangle.b.x, triangle.b.y, triangle.b.z);
                        Vector3f c = frame.transform(triangle.c.bone, triangle.c.x, triangle.c.y, triangle.c.z);
                        Vector3f normal = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
                        if (normal.lengthSquared() < 0.00000001f) continue;
                        normal.normalize();
                        for (Point point : new Point[] {triangle.a, triangle.b, triangle.c, triangle.c}) {
                            Vector3f moved = frame.transform(point.bone, point.x, point.y, point.z);
                            output.addVertex(transform, moved.x, moved.y, moved.z)
                                    .setColor(255, 255, 255, 255).setUv(point.u, point.v)
                                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                                    .setNormal(transform, normal.x, normal.y, normal.z);
                        }
                    }
                });
        pose.popPose();
        return true;
    }

    private static List<Triangle> load() {
        try (var stream = AmeliaFirstPersonRenderer.class.getResourceAsStream(MESH)) {
            if (stream == null) return List.of();
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            List<Triangle> triangles = new ArrayList<>();
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
                        triangles.add(new Triangle(first, point(ids.get(i).getAsString(), positions, uv, bone),
                                point(ids.get(i + 1).getAsString(), positions, uv, bone)));
                }
            }
            return List.copyOf(triangles);
        } catch (Exception exception) {
            Zeravorn.LOGGER.warn("Cannot load Amelia first-person mesh", exception);
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
