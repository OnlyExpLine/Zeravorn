# Zeravorn Implementation Status

## Current milestone
M1 — Match Core (completed)

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

## Important architecture decisions
- Minecraft Java + Fabric.
- JDK 25 / Java release 25.
- Server-authoritative gameplay architecture.
- Gameplay and balance systems are deferred to later milestones.
- `MatchSession` is a common/server domain class and has no client-only dependencies.
- Common/server code must not depend on client-only classes.

## Assumptions
- The repository has Git metadata, but the current sandbox identity is not the repository owner; Git commands require an explicit safe-directory override.

## Known issues
- Hero, combat, economy, map, HUD, and bot systems are not implemented yet.
- No client or server runtime smoke test has been performed yet.

## Files/modules added
- `docs/IMPLEMENTATION_STATUS.md`
- `src/main/java/com/zeravorn/match/`
- `src/main/java/com/zeravorn/team/`
- `src/test/java/com/zeravorn/match/`

## Files/modules changed
- `gradle.properties`
- `README.md`
- `src/main/resources/fabric.mod.json`
- `src/main/java/com/zeravorn/Zeravorn.java`
- `build.gradle` (JUnit 5 test runtime configuration)
- Removed template mixin classes and descriptors.

## Verification
- `.\gradlew.bat --no-daemon test` — PASS
- `.\gradlew.bat --no-daemon build` — PASS

## Next milestone
M2 — Hero Core + Levels + Skill Points

## Git checkpoint
- Branch: `main`
- Commit: none — repository has no commits yet
