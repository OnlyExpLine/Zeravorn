# Zeravorn Implementation Status

## Current milestone
M5 — Lane Minions + Structures + Throne (completed)

## Last completed milestone
M5 — Lane Minions + Structures + Throne

## Build status
- build: PASS (`.\gradlew.bat build`)
- test: PASS (`.\gradlew.bat test`)
- client run: NOT CHECKED
- server run: NOT CHECKED

## Implemented
- Verified the Fabric project structure and Gradle configuration.
- Confirmed Mod ID `zeravorn`, display name `Zeravorn`, and Java namespace `com.zeravorn`.
- Updated the Fabric Loader target to `0.19.4` as required by the technical specification.
- Removed template mixin classes and mixin descriptors.
- Replaced template metadata and initialization logging.
- Added a project README baseline.
- Added server-side match lifecycle and team roster domain models.
- Added strict match state transition validation and idempotent match finishing.
- Added Match Core unit tests for lifecycle, invalid transitions, finish idempotency, and roster limits.
- Added immutable definitions for all five heroes loaded from validated default config resources, with level 1–10 balance tables, hitboxes, movement, attack interval, damage type, and ability caps.
- Added authoritative `HeroRuntime` with owner/team, resources, level, XP, skill points, ability ranks, and alive state.
- Added level progression using the documented cumulative XP thresholds and one skill point per gained level.
- Added server-side ability upgrade validation, including R unlock at level 4, hero-specific F availability, rank caps, and upgrades while dead.

## Important architecture decisions
- Minecraft Java + Fabric.
- JDK 25 / Java release 25.
- Server-authoritative gameplay architecture.
- Balance values live in `config_defaults` hero/progression definitions, not gameplay Java logic.
- M1 finish is the only transition into FINISHED, so winner and finish reason are always recorded; match reset clears both rosters.
- HeroRuntime exposes effective base stats, stores ability ranks by `AbilityId`, and keeps progression/rank mutations inside authoritative services.
- M2 is complete and verified by the project build.
- M3 adds server-side combat primitives; concrete hero ability effects remain deferred to M4/M8.
- M3 includes damage, cooldown, mana, CC, targeting, basic attacks, projectiles, combat events, and cast validation.
- M4 Jason slice includes server-simulated Q, two-phase E, radial R, rank/cooldown/range validation, Q healing, and Jason-specific tests.
- M4 also includes server-confirmed ability events and Jason dash/flight validation hooks.
- M4 implementation is verified by the full project test/build.
- M5 adds config-backed default lane minion definitions, deterministic 30-second wave scheduling, 3-melee/2-ranged composition with every third siege minion, waypoint state, and documented time scaling.
- M5 adds server-side Tower/Throne controllers with T1→T2→T3 vulnerability, throne unlock after an enemy T3 falls, no-wave hero damage protection, basic-attack-only structure damage, and tank/non-tank damage rules.
- `MatchSession` is a common/server domain class and has no client-only dependencies.
- Common/server code must not depend on client-only classes.

## Assumptions
- The repository has Git metadata, but the current sandbox identity is not the repository owner; Git commands require an explicit safe-directory override.

## Known issues
- Concrete hero ability effects, combat, economy, map, HUD, and bot systems are not implemented yet.
- No known M5 test/build failures remain.
- No client or server runtime smoke test has been performed yet.
- M5 uses engine-independent domain models; actual world geometry and Fabric entity/render integration remain deferred to M13.

## Files/modules added
- `docs/IMPLEMENTATION_STATUS.md`
- `src/main/java/com/zeravorn/match/`
- `src/main/java/com/zeravorn/team/`
- `src/test/java/com/zeravorn/match/`
- `src/main/java/com/zeravorn/hero/`
- `src/main/java/com/zeravorn/ability/`
- `src/test/java/com/zeravorn/hero/HeroCoreTest.java`
- `src/main/java/com/zeravorn/combat/`
- `src/main/java/com/zeravorn/combat/CombatEvent.java`
- `src/main/java/com/zeravorn/projectile/`
- `src/main/java/com/zeravorn/ability/AbilityExecution.java`
- `src/main/java/com/zeravorn/ability/AbilityContext.java`
- `src/main/java/com/zeravorn/ability/AbilityExecutionResult.java`
- `src/main/java/com/zeravorn/ability/AbilityRuntime.java`
- `src/main/java/com/zeravorn/ability/AbilityCastValidator.java`
- `src/main/java/com/zeravorn/hero/JasonAbilityDefinitions.java`
- `src/main/java/com/zeravorn/hero/JasonAbilityResult.java`
- `src/main/java/com/zeravorn/hero/JasonAbilityService.java`
- `src/main/java/com/zeravorn/hero/JasonMovementResult.java`
- `src/main/java/com/zeravorn/hero/JasonMovementService.java`
- `src/main/java/com/zeravorn/ability/AbilityEvent.java`
- `src/test/java/com/zeravorn/hero/JasonAbilityTest.java`
- `src/test/java/com/zeravorn/combat/CombatCoreTest.java`
- `src/main/java/com/zeravorn/map/Position.java`
- `src/main/java/com/zeravorn/minion/`
- `src/main/java/com/zeravorn/structure/`
- `src/test/java/com/zeravorn/minion/LaneMinionTest.java`
- `src/test/java/com/zeravorn/structure/StructureServiceTest.java`

## Files/modules changed
- `gradle.properties`
- `README.md`
- `src/main/resources/fabric.mod.json`
- `src/main/java/com/zeravorn/Zeravorn.java`
- `build.gradle` (JUnit 5 test runtime configuration)
- Removed template mixin classes and descriptors.

## Verification
- M1: ` .\gradlew.bat --no-daemon test` — PASS.
- M1: ` .\gradlew.bat --no-daemon build` — PASS.
- M2: `.\gradlew.bat build` — PASS (confirmed by user).
- M3: `.\gradlew.bat build` — PASS (confirmed by user).
- M4: `.\gradlew.bat --no-daemon test` — PASS.
- M4: `.\gradlew.bat --no-daemon build` — PASS.
- M5: `.\gradlew.bat --no-daemon test` — PASS.
- M5: `.\gradlew.bat --no-daemon build` — PASS.

## Next milestone
M6 — Economy + Items + Shop

## Git checkpoint
- Branch: `main`
- Existing repository history contains M0/M1 commits; M2 changes are currently uncommitted.
