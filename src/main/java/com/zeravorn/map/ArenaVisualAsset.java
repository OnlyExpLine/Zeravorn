package com.zeravorn.map;

/** Shared naming contract for exported assets; resource identifiers never come from client input. */
public record ArenaVisualAsset(String folder, String stem, String texture, boolean projectile) {
    public static ArenaVisualAsset resolve(String id) {
        if (id == null || !id.matches("[a-z0-9_]+")) return null;
        if (id.startsWith("jungle_")) return new ArenaVisualAsset("jungle", id, id, false);
        int split = id.lastIndexOf('_');
        if (split < 0) return null;
        String stem = id.substring(0, split), variant = id.substring(split + 1);
        if (!java.util.Set.of("blue", "red", "forest", "basic", "fire", "poison").contains(variant)) return null;
        String folder;
        boolean shot = stem.startsWith("projectile_") || stem.equals("tower_shot") || stem.equals("throne_shot");
        if (shot) folder = "projectiles";
        else if (stem.startsWith("lane_")) folder = "lane";
        else if (stem.matches("tower_t[123]")) folder = "towers";
        else if (stem.equals("throne") || stem.equals("fountain")) folder = stem;
        else if (stem.startsWith("tree_") || stem.startsWith("bush_") || stem.startsWith("rock_") || stem.startsWith("ruin_") || stem.startsWith("lantern_")) folder = "environment";
        else return null;
        return new ArenaVisualAsset(folder, stem, id, shot);
    }
    public String geometryPath() { return "/assets/zeravorn/" + folder + "/" + stem + ".cubes.json"; }
    public String animationPath() { return "/assets/zeravorn/" + folder + "/" + stem + ".animations.json"; }
    public String texturePath() { return folder + "/" + texture + ".png"; }
    public String emissivePath() { return folder + "/" + texture + "_emissive.png"; }
}
