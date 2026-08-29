# MINECRAFT MOBA - UI/UX AND DATA CONTRACTS v1.0

## Назначение

Документ фиксирует клиентский flow, HUD, lobby interaction, состояния экранов, request/result contracts, базовые JSON-схемы и обязательные edge cases. Он дополняет Technical Specification и не переносит authority с сервера на клиент.

## 1. Общий UI-flow

`LOBBY_WORLD -> HERO/SPELL SELECT -> READY -> LOCK -> LOADING -> COUNTDOWN -> PLAYING -> FINISHED -> POST_GAME -> LOBBY_WORLD`

UI всегда отражает server-confirmed state.

## 2. Lobby

Lobby - физическая комната, где игрок может ходить.

Одновременно доступен GUI overlay/menu:
- Team;
- Hero;
- Summoner Spell;
- Ready.

### Lobby card игрока

Показывает:
- nickname;
- team;
- selectedHero;
- selectedSpell;
- ready.

### Hero pedestals

Для каждого стартового героя:
- third-person preview;
- имя;
- класс;
- краткое описание;
- interaction hint.

Interaction с pedestal открывает/выбирает соответствующую hero card, но все герои также доступны через обычный Hero Select GUI.

### Ready

Ready доступен только когда:
- выбран допустимый hero;
- выбран допустимый spell;
- игрок состоит в команде.

После server lock:
- hero/spell/team нельзя менять;
- показывается LOADING.

## 3. Hero Select screen

Для каждого героя:
- portrait;
- name;
- class;
- краткое описание;
- Q/E/F/R preview;
- selected/available/locked state.

Союзник уже выбрал hero -> `UNAVAILABLE_FOR_TEAM`.

F отображается только у Shelianer.

Если игрок не выбрал героя до lock:
сервер назначает допустимого свободного hero.

## 4. Spell Select

Показывает:
- Flash;
- Retribution;
- Cleanse;
- Regeneration.

Retribution:
- доступен только назначенному jungler;
- disabled с объяснением для остальных.

## 5. Loading / Countdown

Loading screen:
- обе команды;
- hero portraits;
- selected spells;
- progress/status без gameplay authority.

После arena ready:
- камера/герой на fountain;
- movement/combat input disabled;
- overlay `5 4 3 2 1 START`.

На START сервер переводит MatchState в PLAYING.

## 6. Main HUD layout

Рекомендуемая структура:

```text
                     MATCH TIMER
                 BLUE SCORE : RED SCORE

                                         MINIMAP

HP  [====================]
MP  [====================]   (нет у Jason)
LVL 5   XP [=======---]   GOLD 1540
K/D/A  3/1/4

                    [Q] [E] [F*] [R] [SPELL]
                    r2  r1       r1
                    CD  CD        CD

ITEMS: [1][2][3][4][5]    BOOTS:[B]
```

`F*` существует только у Shelianer.

## 7. Ability HUD states

Каждая hero ability имеет:
- icon;
- bind;
- currentRank/maxRank;
- cooldown overlay;
- mana cost;
- locked/not learned/ready/disabled state.

### Rank 0
Показывать `NOT LEARNED`.

### Skill Point available
Над способностями, которые разрешено улучшить, показывать `+`.

### R до LVL4
`LOCKED - LVL 4`.

### Invalid use feedback
Краткий локальный UI feedback после server reject:
- NO_MANA;
- COOLDOWN;
- OUT_OF_RANGE;
- INVALID_TARGET;
- STUNNED;
- ROOT_BLOCK;
- ABILITY_NOT_AVAILABLE.

Клиент может дать prediction-подсказку, но окончательное решение - server RequestResult.

## 8. Skill Upgrade

При нажатии `+`:
`UpgradeAbilityRequest(abilityId, clientSequenceId)`.

Успех:
- AbilityRankChanged;
- новый rank;
- remaining skill points;
- HUD обновляется.

Ошибка не тратит очко.

Skill Upgrade разрешен во время смерти.

## 9. Shop

B открывает Shop без паузы матча.

Категории:
- Physical;
- Magical;
- Defense;
- Boots.

Item card:
- icon;
- name;
- price;
- stats;
- owned state;
- disabled reason.

Buy/Sell - двойной щелчок согласно Game Requirements.

Ошибки:
- NO_GOLD;
- INVENTORY_FULL;
- DUPLICATE_ITEM;
- CLASS_RESTRICTED;
- WRONG_SLOT.

Продажа:
- 60% server price;
- клиент не рассчитывает authoritative refund.

