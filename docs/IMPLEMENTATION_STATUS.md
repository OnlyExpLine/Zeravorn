# Zeravorn Implementation Status

## Current milestone
M13 — MapDefinition and arena blockout integration (runtime client smoke pending)

2026-10-03: M13 visual-world continuation adds an incremental, save-preserving arena scenery pass (forest canopy, sparse rock walls, walkable bush vegetation, camp rings, decorated bases/towers/thrones) and an arena-only vanilla Mob removal hook. Existing generated worlds are upgraded in place without deleting saves; unknown player blocks are not overwritten. This is map presentation and LOS geometry only. Runtime minion, jungle mob, tower and throne entities/controllers remain unbound, so full MOBA arena gameplay and in-game acceptance are still pending. Gradle/test/build and client smoke were not run at the owner's request.

2026-10-03: M13 added `arena_v1.json`, `MapDefinition`, `MapLoader`, `MapValidator`, an independent `zeravorn:arena_v1` flat dimension, conservative incremental `ArenaWorld` blockout, world LOS/endpoint checks, a configured lobby-to-arena entry adapter, and a F9 map debug overlay. Map validation covers both bases, spawn/fountains/danger, thrones, 18 ordered towers, 6 routes with <=12-block runtime steps, 10 camps with valid M7 mob references/leash, bushes, boundary, route/hitbox separation and forbidden epic boss IDs. Player-vs-player requests now use live positions and existing server combat services. The arena is a blockout, not a fully functional match: minion/bot world actors, damageable structure actors and jungle mob entities are not yet wired, and Recall/respawn belong to M14. See `docs/M13_ARENA.md`; in-game visual/runtime acceptance pending.

2026-10-03: M12 first slice adds BotController, BotPerception, BotRouteProfile, BotBuildProfile, BotBrain, BotActionExecutor boundary and deterministic BotRosterPlanner. The policy runs at 4 decisions/second, prioritizes low-health recall/retreat, considers only caller-supplied visible legal targets, and forbids structure attack without allied wave. Fixed class builds and unique-hero five-slot fill plans are tested. No bot actors are attached to the current M11 test arena: it has no M13 lane paths, waves, structures, world target queries or authoritative combat executor. M12 is NOT complete; live roster fill, actions through gameplay services, movement, purchases, ability use and respawn require follow-up integration. `gradlew.bat --no-daemon build --rerun-tasks` passed.

2026-10-03: M12 continuation after owner explicitly declined bringing M13 map work forward. Lobby lock now fills both five-slot rosters with deterministic unique bots and includes bot hero/spell names in loading snapshots; the M11 test transfer skips their non-player UUIDs and the gateway can hold their HeroRuntime for scoreboard state. ServerBotActionExecutor sends buy, skill, basic, ability and recall through ServerRequestProcessor, with a server-only WorldPort for movement and live visible-target recheck. BotFleet schedules match-scoped decisions only in PLAYING. The WorldPort and perception source cannot be bound to the M11 test arena because it has no legal map paths, waves, structures or world combat queries; bots are roster/runtime entries there, not moving entities. Full M12 gameplay acceptance remains blocked on M13 and M14 world/lifecycle bindings, per the owner's stage-order choice. No fake bot combat is enabled in the test arena.

2026-10-03 owner verification: `test` and `build` both stopped at `compileJava` with 10 Minecraft 26.2 API symbol errors. The M11 source was corrected to use `ServerPlayer.level().getServer()` and `Blocks.CONCRETE.blue()/red()`. These fixes have not yet been re-tested by the owner; Codex did not run Gradle.

The owner's next `build`/`test` attempts confirmed the 10 symbol errors were gone and exposed one further `compileJava` error: the reassigned `LobbyRoom.pedestals` record parameter was captured by a lambda. The constructor now validates an effectively-final immutable copy and assigns it to the record component after validation. Re-test pending; Codex did not run Gradle.

The next owner `test` run passed main-source compilation and stopped at five `compileClientJava` references to removed `Minecraft.screen`. M11 client screen checks now use the confirmed Minecraft 26.2 accessor `client.gui.screen()`. Owner re-test pending; Codex did not run Gradle.

2026-10-03: M11 has server-owned lobby selection and snapshots, five slots per team, ally-unique heroes, jungler-only Retribution, Ready lock, pre-lock disconnect slot release, locked-player reconnect, client cards and spell/hero controls. Ready without a hero is explicitly a temporary owner-approved testing mode; at lock the server assigns a free hero. The GUI flags this mode and explains disabled Retribution. Test sources exist but were not run.

