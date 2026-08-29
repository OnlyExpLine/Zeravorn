# MINECRAFT MOBA - TECHNICAL SPECIFICATION v1.1

Архитектура Java/Fabric, server-authoritative правила, сеть, конфиги,
тесты и инструкция для Codex

Назначение: этот документ определяет КАК писать игру. Codex должен считать Game Requirements v1.1 источником gameplay semantics, Balance v1.1 источником чисел, Hero Design v1.1 источником hero presentation, Map and Match Design v1.0 источником геометрии/топологии карты, UI UX and Data Contracts v1.0 источником client-flow/network contracts, а этот файл - источником архитектуры.

## 1. Зафиксированный технический target

Компонент Версия / решение

Minecraft Java 26.2

JDK 25

Fabric Loader 0.19.4

Fabric API 0.158.0+26.2

Fabric Loom 1.17

Gradle 9.5.1

IDE VS Code

Язык Java

Модель сети server-authoritative

Версии должны храниться централизованно в gradle.properties/version
catalog. Обновление версии - отдельная задача, а не побочный эффект
feature-изменения.

## 2. Ключевые архитектурные принципы

• Сервер - единственный источник истины для HP, Mana, damage, Gold, XP,
Levels, Items, cooldown, Buffs, CC, Vision, Minions, Jungle, Towers,
Throne и Match State. • Клиент отправляет намерения:
basic/cast/spell/shop/recall/input. Клиент не присылает готовый
damage/gold/xp. • Балансные значения не хардкодить в ability-классах;
читать из конфигов. • Core-системы независимы от финальных Blockbench
assets. До art-polish используются placeholder модели/иконки/звуки. •
Новые герои подключаются через registry/definitions и ability
implementations без переписывания core. • Системы не должны создавать
циклические зависимости. Hero не управляет напрямую
shop/towers/bots/network.

## 3. Структура проекта

src/main/java/`<base>`{=html}/ MobaMod.java common/ match/ MatchManager,
MatchSession, MatchState team/ TeamId, TeamService, TeamRoster hero/
HeroDefinition, HeroRuntime, HeroRegistry, HeroStatsService, SkillPointService, AbilityLevelService ability/
Ability, AbilityContext, AbilityInstance, AbilityRegistry combat/
BasicAttackService, DamageService, CrowdControlService projectile/
ProjectileService, MobaProjectile, CollisionPolicy item/ ItemDefinition,
MobaInventory, ItemService economy/ GoldService, ExperienceService,
LevelService spell/ SummonerSpell, SpellService minion/ LaneMinion,
WaveService, LanePathService jungle/ JungleMob, JungleCamp,
JungleService, BuffService structure/ Tower, Throne, StructureService
vision/ VisionService, RevealService bot/ BotController, BotBrain,
BotState map/ MapDefinition, Zone, WaypointPath, MapValidator stats/
MatchStatsService config/ ConfigLoader, ConfigValidator

network/ NetworkService, packets/ client/ input/ KeyBindings,
ClientInputRouter hud/ MainHud, SkillHud, ItemHud, ScoreboardHud screen/
Lobby, HeroSelect, Shop, Stats, PostGame minimap/ MinimapRenderer
render/ HeroRenderer, FirstPersonHeroRenderer, ProjectileRenderer
animation/ AnimationStateBridge particle/ MobaParticles sound/
MobaSounds src/main/resources/ fabric.mod.json assets/`<modid>`{=html}/
data/`<modid>`{=html}/ config_defaults/

## 4. Главные domain-сущности

Сущность Ответственность

MatchSession state, timer, teams, map, winner, active systems

HeroRuntime
owner/team/heroId/alive/HP/Mana/LVL/XP/Gold/items/cooldowns/buffs/CC/availableSkillPoints/abilityRanks

HeroDefinition base/growth stats, class, damage type, hitbox, move/base
attack

AbilityDefinition bind, manaByRank, cdByRank, range, radius, formula, timing, targeting, unlockLevel, maxRank

MobaInventory 5 normal slots + boots

LaneMinion team/lane/wave/waypoint/HP/target/state

JungleCamp spawn, leash, mob, respawn state

Tower/Throne team, lane/order, HP, target, vulnerability

