# ZERAVORN — Phase B / Jungle creeps

Дата: 2026-10-17. **Первая версия для визуального ревью; не production-ready.**

После команды пользователя «давай дальше» выполнен проход Phase B: Red Buff, Blue Buff, Green 1 и Green 2. Референс — приложенные изображения и разделы 10–18 master prompt. Башни, боссы и остальные фазы не начаты. Открытые требования интеграции Phase A остаются открытыми; lane-файлы не менялись.

## Созданные модели

| Asset | Bones | Cuboids | Треугольники | Клипы | Высота, блоки |
|---|---:|---:|---:|---:|---:|
| Red Buff | 18 | 105 | 1260 | 6 | 2.04 |
| Blue Buff | 18 | 98 | 1176 | 6 | 2.01 |
| Green 1 | 18 | 103 | 1236 | 7 | 2.03 |
| Green 2 | 18 | 110 | 1320 | 7 | 3.14 с рогами |

Все четыре существа имеют собственную геометрию. Red Buff — высокий магматический громила с лавовыми швами, Blue Buff — низкий кристаллический зверь с массивными передними лапами, Green 1 — лесной таран с широкими кулаками и короткими разветвлёнными рогами, Green 2 — светлый древесно-каменный страж с высокими ветвистыми рогами.

**Mesh elements в `.bbmodel`: 0.** Использованы cuboids и rotated cuboids; glTF содержит их треугольный экспорт. Texture resolution всех моделей — **128×128 RGBA**, nearest filtering. На модель одна текстура и одна отдельная emissive-маска. Командные Blue/Red варианты не создавались: это нейтральные лесные существа, а имена Red/Blue Buff обозначают их elemental identity.

## Файлы

| Модель | Editable source |
|---|---|
| Red Buff | [jungle_red_buff.bbmodel](../../assets/source/blockbench/jungle/red_buff/jungle_red_buff.bbmodel) |
| Blue Buff | [jungle_blue_buff.bbmodel](../../assets/source/blockbench/jungle/blue_buff/jungle_blue_buff.bbmodel) |
| Green 1 | [jungle_green_1.bbmodel](../../assets/source/blockbench/jungle/green_1/jungle_green_1.bbmodel) |
| Green 2 | [jungle_green_2.bbmodel](../../assets/source/blockbench/jungle/green_2/jungle_green_2.bbmodel) |

Для `kind = red_buff / blue_buff / green_1 / green_2`, относительно корня репозитория:

| Содержимое | Путь |
|---|---|
| Source texture | `assets/source/textures/jungle/{kind}/jungle_{kind}.png` |
| Source emissive | `assets/source/textures/jungle/{kind}/jungle_{kind}_emissive.png` |
| Source animations | Встроены в `.bbmodel` и `assets/source/blockbench/jungle/{kind}/jungle_{kind}.animations.json` |
| Runtime cuboids | `src/main/resources/assets/zeravorn/jungle/jungle_{kind}.cubes.json` |
| Runtime animations | `src/main/resources/assets/zeravorn/jungle/jungle_{kind}.animations.json` |
| glTF | `src/main/resources/assets/zeravorn/jungle/jungle_{kind}.gltf` |
| Runtime texture/emissive | `src/main/resources/assets/zeravorn/jungle/jungle_{kind}.png`, `jungle_{kind}_emissive.png` |
| VFX sprites | `assets/source/textures/jungle/vfx/shockwave.png`, `root_eruption.png` — 32×32, прозрачный фон |
| Runtime VFX assets | `src/main/resources/assets/zeravorn/jungle/vfx/` и `jungle.vfx.json` |

В glTF основная текстура встроена, emissive PNG расположен рядом и должен переноситься вместе с glTF. Cuboid/animation JSON используют описанный в файлах формат Zeravorn; vanilla Minecraft не загружает этот формат автоматически.

## Анимации

Префикс: `animation.zeravorn.jungle_{kind}.`.

- Все модели: `idle`, `walk`, `attack`, `hit`, `death`, `leash_return`.
- Green 1/2 дополнительно: `special`.
- Idle, walk и leash_return зациклены. Death удерживает последнюю позу.
- Blue Buff атакует обеими передними лапами; у Red Buff основная атака — тяжёлый удар правой рукой.
- Green 1 special: подготовка с наклоном и вынос обеих рук вперёд; marker `KNOCKBACK_SHOCKWAVE`.
- Green 2 special: подъём обеих рук и удар вниз; marker `ROOT_ERUPTION`.
- Root compensation удерживает posed geometry над землёй; это визуальная поправка, не server movement.

