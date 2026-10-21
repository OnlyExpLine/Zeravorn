package com.zeravorn.map;

import com.zeravorn.team.TeamId;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** One-time, versioned migration of known generated blocks, followed by match-scoped models. */
public final class ArenaScenery {
    private final MapDefinition map;
    private final ArenaSceneryPlan plan;
    private final List<ArenaSceneryPlan.Placement> placements;
    private final List<ArenaActorEntity> entities=new ArrayList<>();
    private final int floor;
    private int migrated;
    private boolean prepared;
    public ArenaScenery(MapDefinition map){this.map=map;plan=new ArenaSceneryPlan(map);placements=plan.placements();floor=(int)map.spawns().get(TeamId.BLUE).position().y()-1;}
    public boolean prepare(ServerLevel level){
        if(prepared)return true;
        BlockPos marker=new BlockPos(0,floor-2,0);
        if(!level.getBlockState(marker).is(Blocks.AMETHYST_BLOCK)){
            int end=Math.min(placements.size(),migrated+12);
            for(;migrated<end;migrated++){
                var p=placements.get(migrated);
                int x=(int)p.position().x(),z=(int)p.position().z();
                if(p.visual().startsWith("tree_"))for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)for(int y=1;y<=5;y++){
                    BlockPos pos=new BlockPos(x+dx,floor+y,z+dz);var state=level.getBlockState(pos);
                    if(state.is(Blocks.SPRUCE_LOG))level.setBlock(pos,Blocks.BARRIER.defaultBlockState(),2);
                    else if(state.is(Blocks.SPRUCE_LEAVES))level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
                }
                if(p.visual().startsWith("rock_"))for(int dx=-1;dx<=1;dx++)for(int y=1;y<=2;y++){
                    BlockPos pos=new BlockPos(x+dx,floor+y,z);
                    if(level.getBlockState(pos).is(Blocks.MOSSY_COBBLESTONE))level.setBlock(pos,Blocks.BARRIER.defaultBlockState(),2);
                }
            }
            if(migrated<placements.size())return false;
            for(var p:map.towers())removePlaceholder(level,p.position(),3);
            for(var p:map.thrones().values())removePlaceholder(level,p,4);
            for(var bush:map.bushZones())for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++){
                BlockPos pos=new BlockPos((int)bush.center().x()+x,floor+1,(int)bush.center().z()+z);
                if(level.getBlockState(pos).is(Blocks.OAK_LEAVES))level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            }
            level.setBlock(marker,Blocks.AMETHYST_BLOCK.defaultBlockState(),2);
        }
        // Towers destroyed last match regain their collision for this match.
        for(var p:map.towers())restoreCore(level,p.position());
        for(var p:map.thrones().values())restoreCore(level,p);
        for(var p:placements){var e=ArenaStructureRuntime.spawn(level,p.position(),p.visual(),true);e.setYRot(p.yaw());entities.add(e);}
        prepared=true;return true;
    }
    private void removePlaceholder(ServerLevel level,Position p,int radius){
        Set<Block> known=Set.of(Blocks.CONCRETE.blue(),Blocks.CONCRETE.red(),Blocks.DIAMOND_BLOCK,Blocks.REDSTONE_BLOCK,Blocks.SEA_LANTERN,Blocks.CHISELED_POLISHED_BLACKSTONE);
        for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)for(int y=1;y<=6;y++){
            BlockPos pos=new BlockPos((int)p.x()+x,floor+y,(int)p.z()+z);
            if(known.contains(level.getBlockState(pos).getBlock()))level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        }
    }
    private void restoreCore(ServerLevel level,Position p){
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=0;y<3;y++){
            BlockPos pos=new BlockPos((int)p.x()+x,(int)p.y()+y,(int)p.z()+z);
            if(level.getBlockState(pos).isAir())level.setBlock(pos,Blocks.BARRIER.defaultBlockState(),2);
        }
    }
    public void clear(){entities.forEach(ArenaActorEntity::discard);entities.clear();}
}
