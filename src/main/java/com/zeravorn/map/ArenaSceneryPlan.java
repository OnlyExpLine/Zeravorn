package com.zeravorn.map;

import com.zeravorn.team.TeamId;
import java.util.ArrayList;
import java.util.List;

/** Deterministic replacement positions matching the original blockout decorator. */
public final class ArenaSceneryPlan {
    public record Placement(String visual, Position position, float yaw) { }
    private final MapDefinition map;
    private final double y;
    public ArenaSceneryPlan(MapDefinition map) { this.map=map; y=map.spawns().get(TeamId.BLUE).position().y(); }
    public List<Placement> placements() {
        List<Placement> result=new ArrayList<>();
        for(int x=(int)map.playableBoundary().minX()+3;x<map.playableBoundary().maxX()-2;x++)
            for(int z=(int)map.playableBoundary().minZ()+3;z<map.playableBoundary().maxZ()-2;z++) {
                if(x%11==0&&z%11==0&&treeAt(x,z)) {
                    result.add(new Placement("tree_"+(char)('a'+hash(x,z)%3)+"_forest",new Position(x,y,z),hash(x,z)%4*90));
                    if(hash(x,z)%5==0)result.add(new Placement("lantern_post_blue",new Position(x+3,y,z),0));
                    if(hash(x,z)%7==0)result.add(new Placement("ruin_column_broken_forest",new Position(x-3,y,z),0));
                }
                if(x%23==0&&z%23==0&&rockAt(x,z))for(int dx:new int[]{-2,0,2})result.add(new Placement("rock_large_forest",new Position(x+dx,y,z),0));
            }
        for(var bush:map.bushZones())for(int x=-4;x<=4;x+=2)for(int z=-4;z<=4;z+=2)
            if(Math.hypot(x,z)<=bush.radius()-.8)result.add(new Placement("bush_"+(Math.abs(x)+Math.abs(z)>4?"corner":"large")+"_forest",new Position(bush.center().x()+x,y,bush.center().z()+z),hash(x,z)%4*90));
        return List.copyOf(result);
    }
    public boolean treeAt(int x,int z){return hash(x,z)%3!=0&&laneDistance(x,z)>=10&&!nearImportant(x,z,10);}
    public boolean rockAt(int x,int z){return hash(x,z)%3==0&&laneDistance(x,z)>=10&&!nearImportant(x,z,10);}
    private boolean nearImportant(int x,int z,double radius){
        for(var s:map.spawns().values())if(distance(x,z,s.position())<radius+3)return true;
        for(var p:map.thrones().values())if(distance(x,z,p)<radius)return true;
        for(var p:map.towers())if(distance(x,z,p.position())<radius)return true;
        for(var p:map.jungleCamps())if(distance(x,z,p.position())<radius)return true;
        for(var p:map.bushZones())if(distance(x,z,p.center())<radius)return true;
        return false;
    }
    private double laneDistance(double x,double z){
        double best=Double.POSITIVE_INFINITY;
        for(var byTeam:map.lanePaths().values()){
            var path=byTeam.get(TeamId.BLUE);
            for(int i=1;i<path.size();i++){
                Position a=path.get(i-1),b=path.get(i);double vx=b.x()-a.x(),vz=b.z()-a.z();
                double t=Math.max(0,Math.min(1,((x-a.x())*vx+(z-a.z())*vz)/Math.max(.001,vx*vx+vz*vz)));
                best=Math.min(best,Math.hypot(a.x()+t*vx-x,a.z()+t*vz-z));
            }
        }return best;
    }
    private static double distance(double x,double z,Position p){return Math.hypot(x-p.x(),z-p.z());}
    public static int hash(int x,int z){int h=Math.abs(x)*73428767^Math.abs(z)*912931;return(h^h>>>16)&Integer.MAX_VALUE;}
}
