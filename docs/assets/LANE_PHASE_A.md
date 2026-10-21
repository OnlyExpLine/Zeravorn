# ZERAVORN — Phase A / Lane creeps

Дата: 2026-10-17. Статус: **первая версия для визуального ревью, не production-ready**.

Созданы Warrior, Ranged и Siege в Blue/Red. Исходники используют одну геометрию на тип и две командные текстуры. Следующие фазы не начаты.

Референс — изображения из задания пользователя. Превью ниже получены растеризацией созданных cuboids и ключевых кадров; AI-изображения не использовались.

## Результаты и исходники

Пути относительно корня репозитория:

| Модель | Editable source | Bones | Cuboids | Треугольники экспорта | Клипы |
|---|---|---:|---:|---:|---:|
| Warrior | `assets/source/blockbench/lane/warrior/lane_warrior.bbmodel` | 15 | 91 | 1092 | 6 |
| Ranged | `assets/source/blockbench/lane/ranged/lane_ranged.bbmodel` | 18 | 91 | 1092 | 5 |
| Siege | `assets/source/blockbench/lane/siege/lane_siege.bbmodel` | 10 | 101 | 1212 | 5 |

Во всех исходниках **0 mesh elements**: только cuboids, включая повёрнутые. glTF содержит треугольники, вычисленные из тех же cuboids. Бюджет валидатора — не более 128 cuboids на модель; фактическая производительность в Minecraft ещё не измерена.

Текстуры: **128×128 RGBA**, nearest filtering. Blue/Red отличаются палитрой брони, ткани, глаз и кристалла. Emissive-маска содержит только два участка атласа — crystal и glow.

Для каждого `kind = warrior / ranged / siege`:

| Содержимое | Путь |
|---|---|
| Blue/Red texture | `assets/source/textures/lane/{kind}/lane_{kind}_{blue|red}.png` |
| Emissive texture | `assets/source/textures/lane/{kind}/lane_{kind}_{blue|red}_emissive.png` |
| Исходные анимации | Внутри `.bbmodel` и `assets/source/blockbench/lane/{kind}/lane_{kind}.animations.json` |
| Cuboid runtime export | `src/main/resources/assets/zeravorn/lane/lane_{kind}.cubes.json` |
| Animation runtime export | `src/main/resources/assets/zeravorn/lane/lane_{kind}.animations.json` |
| glTF с анимациями | `src/main/resources/assets/zeravorn/lane/lane_{kind}_{blue|red}.gltf` |
| Runtime textures/emissive | `src/main/resources/assets/zeravorn/lane/` — те же имена PNG |

glTF содержит встроенную основную текстуру и ссылается на соседний emissive PNG; при переносе сохраняйте их вместе. Эти экспорты сами по себе не подключают модель к Fabric.

## Анимации и sockets

Префикс каждого клипа: `animation.zeravorn.lane_{kind}.`.

- Warrior: `idle`, `walk`, `attack`, `hit`, `death`, `spawn`.
- Ranged: `idle`, `walk`, `attack`, `hit`, `death`.
- Siege: `idle`, `move`, `attack`, `hit`, `death`.

Idle/locomotion зациклены; смерть удерживает финальную позу. Все четыре колеса Siege вращаются на 360° за цикл. Атака включает поворот башенки и отдачу ствола. Root имеет визуальную компенсацию контакта с землёй, вычисленную по всей геометрии; это не перемещение entity и не изменение collision.

| Модель | Socket | Визуальный marker |
|---|---|---|
| Warrior | `weapon_socket`, `shield_socket` | `HIT_FRAME` — 0.38 s |
| Ranged | `projectile_origin` под `weapon` | `PROJECTILE_RELEASE` — 0.65 s |
| Siege | `projectile_origin` под `barrel` | `PROJECTILE_RELEASE` — 0.40 s; `EXTINGUISH_CORE` — 1.10 s в death |

Markers находятся в метаданных клипов `.bbmodel`, отдельном animation JSON и glTF extras. Это **не подключённые игровые callbacks**. Скорость проигрывания и выпуск игрового снаряда должны согласовываться с уже существующим серверным состоянием. Длительности клипов не задают attack interval, damage или другие balance values. Гашение emissive по marker требует runtime-реализации.

## Просмотр

- [Общий lineup](../../assets/source/blockbench/lane/previews/lineup.png): сверху Blue, снизу Red; слева направо Warrior, Ranged, Siege. Одинаковый масштаб камеры для всех.
- [Локальный просмотрщик](../../assets/source/blockbench/lane/previews/lane_review.html): открыть в браузере; выбор модели, команды, клипа, времени и силуэта. Вращение мышью, масштаб колесом. Не требует сервера/CDN.
- [Warrior Blue](../../assets/source/blockbench/lane/warrior/previews/blue_turnaround.png) / [Red](../../assets/source/blockbench/lane/warrior/previews/red_turnaround.png).
- [Ranged Blue](../../assets/source/blockbench/lane/ranged/previews/blue_turnaround.png) / [Red](../../assets/source/blockbench/lane/ranged/previews/red_turnaround.png).
- [Siege Blue](../../assets/source/blockbench/lane/siege/previews/blue_turnaround.png) / [Red](../../assets/source/blockbench/lane/siege/previews/red_turnaround.png).

