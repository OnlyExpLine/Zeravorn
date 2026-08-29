# ZERAVORN — CODEX IMPLEMENTATION PLAYBOOK v1.0

> Назначение: единый управляющий документ для поэтапной реализации Zeravorn через несколько независимых чатов Codex.
>
> Этот файл НЕ заменяет игровые и технические документы. Он определяет порядок разработки, правила работы Codex, границы каждого чата, критерии завершения этапов и формат передачи состояния следующему чату.

---

# 0. Как использовать этот файл

Разработка Zeravorn должна идти **не в одном огромном чате Codex**, а последовательностью ограниченных этапов.

Для каждого этапа:

1. Открыть проект Zeravorn целиком в VS Code.
2. Убедиться, что в репозитории лежат все 6 проектных MD-документов.
3. Создать **новый чат Codex**.
4. Скопировать в него:
   - `MASTER PROMPT` из раздела 4;
   - затем промт конкретного этапа.
5. Codex сначала изучает проект и документы.
6. Codex реализует только текущий этап.
7. Codex запускает проверки и исправляет ошибки текущего этапа.
8. В конце Codex обновляет файл `docs/IMPLEMENTATION_STATUS.md`.
9. После успешного завершения этапа изменения коммитятся.
10. Следующий этап выполняется в **новом чате Codex**.

Главное правило:

> Один чат = один ограниченный milestone или одна крупная подсистема.

Не просить Codex «реализовать всю MOBA целиком».

---

# 1. Источники требований

В проекте должны находиться следующие документы:

1. `Minecraft_MOBA_Game_Requirements_v1.1.md`
2. `Minecraft_MOBA_Balance_v1.1.md`
3. `Minecraft_MOBA_5_Heroes_Design_v1.1.md`
4. `Minecraft_MOBA_Technical_Specification_v1.1.md`
5. `Minecraft_MOBA_Map_and_Match_Design_v1.0.md`
6. `Minecraft_MOBA_UI_UX_and_Data_Contracts_v1.0.md`

Допустимо переименовать их с префиксом `Zeravorn_`, если содержимое и версии сохранены.

Рекомендуемое расположение:

```text
Zeravorn/
└── docs/
    ├── 01_Zeravorn_Game_Requirements_v1.1.md
    ├── 02_Zeravorn_Balance_v1.1.md
    ├── 03_Zeravorn_5_Heroes_Design_v1.1.md
    ├── 04_Zeravorn_Technical_Specification_v1.1.md
    ├── 05_Zeravorn_Map_and_Match_Design_v1.0.md
    ├── 06_Zeravorn_UI_UX_and_Data_Contracts_v1.0.md
    └── IMPLEMENTATION_STATUS.md
```

Если фактические имена отличаются, Codex обязан найти эти документы по содержимому и использовать наиболее новые версии.

---

# 2. Приоритет требований

Если документы пересекаются, использовать следующий приоритет:

1. **Game Requirements** — что игра должна уметь.
2. **Balance** — числовые игровые значения.
3. **Hero Design** — presentation/visual/audio/hero-specific UX.
4. **Map and Match Design** — карта, маршруты, зоны, lobby/arena topology.
5. **UI/UX and Data Contracts** — интерфейсы и сетевые контракты.
6. **Technical Specification** — архитектура и технический способ реализации.

Но:

- архитектурные ограничения server-authoritative из Technical Specification обязательны;
- числовые значения нельзя брать из Game Requirements, если точное значение задано в Balance;
- запрещено молча изменять баланс;
- запрещено придумывать новую механику, если она противоречит документам.

При неразрешимом противоречии:

1. не придумывать решение скрытно;
2. выбрать минимально рискованный вариант, сохраняющий существующую архитектуру;
3. отметить решение как `ASSUMPTION` в `IMPLEMENTATION_STATUS.md`;
4. не делать широкую переработку соседних систем.

---

# 3. Зафиксированные правила Zeravorn

Название проекта:

```text
Zeravorn
```

Основной Mod ID:

```text
zeravorn
```

Основной Java namespace:

```text
com.zeravorn
```

Все новые Java packages должны находиться внутри:

```text
com.zeravorn.*
```

Все собственные resource identifiers должны использовать namespace:

```text
zeravorn:*
```

Примеры:

```text
zeravorn:jason
zeravorn:shelianer
zeravorn:iron_blade
zeravorn:jason_q
zeravorn:arena_v1
```

Ключевые продуктовые решения:

- Minecraft Java + Fabric.
- IDE: VS Code.
- Java.
- Server-authoritative gameplay.
- 5v5.
- Максимальный уровень героя: 10.
- Обычные герои: `Q 4 / E 4 / R 2`.
- Shelianer: `Q 3 / E 3 / F 2 / R 2`.
- F — геройская способность только Shelianer.
- R доступен с 4 уровня.
- Каждый уровень даёт 1 Skill Point.
- Skill Point можно не тратить сразу.
- Skill Point можно потратить во время смерти героя.
- Tank не привязан жёстко к Bot и может роумить.
- Нет Lord/Turtle/Baron/Dragon и аналогичных epic boss objectives в v1.
- Lobby — физическая 3D-зона + GUI.
- Первый AI — простой функциональный bot slot-filler, без сложного utility AI.
- Баланс хранится в конфиге, а не размазывается magic numbers по Java-коду.
- Red Buff `+10% outgoing hero damage` в текущем Balance является provisional config value и не должен быть превращён в hardcoded invariant.
- Blue Buff и остальные balance numbers брать из актуального Balance-документа.

