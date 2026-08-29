package com.zeravorn.structure;

import com.zeravorn.team.TeamId;
import java.util.Objects;

public final class Throne {
    private final String id; private final TeamId team; private int health=StructureBalance.throne().health();
    public Throne(String id, TeamId team){this.id=Objects.requireNonNull(id);this.team=Objects.requireNonNull(team);}
    public String id(){return id;} public TeamId team(){return team;} public int health(){return health;} public int maxHealth(){return StructureBalance.throne().health();} public int damage(){return StructureBalance.throne().damage();} public double attackRange(){return StructureBalance.throne().attackRange();} public double attackIntervalSeconds(){return StructureBalance.throne().attackIntervalSeconds();} public boolean destroyed(){return health==0;}
    public void damage(int amount){if(amount<0)throw new IllegalArgumentException("negative damage");health=Math.max(0,health-amount);}
}