The separate `zeravorn:lobby` flat dimension and a conservative room builder are implemented. Joining players are teleported to a stone/glass safe room if the dimension loads and the reserved construction space is empty. Five clickable pedestal blocks have named armor-stand preview placeholders; team blocks and a Ready block are also clickable. The physical callbacks check the actual dimension. The builder writes a completion marker and refuses to overwrite occupied space. The dimension load, teleport and interactions still require owner runtime verification; placeholders are not the final hero models.

Lobby blocks are protected from player break/placement/attack/use outside the intended selection interactions. Floating labels identify Team, Ready and the L-key GUI access. These protections do not replace a full world-protection policy for non-player explosions or external mods.

For M11 entry-flow testing, the owner approved a disposable `zeravorn:test_arena` flat dimension with separated blue/red spawn platforms. After LOADING, the server moves every selected player there, creates/retains their selected `HeroRuntime`, attaches one M10 match gateway, checks each player is present in that exact dimension with a live gateway, then runs a 5-second COUNTDOWN and server-confirmed START/PLAYING. Reconnect reuses the locked selection, hero runtime and gateway. The test gateway exposes only owner-safe visibility and rejects world-dependent combat/spell/recall actions with `TEST_ARENA_NO_GAMEPLAY`; the HUD labels this mode. Its floor and glass divider are protected from player interactions. M13 must replace this adapter with the full validated map and real world queries. The M11 controller owns match elapsed ticks after START; the client retains same-match loading UI across a gameplay snapshot. No Gradle/test/build commands were run and no cache was generated by Codex.

The match-state transition is sent immediately to bound players at COUNTDOWN and START, with a countdown/START HUD overlay. `GameplayRequestValidator` now handles the M11 `SET_JUNGLER` intent exhaustively; source-level tests cover its payload validation and same-match lobby/loading snapshot continuity. Owner build, tests and in-game smoke are still pending.

Loading now has a separate read-only roster screen with both teams, hero/spell choices, five slots each and server-status text. It closes at COUNTDOWN so players see the fountain and overlay. Portrait slots use clearly temporary initial-letter cards because final portraits for all five heroes are not yet available; these are not claimed as finished M15 art.

M10 — Gameplay HUD + Input + Network Contracts (implementation delivered; owner build/test and runtime acceptance pending)

2026-10-03: M10 network/HUD implementation adds Fabric 26.2 play payload registration, explicit JSON wire discriminators, C2S request handling, RequestResult, per-player sequence high-water protection, owner-only HeroDelta, full reconnect snapshot/sequence floor, periodic server deltas, vision-filtered minimap and event DTOs. Client-side input routing now has rebindable MOBA keys, suppresses conflicting vanilla combat/drop/inventory/hotbar controls during confirmed PLAYING, and sends Basic/Q/E/F/R, spell, recall, shop and skill requests. HUD renders confirmed HP/Mana/XP/Gold/KDA/ability ranks/cooldowns/items/spell, minimap and scoreboard; B opens a non-pausing double-click shop, C opens stats with server-requested skill plus controls. Source-level tests cover codecs, duplicate requests, ownership, dead-state exceptions, and hidden-position removal. G is the provisional summoner-spell key because project docs do not specify a default key. No Gradle, test or build command was run at the owner's request.

M10 code coverage for the Playbook checklist is delivered, but the milestone is not yet verified/accepted: there is still no match-entry producer (M11), real arena/world query provider (M13), or live server runtime smoke test. The M10 gateway and action adapter expose explicit attachment interfaces; combat and spell requests cannot execute until those server-owned providers are installed. Jason E movement and Recall remain dependent on M13/M14. See Known issues.

2026-10-03: Per owner decision, the Implementation Playbook controls milestone ordering where the Technical Specification's older milestone map differs. The initial M10.1 checkpoint added engine-independent typed C2S request intents, protocol/sequence envelope, stable request error/result types, a per-player sequence high-water guard and focused tests. The Fabric/HUD layers were added later the same day. Tests/build were intentionally not run and no Gradle cache was generated at the owner's request.

2026-10-03: M10.2 adds a server-facts-only request admission validator: roster/hero ownership and team checks, match-state gating, dead-state exceptions for shop and skill upgrades, learned ability and selected spell checks, Retribution jungler restriction, and finite aim/point validation. It is not a gameplay executor: live cooldown, mana, target visibility/range, map safety, shop and lobby rules still belong to downstream authoritative services. Focused tests were added but not run.

An out-of-sequence M15 source-art preparation task has added deterministic Jason Blockbench source models, texture atlases, animation contracts, and validation under `assets/source/` and `tools/`. It does not add renderer or gameplay integration.