---

# 4. MASTER PROMPT — добавлять в начало КАЖДОГО нового чата Codex

Скопировать полностью:

```text
Ты работаешь над Minecraft Java Fabric проектом Zeravorn.

Перед любыми изменениями:

1. Изучи текущее состояние репозитория.
2. Найди и прочитай все 6 актуальных проектных документов в папке docs:
   - Game Requirements
   - Balance
   - 5 Heroes Design
   - Technical Specification
   - Map and Match Design
   - UI/UX and Data Contracts
3. Если существует docs/IMPLEMENTATION_STATUS.md — обязательно прочитай его полностью.
4. Проверь текущий git diff/status, но ничего не откатывай без необходимости.
5. Не предполагай, что предыдущий чат Codex что-то сообщил тебе устно: источником истины являются код, документы и IMPLEMENTATION_STATUS.md.

Проект:
- Name: Zeravorn
- Mod ID: zeravorn
- Java namespace: com.zeravorn
- IDE: VS Code
- Platform: Minecraft Java + Fabric
- Architecture: server-authoritative

Обязательные инженерные правила:

- Клиент отправляет intent/request, но не определяет результат боя.
- Server owns HP, Mana, Gold, XP, Level, Skill Points, Ability Ranks, Items, Cooldowns, Buffs, CC, Structures, Minions, Jungle, Vision и Match State.
- Gameplay/balance numbers должны приходить из config/definitions, а не быть разбросаны magic numbers по gameplay-классам.
- Не менять согласованный gameplay молча.
- Не добавлять несогласованные крупные механики.
- Не делать массовый форматинг всего репозитория.
- Не рефакторить несвязанные с задачей части проекта.
- Не трогать generated/build directories.
- Не ослаблять server validation ради удобства клиента.
- Сохранять package boundaries из Technical Specification.
- Для F ability помнить: F существует только у Shelianer.
- Не создавать epic jungle bosses в v1.
- Боты первого релиза должны оставаться простыми и функциональными.
- Код должен быть расширяемым, но не overengineered.
- Там, где финальные art assets отсутствуют, использовать понятные placeholders и abstraction points.

Порядок работы:

A. Сначала коротко перечисли, что уже реализовано и какие файлы относятся к текущей задаче.
B. Составь краткий план только текущего этапа.
C. Реализуй этап полностью.
D. Добавь/обнови тесты там, где это возможно.
E. Выполни доступные проверки проекта.
F. Исправь ошибки, вызванные своими изменениями.
G. Не переходи к следующему milestone.
H. Обнови docs/IMPLEMENTATION_STATUS.md.

Минимальные проверки после изменений:
- ./gradlew build
- ./gradlew test
или Windows-эквивалент:
- .\gradlew.bat build
- .\gradlew.bat test

Если отдельная Fabric/GameTest задача предусмотрена проектом — выполни её, когда это относится к этапу.

В конце ответа дай:
1. Что реализовано.
2. Какие ключевые файлы созданы/изменены.
3. Какие тесты/проверки выполнены и их результат.
4. Какие известные ограничения остались.
5. Что должен делать следующий milestone.

Не начинай следующий milestone самостоятельно.
```

---

# 5. IMPLEMENTATION_STATUS.md — обязательная память между чатами

Если файла ещё нет, первый чат должен создать:

```text
docs/IMPLEMENTATION_STATUS.md
```

Формат:

```markdown
# Zeravorn Implementation Status

## Current milestone
M0 — Bootstrap

## Last completed milestone
None

## Build status
- build: PASS / FAIL
- test: PASS / FAIL
- client run: PASS / NOT CHECKED
- server run: PASS / NOT CHECKED

## Implemented
- ...

## Important architecture decisions
- ...

## Assumptions
- ...

## Known issues
- ...

## Files/modules added
- ...

## Next milestone
M1 — Match Core

## Git checkpoint
- Branch:
- Commit:
```

Каждый последующий Codex-чат ОБЯЗАН обновлять этот файл.

Нельзя использовать его вместо исходных документов. Это только handoff/state file.

---

# 6. Рекомендуемая Git-стратегия

Одна milestone-задача — один отдельный commit.

Примеры:

```text
chore: bootstrap zeravorn fabric project
feat: implement match lifecycle core
feat: add hero runtime and skill progression
feat: implement combat core
feat: implement jason vertical slice
feat: add lane minions and structures
feat: implement economy and shop
feat: add jungle buffs and summoner spells
feat: implement remaining heroes
feat: add vision and gameplay hud
feat: add simple bots
feat: add lobby and arena flow
feat: add reconnect and match telemetry
test: complete zeravorn gameplay integration coverage
```