Shop доступен во время смерти.

## 10. Stats screen

C:
- HP / MaxHP;
- HP Regen;
- Mana / MaxMana;
- Mana Regen;
- Attack;
- AP;
- Attack Speed bonus;
- effective attack interval;
- Move Speed;
- Lifesteal;
- Spell Vamp;
- CDR;
- active buffs.

## 11. Scoreboard

Tab:
- обе команды;
- nickname;
- hero;
- level;
- K/D/A;
- gold;
- final/current items;
- respawn timer для dead hero при необходимости.

Не показывает скрытые position данные.

## 12. Minimap

Показывает:
- arena shape;
- allied heroes всегда;
- enemy heroes только при team vision/reveal;
- towers;
- throne;
- destroyed structure state.

Не показывает hidden enemy position, даже если клиент когда-то видел enemy ранее.
После потери vision icon скрывается; допустимо короткое purely visual fade без передачи новой позиции.

## 13. Death UI

После смерти:
- combat input blocked;
- respawn timer;
- scoreboard доступен;
- shop доступен;
- skill upgrade доступен;
- camera behavior может быть free/spectate allied в пределах допустимых данных.

После respawn:
- 100% HP/Mana;
- управление возвращается.

## 14. Announcements

Минимум:
- First Blood;
- Double/Triple/Quadra/Penta;
- Tower Destroyed;
- Ultimate Unlocked;
- Level Up;
- Throne under attack (опционально);
- Victory/Defeat.

Announcements не должны закрывать центр экрана надолго.

## 15. Post Game

Показывает:
- winner;
- duration;
- обе команды;
- K/D/A;
- hero damage;
- damage taken;
- healing;
- structure damage;
- lane kills;
- jungle kills;
- earned gold;
- final build.

Кнопка/таймер возврата в Lobby.

## 16. Settings

Локально сохраняются:
- Master Volume;
- Music;
- SFX;
- Mouse Sensitivity;
- HUD Scale;
- Minimap Scale;
- Damage Numbers;
- keybinds.

## 17. Общий network envelope

Каждый state-changing C2S packet:

```json
{
  "protocolVersion": 1,
  "clientSequenceId": 12345,
  "payload": {}
}
```

Server RequestResult:

```json
{
  "clientSequenceId": 12345,
  "success": true,
  "errorCode": null
}
```

Duplicate sequence для idempotency-sensitive action не выполняется повторно.

## 18. C2S contracts

### BasicAttackRequest
- aimDirection;
- optional targetHint;
- clientSequenceId.

### CastAbilityRequest
- abilityId;
- один из targetId / aimDirection / groundPoint в зависимости от TargetingType;
- clientSequenceId.

### UpgradeAbilityRequest
- abilityId;
- clientSequenceId.

### UseSpellRequest
- spellId;
- aim/target при необходимости;
- clientSequenceId.

### BuyItemRequest
- itemId;
- clientSequenceId.

### SellItemRequest
- slot;
- clientSequenceId.

### RecallRequest
- action START/CANCEL;
- clientSequenceId.

### LobbyRequest
- action;
- heroId/spellId/team/ready value;
- clientSequenceId.

## 19. S2C contracts

### HeroDelta
- entityId;
- HP/MaxHP;
- Mana/MaxMana;
- level/xp;
- gold;
- items;
- cooldowns;
- CC;
- buffs;
- availableSkillPoints;
- abilityRanks.

### AbilityEvent
- casterId;
- abilityId;
- phase START/HIT/CANCEL/END;
- serverTick;
- visual seed/instance id при необходимости.

### AbilityRankChanged
- heroId;
- abilityId;
- newRank;
- remainingSkillPoints.

### ProjectileEvent
- instanceId;
- definitionId;
- spawn/update/remove;
- allowed visible transform.

### MatchStateEvent
- state;
- timer;
- winner when finished.

### VisionDelta
- team;
- showIds;
- hideIds;
- reveal timers where needed.

### StructureState
- structureId;
- HP;
- destroyed;
- currentTarget where allowed.

### MinimapDelta
Только позиции, разрешенные VisionService.

### CombatEvent
- damage/heal/kill/assist/feed display data.

## 20. Error codes

Обязательный enum:

