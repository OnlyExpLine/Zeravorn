package com.zeravorn.structure;

import com.zeravorn.hero.HeroDefinitions;
import com.zeravorn.hero.HeroRuntime;
import com.zeravorn.team.TeamId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FountainServiceTest {
    @Test void healsResourcesOncePerSecondAndDoesNotResurrect(){
        var hero=new HeroRuntime(UUID.randomUUID(),TeamId.BLUE,HeroDefinitions.byId("amelia"));var f=new FountainService();
        int hp=hero.health(),mana=hero.mana();hero.receiveDamage(hp/2);hero.spendMana(mana/2);int damaged=hero.health(),spent=hero.mana();
        f.heal(hero,19);assertEquals(damaged,hero.health());assertEquals(spent,hero.mana());
        f.heal(hero,20);assertEquals(damaged+(int)Math.ceil(hp*.2),hero.health());assertEquals(spent+(int)Math.ceil(mana*.2),hero.mana());
        hero.receiveDamage(hero.health());f.heal(hero,40);assertFalse(hero.alive());assertEquals(0,hero.health());
    }
    @Test void dangerKillsFromCustomHealthOnHalfSecondCadence(){
        var hero=new HeroRuntime(UUID.randomUUID(),TeamId.RED,HeroDefinitions.byId("amelia"));var f=new FountainService();int hp=hero.health();
        assertEquals(0,f.danger(hero,9));assertEquals((int)Math.ceil(hp*.35),f.danger(hero,10));
        assertEquals(0,f.danger(hero,19));f.danger(hero,20);f.danger(hero,30);assertEquals(0,hero.health());assertFalse(hero.alive());
        assertEquals(0,f.danger(hero,40));
    }
}