2026-09-08: Amelia source-art pipeline has been created independently from Jason and revised after owner feedback. The current model has a narrower adult face, confident expression, denser profile hair, an open rounded bust/corset, layered dress, crown and staff. It includes 18 third-person clips, 13 first-person clips, source-ready facial states, UI icons, glTF/runtime payloads, previews and an offline inspector. Structural/motion validation passes; Blockbench GUI, owner visual acceptance and Fabric rendering remain pending. Details: `docs/assets/AMELIA_ASSET.md`.

2026-09-06: Jason source pipeline v2 replaces the rejected cube placeholder with a reference-based polygonal reconstruction (7205 triangles, 28 bones), embedded 256x256 textures, 15 third-person and 11 first-person clips, automatic glTF exports and geometry-based previews. Artwork acceptance remains pending; this is not a completed M15 runtime integration. Details: `docs/assets/JASON_ASSET.md`.

## Last completed milestone
M9 — Vision + Bushes + Minimap State

## Build status
- M10 current changes: test/build NOT RUN at owner request; runtime smoke NOT CHECKED.
- Amelia asset revision (2026-09-08): `gradlew.bat --no-daemon clean build` PASS; asset validation and deterministic regeneration PASS.
- build: NOT RUN after projectile follow-up — last PASS: `gradlew.bat --no-daemon build` (M0–M9 audit)
- test: NOT RUN after projectile follow-up — last PASS: `gradlew.bat --no-daemon test`, 61 tests (M0–M9 audit)
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
- M6 adds server-owned gold/XP services, passive income from 00:30, item catalog/stat definitions, 5 normal slots plus boots, class restrictions, duplicate protection, and 60% sell refunds.
- M6 shop operations are server-validated and remain available while the hero is dead; buying does not pause the match.
- Audit remediation fixes throne vulnerability to depend on the defending team's destroyed T3 and calls idempotent match finish when a throne is destroyed.
- Item definitions, Jason rank data, lane minion definitions and structure definitions are now loaded from `config_defaults` resources.
- Effective hero stats now include equipped item bonuses and the documented AS/lifesteal/spell-vamp/move caps.
- Added server-owned economy reward helpers for kills, assists, towers and lane minions; passive gold correctly accrues under per-tick orchestration.
- `MatchSession` is a common/server domain class and has no client-only dependencies.
- Common/server code must not depend on client-only classes.
- M7 adds config-backed jungle mob definitions, camp state transitions, leash reset, respawn, documented scaling, and reward dispatch.
- M7 buffs are server-owned, expire by server tick, are removed on death, and Red applies only to outgoing hero damage.
- M7 summoner spells are server-validated and use the existing cooldown/CC services; Flash delegates endpoint safety to the map integration boundary.
- M8A adds Shelianer Q/E/F/R server execution with config-driven ranks, mana costs, cooldowns, physical/magical damage, poison ticks, slow, dash, and six-hit ultimate.
- M8B adds Esaki Q/E/R server execution with config-driven physical damage, mana, cooldowns, projectile range validation, knockback, and three ultimate pulses.
- M8C adds Amelia Q/E/R server execution with config-driven magical damage, AP scaling, six channel ticks, slow, knockback, and delayed Ice Spikes.
- M8D adds Loki Q/E/R server execution with config-driven physical damage, PULL/ROOT control, timed Rampage movement bonus, and Iron Prison AoE.
- M9 adds server-authoritative VisionState per team, LOS/bush/reveal rules, VisionDelta, and minimap position filtering.
- M0-M9 audit remediation moved M9 vision radii/reveal durations into validated `vision.json` balance data.
- M0-M9 audit remediation applies Blue Buff CDR to newly started hero ability cooldowns and exposes authoritative buff-adjusted Max Mana (Jason still receives CDR only).
- M0-M9 audit remediation makes jungle spawn HP use time scaling and implements documented leash recovery stages (50% immediately, 75% after 2s, 100% after another 2s).
- M0-M9 audit remediation makes Jason ability Attack scaling use effective item-adjusted Attack, preserves overlapping slows so weaker effects resume, rejects friendly/repeated structure damage, and keeps detached poison active after caster death.
- M0-M9 audit remediation routes basic attacks through lifesteal and abilities through spell vamp, with Red Buff excluded from true damage, and bounds duplicate DamageInstance retention.
- M0-M9 audit remediation converts Esaki R, Amelia Q/R and Shelianer R to server-tick-owned executions; they no longer apply future damage during the cast request.
- M0-M9 audit remediation routes Shelianer Q, Esaki Q and Loki Q through `ProjectileService`: the cast creates an authoritative projectile, and damage/CC require a later server collision confirmation.

