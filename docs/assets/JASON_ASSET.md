# Jason — Blockbench pipeline v2

Обновлено 2026-09-06. Референс: первая картинка владельца, сохранённая в `assets/source/references/jason_concept_reference.png`. Это low-poly реконструкция; скрытые поверхности и промежуточные позы восстановлены по видимым формам. Точное художественное совпадение и визуальное принятие владельцем пока не подтверждены.

## Открыть готового Jason

1. В Blockbench выбрать **File → Open Model / Файл → Открыть модель**.
2. Открыть `assets/source/blockbench/jason.bbmodel` из Zeravorn. Открывать обновлённый файл, а не старую вкладку. Формат — **Generic Model / free**, со свободными полигональными мешами.
3. Текстура уже встроена как PNG/base64; назначать её граням вручную не нужно. Внешняя копия PNG также сохранена рядом.
4. Перейти в **Animate / Анимация**, выбрать `jason.*`, нажать Play. Для исходной позы остановить клип и перейти в Edit.
5. Руки: открыть `jason_first_person.bbmodel` и выбрать `jason.fp.idle`. Bind pose этого рига служит исходной позой, idle — боевой позицией перед камерой.

Дополнительный просмотр: открыть `assets/source/blockbench/previews/jason_viewer.html` браузером. В нём встроены реальные модели, текстуры и клипы; интернет, сервер, npm и CDN не требуются. Мышь вращает модель, колесо меняет масштаб; доступны выбор клипа и ползунок времени. Для первого лица камера смотрит по −Z.

## Исходники и команды

| Путь относительно проекта | Содержание |
|---|---|
| `assets/source/blockbench/jason.bbmodel` | Third-person модель, риг, 15 клипов |
| `assets/source/blockbench/jason_first_person.bbmodel` | Руки, риг, 11 клипов |
| `assets/source/blockbench/textures/jason_atlas.png` | Атлас тела, 256×256 RGBA |
| `assets/source/blockbench/textures/jason_first_person_atlas.png` | Атлас рук, 256×256 RGBA |
| `assets/source/blockbench/jason.events.json` | Имена/тайминги/gates маркеров |
| `assets/source/blockbench/exports/*.gltf` | glTF 2.0 тела и рук, встроенные buffers/PNG/анимации |
| `assets/source/blockbench/previews/` | Рендеры и локальный просмотрщик |
| `tools/generate_jason_blockbench.mjs` | Генерация моделей, атласов, событий и glTF |
| `tools/preview_jason_blockbench.mjs` | Превью из сохранённых `.bbmodel` |
| `tools/validate_jason_blockbench.mjs` | Проверки ассетов и регрессий |
| `tools/jason/geometry.mjs` | Форма и иерархия |
| `tools/jason/animations.mjs` | Позы, фазы, тайминги |
| `tools/jason/mesh.mjs` | Геометрические примитивы, UUID, UV, палитра, PNG |
| `tools/jason/runtime.mjs`, `gltf.mjs` | Преобразования и экспорт |
| `tools/jason/preview.mjs`, `viewer.js`, `validation.mjs` | Отрисовка, просмотр и проверки |

Node.js 20+, без дополнительных пакетов. Из корня проекта:

```powershell
node tools/generate_jason_blockbench.mjs
node tools/preview_jason_blockbench.mjs
node tools/validate_jason_blockbench.mjs
```

Команды пишут только под `assets/source/blockbench`; Gradle и его кэш не запускаются. Данные детерминированы, без времени генерации или случайного seed. После изменения исходных модулей выполнять команды в указанном порядке. Regeneration заменяет `.bbmodel`, PNG, glTF и превью; ручные варианты предварительно сохранять под другим именем.

## Геометрия и риг

16 единиц = 1 блок; вперёд −Z, вверх +Y, ноги на Y=0, верх волос около Y=36 (≈2,25 блока). Gameplay hitbox `0.75 × 2.25` остаётся в hero definitions. 170 мешей / 7205 треугольников / 28 костей у тела; 66 / 2216 / 10 у рук. Используется rigid hierarchy, без smooth skin/weight painting.

Торс — единая поверхность с рельефом груди/пресса. Отдельно сформированы лицо, волосы, шрамы, пальцы, костяшки, витки бинтов, складки и неровные края ткани. Атлас содержит оригинальную палитру кожи, ткани, бинтов, волос, глаз, губ и шрамов. Каждая грань имеет UV; повторное использование ячеек материала намеренное. Концепт-арт не используется как текстура на плоской геометрии.

```text
root
├── body
│   ├── pelvis
│   │   ├── leg_left → shin_left → foot_left
│   │   ├── leg_right → shin_right → foot_right
│   │   └── belt → belt_tail_left / belt_tail_right
│   └── chest
│       ├── head → hair / scar_face
│       ├── scar_chest / scar_back
│       ├── arm_left → forearm_left → fist_left → socket_fist_left
│       ├── arm_right → forearm_right → fist_right → socket_fist_right
│       └── socket_chest
└── socket_ground
```

Бинты следуют своим предплечьям/кулакам, волосы и лицевой шрам — голове, пояс — тазу. UUID мешей и костей раздельные. First-person hierarchy: `root → camera_anchor → arm_* → forearm_* → fist_* → socket_fist_*`.

## Анимации