Порядок панелей turnaround: FRONT → 3/4 → SIDE → BACK. В каждой папке `previews` дополнительно есть отдельные PNG этих видов, `blue_combat.png`, `red_combat.png`, `silhouettes.png`, а также `{clip}_motion.png` с пятью моментами каждого клипа.

В Blockbench открыть `.bbmodel`, перейти в Animate и выбрать клип. По умолчанию faces назначены Blue; для Red выделить все cuboids и применить вторую встроенную текстуру ко всем faces, сохранив UV. Геометрию дублировать не нужно. Импорт через GUI Blockbench в этой сессии не проверен.

PNG используют software Z-buffer. Интерактивный Canvas-просмотрщик сортирует треугольники по глубине; на пересечениях возможны артефакты сортировки. Он предназначен для проверки rig/поз, не для оценки игрового освещения и bloom.

## Проверки

- `node tools/blockbench/lane/validate.mjs` — PASS. 1296 поз: Warrior 486, Ranged 405, Siege 405.
- Проверены UUID, принадлежность элементов bones, родительские связи, UV, размеры cuboids, обязательные клипы, времена/числа ключей, markers/sockets, геометрический бюджет, PNG, конечность posed vertices, контакт с землёй с допуском 0.025 authoring unit, равенство геометрии Blue/Red, glTF buffer/accessor integrity и синтаксис JavaScript просмотрщика.
- Отчёт: `assets/source/blockbench/lane/validation.json`; размеры и список клипов: `statistics.json` в той же папке.
- `.\gradlew.bat --offline --gradle-user-home .gradle-local build test` — BUILD SUCCESSFUL; **95 tests, 0 failures, 0 errors, 0 skipped**.
- Первая попытка через `.gradle-user` остановилась на AccessDenied для `minecraft-server.jar`, в том числе после повторения вне песочницы. Существующий `.gradle-local` успешно собрал проект; версии Fabric/Java/Gradle не менялись.

## Незавершённые требования и ограничения

1. **Интеграция и проверка в Minecraft отсутствуют.** В проверенном checkout нет lane entity renderer/синхронизации render state. Файлы упаковываются в resources, но это не делает их видимыми в игре. Состояние явно описано в `assets/source/blockbench/lane/integration.json`.
2. In-game debug gallery, освещение, реальные тени, emissive-pass, дистанционная читаемость, FPS и масштаб рядом с игровым героем ещё не проверены. Offline gallery не заменяет dev client.
3. 16 authoring units = 1 блок, направление вперёд −Z. Пехота высотой примерно 1.45 блока с плюмажем; Siege примерно 1.26 блока высотой и 1.22 блока шириной. Gameplay hitboxes не менялись. В lineup нет модели игрового героя; нужен отдельный in-game scale test.
4. Детализация и пластика проще приложенного reference. Броня использует крупные плоскости и общий материал-атлас; нет индивидуально нарисованного орнамента, характерного для reference. Кристалл Siege пока ступенчатый cuboid, не гранёный mesh.
5. **Clipping-free не заявлен.** Слои брони намеренно пересекаются. В крайних атакующих/смертельных позах возможны пересечения наплечников, оружия и корпуса. Нужна покадровая ручная проверка в Blockbench и игре.
6. Ranged требует доводки хвата и натяжения: руки двигаются, две половины тетивы имеют pivots у концов лука, но точное совпадение кисти с центром тетивы не обеспечено IK. В предельном натяжении возможен небольшой зазор между половинами тетивы. Стрела в руке/на тетиве пока не моделируется.
7. Walk — простой keyframed цикл, без IK/foot locking; возможны скольжение стоп и неестественный перенос веса. Смерть — контролируемое сложение позы, нуждается в художественном ревью.
8. Геометрические и build-тесты не доказывают визуальное соответствие. Эта версия не считается принятой по критериям раздела 79 задания.

## Что проверить владельцу

Сравнить силуэты с reference: Warrior — крупный щит/меч, Ranged — более узкий корпус/лук, Siege — широкая платформа/колёса/ствол. Проверить размер головы и наплечников, читаемость Blue/Red, пропорции Siege относительно пехоты, оба combat preview и motion sheets. Отдельно решить, достаточна ли детализация или требуется ещё один художественный проход Phase A.

После этого отчёта работа останавливается в рамках Phase A согласно разделам 62, 80 и 81 задания. Phase B не начинать без подтверждения пользователя. Визуальное подтверждение этих превью само по себе не закрывает перечисленные технические требования.

## Воспроизведение и сохранность

```powershell
node tools/blockbench/lane/generate.mjs
node tools/blockbench/lane/validate.mjs
.\gradlew.bat --offline --gradle-user-home .gradle-local build test
```

Генератор перед заменой существующего lane-набора копирует source models, source textures и runtime exports в `assets/backups/lane/<timestamp>/`. Ручные изменения `.bbmodel` сохранятся в backup, но новый основной файл будет построен из генератора. Генератор и валидатор независимы от файлов героев. Существующие пользовательские изменения героев и Java-кода не редактировались.