Если этап слишком большой, допускаются несколько атомарных commits внутри milestone, но не смешивать разные milestones.

---

# 7. Карта этапов

```text
M0   Bootstrap / project hygiene
M1   Match lifecycle + teams
M2   Hero definitions/runtime + stats + XP + skill points/ranks
M3   Combat core + cooldown + mana + CC + projectiles
M4   Jason vertical slice
M5   Lane minions + structures + throne
M6   Economy + items + shop
M7   Jungle + buffs + summoner spells
M8A  Shelianer
M8B  Esaki
M8C  Amelia
M8D  Loki
M9   Vision + bushes + minimap state
M10  Main gameplay HUD + input + networking polish
M11  Lobby + hero/spell/team select + loading/countdown
M12  Simple bots
M13  MapDefinition + arena integration + validators/dev tools
M14  Death/respawn/recall/fountain + reconnect/AFK takeover
M15  VFX/SFX/animation hooks/placeholders
M16  Post-game/stats/settings/performance polish
M17  Full 5v5 integration + regression + balancing hooks
M18  Release-readiness audit
```

Можно выполнять M8A–M8D в отдельных чатах и даже параллельно только если они не меняют общие combat APIs. В обычной последовательной разработке лучше идти по порядку.

---

# 8. M0 — Bootstrap / проектная база

## Цель

Получить чистый, собираемый Fabric-проект Zeravorn, пригодный для дальнейшей разработки.

## PROMPT ДЛЯ CODEX

```text
Выполни milestone M0 — Bootstrap Zeravorn.

Не реализуй gameplay.

Задачи:

1. Проверь сгенерированный Fabric-проект и текущие версии из Gradle-конфигурации.
2. Убедись, что:
   - mod id = zeravorn;
   - display name = Zeravorn;
   - package root = com.zeravorn.
3. Приведи стартовые package/class names к Zeravorn, если template оставил example placeholders.
4. Проверь fabric.mod.json и resource namespace.
5. Создай только базовую package structure, которая действительно нужна сейчас, без пустых сотен классов.
6. Создай docs/IMPLEMENTATION_STATUS.md.
7. Добавь минимальный logging при initialization мода.
8. Убедись, что common/server код не зависит от client-only Minecraft классов.
9. Проверь Gradle wrapper/build.
10. Не добавляй героев, combat, GUI, карту или ботов.

Acceptance:
- проект собирается;
- test task проходит;
- Zeravorn корректно определяется Fabric;
- нет examplemod/example package;
- IMPLEMENTATION_STATUS.md создан и заполнен;
- структура готова к M1.
```

---

# 9. M1 — Match Core

## Цель

Матч, команды и state machine без gameplay-систем.

## PROMPT

```text
Выполни milestone M1 — Match Core.

Реализуй только основу жизненного цикла матча согласно Technical Specification и Game Requirements.

Нужно:

- MatchState:
  LOBBY
  HERO_SELECT
  LOADING
  COUNTDOWN
  PLAYING
  FINISHED
  POST_GAME

- MatchSession.
- Match identifier.
- BLUE и RED Team.
- Player/participant slot model.
- 5 slots per team.
- transitions между MatchState.
- строгая server-side validation переходов.
- begin/end/reset lifecycle.
- базовый tick orchestration entry point без реализации gameplay подсистем.
- победитель/finish reason model.
- idempotent finish: матч не может завершиться второй раз.
- unit tests state transitions.

Не реализуй:
- heroes;
- combat;
- items;
- minions;
- jungle;
- HUD;
- bots.

Интегрируй минимально с Fabric server lifecycle только там, где это нужно.

Acceptance:
- state machine протестирована;
- invalid transitions отклоняются;
- MatchSession не зависит от client code;
- build/test PASS;
- IMPLEMENTATION_STATUS.md обновлён.
```

---

# 10. M2 — Hero Core + Levels + Skill Points

## PROMPT

```text
Выполни milestone M2 — Hero Core.

Реализуй data/domain слой героев без конкретной полной реализации abilities.

Нужно:

1. HeroDefinition.
2. HeroRuntime.
3. Effective hero stats model.
4. Team ownership.
5. HP/Mana/base stats.
6. XP progression LVL1–10 по Balance.
7. LevelService / XP service.
8. SkillPointService.
9. AbilityLevelService.
10. Ability rank storage.

Правила:

Обычные герои:
- Q max 4
- E max 4
- R max 2
- F отсутствует

Shelianer:
- Q max 3
- E max 3
- F max 2
- R max 2

- каждый level даёт 1 skill point;
- LVL1 можно взять Q/E, Shelianer Q/E/F;
- R unlockLevel=4;
- unspent points разрешены;
- ability можно улучшать во время смерти;
- rank > maxRank запрещён;
- upgrade без point запрещён.

Создай definitions для всех 5 героев как data/config definitions, но не реализуй их abilities.

Добавь тесты:
- XP/level;
- максимум LVL10;
- skill point accumulation;
- upgrade validation;
- R locked before LVL4;
- F rejected for non-Shelianer;
- rank caps.

Не переходи к combat.
```

