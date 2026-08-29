package com.zeravorn.economy;
import com.zeravorn.hero.HeroRuntime;
public final class ExperienceService { public int grant(HeroRuntime hero,int amount,ExperienceReason reason){if(amount<0)throw new IllegalArgumentException("negative xp");int old=hero.level();hero.addExperience(amount);return hero.level()-old;} }
