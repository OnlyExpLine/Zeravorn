# ZERAVORN — Phase C / Towers

Дата: 2026-10-20. Первая версия для отложенного визуального ревью.

Созданы три самостоятельные модели: T1 — узкая башня, T2 — усиленная башня с боковыми опорами, T3 — массивная башня с архитектурной короной и дополнительными кристаллами. Для каждой есть blue/red текстуры на общей геометрии.

| Модель | Кости | Кубоиды | Треугольники | Клипы | Высота, блоки |
|---|---:|---:|---:|---:|---:|
| tower_t1 | 12 | 58 | 696 | 6 | 4.75 |
| tower_t2 | 12 | 76 | 912 | 6 | 5.69 |
| tower_t3 | 12 | 100 | 1200 | 6 | 6.80 |

Для каждого уровня сохранены `.bbmodel`, встроенные текстуры, отдельные PNG 128×128 и emissive PNG, cuboid/animation JSON и blue/red glTF. Исходники используют только cuboids, включая повёрнутые. Экспорты расположены в `src/main/resources/assets/zeravorn/towers/`.

Состояния: `idle`, `target`, `attack`, `hit`, `low_hp`, `destroy`. Имена имеют префикс `animation.zeravorn.tower_tN.`. Разрушение оставляет неподвижное основание и повреждённую верхнюю часть. Attack содержит `PROJECTILE_RELEASE`; destroy — `CORE_OVERLOAD` и `DEBRIS_BURST`. Эти markers — визуальные metadata, не игровые callbacks.

- [Общий рендер Blue/Red, T1 → T2 → T3](../../assets/source/blockbench/towers/previews/lineup.png)
- [Интерактивный просмотрщик](../../assets/source/blockbench/towers/previews/tower_review.html)
- [T1](../../assets/source/blockbench/towers/t1/tower_t1.bbmodel)
- [T2](../../assets/source/blockbench/towers/t2/tower_t2.bbmodel)
- [T3](../../assets/source/blockbench/towers/t3/tower_t3.bbmodel)
- [Техническая проверка](../../assets/source/blockbench/towers/validation.json)
- [Статус интеграции](../../assets/source/blockbench/towers/integration.json)

## Проверки и ограничения

Структурная валидация: PASS, 18 клипов и 738 sampled poses. Проверены UUID, иерархия, UV, геометрический бюджет, finite transforms, контакт с землёй, замыкание циклов, соответствие исходников и экспортов, одинаковая геометрия команд, PNG, glTF buffers и синтаксис просмотрщика.

Превью отрендерены из фактических моделей с их текстурами; AI-изображения не использовались. Визуальная приёмка пользователем, GUI Blockbench, игровой масштаб, взаимные пересечения деталей и FPS остаются открытыми.

**Новые башни ещё не подключены к Minecraft.** Временные блочные постройки на карте остаются. Требуются structure renderer, синхронизация состояний/прицеливания, свечение и обработка VFX markers. Клип target показывает пример поворота, а реальный угол должен задаваться игровым target. Idle/low_hp содержат движение кристалла; пульсация света и трещины/искры требуют runtime. Gameplay balance и collision не изменены.

Генерация: `node tools/blockbench/towers/generate.mjs`.
Проверка: `node tools/blockbench/towers/validate.mjs`.
Полный проход C–G: `node tools/blockbench/world/build.mjs`.
Перед повторной генерацией предыдущие исходники, текстуры и экспорты сохраняются в `assets/backups/towers/<timestamp>/`.
