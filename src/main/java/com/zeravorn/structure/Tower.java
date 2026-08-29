package com.zeravorn.structure;

import com.zeravorn.minion.Lane;
import com.zeravorn.team.TeamId;
import java.util.Objects;

public final class Tower {
    private final String id; private final TeamId team; private final Lane lane; private final TowerOrder order;
    private int health;
    public Tower(String id, TeamId team, Lane lane, TowerOrder order) {
        this.id=Objects.requireNonNull(id); this.team=Objects.requireNonNull(team); this.lane=Objects.requireNonNull(lane); this.order=Objects.requireNonNull(order); this.health=maxHealth();
    }
    public String id(){return id;} public TeamId team(){return team;} public Lane lane(){return lane;} public TowerOrder order(){return order;}
    public int maxHealth(){return StructureBalance.tower(order).health();} public int health(){return health;} public int damage(){return StructureBalance.tower(order).damage();}
    public double attackRange(){return StructureBalance.tower(order).attackRange();} public double attackIntervalSeconds(){return StructureBalance.tower(order).attackIntervalSeconds();}
    public boolean destroyed(){return health==0;}
    public void damage(int amount){if(amount<0)throw new IllegalArgumentException("negative damage"); health=Math.max(0,health-amount);}
}