VisionState visible ids/reveal timers per team

MatchStats KDA, damage, healing, farm, structures, earned gold

## 5. Match State Machine

LOBBY -\> HERO_SELECT -\> LOADING -\> COUNTDOWN -\> PLAYING -\> FINISHED
-\> POST_GAME -\> LOBBY

Все игровые tick-сервисы обязаны проверять PLAYING. Завершение матча
должно быть idempotent: повторный event трона не меняет winner и не
запускает post-game второй раз.

## 6. Server tick orchestration

Каждый server tick: \## 1. Match timer / scheduled spawns \## 2.
Respawn + Recall timers \## 3. Buff/CC expiration \## 4. Active
ability/channel instances \## 5. Projectiles + collision \## 6. Lane
minion movement/combat \## 7. Jungle AI/leash \## 8. Tower/Throne
targeting \## 9. Bot movement/action executor \## 10. Rate-limited bot
decisions \## 11. Rate-limited vision/reveal \## 12. Delta sync clients

Не делать глобальный scan всех entities мира каждый tick. Использовать
range/bounding-box queries и более редкие интервалы для AI/Vision.

## 7. Hero / Stats

EffectiveStats = level base/growth + item flat stats + percentage item
modifiers + buffs -\> caps

HeroRuntime хранит current state, HeroDefinition - неизменяемое
описание. Покупка MaxHP поднимает максимум, но не лечит автоматически на
величину бонуса. При снятии временного MaxMana current mana clamp до
нового max.


### 7.1 Skill Point / Ability Rank

HeroRuntime:
- `availableSkillPoints`
- `Map<AbilityId, Integer> abilityRanks`

Level-up:
1. LevelService повышает level.
2. SkillPointService добавляет +1 availableSkillPoints.
3. Сервер отправляет HeroDelta/LevelUpEvent.
4. Игрок может потратить Skill Point позже.

UpgradeAbilityRequest validation:
- ability принадлежит hero;
- availableSkillPoints > 0;
- currentRank < maxRank;
- heroLevel >= unlockLevel;
- match state допускает UI action.

Skill upgrade разрешен во время смерти. Он не считается combat input.

Обычные hero definitions: Q max4, E max4, R max2.
Shelianer: Q max3, E max3, F max2, R max2.
R unlockLevel=4.

## 8. Ability API

interface Ability { AbilityCheck check(AbilityContext ctx);
AbilityInstance start(AbilityContext ctx); void tick(AbilityInstance
instance); void cancel(AbilityInstance instance, CancelReason reason); }

AbilityContext: ServerWorld MatchSession HeroRuntime caster targetId /
aimDirection / groundPoint serverTick

check(): PLAYING, alive, unlocked, CC-state, cooldown, Mana, range, LOS,
target team/type, collision rules.

TargetingType Примеры

SELF Loki E, AoE around caster

DIRECTION Shelianer Q, Esaki Q, Loki Q

GROUND_POINT Amelia R

TargetingType Примеры

TARGET_ENTITY Shelianer R

DASH Jason E

CHANNEL_CONE Amelia Q

## 9. Combat pipeline

Client input -\> server validation -\> basic/ability instance -\>
hitbox/projectile -\> CollisionPolicy + team filter -\> DamageRequest
-\> formula + modifiers -\> apply HP -\> lifesteal/spell vamp -\> death
check -\> stats event -\> S2C combat/animation event

Каждая DamageInstance получает unique id. Multi-hit skill создает
отдельный id на каждый разрешенный tick. Это предотвращает повторный
damage от повторной collision обработки.

## 10. Crowd Control

CC Правило

STUN запрещает movement/basic/skills/spells

ROOT запрещает movement/dash/Flash; basic и stationary skills разрешены

SLOW уменьшает movement; strongest active wins

KNOCK_UP forced movement + input lock

KNOCKBACK forced movement + input lock

NAUSEA визуальный debuff; сам по себе не server CC

CC хранится сервером с expiresAtTick и cleanseable flag. Cleanse удаляет
только разрешенные эффекты.

## 11. Projectile system

ProjectileDefinition: speed maxRange collisionRadius/shape wallPolicy
allowedTargetTypes structureDamage=false default pierceCount=0 default

