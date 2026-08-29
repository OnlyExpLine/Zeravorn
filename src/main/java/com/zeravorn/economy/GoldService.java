package com.zeravorn.economy;
import com.zeravorn.hero.HeroRuntime;
public final class GoldService {
 public static final long PASSIVE_START_TICKS=600; public static final int PASSIVE_PER_SECOND=3;
 public int grant(HeroRuntime hero,int amount,GoldReason reason){if(amount<0)throw new IllegalArgumentException("negative gold");hero.addGold(amount);return amount;}
 public int grantPassive(HeroRuntime hero,long previousTicks,long currentTicks){if(currentTicks<=PASSIVE_START_TICKS)return 0;long from=Math.max(previousTicks,PASSIVE_START_TICKS);int amount=(int)Math.max(0,(currentTicks-from)/20*PASSIVE_PER_SECOND);grant(hero,amount,GoldReason.PASSIVE);return amount;}
}
