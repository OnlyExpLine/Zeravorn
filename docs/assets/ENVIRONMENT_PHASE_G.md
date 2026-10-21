# ZERAVORN — Phase G / Окружение

Дата: 2026-10-20. Первая версия для отложенного визуального ревью.

Создано моделей: **21**. Статус: **исходники и экспорты подготовлены; к игровому рендереру не подключены**.

| Модель | Варианты | Кубоиды | Клипы | Высота, блоки |
|---|---|---:|---:|---:|
| bush_small | forest | 45 | 0 | 1.15 |
| bush_medium | forest | 45 | 0 | 1.45 |
| bush_large | forest | 45 | 0 | 1.75 |
| bush_corner | forest | 40 | 0 | 1.57 |
| tree_a | forest | 26 | 0 | 4.85 |
| tree_b | forest | 26 | 0 | 5.47 |
| tree_c | forest | 26 | 0 | 6.10 |
| rock_small | forest | 10 | 0 | 0.59 |
| rock_medium | forest | 10 | 0 | 1.03 |
| rock_large | forest | 10 | 0 | 1.59 |
| rock_cluster | forest | 15 | 0 | 1.12 |
| ruin_column_full | forest | 11 | 0 | 3.56 |
| ruin_column_broken | forest | 12 | 0 | 1.81 |
| ruin_arch | forest | 21 | 0 | 3.88 |
| ruin_wall_small | forest | 16 | 0 | 1.48 |
| ruin_base | forest | 3 | 0 | 0.75 |
| ruin_stairs | forest | 5 | 0 | 1.25 |
| ruin_rubble | forest | 40 | 0 | 0.46 |
| lantern_post | blue, red | 19 | 1 | 3.08 |
| lantern_wall | blue, red | 18 | 1 | 1.69 |
| lantern_hanging | blue, red | 16 | 1 | 1.28 |

Для каждой модели сохранены `.bbmodel`, PNG 128×128 и emissive PNG, cuboid/animation JSON, glTF, четыре ракурса, силуэты и motion sheets для клипов.

- [Просмотрщик](../../assets/source/blockbench/environment/previews/review.html)
- [Общий рендер](../../assets/source/blockbench/environment/previews/lineup.png)
- [Отчёт валидации](../../assets/source/blockbench/environment/validation.json)
- [Статус интеграции](../../assets/source/blockbench/environment/integration.json)

Техническая проверка: PASS; 123 sampled poses. Проверены обязательный набор, UUID, иерархия, UV, бюджеты, ключевые кадры, замыкание циклов, source/export parity, PNG, glTF buffers и синтаксис просмотрщика.

## Ограничения

Визуальное соответствие референсам и проверка в GUI Blockbench/Minecraft отложены. Просмотрщик показывает реальную геометрию и ключевые кадры, но не частицы и динамический emissive. Не подтверждены отсутствие взаимных пересечений деталей и производительность в игре.

Gameplay/config, collision, hitboxes, damage и серверные правила не изменены. Наличие экспортов в resources не означает автоматическое отображение в Fabric. Требуются регистрация рендерера, выбор состояний и обработка визуальных событий.

Повторная сборка: `node tools/blockbench/world/build.mjs`. Перед генерацией предыдущие файлы сохраняются в `assets/backups/<category>/<timestamp>/`.

21 модуль: четыре куста, три дерева, четыре камня, семь руин и три фонаря. Фонари имеют blue/red варианты и idle sway. Декоративные модели сами по себе не задают collision и границы gameplay-кустов.