- WRONG_STATE
- DEAD
- STUNNED
- ROOT_BLOCK
- COOLDOWN
- NO_MANA
- OUT_OF_RANGE
- INVALID_TARGET
- NO_GOLD
- INVENTORY_FULL
- DUPLICATE_ITEM
- CLASS_RESTRICTED
- WRONG_SLOT
- NO_SKILL_POINT
- ABILITY_NOT_AVAILABLE
- MAX_RANK
- LEVEL_REQUIREMENT
- INVALID_ABILITY
- DUPLICATE_SEQUENCE
- OUTSIDE_BOUNDARY
- INVALID_FLASH_ENDPOINT

## 21. HeroDefinition example

```json
{
  "id": "jason",
  "class": "FIGHTER",
  "damageType": "PHYSICAL",
  "baseStats": {},
  "growth": {},
  "hitbox": {"width": 0.75, "height": 2.25},
  "abilities": ["jason_q", "jason_e", "jason_r"],
  "spellRestrictions": []
}
```

## 22. AbilityDefinition example

```json
{
  "id": "jason_q",
  "bind": "Q",
  "maxRank": 4,
  "unlockLevel": 1,
  "targeting": "DIRECTION",
  "baseDamageByRank": [60, 70, 80, 90],
  "manaByRank": [0, 0, 0, 0],
  "cdByRank": [8, 8, 8, 8],
  "scaling": {"attack": 1.0},
  "range": 2,
  "timing": {"hit": 0.30}
}
```

Rank arrays имеют длину maxRank.

## 23. ItemDefinition example

```json
{
  "id": "iron_blade",
  "category": "PHYSICAL",
  "price": 1800,
  "slotType": "NORMAL",
  "stats": {"attack": 35, "maxHp": 150},
  "allowedHeroTypes": ["PHYSICAL", "MIXED"]
}
```

## 24. BotProfile example

```json
{
  "heroId": "loki",
  "routeId": "ROAM_SIMPLE",
  "buildOrder": ["titan_armor", "traveler_boots"],
  "preferredSpell": "REGENERATION"
}
```

## 25. Match/Lobby data

`LobbyPlayerState`:
- playerId;
- nickname;
- team;
- heroId?;
- spellId?;
- ready.

`MatchClientState`:
- matchId;
- state;
- timer;
- ownHero replicated state;
- visible team/enemy entities;
- structures;
- allowed minimap data.

## 26. Обязательные edge cases

1. Death during Recall -> Recall cancel.
2. Damage/movement/basic/skill/spell during Recall -> Recall cancel.
3. Death during non-detached cast/channel -> cancel, если ability не помечена иначе.
4. Caster dies after valid projectile launch -> projectile продолжает по definition, пока match PLAYING.
5. Target dies before projectile arrival -> мертвая цель не получает hit.
6. Match FINISHED -> никакой новый damage/CC не применяется.
7. Level-up while dead -> level и Skill Point применяются.
8. Buy/Sell while dead -> разрешено.
9. Skill Upgrade while dead -> разрешено.
10. Two lethal hits same tick -> deterministic server order; один killer.
11. Repeated death event -> ignored.
12. Flash outside playableBoundary -> reject, cooldown не тратится.
13. Flash into invalid solid -> reject, cooldown не тратится.
14. Knockback into boundary -> clamp/collision.
15. Disconnect during cast -> server instance следует обычным правилам, затем control bot.
16. Disconnect while dead -> HeroRuntime сохраняется; bot получает контроль.
17. Reconnect -> тот же HeroRuntime.
18. Duplicate Buy/Sell/UpgradeAbility sequence -> no second execution.
19. Throne destroy -> idempotent winner.
20. Shop request на exact death tick -> server order определяет state, но shop разрешен и в death state.
21. Skill point request одновременно с level-up packet -> server authoritative level state; при уже начисленном point запрос проходит, иначе клиент повторяет после state delta.
22. Ability rank update не меняет уже запущенный ability instance задним числом.
23. Item stat removal clamp CurrentHP/CurrentMana только если новое Max ниже current.
24. Hidden enemy не должен просачиваться через minimap/network packet.

## 27. Acceptance criteria UI/Data v1.0

- Lobby world + GUI позволяют выбрать hero/spell и Ready.
- Hero lock корректно предотвращает duplicate hero в одной команде.
- HUD показывает только реальные abilities героя.
- Skill Point можно распределять согласно caps/unlockLevel.
- Shop отображает server reject reason.
- Scoreboard/PostGame содержат требуемую статистику.
- Fog-of-War не раскрывает hidden enemy через UI/network.
- Duplicate sequence не вызывает повторную покупку/прокачку.
- Все state-changing requests валидируются сервером.
- UI не применяет authoritative gameplay state самостоятельно.