## Assumptions
- M10 UX assumption: G is the default summoner-spell key; the six MOBA sell slots use wire indices 0–4 for normal items and 5 for boots. The documents specify the actions/slots but not these exact binding/index values; both are local protocol/UI choices, not balance changes.
- The repository has Git metadata, but the current sandbox identity is not the repository owner; Git commands require an explicit safe-directory override.

## Known issues
- M10 sequence high-water state is match-scoped and survives disconnect in the gateway; M11/M14 must reuse that gateway across reconnect and clear it only on match replacement.
- M10 admission validation is wired to Fabric receive/dispatch. Actual target/range/LOS and Flash safety still need the M13 world adapter; lobby selection rules need M11. Accepted admission is never reported as a successful action until its authoritative handler succeeds.
- M10 Fabric receiver/dispatcher is now wired, but no match entry currently calls `FabricMobaNetworking.attach`, so the in-game HUD has no active MatchSession to display. M11 must bind the player to the same HeroRuntime on entry/reconnect.
- M10 `DomainGameplayActionAdapter.WorldQueries` must be supplied from M13 real world/map state; current code does not fabricate targets, range, walls, Flash endpoints or jungle camps. Jason E dash movement needs the world adapter; Recall requires M14 lifecycle implementation; lobby requests require M11.
- M10 scoreboard K/D/A producer is exposed through `recordKill`/`recordAssist`, but combat orchestration must call it. Respawn timer and announcements await M14/M16; minimap arena shape/structure positions await M13.
- M10 no client/server smoke test or build/test run after network and HUD changes; code remains unverified until owner runs checks.
- Amelia assets are not connected to Minecraft because the repository still has no mesh-capable HeroRenderer/AnimationStateBridge. Blockbench GUI import, full animation clipping audit and owner visual acceptance are pending. Amelia E gameplay timing and Q hard-CC cancellation differ from the presentation contract and were not modified by the asset task.
- Jason assets: real Blockbench GUI import and in-game renderer smoke tests are not performed. The source requires a mesh-capable renderer; cube-only export cannot preserve its shape. Hidden reference surfaces are reconstructed; exact visual likeness is not certified.
- MEDIUM — projectile abilities now use `ProjectileService`, but the MapDefinition/world adapter still must provide real position transforms, wall collision policy and Loki Hook's visible return path in M13.
- MEDIUM — Blue Buff adjusted Max Mana is exposed by `BuffService`, but regeneration/current-mana clamping on buff expiry still needs server tick orchestration integration.
- MEDIUM — economy reward values, sell ratio and several structure rules remain Java constants instead of canonical validated config fields.
- Economy reward helpers await integration with future world/entity event producers; M10 client/network/HUD now exists but awaits M11/M13 match/world attachment.
- Full minion movement/combat and structure entity ticking require MapDefinition/world integration scheduled for M13; current M5 domain controllers provide deterministic wave, target-priority and damage rules.
- No client or server runtime smoke test has been performed yet.
- M5 uses engine-independent domain models; actual world geometry and Fabric entity/render integration remain deferred to M13.
- Jungle camp placement, concrete world movement/AI, and Flash solid-block validation await MapDefinition/world integration in M13; M7 provides the authoritative domain hooks.