---

# 11. M3 — Combat Core

## PROMPT

```text
Выполни milestone M3 — Combat Core.

Создай общую server-authoritative боевую инфраструктуру, не реализуя полностью всех героев.

Нужно:

- DamageType.
- DamageInstance с уникальным ID.
- DamageService / CombatResolver.
- DamageResult/CombatEvent.
- basic attack framework.
- attack interval / attack speed calculation.
- cooldown service.
- mana spending/validation.
- CC model:
  stun
  root
  slow
  knockup
  knockback
- правила stacking/strongest slow согласно Balance.
- AbilityDefinition runtime access by rank.
- Ability execution interface/API.
- targeting primitives.
- server-side range/target/dead/state validation.
- projectile abstraction.
- server projectile hit confirmation.
- duplicate damage prevention.
- death trigger boundary, но полный respawn flow оставить позднее.

Важно:
- клиент никогда не сообщает фактический damage;
- damage рассчитывает сервер;
- animation callback клиента не наносит damage;
- значения abilities читаются по rank из config/definition.

Добавь unit tests на formulas, cooldowns, mana, CC, duplicate DamageInstance и validation.

Не реализуй ещё весь Jason — только core.
```

---

# 12. M4 — Jason Vertical Slice

## PROMPT

```text
Выполни milestone M4 — Jason Vertical Slice.

Используя готовый Hero/Combat core, полностью реализуй одного героя Jason как эталон вертикального среза.

Нужно реализовать:

- Jason base stats LVL1–10 из Balance.
- no mana behavior.
- basic attack.
- Q rank 1–4.
- E rank 1–4 с двухфазной/описанной в документах механикой.
- R rank 1–2.
- cooldowns.
- healing Q.
- movement/dash/flight rules.
- knockup/stun.
- server-side targeting.
- ability events для клиента.
- placeholder VFX/SFX event hooks, но без финального art.
- тесты каждого ability rank и invalid casts.

Все числовые значения — из Balance v1.1.

Не менять generic APIs без необходимости. Если API всё же требует корректировки — сделать минимально и покрыть тестами.

Acceptance:
- Jason playable на server simulation level;
- basic/Q/E/R валидируются сервером;
- нет F;
- build/test PASS.
```

---

# 13. M5 — Lane Minions + Towers + Throne

## PROMPT

```text
Выполни milestone M5 — Lane Minions and Structures.

Реализуй:

Lane:
- TOP/MID/BOT lane model;
- wave scheduler;
- first wave time и interval из Balance;
- 3 melee + 2 ranged;
- каждый третий wave siege;
- scaling из Balance;
- waypoint path following;
- target selection;
- minions не должны самостоятельно агриться на heroes, если документы это запрещают;
- lane proximity/reward hooks.

Structures:
- T1/T2/T3;
- Throne;
- stats из Balance;
- attack logic;
- tower range;
- tower attack interval;
- tower damage rule для hero classes;
- wave protection/-80% rule;
- only basic attacks damage structures;
- vulnerability chain;
- throne finish вызывает match finish ровно один раз.

Создай чистые interfaces с MapDefinition positions, но полноценную карту пока не строй.

Добавь tests для wave generation, siege cadence, tower protection, structure ordering и throne finish.
```

---

# 14. M6 — Economy + Items + Shop

## PROMPT

```text
Выполни milestone M6 — Economy, Inventory, Items and Shop.

Реализуй:

Economy:
- starting gold;
- passive gold starting at configured match time;
- kill/assist rewards;
- tower team/last-hit rewards;
- minion rewards;
- XP sharing/proximity rules.

Inventory:
- 5 normal item slots;
- separate boots slot;
- completed item duplicate restriction;
- class/category restrictions из документов;
- sell = 60%.

Items:
- перенеси все item definitions/stat bonuses из Balance в config/data definitions.
- поддержи Attack, AP, HP, Mana, regen, AS, lifesteal, spell vamp, move modifiers.
- caps/formulas из Balance.

Shop:
- buy anywhere;
- shop does not pause game;
- buying while dead разрешено, если документы не запрещают;
- server validation;
- error/result model.

Добавь tests на стоимость, gold, sell, duplicate, inventory full, boots slot, effective stats/caps.

Не делай финальный GUI — только domain/network-ready слой.
```

---

# 15. M7 — Jungle + Buffs + Summoner Spells

## PROMPT

```text
Выполни milestone M7 — Jungle, Buffs and Summoner Spells.

Jungle:
- Red/Blue/GreenA/GreenB на каждой стороне.
- 2 contested normal camps.
- никаких epic boss objectives.
- spawn time, respawn, HP, damage, attack interval, gold, XP, leash из Balance.
- jungle scaling.
- leash/reset behavior.
- Green special CC behavior из требований.
- camp state model.

Buffs:
- timed buff model.
- remove on death.
- Red buff: использовать текущее config значение Balance v1.1; не hardcode.
- Blue buff: использовать Balance v1.1.
- Jason Blue behavior отдельно согласно Balance.

Summoner spells:
- Flash.
- Retribution.
- Cleanse.
- Regen.
- cooldowns/effects из Balance.
- Retribution доступен только назначенному jungler.
- boundary validation для Flash.

Добавь tests для respawn/leash/buff expiration/death removal/Retribution restriction/Flash boundary/Cleanse.
```

