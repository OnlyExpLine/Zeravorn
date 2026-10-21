package com.zeravorn.map;

import com.google.gson.JsonParser;
import com.zeravorn.team.TeamId;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class ArenaAssetIntegrationTest {
    private void resource(String path){try(var in=getClass().getResourceAsStream(path)){assertNotNull(in,path);}catch(Exception e){fail(e);}}
    private void asset(String name){var a=ArenaVisualAsset.resolve(name);assertNotNull(a,name);resource(a.geometryPath());resource(a.animationPath());resource("/assets/zeravorn/"+a.texturePath());resource("/assets/zeravorn/"+a.emissivePath());}
    @Test void everyPlacedModelResolvesToPackagedResources(){
        var map=MapLoader.loadDefault();var plan=new ArenaSceneryPlan(map);assertEquals(plan.placements(),new ArenaSceneryPlan(map).placements());
        assertTrue(plan.placements().size()>50);
        for(var p:plan.placements()) {asset(p.visual());assertTrue(map.playableBoundary().contains(p.position()));for(var s:map.spawns().values())assertTrue(s.position().distanceSquared(p.position())>25);}
        for(var t:map.towers())asset("tower_"+t.order().name().toLowerCase()+"_"+t.team().name().toLowerCase());
        for(var team:TeamId.values()){asset("throne_"+team.name().toLowerCase());asset("fountain_"+team.name().toLowerCase());asset("tower_shot_"+team.name().toLowerCase());asset("throne_shot_"+team.name().toLowerCase());}
        asset("projectile_basic_basic");asset("projectile_ice_blue");
    }
    @Test void rendererRecognizesEveryExportedVariant() throws Exception {
        for(String folder:new String[]{"throne","fountain","projectiles","environment"}){
            var path=java.nio.file.Path.of("assets/source/blockbench",folder,"statistics.json");
            var stats=JsonParser.parseString(java.nio.file.Files.readString(path)).getAsJsonArray();
            for(var entry:stats){var model=entry.getAsJsonObject();for(var variant:model.getAsJsonArray("variants"))asset(model.get("name").getAsString()+"_"+variant.getAsString());}
        }
        assertNull(ArenaVisualAsset.resolve("../../other"));
    }
}