## Files/modules added
- `src/main/java/com/zeravorn/network/` — M10 contracts, codec, server validation/dispatch, Fabric gateway, state DTOs.
- `src/client/java/com/zeravorn/client/` — M10 input, replication receive, gameplay HUD, shop/stats screens.
- `src/client/java/com/zeravorn/client/mixin/MouseHandlerMixin.java` — blocks vanilla hotbar wheel during MOBA play.
- `src/main/resources/zeravorn.client.mixins.json` and `assets/zeravorn/lang/` — client registration and key labels.
- `src/test/java/com/zeravorn/network/` — M10 protocol, sequence, validation and dispatch tests.
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
- `src/main/java/com/zeravorn/item/`
- `src/main/java/com/zeravorn/economy/`
- `src/test/java/com/zeravorn/item/ShopServiceTest.java`
- `src/main/java/com/zeravorn/jungle/`
- `src/main/java/com/zeravorn/buff/`
- `src/main/java/com/zeravorn/spell/`
- `src/main/resources/config_defaults/jungle.json`
- `src/main/resources/config_defaults/spells.json`
- `src/test/java/com/zeravorn/jungle/JungleAndSpellTest.java`
- `src/main/java/com/zeravorn/hero/ShelianerAbilityDefinitions.java`
- `src/main/java/com/zeravorn/hero/ShelianerAbilityResult.java`
- `src/main/java/com/zeravorn/hero/ShelianerAbilityService.java`
- `src/main/resources/config_defaults/shelianer_abilities.json`
- `src/test/java/com/zeravorn/hero/ShelianerAbilityTest.java`
- `src/main/java/com/zeravorn/hero/EsakiAbilityDefinitions.java`
- `src/main/java/com/zeravorn/hero/EsakiAbilityResult.java`
- `src/main/java/com/zeravorn/hero/EsakiAbilityService.java`
- `src/main/resources/config_defaults/esaki_abilities.json`
- `src/test/java/com/zeravorn/hero/EsakiAbilityTest.java`
- `src/main/java/com/zeravorn/hero/AmeliaAbilityDefinitions.java`
- `src/main/java/com/zeravorn/hero/AmeliaAbilityResult.java`
- `src/main/java/com/zeravorn/hero/AmeliaAbilityService.java`
- `src/main/resources/config_defaults/amelia_abilities.json`
- `src/test/java/com/zeravorn/hero/AmeliaAbilityTest.java`
- `src/main/java/com/zeravorn/hero/LokiAbilityDefinitions.java`
- `src/main/java/com/zeravorn/hero/LokiAbilityResult.java`
- `src/main/java/com/zeravorn/hero/LokiAbilityService.java`
- `src/main/resources/config_defaults/loki_abilities.json`
- `src/test/java/com/zeravorn/hero/LokiAbilityTest.java`
- `src/main/java/com/zeravorn/vision/`
- `src/main/resources/config_defaults/vision.json`
- `src/test/java/com/zeravorn/vision/VisionServiceTest.java`

## Files/modules changed
- `gradle.properties`
- `README.md`
- `src/main/resources/fabric.mod.json`
- `src/main/java/com/zeravorn/Zeravorn.java`
- `build.gradle` (JUnit 5 test runtime configuration)
- Removed template mixin classes and descriptors.

## Verification
- M10 static checks: Fabric 26.2 API signatures inspected from existing local jars; official Fabric 26.2 documentation consulted; new JSON files parsed; tracked diff whitespace check completed. Gradle test/build and GUI/runtime checks intentionally not run by Codex.
- Jason pipeline v2: asset-only generation, structural validation, deterministic regeneration, embedded/external PNG consistency, glTF checks, 5 defect regressions and 347 first-person reticle poses passed. Geometry-based visual previews inspected. Gradle build/test and cache generation intentionally not run at the owner's request.
- M0-M9 audit: all `docs` files re-read; source/config/test static review completed; `git diff --check` completed with no whitespace errors (line-ending conversion warnings only).
- M0-M9 audit: `GRADLE_USER_HOME=.gradle-user .\gradlew.bat --no-daemon test` — PASS (61 tests).
- M0-M9 audit: `GRADLE_USER_HOME=.gradle-user .\gradlew.bat --no-daemon build` — PASS.
- Projectile follow-up: tests/build intentionally NOT RUN at the owner's request; no Gradle cache was created.
- M1: ` .\gradlew.bat --no-daemon test` — PASS.
- M1: ` .\gradlew.bat --no-daemon build` — PASS.
- M2: `.\gradlew.bat build` — PASS (confirmed by user).
- M3: `.\gradlew.bat build` — PASS (confirmed by user).
- M4: `.\gradlew.bat --no-daemon test` — PASS.
- M4: `.\gradlew.bat --no-daemon build` — PASS.
- M5: `.\gradlew.bat --no-daemon test` — PASS.
- M5: `.\gradlew.bat --no-daemon build` — PASS.
- M6: `.\gradlew.bat --no-daemon test` — PASS.
- M6: `.\gradlew.bat --no-daemon build` — PASS.
- M7: `GRADLE_USER_HOME=.gradle-user .\gradlew.bat test` — NOT VERIFIED; initial sandbox run was denied network access, approved retry timed out while downloading Gradle 9.5.1.
- M7: `.\gradlew.bat build` and `.\gradlew.bat test` — PASS (confirmed by user).
- M8A: `.\gradlew.bat build` and `.\gradlew.bat test` — PASS (confirmed by user).
- M8B: `.\gradlew.bat build` and `.\gradlew.bat test` — PASS (confirmed by user).
- M8C: `.\gradlew.bat build` and `.\gradlew.bat test` — PASS (confirmed by user).
- M8D: `.\gradlew.bat build` and `.\gradlew.bat test` — PASS (confirmed by user).

## Next milestone
M10 — Gameplay HUD + Input + Network Contracts

## Git checkpoint
- Branch: `main`
- Existing repository history contains M0/M1 commits; M2 changes are currently uncommitted.
