# MINECRAFT MOBA - MAP AND MATCH DESIGN v1.0

## Назначение

Документ фиксирует логическую топологию арены, обязательные зоны, приблизительный масштаб, правила перемещения и контракт `MapDefinition`. Геометрия v1.0 намеренно допускает последующую правку после первых игровых тестов без изменения core-кода.

## 1. Главные принципы карты

1. Арена симметричная по gameplay-функциям для Blue и Red.
2. Три линии: TOP, MID, BOT.
3. Между линиями расположен лес обеих команд с несколькими соединительными проходами.
4. Есть центральная нейтральная/спорная область, связывающая стороны.
5. В v1.0 нет Lord, Turtle, Baron, Dragon и других крупных нейтральных командных objectives.
6. Победа достигается через уничтожение трона.
7. Геометрия должна быть читаемой с первого матча: игрок визуально понимает, где lane, jungle entrance, bush, tower, base.
8. Карта может быть приблизительно похожа по общей топологии на классическую MLBB-подобную трехлинейную арену, но не копирует конкретную карту, ассеты или точные координаты.

## 2. Рабочий масштаб

Стартовый рекомендованный bounding box арены: приблизительно **220 x 220 блоков**.

Рабочая система координат:
- логический центр карты: `(0, 64, 0)`;
- Blue base: юго-западная часть;
- Red base: северо-восточная часть;
- MID проходит близко к диагонали между базами;
- TOP огибает северо-западную часть;
- BOT огибает юго-восточную часть.

Точный Y зависит от построенной карты. Gameplay-конфиги не должны предполагать фиксированный Y=64.

Допустимо изменить размер на ±20-25% после первого теста движения/дальности, сохранив относительную топологию.

## 3. Playable Boundary

`playableBoundary` задается polygon/box конфигурацией.

Правила:
- обычное движение не выводит героя за boundary;
- dash проверяет конечную точку;
- Flash может пересечь стену, но не boundary;
- knockback/knock-up clamp/collide на границе;
- projectile за границей удаляется;
- minions/bots при отклонении возвращаются в допустимую зону.

## 4. Базы

Каждая база содержит:
- Hero Spawn;
- Fountain Zone;
- Enemy Fountain Danger Zone;
- Throne;
- подходы от T3 трех линий;
- безопасную визуальную область respawn.

### Blue
`blueSpawn`, `blueFountain`, `blueThrone`.

### Red
`redSpawn`, `redFountain`, `redThrone`.

Spawn point должен иметь:
- position;
- facing direction;
- safe radius;
- достаточное свободное пространство для 5 героев.

## 5. Линии

Каждая линия имеет ordered waypoint path.

### TOP
`blueTopPath[]` и зеркальный/согласованный `redTopPath[]`.

### MID
Наиболее прямой маршрут между базами.

### BOT
Симметричный по функции TOP, но на противоположной стороне.

Каждый path:
- начинается у base exit;
- проходит через T3 -> T2 -> T1;
- встречается с enemy path в центральной части линии;
- заканчивается у enemy structure chain;
- не требует глобального Minecraft pathfinding для штатного движения миньонов.

Рекомендуемый интервал между waypoint: 6-12 блоков, чаще на поворотах.

## 6. Башни и трон

На каждой линии:
- T1 - наружная;
- T2 - средняя;
- T3 - базовая.

Обязательная структура ID:
- `BLUE_TOP_T1`, `BLUE_TOP_T2`, `BLUE_TOP_T3`;
- аналогично MID/BOT/RED.

Каждая structure config:
- id;
- team;
- lane;
- order;
- position;
- facing;
- hitbox;
- attackRadius;
- visionRadius.

Геометрия должна оставлять достаточно пространства для обхода героями и крипами, но не позволять случайно прятаться внутри collision структуры.

## 7. Лес

На стороне каждой команды:
- Red Camp;
- Blue Camp;
- Green Camp A;
- Green Camp B.

Общие/спорные:
- Contested Camp A;
- Contested Camp B.

Для каждого JungleCamp:
- `id`;
- `spawnPoint`;
- `leashZone`;
- `mobDefinitionId`;
- `respawnSeconds`;
- `teamAffinity` или `NEUTRAL`;
- `routeTags[]`.

Red/Blue размещаются достаточно глубоко в своем лесу, чтобы первые баффы не были бесплатным enemy steal без захода во вражескую территорию.

Green camps служат дополнительным фармом и маршрутизацией.

Contested camps находятся ближе к центральным проходам и создают локальный риск, но не являются обязательными team objectives.

## 8. Стартовый jungle route

Для простого лесника/bot profile разрешен фиксированный маршрут, например:

Blue side:
`BLUE_RED -> BLUE_GREEN_A -> BLUE_BLUE -> BLUE_GREEN_B -> nearest lane`

Red side зеркально.