## Sockets и VFX

Общие bones: `root`, `pelvis`, `torso`, `head`, `upper_arm_left/right`, `forearm_left/right`, `hand_left/right`, `leg_left/right`, `foot_left/right`, `energy_core`, `attack_origin`, `vfx_origin`. Дополнительная кость: `lava_core`, `crystal_crown`, `horns` или `antlers` соответственно.

| Marker | Время в исходном клипе | Socket |
|---|---|---|
| Red Buff `HIT_FRAME` | 0.72 s в attack | `attack_origin` |
| Blue Buff `HIT_FRAME` | 0.62 s в attack | `attack_origin` |
| Green 1/2 `HIT_FRAME` | 0.65 s в attack | `attack_origin` |
| Green 1 `KNOCKBACK_SHOCKWAVE` | 0.80 s в special | `vfx_origin` |
| Green 2 `ROOT_ERUPTION` | 0.90 s в special | `vfx_origin` |
| `SPECIAL_WINDUP` | 0.25 s в special | `energy_core` |
| `CORE_PULSE` | 1.50 s в idle | `energy_core` |
| `CORE_EXTINGUISH` | 1.40 s в death | `energy_core` |

Эти события находятся в metadata `.bbmodel`, animation JSON и glTF extras; это не подключённые игровые callbacks. VFX sprites отделены от основной геометрии. `jungle.vfx.json` описывает только визуальный lifetime и способ отображения, без damage/radius/CC values. Пульсация и гашение emissive, расширение ударной волны и выброс корней требуют runtime-подключения и в PNG/model viewer сейчас не проигрываются.

Сопоставление с существующим `src/main/resources/config_defaults/jungle.json`: Red Buff → `RED`, Blue Buff → `BLUE`, Green 1 → `GREEN_A`, Green 2 → `GREEN_B`. `CONTESTED_A/B` не создавались: они не входят в заданную Phase B. Gameplay values и hitboxes не менялись. Скорость анимации должна адаптироваться к серверным событиям, а не определять damage/attack interval/leash.

## Реальные превью

Рендеры созданы из сохранённой cuboid geometry и ключевых кадров. AI-generated preview не использовались.

- [Общий lineup](../../assets/source/blockbench/jungle/previews/lineup.png): слева направо Red Buff, Blue Buff, Green 1, Green 2; одинаковый масштаб камеры.
- [Силуэты](../../assets/source/blockbench/jungle/previews/silhouette_lineup.png).
- [Сравнение масштаба](../../assets/source/blockbench/jungle/previews/scale_comparison.png): последняя фигура — нейтральный измерительный манекен высотой 1.8 блока, не игровой герой.
- [Локальный просмотрщик](../../assets/source/blockbench/jungle/previews/jungle_review.html): выбор модели, анимации, времени; вращение мышью, масштаб колесом, силуэт.
- [Red Buff: четыре ракурса](../../assets/source/blockbench/jungle/red_buff/previews/turnaround.png).
- [Blue Buff: четыре ракурса](../../assets/source/blockbench/jungle/blue_buff/previews/turnaround.png).
- [Green 1: четыре ракурса](../../assets/source/blockbench/jungle/green_1/previews/turnaround.png).
- [Green 2: четыре ракурса](../../assets/source/blockbench/jungle/green_2/previews/turnaround.png).
- [Green 1 special](../../assets/source/blockbench/jungle/green_1/previews/special_motion.png), [Green 2 special](../../assets/source/blockbench/jungle/green_2/previews/special_motion.png).

В `turnaround.png` порядок FRONT → 3/4 → SIDE → BACK. В каждой папке `previews/` также сохранены отдельные ракурсы, silhouette sheet, `attack_pose.png`, motion sheets каждого клипа и `special_pose.png` у Green 1/2. PNG и интерактивный canvas используют одинаковый software Z-buffer. Просмотрщик ограничивает внутреннее разрешение шириной 960 px; его CPU-скорость не отражает производительность игрового рендерера.