---

# 16. M8A — Shelianer

## PROMPT

```text
Выполни milestone M8A — Shelianer.

Полностью реализуй Shelianer по Hero Design + Balance.

Особенно:
- Shelianer единственный герой с F.
- Q max3.
- E max3.
- F max2.
- R max2.
- R unlock LVL4.
- физический/магический damage type по таблице.
- DoT.
- slow.
- dash.
- multi-hit R.
- target stun.
- mana costs.
- projectile/hit timings.

Переиспользуй generic combat APIs.

Добавь tests всех rank и ключевых edge cases.
Не реализуй других героев.
```

---

# 17. M8B — Esaki

## PROMPT

```text
Выполни milestone M8B — Esaki.

Реализуй полностью Esaki по Hero Design и Balance.

Обязательно:
- Q4/E4/R2.
- F отсутствует.
- ranged basic.
- basic attack preparation/lock 2 sec.
- герой не может двигаться во время обязательной prep/lock фазы согласно документам.
- Q projectile.
- E knockback.
- R multi-hit/area behavior и nausea presentation event.
- physical damage.
- mana/cooldowns/ranks.

Server determines hit/damage.

Добавь tests и не переходи к Amelia.
```

---

# 18. M8C — Amelia

## PROMPT

```text
Выполни milestone M8C — Amelia.

Реализуй полностью Amelia.

Обязательно:
- Q4/E4/R2.
- F отсутствует.
- magical basic attack.
- короткий animation/cast lock согласно документам.
- Q cone multi-tick + slow.
- E AoE knockback.
- R target/area/delay.
- AP scaling.
- mana/cooldowns.
- server authority.

Добавь tests всех ranks и edge cases.
```

---

# 19. M8D — Loki

## PROMPT

```text
Выполни milestone M8D — Loki.

Реализуй полностью Loki.

Обязательно:
- Q4/E4/R2.
- F отсутствует.
- tank stats.
- Q chain/pull behavior.
- E movement utility.
- R area root.
- physical damage where specified.
- mana/cooldowns/ranks.
- hitbox/ranges from Balance.

Добавь tests.

После завершения все 5 heroes должны иметь полноценный gameplay implementation.
```

---

# 20. M9 — Vision + Bushes + Minimap State

## PROMPT

```text
Выполни milestone M9 — Vision System.

Реализуй server-authoritative team vision.

Нужно:
- VisionState per team.
- hero vision radius.
- lane creep vision.
- tower/throne/fountain vision.
- wall LOS blocking.
- bush zones.
- enemy inside bush hidden, кроме правил same-bush/reveal.
- attack-from-bush temporary reveal.
- attacking allied creep/tower reveal timing.
- hidden enemy не должен передаваться клиенту как обычная видимая entity state там, где архитектура требует concealment.
- VisionDelta.
- MinimapDelta/data source.

Не делай сложный final minimap renderer — подготовь корректные данные и базовую visual integration.

Добавь tests LOS/reveal/bush/team separation.
```

---

# 21. M10 — Gameplay HUD + Input + Network Contracts

## PROMPT

```text
Выполни milestone M10 — Main Gameplay HUD, Inputs and Network Contracts.

Используй UI/UX and Data Contracts как основной контракт.

Реализуй/доведи:

C2S:
- BasicAttackRequest.
- CastAbilityRequest.
- UpgradeAbilityRequest.
- UseSpellRequest.
- BuyItemRequest.
- SellItemRequest.
- RecallRequest.
- необходимые gameplay requests.

Общее:
- protocolVersion;
- clientSequenceId;
- duplicate protection;
- RequestResult/error codes;
- server validation.

S2C:
- HeroDelta;
- AbilityRankChanged;
- AbilityEvent;
- ProjectileEvent;
- MatchStateEvent;
- VisionDelta;
- StructureState;
- MinimapDelta;
- CombatEvent.

HUD:
- HP;
- Mana/no-mana behavior;
- LVL/XP;
- Gold;
- K/D/A;
- Q/E/R;
- F только Shelianer;
- ability ranks;
- cooldowns;
- available Skill Point plus marker;
- R LOCKED LVL4;
- items + boots;
- summoner spell;
- minimap basic renderer;
- invalid action feedback.

Нельзя доверять клиенту authoritative values.

UI должен переживать reconnect/state resync.
```

---

# 22. M11 — Lobby + Hero Select + Loading

## PROMPT

```text
Выполни milestone M11 — Lobby and Match Entry Flow.

Реализуй hybrid lobby:

- отдельная Lobby world/room abstraction;
- физические hero pedestals/preview placeholders;
- GUI:
  - Team;
  - Hero Select;
  - Spell Select;
  - Ready;
- один hero не выбирается двумя союзниками;
- Retribution только jungler;
- tank flexible, не фиксировать Bot;
- bot filling позже, но slots подготовить;
- Ready validation;
- все готовы -> lock selections;
- LOADING;
- transfer/load Arena;
- COUNTDOWN;
- PLAYING.

Обязательно server authoritative lobby state.

Добавь disconnect handling на стадии lobby/select хотя бы безопасным reset/slot state.

Не делай complex bots в этом этапе.
```