Friendly entities игнорируются. Collision - server-side. Loki Q имеет
state OUTBOUND -\> ATTACHED/PULLING или RETURNING -\> DONE. Projectile
removal обязателен при hit, wall policy, max range, caster invalid,
match finished.

## 12. Shop / Items

BUY: C2S itemId -\> server catalog lookup -\> hero/class restriction -\>
sufficient gold -\> slot + duplicate rule -\> subtract server price -\>
insert item -\> recalc stats -\> sync

SELL: slot -\> server item lookup -\> remove -\> +floor(price\*0.60) -\>
recalc -\> sync

Клиент никогда не присылает цену предмета как доверенное значение.

## 13. Gold / XP / Level

Все изменения Gold имеют Reason enum: PASSIVE, LANE_LAST_HIT,
LANE_PROXIMITY, JUNGLE, KILL, ASSIST, TOWER, SELL. XP аналогично. Это
упрощает тесты и post-game статистику. LevelService после добавления XP
обрабатывает level-up и публикует LevelUpEvent; SkillPointService начисляет 1 Skill Point за каждый level-up. AbilityLevelService валидирует и применяет UpgradeAbilityRequest; R имеет unlockLevel=4, остальные hero abilities - unlockLevel=1.

## 14. Lane Minions

WaveService: firstSpawn=00:30 interval=30s composition=3 melee + 2
ranged every 3rd += siege

LaneMinionState: MOVE_PATH FIGHT_MINION ATTACK_STRUCTURE RECOVER_PATH

Движение идет по MapDefinition waypoints. Не использовать глобальный
pathfinding по всей карте, если следующий waypoint достижим.

## 15. Jungle

JungleCampState: WAITING ALIVE LEASH_RETURN DEAD_WAIT_RESPAWN

Leash break: clear target return to camp spawn restore HP according to
GDD stages reset combat state

Last hitter получает reward/buff. BuffService хранит endTick и удаляет
buff при death.

## 16. Towers / Throne

StructureService отвечает за vulnerability chain, 80% protection без
enemy wave, targeting, damage и winner event. Башню/трон не требуется
делать обычным Mob. Рекомендуется отдельная server
entity/controller/hitbox, привязанная к геометрии структуры карты. Tower
aggro: minions по умолчанию; если enemy hero внутри radius наносит
damage allied hero - переключение на aggressor.

## 17. Vision / Fog of War

VisionService per team: sources = heroes + lane minions + towers +
throne/fountain LOS + wall occlusion bush-zone rules reveal timers
visibleEntitySet

Sync: hidden enemy sensitive state не отправлять клиенту без
необходимости

Недостаточно просто скрыть модель на клиенте, если клиент продолжает
получать точные координаты скрытого enemy. Сервер должен
фильтровать/ограничивать sensitive sync.

## 18. Map Definition

maps/`<map_id>`{=html}.json { blueSpawn, redSpawn, blueFountain,
redFountain, fountainDangerZones, blueThrone, redThrone,
towers:\[team,lane,order,pos\], lanePaths:{top,mid,bot}, jungleCamps,
bushZones, playableBoundary }

MapValidator до старта проверяет обязательные зоны, порядок T1/T2/T3,
существование путей и корректные границы.

## 19. Bot architecture

Bot v1.0 специально упрощен. Архитектура должна позволять последующее расширение, но первая версия не требует полноценного Utility AI.

Компоненты:
- BotController - владелец управления HeroRuntime при bot slot/AFK/disconnect.
- BotPerception - только разрешенный Team Vision.
- BotRouteProfile - назначенная линия/лесной маршрут и waypoint-набор.
- BotBuildProfile - фиксированный порядок покупок.
- BotBrain - простое состояние + ограниченные эвристики.
- BotActionExecutor - движение, basic, ability/spell, Recall, buy.

Минимальные состояния:
SPAWN_BUY, MOVE_ROUTE, LANE_FARM, FIGHT, RETREAT, RECALL, PUSH, ATTACK_STRUCTURE, RESPAWN_RETURN, JUNGLE_ROUTE(optional).

