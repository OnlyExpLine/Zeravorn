# ZERAVORN — Phase E / Фонтан

Дата: 2026-10-20. Первая версия для отложенного визуального ревью.

Создано моделей: **1**. Статус: **исходники и экспорты подготовлены; к игровому рендереру не подключены**.

| Модель | Варианты | Кубоиды | Клипы | Высота, блоки |
|---|---|---:|---:|---:|
| fountain | blue, red | 91 | 3 | 3.78 |

Для каждой модели сохранены `.bbmodel`, PNG 128×128 и emissive PNG, cuboid/animation JSON, glTF, четыре ракурса, силуэты и motion sheets для клипов.

- [Просмотрщик](../../assets/source/blockbench/fountain/previews/review.html)
- [Общий рендер](../../assets/source/blockbench/fountain/previews/lineup.png)
- [Отчёт валидации](../../assets/source/blockbench/fountain/validation.json)
- [Статус интеграции](../../assets/source/blockbench/fountain/integration.json)

Техническая проверка: PASS; 123 sampled poses. Проверены обязательный набор, UUID, иерархия, UV, бюджеты, ключевые кадры, замыкание циклов, source/export parity, PNG, glTF buffers и синтаксис просмотрщика.

## Ограничения

Визуальное соответствие референсам и проверка в GUI Blockbench/Minecraft отложены. Просмотрщик показывает реальную геометрию и ключевые кадры, но не частицы и динамический emissive. Не подтверждены отсутствие взаимных пересечений деталей и производительность в игре.

Gameplay/config, collision, hitboxes, damage и серверные правила не изменены. Наличие экспортов в resources не означает автоматическое отображение в Fabric. Требуются регистрация рендерера, выбор состояний и обработка визуальных событий.

Повторная сборка: `node tools/blockbench/world/build.mjs`. Перед генерацией предыдущие файлы сохраняются в `assets/backups/<category>/<timestamp>/`.

Состояния: idle/heal/danger; четыре плавающих осколка. Центральный луч и частицы лечения требуют runtime.
