# ZERAVORN — Phase F / Снаряды и VFX

Дата: 2026-10-20. Первая версия для отложенного визуального ревью.

Создано моделей: **6**. Статус: **исходники и экспорты подготовлены; к игровому рендереру не подключены**.

| Модель | Варианты | Кубоиды | Клипы | Высота, блоки |
|---|---|---:|---:|---:|
| projectile_basic | basic | 3 | 1 | 0.04 |
| projectile_ice | blue | 8 | 1 | 0.28 |
| projectile_fire | fire | 6 | 1 | 0.26 |
| projectile_poison | poison | 6 | 1 | 0.21 |
| tower_shot | blue, red | 8 | 1 | 0.38 |
| throne_shot | blue, red | 12 | 1 | 0.59 |

Для каждой модели сохранены `.bbmodel`, PNG 128×128 и emissive PNG, cuboid/animation JSON, glTF, четыре ракурса, силуэты и motion sheets для клипов.

- [Просмотрщик](../../assets/source/blockbench/projectiles/previews/review.html)
- [Общий рендер](../../assets/source/blockbench/projectiles/previews/lineup.png)
- [Отчёт валидации](../../assets/source/blockbench/projectiles/validation.json)
- [Статус интеграции](../../assets/source/blockbench/projectiles/integration.json)

Техническая проверка: PASS; 246 sampled poses. Проверены обязательный набор, UUID, иерархия, UV, бюджеты, ключевые кадры, замыкание циклов, source/export parity, PNG, glTF buffers и синтаксис просмотрщика.

## Ограничения

Визуальное соответствие референсам и проверка в GUI Blockbench/Minecraft отложены. Просмотрщик показывает реальную геометрию и ключевые кадры, но не частицы и динамический emissive. Не подтверждены отсутствие взаимных пересечений деталей и производительность в игре.

Gameplay/config, collision, hitboxes, damage и серверные правила не изменены. Наличие экспортов в resources не означает автоматическое отображение в Fabric. Требуются регистрация рендерера, выбор состояний и обработка визуальных событий.

Повторная сборка: `node tools/blockbench/world/build.mjs`. Перед генерацией предыдущие файлы сохраняются в `assets/backups/<category>/<timestamp>/`.

Локальное направление полёта: -Z. Основная геометрия — 3–12 кубоидов; trail, impact, shield/heal/danger sprites и параметры визуального lifetime лежат в `world.vfx.json`. Скорость, урон, радиус и столкновения задаются сервером.