Это стартовый маршрут, а не обязательное правило для реального игрока.

## 9. Кусты

Каждый bush хранится как отдельная `BushZone`.

Рекомендации:
- 1-2 куста на ключевых lane участках;
- кусты у jungle entrances;
- кусты в центральной/спорной зоне;
- не ставить куст так, чтобы он полностью перекрывал единственный проход.

Bush rules задаются Game Requirements/Vision:
- enemy hidden при отсутствии team vision;
- same-bush reveal;
- attack/ability reveal timer;
- союзники всегда видимы своей команде.

## 10. Стены и проходы

`wall geometry` берется из реальных блоков мира; MapDefinition может дополнительно хранить navigation/vision helper zones.

Стена:
- блокирует обычное движение;
- блокирует LOS;
- уничтожает обычные projectile по их wallPolicy;
- Flash может пересечь стену при безопасной конечной точке;
- dash проходит только если конкретная ability это разрешает.

Нужно избегать проходов уже примерно 2.5-3 блоков там, где должны комфортно двигаться крупные герои вроде Loki.

## 11. Центральная зона

В центре карты нет boss objective.

Функции:
- пересечение маршрутов;
- contested camps;
- bush/vision gameplay;
- переходы между MID и боковыми jungle sections.

## 12. Lane/Role использование игроками

Игра не фиксирует жесткий 1-1-2-1.

Ожидаемое первое распределение может быть:
- один герой TOP;
- один MID;
- один BOT;
- один JUNGLE;
- Tank/Roamer помогает любой зоне.

Но это рекомендация, а не серверное ограничение.

Танк может:
- сопровождать стрелка;
- помогать mid;
- заходить в jungle;
- roaming между линиями;
- защищать tower;
- участвовать в gank.

## 13. Bot routes v1.0

Простой BotProfile получает `routeId`.

Примеры:
- `TOP_LANE`;
- `MID_LANE`;
- `BOT_LANE`;
- `ROAM_SIMPLE`;
- `JUNGLE_SIMPLE`.

Lane bot:
1. идет к текущему safe waypoint;
2. останавливается/дерется при допустимой цели;
3. после очистки продолжает путь;
4. с allied wave атакует структуру;
5. после уничтожения T1 переходит к T2, затем T3, затем throne.

ROAM_SIMPLE:
- выбирает заранее заданную последовательность MID -> BOT -> MID -> TOP -> MID;
- не выполняет сложный анализ карты.

## 14. Lobby World

Lobby является отдельной комнатой/миром, а не частью арены.

Обязательные зоны:
- team presentation;
- 5 hero preview/pedestal;
- Ready area/button;
- визуальные подсказки Hero Select и Spell Select;
- безопасная spawn zone.

Игрок может ходить по lobby.

Основные действия доступны также через GUI, чтобы выбор не зависел от физического добегания до pedestal.

После lock lobby state не переносит игровые entities в arena; сервер создает/привязывает MatchSession и загружает arena world.

## 15. MapDefinition contract

Пример:

```json
{
  "id": "arena_v1",
  "playableBoundary": {"type": "polygon", "points": []},
  "blueSpawn": {"pos": [0, 0, 0], "yaw": 0},
  "redSpawn": {"pos": [0, 0, 0], "yaw": 180},
  "blueFountain": {"zone": []},
  "redFountain": {"zone": []},
  "fountainDangerZones": [],
  "blueThrone": {"pos": [0, 0, 0]},
  "redThrone": {"pos": [0, 0, 0]},
  "towers": [],
  "lanePaths": {
    "top": {"blue": [], "red": []},
    "mid": {"blue": [], "red": []},
    "bot": {"blue": [], "red": []}
  },
  "jungleCamps": [],
  "bushZones": [],
  "botRoutes": []
}
```

Фактические координаты заполняются после построения первой версии мира.

## 16. MapValidator

До старта матча validator обязан проверить:
- оба spawn;
- оба fountain;
- оба throne;
- 18 towers (3 линии x 3 башни x 2 команды);
- корректный порядок T1/T2/T3;
- lane paths обеих команд;
- jungle camps;
- leash zones;
- bush zone IDs;
- playable boundary;
- все required points находятся внутри boundary;
- отсутствие duplicate IDs.

Ошибка map config запрещает старт матча и пишет понятную диагностическую причину.

## 17. Definition of Ready для первой карты

Первая arena считается достаточно готовой для gameplay-теста, когда:
- можно пройти из каждой базы по всем 3 линиям;
- minion waypoint routes не застревают;
- jungle camps доступны и не пересекаются leash-zone некорректно;
- towers/throne имеют правильные positions/hitboxes;
- кусты работают с vision;
- Flash/boundary не позволяют выйти за карту;
- 10 героев и minions могут одновременно двигаться без критичных узких мест;
- карта не требует финального art polish для начала тестов.