Decision loop рекомендуется 4-8 раз/сек. Movement/action executor может обновляться чаще. Bots используют те же Ability/Shop/Combat APIs, что и реальные игроки.

## 20. Bot first-version policies

- Bot не видит enemy через стены/кусты без team vision.
- HP < 30% резко повышает Retreat/Recall.
- Bot не dive tower без allied lane wave.
- Lane bot движется по заданному lane route и выбирает ближайшую допустимую цель.
- Last-hit желательно, но perfect last-hit не является acceptance criterion v1.1.
- Hero abilities используются простой эвристикой: есть валидная цель, хватает Mana, ability готова, цель в range.
- Build order фиксирован в BotBuildProfile.
- Jungle bot, если назначен, использует фиксированный camp route и Retribution; сложные gank/invade/objective решения не требуются.
- Не реализовывать prediction, combo planning, counter-build, teamfight coordinator и сложные rotations до отдельной версии AI.

## 21. Networking contracts

Все state-changing C2S requests содержат:
- protocolVersion
- clientSequenceId
- payload

Сервер ведет короткое окно обработанных sequenceId на соединение для защиты от повторного выполнения idempotency-sensitive действий.

### C2S

| Packet | Payload |
|---|---|
| BasicAttackRequest | aim/targetHint + clientSequenceId |
| CastAbilityRequest | abilityId + target/aim/groundPoint + clientSequenceId |
| UpgradeAbilityRequest | abilityId + clientSequenceId |
| UseSpellRequest | spellId + aim/target + clientSequenceId |
| BuyItemRequest | itemId + clientSequenceId |
| SellItemRequest | slot + clientSequenceId |
| RecallRequest | start/cancel + clientSequenceId |
| LobbyRequest | ready/hero/spell/team action + clientSequenceId |

Клиент никогда не присылает доверенные damage, HP, Mana, Gold, XP, item price, cooldown result, kill, victory или итоговую позицию ability movement.

### S2C

| Packet | Payload |
|---|---|
| HeroDelta | HP/Mana/LVL/XP/Gold/items/cooldowns/CC/skillPoints/abilityRanks |
| AbilityRankChanged | abilityId/newRank/remainingSkillPoints |
| RequestResult | sequenceId/success/errorCode |
| AbilityEvent | start/hit/cancel for animation/VFX |
| ProjectileEvent | spawn/update/remove |
| MatchStateEvent | state/timer/winner |
| VisionDelta | show/hide/reveal |
| StructureState | HP/destroyed/target |
| MinimapDelta | разрешенные positions |
| CombatEvent | damage/heal/kill/feed |

### Request errors

Обязательные error codes:
WRONG_STATE, DEAD, STUNNED, ROOT_BLOCK, COOLDOWN, NO_MANA, OUT_OF_RANGE,
INVALID_TARGET, NO_GOLD, INVENTORY_FULL, DUPLICATE_ITEM, CLASS_RESTRICTED,
NO_SKILL_POINT, ABILITY_NOT_AVAILABLE, MAX_RANK, LEVEL_REQUIREMENT,
INVALID_ABILITY, DUPLICATE_SEQUENCE, OUTSIDE_BOUNDARY.

### Rate policy

- movement/input: частый поток, но server-authoritative sanity checks;
- basic/ability/spell: фактическая частота ограничивается серверным cooldown/cast state;
- shop/lobby/skill-upgrade: low-frequency; repeated spam может быть отброшен;
- точные packet-per-second лимиты не являются балансным числом и могут настраиваться после profiling.

## 22. Client input / HUD

ClientInputRouter активирует MOBA-controls только во время матча.
Q/E/R/B/C/V/Tab/ЛКМ не должны конфликтовать со стандартными Drop/Inventory/Hotbar.
F активируется как hero ability только у Shelianer.
HUD читает client replicated state, но не изменяет server state напрямую.
Shop UI и Skill Upgrade UI отправляют request и ждут server RequestResult/state delta.

SkillHud обязан поддерживать:
- rank 0 = NOT LEARNED;
- currentRank/maxRank;
- available Skill Point marker;
- R LOCKED LVL4;
- отсутствие F slot у героев без F.

Полная UI/UX спецификация: Minecraft_MOBA_UI_UX_and_Data_Contracts_v1.0.md.

