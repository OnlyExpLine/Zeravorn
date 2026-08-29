package com.zeravorn.hero;

import java.util.List;

public final class HeroDefinitions {
	private static final List<HeroDefinition> ALL = HeroConfigLoader.loadDefaultHeroes();
	public static final HeroDefinition JASON = byId("jason");
	public static final HeroDefinition SHELIANER = byId("shelianer");
	public static final HeroDefinition ESAKI = byId("esaki");
	public static final HeroDefinition AMELIA = byId("amelia");
	public static final HeroDefinition LOKI = byId("loki");

	private HeroDefinitions() { }
	public static List<HeroDefinition> all() { return ALL; }
	public static HeroDefinition byId(String id) { return ALL.stream().filter(hero -> hero.id().equals(id)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown hero: " + id)); }
}
