# Zeravorn Implementation Status

## Current milestone
M2 — Hero Core + Levels + Skill Points (implementation corrected; verification pending)

## Last completed milestone
M1 — Match Core

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
- Concrete ability effects and combat systems are deferred to later milestones; M2 contains definitions and progression only.
- `MatchSession` is a common/server domain class and has no client-only dependencies.
- Common/server code must not depend on client-only classes.

## Assumptions
- The repository has Git metadata, but the current sandbox identity is not the repository owner; Git commands require an explicit safe-directory override.

## Known issues
- Concrete hero ability effects, combat, economy, map, HUD, and bot systems are not implemented yet.
- No client or server runtime smoke test has been performed yet.

## Files/modules added
- `docs/IMPLEMENTATION_STATUS.md`
- `src/main/java/com/zeravorn/match/`
- `src/main/java/com/zeravorn/team/`
- `src/test/java/com/zeravorn/match/`
- `src/main/java/com/zeravorn/hero/`
- `src/main/java/com/zeravorn/ability/`
- `src/test/java/com/zeravorn/hero/HeroCoreTest.java`

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
- M2: source and tests added/expanded; Gradle verification is blocked because the Gradle 9.5.1 distribution is absent from all available local caches. An elevated download attempt did not complete and did not create a usable distribution.

## Next milestone
M3 — Combat Core (after Gradle verification of M2)

## Git checkpoint
- Branch: `main`
- Existing repository history contains M0/M1 commits; M2 changes are currently uncommitted.
