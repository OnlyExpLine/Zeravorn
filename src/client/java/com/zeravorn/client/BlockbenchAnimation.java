package com.zeravorn.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Samples authored Blockbench bone tracks and applies their pivot hierarchy. */
final class BlockbenchAnimation {
    private final Map<String, Bone> bones = new HashMap<>();
    private final Map<String, String> geometryBones = new HashMap<>();
    private final Map<String, Clip> clips = new HashMap<>();

    static BlockbenchAnimation load(String geometryPath, String animationPath, boolean mesh) {
        BlockbenchAnimation result = new BlockbenchAnimation();
        try {
            JsonObject geometry = json(geometryPath);
            if (mesh) {
                for (JsonElement root : geometry.getAsJsonArray("bones")) result.meshBone(root.getAsJsonObject(), null);
            } else for (JsonElement element : geometry.getAsJsonArray("bones")) {
                JsonObject bone = element.getAsJsonObject();
                result.bones.put(bone.get("name").getAsString(), new Bone(
                        bone.has("parent") && !bone.get("parent").isJsonNull() ? bone.get("parent").getAsString() : null,
                        vector(bone.getAsJsonArray("pivot")), new Vector3f()));
            }
            JsonObject animation = json(animationPath);
            for (JsonElement element : animation.getAsJsonArray("clips")) {
                JsonObject source = element.getAsJsonObject();
                String name = source.get("name").getAsString();
                name = name.substring(name.lastIndexOf('.') + 1);
                Map<String, Map<String, List<Key>>> tracks = new HashMap<>();
                if (mesh) {
                    for (var animator : source.getAsJsonObject("animators").entrySet()) {
                        JsonObject owner = animator.getValue().getAsJsonObject();
                        String bone = owner.get("name").getAsString();
                        for (JsonElement keyElement : owner.getAsJsonArray("keyframes")) {
                            JsonObject key = keyElement.getAsJsonObject();
                            JsonArray values = key.getAsJsonArray("data_points");
                            if (values == null || values.isEmpty()) continue;
                            JsonObject value = values.get(0).getAsJsonObject();
                            add(tracks, bone, key.get("channel").getAsString(),
                                    key.get("time").getAsFloat(), new Vector3f(value.get("x").getAsFloat(),
                                            value.get("y").getAsFloat(), value.get("z").getAsFloat()));
                        }
                    }
                } else for (var owner : source.getAsJsonObject("tracks").entrySet())
                    for (JsonElement keyElement : owner.getValue().getAsJsonArray()) {
                        JsonObject key = keyElement.getAsJsonObject();
                        add(tracks, owner.getKey(), key.get("channel").getAsString(),
                                key.get("time").getAsFloat(), vector(key.getAsJsonArray("value")));
                    }
                String loop = source.has("loop") ? source.get("loop").getAsString() : "once";
                result.clips.put(name, new Clip(source.get("length").getAsFloat(),
                        "loop".equals(loop), "hold".equals(loop), tracks));
            }
        } catch (Exception exception) {
            com.zeravorn.Zeravorn.LOGGER.warn("Cannot load Blockbench animations {}", animationPath, exception);
        }
        return result;
    }

    private void meshBone(JsonObject node, String parent) {
        if (!node.has("name") || !node.has("children")) return;
        String name = node.get("name").getAsString();
        bones.put(name, new Bone(parent, vector(node.getAsJsonArray("origin")),
                node.has("rotation") ? vector(node.getAsJsonArray("rotation")) : new Vector3f()));
        for (JsonElement child : node.getAsJsonArray("children")) {
            if (child.isJsonPrimitive()) geometryBones.put(child.getAsString(), name);
            else if (child.isJsonObject()) meshBone(child.getAsJsonObject(), name);
        }
    }

    String geometryBone(String uuid) { return geometryBones.get(uuid); }
    float length(String clip) { Clip data = clips.get(clip); return data == null ? 0 : data.length; }