## Проверки

- Asset validation — **PASS**: 26 клипов, **2106 поз** (81 поза каждого клипа).
- Проверены UUID, иерархия, UV, принадлежность cuboids костям, геометрический бюджет, отличие геометрии четырёх существ, обязательные клипы/markers, времена ключей, finite transforms, контакт с землёй, замыкание циклов, PNG/emissive, равенство source/export, glTF buffers/accessors, JavaScript syntax просмотрщика и соответствие gameplay ID конфигу.
- [Машиночитаемый отчёт](../../assets/source/blockbench/jungle/validation.json), [статистика](../../assets/source/blockbench/jungle/statistics.json).
- Gradle: `.\gradlew.bat --offline --gradle-user-home .gradle-local build test` — **BUILD SUCCESSFUL**.
- Tests: **95, failures 0, errors 0, skipped 0**.
- SHA-256 исходных runtime-ресурсов совпадают с `build/resources/main/assets/zeravorn/jungle/`: несовпадений 0.
- GUI Blockbench не проверен. Первый headless Edge screenshot подтвердил запуск, но выявил артефакты сортировки граней. После этого просмотрщик переведён на Z-buffer; синтаксис обновлённого JavaScript проверен валидатором. Повторный headless-снимок обновлённого просмотрщика не получен, в том числе с отдельным временным профилем; финальная браузерная отрисовка не подтверждена снимком. PNG-превью отдельно отрендерены и просмотрены.

## Открытые требования

1. **Minecraft renderer / render-state sync не подключены; dev client не проверен.** Build подтверждает упаковку ресурсов, но не их отображение. In-game gallery, shadows, emissive-pass, дальняя читаемость, реальный масштаб рядом с героем и FPS остаются открытыми.
2. Отдельные VFX-спрайты и metadata подготовлены, но частицы ещё не зарегистрированы в Fabric. Поэтому shockwave/root eruption и pulse/fade не видны в текущем проигрывателе моделей.
3. **Clipping-free не заявлен.** Каменные слои намеренно пересекаются; возможны пересечения предплечий с корпусом и рогов с плечами в крайних позах. Проверка контакта с землёй не проверяет взаимные пересечения частей.
4. Locomotion — keyframed, без IK/foot locking; возможны скольжение стоп/лап и недостаточно естественный перенос веса. Leash_return — только визуальный цикл, не поведение возврата на сервере.
5. Детализация проще reference: крупные плоскости, общий материал-атлас, ступенчатые cuboid-кристаллы, упрощённые корни и мох. Визуальная идентичность читается, но близость к референсу требует пользовательского художественного ревью.
6. Нейтральная фигура масштаба не заменяет тест рядом с настоящим героем. Высота Green 2 обусловлена рогами, а не большим корпусом.

Статус интеграции и mapping записан в `assets/source/blockbench/jungle/integration.json`.

## Что проверить владельцу

Сравнить силуэты, объём корпуса, пропорции головы/лап, читаемость лавовых швов, количество/форму синих кристаллов, различие Green 1/2 и масштаб рогов Green 2. Отдельно просмотреть special motion sheets: понятно ли отталкивание у Green 1 и удар в землю у Green 2. Уточнить, достаточно ли детализации или нужен следующий художественный проход Phase B.

После отчёта остановка согласно фазовому порядку задания. Phase C — Towers — не начинать без подтверждения пользователя. Подтверждение визуального направления не закрывает перечисленные технические ограничения.

## Воспроизведение

```powershell
node tools/blockbench/jungle/generate.mjs
node tools/blockbench/jungle/validate.mjs
.\gradlew.bat --offline --gradle-user-home .gradle-local build test
```

Для обновления только HTML-просмотрщика из сохранённых `.bbmodel`: `node tools/blockbench/jungle/refresh_viewer.mjs`. Предыдущий HTML сохраняется в `assets/backups/jungle-viewer/<timestamp>/`; модели и runtime exports не затрагиваются.

При повторной генерации весь предыдущий jungle-набор — source models, textures, runtime — сохраняется в `assets/backups/jungle/<timestamp>/`. Ручные правки `.bbmodel` окажутся в backup; основной файл будет заново построен по генератору. Java, balance, герои, lane-assets и версии зависимостей не изменялись.