## 23. Animation / assets integration

Core code использует стабильные animation state ids: idle, walk, run,
basic, q, e, f, r, hit, stun, root, death, recall. Hit timing -
серверное событие. Client animation callback не применяет damage. До
финальных Blockbench assets разрешены placeholders. Это обязательное
правило, чтобы art не блокировал gameplay foundation.

## 24. Config structure

config/
- heroes/
- abilities/
- items/attack.json
- items/defense.json
- items/magic.json
- items/boots.json
- economy.json
- levels.json
- lane_minions.json
- jungle.json
- structures.json
- spells.json
- vision.json
- bots/
- maps/

ConfigValidator проверяет:
- уникальные IDs;
- обязательные поля;
- positive ranges/CD;
- valid references;
- ability maxRank/unlockLevel;
- длину rank-массивов manaByRank/cdByRank/baseDamageByRank;
- caps;
- binds;
- tower lane order;
- inventory restrictions;
- map required zones/paths/boundary;
- bot route/build references.

Примерные контракты:

HeroDefinition:
`id, class, damageType, baseStats, growth, hitbox, abilities[], spellRestrictions`

AbilityDefinition:
`id, bind, maxRank, unlockLevel, targeting, baseDamageByRank, manaByRank, cdByRank, scaling, range, radius, timing, cc`

ItemDefinition:
`id, category, price, stats, allowedHeroTypes, slotType`

BotProfile:
`heroId, routeId, buildOrder[], preferredSpell`

MapDefinition:
определен в Minecraft_MOBA_Map_and_Match_Design_v1.0.md.

## 25. Persistence / reconnect

Активный MatchSession хранится сервером до завершения матча. Disconnect
не удаляет HeroRuntime: owner control передается BotController.
Reconnect прикрепляет игрока к тому же HeroRuntime. Crash recovery
активного матча можно не реализовывать в первой версии; server restart
завершает текущий матч.


## 25.1 Обязательные edge-case правила

- Death during Recall -> Recall cancel.
- Damage/movement/basic/skill/spell during Recall -> Recall cancel.
- Death during non-detached cast/channel -> cast cancel, если ability не помечена иначе.
- Caster dies after projectile launch -> уже созданный projectile продолжает жить по ProjectileDefinition, если match не завершен.
- Target dies before projectile arrival -> hit по мертвой цели невозможен; projectile удаляется либо продолжает путь по своей collision policy.
- Match ends during ability/projectile -> новые gameplay damage/CC после FINISHED не применяются; активные gameplay instances завершаются/удаляются.
- Level-up while dead -> level и Skill Point применяются сразу.
- Buy/Sell и Skill Upgrade while dead -> разрешены.
- Two lethal hits same server tick -> deterministic processing order; первая валидная transition ALIVE->DEAD фиксирует killer, повторная death ignored.
- Flash endpoint outside boundary/inside invalid solid -> request rejected без расхода cooldown.
- Knockback/forced movement into boundary -> position clamp/collision; за playableBoundary герой не выходит.
- Disconnect during cast -> уже валидированный server instance продолжает/отменяется по обычным правилам, затем control переходит BotController.
- Disconnect while dead -> HeroRuntime сохраняется; после respawn им управляет bot до reconnect.
- Reconnect -> новый HeroRuntime не создается.
- Throne destroy event idempotent -> winner фиксируется один раз.
- Duplicate Buy/Sell/UpgradeAbility sequence -> не выполняется второй раз.


## 26. Тестирование

./gradlew clean build ./gradlew test

Unit: - damage formulas - AttackSpeed formula + caps - XP thresholds - skill point accrual - ability rank caps/unlockLevel - UpgradeAbilityRequest -
gold rewards / sell 60% - item restrictions / slots - mana/cooldown
validation - CC expiration / Cleanse - tower vulnerability +
protection - match transitions - buff expiration - respawn table

Integration/Game: - projectile collision - minion waypoint route - tower
aggro switch - throne win - 2-client cast/hit - disconnect -\> bot -\>
reconnect

Codex не завершает feature-задачу без запуска build/tests, если среда
позволяет их запустить.

## 27. Debug/dev tools