    Frame frame(String requested, float elapsedSeconds) {
        Clip clip = clips.get(requested);
        if (clip == null || !clip.loop && !clip.hold && elapsedSeconds > clip.length && clips.containsKey("idle")) {
            clip = clips.get("idle");
            if (clip == null) return new Frame(Map.of());
            elapsedSeconds = elapsedSeconds % Math.max(0.001f, clip.length);
        }
        if (clip == null) return new Frame(Map.of());
        float time = clip.loop ? elapsedSeconds % Math.max(0.001f, clip.length)
                : Math.min(elapsedSeconds, clip.length);
        Map<String, Matrix4f> matrices = new HashMap<>();
        for (String bone : bones.keySet()) matrix(bone, clip, time, matrices);
        return new Frame(matrices);
    }

    private Matrix4f matrix(String name, Clip clip, float time, Map<String, Matrix4f> cache) {
        Matrix4f existing = cache.get(name);
        if (existing != null) return existing;
        Bone bone = bones.get(name);
        if (bone == null) return new Matrix4f();
        Matrix4f parent = bone.parent == null ? new Matrix4f() : new Matrix4f(matrix(bone.parent, clip, time, cache));
        Map<String, List<Key>> channels = clip.tracks.getOrDefault(name, Map.of());
        Vector3f translation = sample(channels.get("position"), time);
        Vector3f rotation = sample(channels.get("rotation"), time).add(bone.rotation);
        Vector3f scale = channels.containsKey("scale") ? sample(channels.get("scale"), time) : new Vector3f(1, 1, 1);
        float x = bone.pivot.x / 16f, y = bone.pivot.y / 16f, z = bone.pivot.z / 16f;
        parent.translate(x + translation.x / 16f, y + translation.y / 16f, z + translation.z / 16f)
                .rotateXYZ((float) Math.toRadians(-rotation.x), (float) Math.toRadians(-rotation.y),
                        (float) Math.toRadians(rotation.z))
                .scale(scale.x, scale.y, scale.z).translate(-x, -y, -z);
        cache.put(name, parent);
        return parent;
    }

    Frame aim(Frame frame, String root, float yawDegrees) {
        Bone origin = bones.get(root);
        if (origin == null || Math.abs(yawDegrees) < .01f) return frame;
        Vector3f p = new Vector3f(origin.pivot).div(16);
        Matrix4f turn = new Matrix4f().translate(p).rotateY((float)Math.toRadians(yawDegrees)).translate(-p.x,-p.y,-p.z);
        Map<String, Matrix4f> result = new HashMap<>(frame.matrices);
        for (String name : bones.keySet()) {
            String ancestor = name;
            while (ancestor != null && !ancestor.equals(root)) ancestor = bones.get(ancestor).parent;
            if (ancestor != null) result.put(name, new Matrix4f(turn).mul(frame.matrices.getOrDefault(name,new Matrix4f())));
        }
        return new Frame(result);
    }

    private static Vector3f sample(List<Key> keys, float time) {
        if (keys == null || keys.isEmpty()) return new Vector3f();
        if (time <= keys.getFirst().time) return new Vector3f(keys.getFirst().value);
        for (int i = 1; i < keys.size(); i++) {
            Key next = keys.get(i), previous = keys.get(i - 1);
            if (time <= next.time) {
                float fraction = (time - previous.time) / Math.max(0.0001f, next.time - previous.time);
                return new Vector3f(previous.value).lerp(next.value, fraction);
            }
        }
        return new Vector3f(keys.getLast().value);
    }

    private static void add(Map<String, Map<String, List<Key>>> tracks, String bone, String channel,
                            float time, Vector3f value) {
        tracks.computeIfAbsent(bone, ignored -> new HashMap<>())
                .computeIfAbsent(channel, ignored -> new ArrayList<>()).add(new Key(time, value));
    }

    private static Vector3f vector(JsonArray array) {
        return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    private static JsonObject json(String path) throws Exception {
        try (var stream = BlockbenchAnimation.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalArgumentException("Missing " + path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    record Frame(Map<String, Matrix4f> matrices) {
        Vector3f transform(String bone, float x, float y, float z) {
            Vector3f point = new Vector3f(x, y, z);
            Matrix4f matrix = matrices.get(bone);
            return matrix == null ? point : matrix.transformPosition(point);
        }
    }
    private record Bone(String parent, Vector3f pivot, Vector3f rotation) { }
    private record Key(float time, Vector3f value) { }
    private record Clip(float length, boolean loop, boolean hold,
                        Map<String, Map<String, List<Key>>> tracks) { }
}
