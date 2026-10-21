package com.zeravorn.client;

import com.google.gson.JsonParser;
import com.zeravorn.map.ArenaVisualAsset;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class ArenaAssetAnimationTest {
    @Test void staticSceneryWithoutIdleClipHasAValidBindPose() {
        for (String name : new String[]{"tree_a_forest","rock_large_forest","ruin_arch_forest","bush_corner_forest"}) {
            var asset=ArenaVisualAsset.resolve(name);
            var animation=BlockbenchAnimation.load(asset.geometryPath(),asset.animationPath(),false);
            var point=animation.frame("idle",500).transform("root",1,2,3);
            assertEquals(1,point.x);assertEquals(2,point.y);assertEquals(3,point.z);
        }
    }
    @Test void allStructureClipsAndAimProduceFiniteVertices() throws Exception {
        for (String name : new String[]{"tower_t1_blue","tower_t2_red","tower_t3_blue","throne_red","fountain_blue"}) {
            var asset=ArenaVisualAsset.resolve(name);
            var animation=BlockbenchAnimation.load(asset.geometryPath(),asset.animationPath(),false);
            try(var stream=getClass().getResourceAsStream(asset.animationPath())) {
                var clips=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("clips");
                for(var e:clips){var clip=e.getAsJsonObject();String shortName=clip.get("name").getAsString().replaceAll(".*\\.","");
                    for(int i=0;i<=10;i++){
                        var frame=animation.frame(shortName,clip.get("length").getAsFloat()*i/10);
                        frame=animation.aim(frame,name.startsWith("throne")?"core_crystal":"crystal",90);
                        var p=frame.transform(name.startsWith("throne")?"core_crystal":"crystal",.3f,3.5f,.4f);
                        assertTrue(Float.isFinite(p.x)&&Float.isFinite(p.y)&&Float.isFinite(p.z),name+" "+shortName);
                    }
                }
            }
        }
    }
}
