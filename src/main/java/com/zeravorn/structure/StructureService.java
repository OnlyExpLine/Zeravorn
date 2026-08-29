package com.zeravorn.structure;

import com.zeravorn.hero.HeroClass;
import com.zeravorn.minion.Lane;
import com.zeravorn.team.TeamId;
import java.util.EnumMap;
import java.util.Map;

/** Server-side structure rules. Only basic attacks may call damage methods. */
public final class StructureService {
    private final Map<TeamId, Map<Lane, Tower[]>> towers = new EnumMap<>(TeamId.class);
    private final Map<TeamId, Throne> thrones = new EnumMap<>(TeamId.class);
    public StructureService() {
        for (TeamId team : TeamId.values()) { Map<Lane,Tower[]> byLane=new EnumMap<>(Lane.class); for(Lane lane:Lane.values()) byLane.put(lane,new Tower[]{new Tower(team+"_"+lane+"_T1",team,lane,TowerOrder.T1),new Tower(team+"_"+lane+"_T2",team,lane,TowerOrder.T2),new Tower(team+"_"+lane+"_T3",team,lane,TowerOrder.T3)}); towers.put(team,byLane); thrones.put(team,new Throne(team+"_THRONE",team)); }
    }
    public Tower tower(TeamId team,Lane lane,TowerOrder order){return towers.get(team).get(lane)[order.ordinal()];}
    public Throne throne(TeamId team){return thrones.get(team);}
    public boolean towerVulnerable(TeamId team,Lane lane,TowerOrder order){ if(order==TowerOrder.T1)return !tower(team,lane,order).destroyed(); return tower(team,lane,order).health()>0 && tower(team,lane,order==TowerOrder.T2?TowerOrder.T1:TowerOrder.T2).destroyed(); }
    public boolean throneVulnerable(TeamId team){ return towers.get(team.opponent()).values().stream().anyMatch(a->a[2].destroyed()); }
    public StructureDamageResult damageTower(TeamId attacker,TeamId defender,Lane lane,TowerOrder order,int amount,boolean alliedWave,boolean basicAttack){
        if(!basicAttack)return StructureDamageResult.rejected("ABILITY_CANNOT_DAMAGE_STRUCTURE",tower(defender,lane,order).health());
        Tower target=tower(defender,lane,order); if(!towerVulnerable(defender,lane,order))return StructureDamageResult.rejected("STRUCTURE_PROTECTED",target.health());
        int applied=amount; if(!alliedWave) applied=(int)Math.floor(amount*0.20); target.damage(applied); return new StructureDamageResult(true,applied,target.destroyed(),"");
    }
    public StructureDamageResult damageThrone(TeamId attacker,TeamId defender,int amount,boolean alliedWave,boolean basicAttack){
        Throne target=throne(defender); if(!basicAttack)return StructureDamageResult.rejected("ABILITY_CANNOT_DAMAGE_STRUCTURE",target.health()); if(!throneVulnerable(defender))return StructureDamageResult.rejected("STRUCTURE_PROTECTED",target.health()); int applied=alliedWave?amount:(int)Math.floor(amount*.20); target.damage(applied); return new StructureDamageResult(true,applied,target.destroyed(),"");
    }
    public int heroDamageToTower(HeroClass heroClass,int heroMaxHealth){return (int)Math.floor(heroMaxHealth*(heroClass==HeroClass.TANK?.17:.26));}
}