Только для dev-сборки: overlay/commands для hitbox, ability range, tower
range, vision circles, waypoints, jungle leash, bot state, current
effective stats. Логировать причины отказа: WRONG_STATE, DEAD, STUNNED,
ROOT_BLOCK, COOLDOWN, NO_MANA, OUT_OF_RANGE, INVALID_TARGET, NO_GOLD,
INVENTORY_FULL.

## 28. AGENTS.md - правила для Codex

\# Minecraft MOBA project rules - Target: Minecraft Java 26.2 / Fabric / JDK 25. - Server authoritative. Never trust client for damage, gold, XP
or victory. - Balance numbers come from config, not hardcoded ability
classes. - Game Requirements v1.1 defines gameplay semantics. - Balance
v1.0 defines numeric values. - Hero Design v1.1 defines
visuals/animation intentions. - Do not silently change gameplay or
balance while implementing a task. - Prefer one bounded feature per
change. - Preserve package boundaries; avoid God classes. - Add/update
tests for pure gameplay logic. - Run ./gradlew clean build and ./gradlew
test before finishing. - Do not edit generated/build directories. - Do
not mass-format unrelated files. - Placeholder art is acceptable;
gameplay must not wait for final art. - Final report: changed files,
tests run, remaining risks.

## 29. Definition of Done

Пункт Требование

Build clean build successful

Tests релевантные тесты проходят

Authority критичное состояние server-owned

Config баланс не захардкожен

Network requests валидируются

Performance нет неоправданного global scan каждый tick

Scope нет случайных несвязанных изменений

Пункт Требование

Docs новый config/API документирован

## 30. Milestones разработки

M Система Definition

M0 Bootstrap Fabric project, versions, config loader, logging, tests

M1 Match Core states, teams, lobby-ready, countdown, win/postgame

M2 Hero Core HeroRuntime/stats/level/skill-points/ability-ranks/death/respawn/recall/fountain

M3 Combat Core basic/damage/CC/projectile/cooldown/mana

M4 Jason Vertical Slice Jason complete + placeholder render/HUD

M5 Lane + Structures waves/path/minions/T1/T2/T3/throne

M6 Economy + Shop gold/items/inventory/shop/stats screen

M7 Jungle + Spells camps/buffs/4 spells

M8 Heroes Amelia -\> Esaki -\> Shelianer -\> Loki

M9 Vision + UI FoW/bush/minimap/scoreboard

M10 Functional Bots v1.0 lane route/simple fight/retreat/build; advanced AI postponed

M11 Assets/Polish Blockbench/animations/VFX/SFX/performance

M12 5v5 QA soak/reconnect/telemetry/balance

## 31. Первые 10 задач Codex

## 1. Создать Fabric 26.2 project, зафиксировать JDK25/Loader/API/Loom/Gradle, выполнить build.

## 2. Создать MatchState и MatchSession с unit tests transitions.

## 3. Создать ConfigLoader/ConfigValidator и тестовый economy/levels config.

## 4. Создать TeamId/TeamService и roster до 5 игроков.

## 5. Создать HeroDefinition/HeroRuntime/HeroStatsService и Jason fixture.

## 6. Создать ExperienceService/LevelService/SkillPointService/AbilityLevelService и тесты LVL1-10 + rank caps.

## 7. Создать DamageService + AttackSpeed formula + caps tests.

## 8. Создать CooldownService/CrowdControlService/Mana checks.

## 9. Создать BasicAttackService + server validation.

## 10. Реализовать Jason Q, затем E, затем R как три отдельные feature-задачи с rank-aware config.

## 32. Что не давать Codex одним промптом

Не ставить задачу 'сделай всю MOBA', 'сделай сразу 5 героев и ботов' или
'сгенерируй весь проект целиком'. Каждая задача должна иметь
ограниченный scope, acceptance criteria и проверки.

## 33. Источник версий

На дату фиксации: Minecraft Java 26.2; для 26.2 Fabric указывает Loom
1.17 и Gradle 9.5.1; JDK 25 используется для актуальной 26.x разработки.
В документе закреплены Fabric Loader 0.19.4 и Fabric API 0.158.0+26.2.
Перед первым коммитом разрешена только централизованная проверка/замена
patch-версий.
