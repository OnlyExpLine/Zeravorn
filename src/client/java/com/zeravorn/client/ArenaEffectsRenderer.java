package com.zeravorn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.zeravorn.Zeravorn;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Bounded effects made from the authored sprites. Gameplay stays on the server. */
final class ArenaEffectsRenderer {
    static void submit(PoseStack pose, SubmitNodeCollector out, String visual, String clip, float age, boolean projectile, boolean protectedStructure) {
        String color=visual.endsWith("_red")?"red":visual.endsWith("_fire")?"fire":visual.endsWith("_poison")?"poison":visual.endsWith("_basic")?"basic":"blue";
        if(projectile){
            for(int i=0;i<2;i++){pose.pushPose();pose.mulPose(Axis.ZP.rotationDegrees(i*90));
                quad(pose,out,color+"_trail",new float[][]{{-.18f,0,.3f},{-.18f,0,2.4f},{.18f,0,2.4f},{.18f,0,.3f}},220);pose.popPose();}
            return;
        }
        boolean throne=visual.startsWith("throne_"), fountain=visual.startsWith("fountain_");
        if(!throne&&!fountain&&!visual.startsWith("tower_t"))return;
        if(throne&&protectedStructure){
            for(int i=0;i<3;i++){pose.pushPose();pose.translate(0,3.1,0);pose.mulPose(Axis.YP.rotationDegrees(i*60));
                float r=1.8f+(float)Math.sin(age*.08)*.05f;
                quad(pose,out,color+"_ring",new float[][]{{-r,-r,0},{r,-r,0},{r,r,0},{-r,r,0}},150);pose.popPose();}
        }
        if(fountain){
            float r=(clip.equals("danger")?3.2f:clip.equals("heal")?2.7f:2.1f)+(float)Math.sin(age*.12)*.12f;
            quad(pose,out,color+"_ring",new float[][]{{-r,.85f,-r},{-r,.85f,r},{r,.85f,r},{r,.85f,-r}},clip.equals("idle")?100:210);
            if(!clip.equals("idle"))for(int i=0;i<6;i++){
                float a=(float)(i*Math.PI/3+age*.025),x=(float)Math.sin(a)*1.8f,z=(float)Math.cos(a)*1.8f,y=1+(age*.04f+i*.3f)%2.8f;
                quad(pose,out,color+"_spark",new float[][]{{x-.12f,y-.3f,z},{x+.12f,y-.3f,z},{x+.12f,y+.3f,z},{x-.12f,y+.3f,z}},200);
            }
        }
        if(clip.equals("destroy")&&age>=14&&age<65){
            float r=.3f+(age-14)*.065f,y=throne?3.3f:3.7f;int alpha=(int)(230*(1-(age-14)/51));
            quad(pose,out,color+"_impact",new float[][]{{-r,y,-r},{-r,y,r},{r,y,r},{r,y,-r}},alpha);
        }
    }
    private static void quad(PoseStack pose,SubmitNodeCollector out,String sprite,float[][] points,int alpha){
        var type=RenderTypes.entityTranslucentEmissive(Zeravorn.id("projectiles/vfx/"+sprite+".png"));
        out.submitCustomGeometry(pose,type,(transform,consumer)->{
            float[][] uv={{0,0},{1,0},{1,1},{0,1}};
            for(int side=0;side<2;side++)for(int j=0;j<4;j++){
                int i=side==0?j:3-j;float[] p=points[i];
                consumer.addVertex(transform,p[0],p[1],p[2]).setColor(255,255,255,alpha).setUv(uv[i][0],uv[i][1])
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(transform,0,side==0?1:-1,0);
            }
        });
    }
}