---

# 23. M12 — Simple Bots

## PROMPT

```text
Выполни milestone M12 — Simple Functional Bots.

Не реализуй advanced Utility AI.

Архитектура:
- BotController
- BotPerception
- BotRouteProfile
- BotBuildProfile
- BotBrain
- BotActionExecutor

Допустимые states:
- SPAWN_BUY
- MOVE_ROUTE
- LANE_FARM
- FIGHT
- RETREAT
- RECALL
- PUSH
- ATTACK_STRUCTURE
- RESPAWN_RETURN
- optional JUNGLE_ROUTE

Нужно:
- заполнять свободные team slots;
- выбирать hero с учётом отсутствующего role/class насколько возможно;
- assigned lane/route;
- follow waypoints;
- атаковать legal visible nearby targets;
- использовать abilities простыми правилами;
- retreat при low HP;
- recall;
- fixed build profile;
- push towers only under valid conditions/wave;
- attack throne;
- respawn/return;
- optional fixed jungler route;
- Retribution legal use для jungler.

Bot должен использовать те же server gameplay APIs, что игрок.

Запрещено:
- future information;
- wallhack;
- hidden enemy knowledge;
- complex ganks;
- prediction engine;
- counter-build;
- invasion planner;
- advanced teamfight coordination.

Decision frequency ориентировочно 4–8 раз/сек, не каждый tick без необходимости.

Добавь deterministic tests для state decisions.
```

---

# 24. M13 — MapDefinition + Arena

## PROMPT

```text
Выполни milestone M13 — MapDefinition and Arena Integration.

Используй Map and Match Design.

Реализуй:
- MapDefinition JSON/config.
- playableBoundary.
- blue/red spawn.
- fountains.
- fountain danger zones.
- thrones.
- T1/T2/T3 positions.
- TOP/MID/BOT lane paths.
- jungle camps.
- bush zones.
- walls/LOS integration hooks.
- lobby definition/reference при необходимости.
- MapLoader.
- MapValidator.

Validator должен проверять:
- обе базы;
- обе команды structures;
- порядок T1/T2/T3;
- paths;
- camp completeness;
- boundary;
- throne placement;
- spawn/fountain;
- waypoint validity;
- отсутствие обязательного epic boss objective.

Используй approximate v1 map geometry. Не пытайся копировать карту Mobile Legends 1:1.

Добавь debug/dev commands или overlay hooks:
- boundary;
- lane waypoints;
- tower radius;
- vision radius;
- jungle leash;
- bush zones.

Если реального world save/arena asset пока нет, создай config-ready integration и документируй, куда помещать фактический мир.
```

---

# 25. M14 — Death / Respawn / Recall / Fountain / Reconnect

## PROMPT

```text
Выполни milestone M14 — Lifecycle Gameplay Systems.

Реализуй:

Death:
- transition alive -> dead ровно один раз;
- kill/assist attribution;
- buffs removed on death where specified;
- cooldown/projectile edge cases according to docs;
- respawn timer LVL1–10 из Balance;
- respawn.

Fountain:
- HP regeneration;
- Mana regeneration;
- enemy fountain danger damage;
- base/spawn placement.

Recall:
- server-controlled recall;
- cancellation rules;
- completion/teleport;
- UI/network event hooks.

Reconnect:
- hero remains in MatchSession;
- temporary bot takeover;
- reconnect получает тот же HeroRuntime;
- state snapshot/resync;
- player retakes control.

AFK:
- configured inactivity threshold;
- simple bot takeover.

Не реализуй crash recovery after full server restart, если v1 docs этого не требуют.
```

---

# 26. M15 — Animation / VFX / SFX Integration Hooks

## PROMPT

```text
Выполни milestone M15 — Presentation Integration.

Цель — не рисовать финальные Blockbench assets, а подготовить корректную техническую систему.

Нужно:
- stable animation IDs;
- idle/walk/run/basic;
- Q/E/R для обычных heroes;
- F только Shelianer;
- hit;
- CC;
- death;
- recall;
- first-person hands/weapon abstraction hooks;
- third-person animation hooks;
- VFX event system;
- SFX event system;
- ability-specific event timing;
- server confirmed impact timing.

Важно:
- animation event клиента никогда самостоятельно не наносит damage.
- сервер отправляет подтверждённые gameplay events.
- placeholder assets допустимы.

Учитывай Hero Design visual/audio identities.

Добавь fallback behavior при отсутствующих assets, чтобы gameplay не ломался.
```

---

# 27. M16 — Post-game / Stats / Settings / Performance

## PROMPT