| ID | Длина | Подача |
|---|---:|---|
| `jason.idle` | 3,00 с, loop | Дыхание грудью, небольшие движения плеч |
| `jason.walk` | 1,12 с, loop | Тяжёлая походка, колени, противоположный ход рук |
| `jason.run` | 0,72 с, loop | Наклон, широкий шаг, работа плеч |
| `jason.basic_1` | 1,00 с | Правый хук |
| `jason.basic_2` | 1,00 с | Левый хук |
| `jason.basic_3` | 1,00 с | Тяжёлый прямой |
| `jason.q` | 0,95 с | Charge → punch → возврат кулака/энергии |
| `jason.e_dash_1` | 0,48 с | Силовой dash pose |
| `jason.e_dash_2` | 0,65 с | Второй dash и восходящий collision strike |
| `jason.r` | 1,60 с | Сжатие, подъём, flight pose, глубокая посадка, recovery |
| `jason.hit` | 0,42 с | Резкая реакция корпуса и головы |
| `jason.stun` | 1,60 с, loop | Опущенная голова, потеря устойчивости |
| `jason.root` | 1,40 с, loop | Верхняя боевая стойка, неподвижные ноги |
| `jason.death` | 1,60 с, hold | Потеря равновесия, падение, удержание лежачей позы |
| `jason.recall` | 5,00 с, hold | Подъём рук, сосредоточенная поза, завершение |

First person с префиксом `jason.fp.`: `idle`, `basic_1`, `basic_2`, `basic_3`, `q`, `e`, `e_dash_1`, `e_dash_2`, `r`, `hit`, `recall`. `e` совместим с E1, E2 имеет отдельный клип. F отсутствует.

Все basic hit = **0,25 с**, Q hit = **0,30 с**, Recall = **5 с** согласно документам. Прочие длины/позы — authoring presentation values, не новые gameplay lock/delay. Скорость basic привязывается к server AttackInterval. Loop endpoints совпадают; death не возвращается в стойку.

Корень стоит на месте. В R предусмотрен небольшой локальный подъём корпуса на 3,5 единицы (0,219 блока); поступательное перемещение и полная траектория остаются за server adapter. Этот presentation offset можно убрать при интеграции, если серверная траектория уже даёт нужный подъём. Посадка и лежачая поза привязаны к земле по геометрии.

## Маркеры и синхронизация

Имена и server gates хранятся в `jason.events.json` и дополнительном поле `zeravorn_event_markers`. В `.bbmodel` также есть нативные цветные timeline markers. Blockbench может отбросить пользовательские поля при ручном сохранении, поэтому sidecar — основной контракт интеграции.

| ID | Условие использования |
|---|---|
| `basic_hit` | Подтверждённый hit, поза на 0,25 с |
| `q_charge` | Подтверждённый старт Q |
| `q_hit` | Подтверждённый hit, поза на 0,30 с |
| `q_heal_return` | Подтверждённое лечение |
| `e_dash` | Подтверждённое начало рывка |
| `e2_hit` | Фактическая server collision |
| `r_launch` | Подтверждённый launch |
| `r_flight` | Server flight state |
| `r_impact` | Фактическое server landing |

Маркеры не исполняют скрипты и не применяют damage/heal/CD/CC/movement. Пыль, тёмно-красная энергия, ударные кольца, THUMP/BOOM, heartbeat и wind остаются точками подключения VFX/SFX; готовые particle effects и audio files не добавляются.

## Экспорт и интеграция

`HeroRenderer`/`AnimationStateBridge` в проекте ещё отсутствуют: эти ассеты пока не подключены к Minecraft. Для них нужен **mesh-capable Fabric renderer**; vanilla Java cube/block/item export потеряет форму. Выбор runtime не зафиксирован документацией, зависимости Gradle не менялись.

glTF 2.0 сохраняет rigid mesh nodes под анимируемыми костями, материал и все клипы; PNG и binary buffer встроены. Корневой scale 1/16 переводит единицы в блоки/метры. Euler-движение дискретизировано в quaternion tracks с сохранением точных ключевых моментов. Loop/hold и event gates находятся в metadata и должны обрабатываться client adapter. E2/R синхронизируются по collision/landing, presentation сбрасывается при cancel/death/resync.

Проверенные первоисточники: [Blockbench project codec 4.12.6](https://github.com/JannisX11/blockbench/blob/v4.12.6/js/io/formats/bbmodel.js), [mesh serialization](https://github.com/JannisX11/blockbench/blob/v4.12.6/js/outliner/mesh.js), [animation coordinates](https://github.com/JannisX11/blockbench/blob/v4.12.6/js/animations/timeline_animators.js). Используется нативная схема 4.10.

## Проверки и ограничения

Валидатор проверяет JSON, тип проекта, UUID, потерянные/повторно привязанные меши, hierarchy, треугольники, UV, PNG, embedded/external равенство, ключи/петли, обязательные ID, Balance timings, death hold, контакт с землёй, glTF buffers/samplers и воспроизводимость. Пять negative regressions: UUID collision, пустая текстура, неверная кость, Q на 0,68 с и возврат из death.

347 first-person поз проверяются triangle/rectangle тестом: центральная область ±12% horizontal NDC и ±10% vertical NDC свободна при вертикальном FOV 70° и 16:9. Иные FOV/aspect и реальная камера проверяются при интеграции.

Превью рендерят меши, UV и ключи **из сохранённых `.bbmodel`**. Просмотрены общий вид, лицо, turnaround, ключевые позы, R и руки. Лист `jason_animations.png` идёт слева направо и сверху вниз в порядке таблицы; `jason_r_phases.png` — 0,18 / 0,45 / 0,86 / 1,30 с.

Blockbench GUI и Fabric renderer в этой среде не запускались; WebGL viewer проверен статически, это не GUI/import smoke test. Gradle build/test оставлены владельцу по его указанию. Визуальное сходство с референсом не может быть доказано структурным валидатором.
