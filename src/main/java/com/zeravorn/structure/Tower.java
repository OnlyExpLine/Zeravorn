package com.zeravorn.structure;

import com.zeravorn.minion.Lane;
import com.zeravorn.team.TeamId;
import java.util.Objects;

public final class Tower {
    public static final int[] MAX_HEALTH = {3200, 4200, 5200};
    public static final int[] DAMAGE = {120, 145, 170};
    private final String id; private final TeamId team; private final Lane lane; private final TowerOrder order;
    private int health;
    public Tower(String id, TeamId team, Lane lane, TowerOrder order) {
        this.id=Objects.requireNonNull(id); this.team=Objects.requireNonNull(team); this.lane=Objects.requireNonNull(lane); this.order=Objects.requireNonNull(order); this.health=maxHealth();
    }
    public String id(){return id;} public TeamId team(){return team;} public Lane lane(){return lane;} public TowerOrder order(){return order;}
    public int maxHealth(){return MAX_HEALTH[order.ordinal()];} public int health(){return health;} public int damage(){return DAMAGE[order.ordinal()];}
    public boolean destroyed(){return health==0;}
    public void damage(int amount){if(amount<0)throw new IllegalArgumentException("negative damage"); health=Math.max(0,health-amount);}
}