```text
Выполни milestone M16 — Post-game, Stats, Settings and Performance.

Реализуй:
- MatchStats.
- K/D/A.
- damage dealt/received, если предусмотрено архитектурой/доками.
- gold/XP relevant match metrics.
- result screen Victory/Defeat.
- post-game scoreboard.
- POST_GAME -> LOBBY flow.
- settings:
  Master Volume
  Music
  SFX
  Mouse Sensitivity
  HUD Scale
  Minimap Scale
  Damage Numbers
- sensible client persistence для settings.

Performance:
- исключить тяжёлые per-tick allocations в критичных системах;
- bots не должны делать тяжёлое decision-making каждый tick;
- bounded arena chunks handling из requirements;
- network deltas вместо ненужных full snapshots там, где уже предусмотрено.

Не менять gameplay balance.
```

---

# 28. M17 — Full 5v5 Integration

## PROMPT

```text
Выполни milestone M17 — Full 5v5 Integration and Regression.

На этом этапе НЕ добавляй новые крупные mechanics.

Проверь end-to-end сценарий:

Lobby
-> Hero Select
-> Spell Select
-> Ready
-> Loading
-> Countdown
-> Playing
-> lane/jungle/combat/economy
-> towers
-> throne
-> Finished
-> Post-game
-> Lobby

Состав команды:
- до 5 players per team;
- недостающие slots bots;
- all five hero implementations;
- role flexibility;
- jungler Retribution.

Проверь:
- 10 hero runtimes simultaneously;
- minion waves;
- structures;
- jungle;
- buffs;
- vision;
- HUD;
- network;
- disconnect/reconnect;
- bots;
- death/respawn;
- throne finish.

Добавь integration/GameTests для основных flows.

Исправляй только реальные integration/regression проблемы.
Не проводи большой redesign.
```

---

# 29. M18 — Release-readiness Audit

## PROMPT

```text
Выполни milestone M18 — Zeravorn v1 Release-readiness Audit.

Это аудит и исправление найденных дефектов, а не создание новой игры.

1. Прочитай все 6 проектных документов заново.
2. Прочитай IMPLEMENTATION_STATUS.md.
3. Построй checklist требований.
4. Для каждого требования укажи:
   - IMPLEMENTED
   - PARTIAL
   - MISSING
   - BLOCKED BY ASSET
   - INTENTIONALLY DEFERRED
5. Найди:
   - нарушения server authority;
   - hardcoded balance;
   - F на героях кроме Shelianer;
   - неправильные skill caps;
   - invalid R unlock;
   - missing network validation;
   - missing error handling;
   - bot cheats;
   - tower/throne edge cases;
   - reconnect issues;
   - map validator gaps;
   - UI desync;
   - duplicated gameplay code;
   - client-only imports в common/server коде.
6. Запусти полный build/test/GameTest набор.
7. Исправь критические и high-priority defects.
8. Не добавляй несогласованные features.
9. Обнови IMPLEMENTATION_STATUS.md итоговым status.

В конце сформируй:
- Release Readiness Summary;
- Passed checks;
- Remaining blockers;
- Asset-only blockers;
- Balance/playtest items;
- список задач, которые должны выполняться вручную в Minecraft.
```

---

# 30. Если отдельный milestone не помещается в контекст Codex

Нельзя просто писать «продолжи».

Разделить subsystem, например:

```text
M6.1 Economy
M6.2 Inventory
M6.3 Item definitions
M6.4 Shop server logic
M6.5 Tests/integration
```

Каждый новый чат всё равно получает MASTER PROMPT и читает `IMPLEMENTATION_STATUS.md`.

Для M3 допустимо:

```text
M3.1 Damage model
M3.2 Cooldowns/mana
M3.3 CC
M3.4 Basic attacks
M3.5 Projectiles
M3.6 Combat integration tests
```

Для M10:

```text
M10.1 Network protocol
M10.2 Request validation
M10.3 HUD core
M10.4 Skill HUD
M10.5 Shop HUD
M10.6 Minimap HUD
M10.7 Resync tests
```

---

# 31. Что НЕЛЬЗЯ поручать нескольким чатам параллельно

Не выполнять параллельно независимыми чатами, если оба будут менять общий API:

- MatchSession;
- HeroRuntime;
- DamageService;
- Ability execution API;
- Network protocol base;
- MobaInventory;
- VisionState;
- MapDefinition.

Иначе высок риск конфликтующих архитектурных решений.

После стабилизации общих APIs отдельных heroes можно реализовывать раздельно.

---

# 32. Правило изменения документации

Если Codex обнаружил, что код невозможно корректно реализовать без уточнения:

Codex НЕ должен самостоятельно переписывать Game Requirements или Balance.

Разрешено:

```text
docs/IMPLEMENTATION_STATUS.md
```

добавить:

```markdown
## Open design question
- ...
```

или:

```markdown
## Assumption
- ...
```

Основные 6 документов изменяются только осознанным решением владельца проекта.

---

# 33. Definition of Done для каждого milestone

Milestone считается завершённым только если:

- код текущего этапа реализован;
- package boundaries соблюдены;
- server-authoritative rule соблюдено;
- balance не захардкожен без причины;
- ошибки текущего этапа обработаны;
- новые domain rules покрыты unit tests, где это применимо;
- integration tests добавлены, где это применимо;
- `build` PASS;
- `test` PASS;
- нет случайных unrelated изменений;
- `IMPLEMENTATION_STATUS.md` обновлён;
- Codex перечислил known limitations;
- следующий milestone не реализован преждевременно.

---

# 34. Отдельный промт для проверки чужого/предыдущего этапа

Если кажется, что предыдущий Codex-чат сделал что-то неправильно, создать новый чат и использовать:

```text
Проведи code review последнего реализованного milestone Zeravorn.

Перед review прочитай:
- все 6 design/technical документов;
- IMPLEMENTATION_STATUS.md;
- git diff относительно commit до milestone.

Не добавляй новые gameplay features.

Проверь:
- соответствие требованиям;
- server authority;
- balance/config usage;
- package architecture;
- edge cases;
- tests;
- Fabric client/server separation;
- race/state issues;
- duplicated logic;
- incorrect assumptions;
- hidden behavior changes.

Сначала дай список найденных проблем по Critical/High/Medium/Low.
Затем исправь Critical и High, а Medium — только если исправление локальное и безопасное.
После исправлений выполни build/test и обнови IMPLEMENTATION_STATUS.md.
```

---

# 35. Промт для поиска бага без разрушения проекта

```text
Найди и исправь указанный баг в Zeravorn.

Перед изменением:
- прочитай относящиеся к багу проектные документы;
- прочитай IMPLEMENTATION_STATUS.md;
- воспроизведи или локализуй причину;
- определи authoritative subsystem, который владеет этим состоянием.

Не переписывай архитектуру целиком ради одного бага.
Не меняй баланс, если баг не в Balance.
Добавь regression test, если это технически возможно.

После исправления:
- build/test;
- кратко укажи root cause;
- перечисли изменённые файлы;
- обнови IMPLEMENTATION_STATUS.md только если баг меняет состояние milestone/known issues.
```

---

# 36. Промт для балансного изменения после playtest

Использовать только когда владелец проекта явно решил изменить balance:

```text
Внеси только указанное балансное изменение Zeravorn.

Сначала найди canonical config/definition, из которого это значение загружается.
Не хардкодь новое значение в gameplay logic.
Проверь все места использования параметра.
Обнови соответствующий Balance MD только если я прямо указал это сделать.
Обнови тесты, если они проверяют старое значение.
Не меняй другие balance values.

После изменения выполни build/test и перечисли точные затронутые параметры.
```

---

# 37. Рекомендуемый рабочий цикл владельца проекта

После каждого Codex milestone:

```text
1. git status
2. git diff
3. .\gradlew.bat build
4. .\gradlew.bat test
5. локальный запуск dev client/server при необходимости
6. ручная проверка результата
7. git add ...
8. git commit -m "..."
9. следующий Codex chat
```

Перед большим следующим этапом полезно делать:

```text
git status
```

и иметь чистое working tree.

---

# 38. Первые этапы, которые нужно выполнить сейчас

Для только что созданного Fabric-проекта порядок:

```text
M0 Bootstrap
↓
M1 Match Core
↓
M2 Hero Core / Level / Skill Points
↓
M3 Combat Core
↓
M4 Jason Vertical Slice
```

После M4 уже должен существовать первый реальный вертикальный gameplay-срез:

```text
server
+ match
+ hero runtime
+ progression
+ combat
+ one fully working hero
```

Только после этого разумно добавлять весь остальной MOBA-контент.

---

# 39. Главное правило проекта

Codex должен воспринимать Zeravorn не как набор случайных Minecraft mixins, а как отдельную server-authoritative MOBA game system, работающую поверх Minecraft/Fabric.

Minecraft предоставляет:

- world;
- rendering;
- entity infrastructure;
- networking foundation;
- input;
- audio;
- resource system.

Но правила Zeravorn принадлежат доменной логике Zeravorn.

Поэтому:

```text
Client Intent
    ↓
Network Request
    ↓
Server Validation
    ↓
Zeravorn Domain Service
    ↓
Authoritative State Change
    ↓
Server Event / Delta
    ↓
Client Presentation
```

Эту границу нельзя разрушать ради быстрого прототипа.

---

# 40. Финальная цель

После прохождения всех milestones должна существовать полноценная первая реализация Zeravorn, соответствующая шести проектным документам:

- hybrid lobby;
- hero select;
- 5v5;
- humans + simple bots;
- 5 heroes;
- manual skill leveling;
- basic attacks;
- abilities;
- summoner spells;
- items/shop/economy;
- 3 lanes;
- minions;
- jungle;
- buffs;
- towers;
- throne;
- vision/bushes;
- minimap;
- HUD;
- death/respawn/recall;
- fountain;
- reconnect/AFK bot takeover;
- post-game;
- server-authoritative networking;
- config-driven balance;
- tests and validation;
- placeholder-ready presentation layer for models/animations/VFX/SFX.

Новые крупные механики, которых нет в исходных документах, должны идти уже после v1 как отдельные design decisions.
