# Amelia — Blockbench asset

> Актуальная визуальная переработка от 2026-10-04, сравнение с concept и известный clipping: [AMELIA_VISUAL_REWORK.md](AMELIA_VISUAL_REWORK.md). Числа и описание ниже относятся к предыдущей версии.

Обновлено 2026-09-08. Основной художественный источник — приложенный владельцем concept sheet Amelia. Геометрия создана специально для Zeravorn и не использует модель, генератор или текстуры Jason.

## Как открыть готовую модель

1. В Blockbench выбрать **File → Open Model**.
2. Открыть `assets/source/blockbench/heroes/amelia/amelia.bbmodel`.
3. Справа открыть **Outliner** для просмотра рига.
4. Перейти во вкладку **Animate**, выбрать, например, `amelia.walk` и нажать Play.
5. Для проверки спереди, сбоку и сзади использовать ортографические виды Blockbench.
6. First-person руки находятся в `amelia_first_person.bbmodel`, отдельный посох — в `amelia_staff.bbmodel` в той же папке.
7. Перед ручным сохранением сделать копию файла: повторный запуск генератора заменяет сгенерированные ассеты.

Без Blockbench модель и все клипы можно проверить в `assets/source/blockbench/heroes/amelia/previews/amelia_viewer.html`. Просмотрщик работает локально, вращает реальную геометрию, воспроизводит клипы и позволяет загрузить concept sheet рядом для сравнения.

## Исходники и экспорт

| Путь | Содержание |
|---|---|
| `assets/source/blockbench/heroes/amelia/amelia.bbmodel` | Полная модель, 65 костей, 18 клипов |
| `assets/source/blockbench/heroes/amelia/amelia_first_person.bbmodel` | Руки и посох, 24 кости, 13 клипов |
| `assets/source/blockbench/heroes/amelia/amelia_staff.bbmodel` | Отдельный посох, 12 костей |
| `assets/source/textures/heroes/amelia/` | Внешние атласы и emissive-копии, 256×256 |
| `assets/source/blockbench/heroes/amelia/faces/` | Шесть source-ready вариантов лица |
| `src/main/resources/assets/zeravorn/amelia/` | Mesh/animation JSON, glTF, текстуры, UI icons |
| `tools/blockbench/amelia/` | Независимый генератор, экспорт, просмотр и validation |

Из корня проекта:

```powershell
node tools/blockbench/amelia/generate_amelia.mjs
node tools/blockbench/amelia/generate_amelia.mjs --verify
node tools/blockbench/amelia/validate_amelia.mjs
```

Параметры находятся в `parameters.json`: `scale`, `headWidth`, `headHeight`, `torsoWidth`, `chestWidth`, `chestDepth`, `waistWidth`, `hipWidth`, `hairLength`, `hairVolume`, `skirtLength`, `staffLength`, `crystalSize`. Чтобы правка не потерялась при следующей генерации, менять нужно `defaults` в `geometry.mjs` или передать JSON-файл параметров генератору.

## Внешность и риг

Модель имеет взрослое узкое лицо, холодный прямой взгляд, отдельные ресницы/брови/нос/губы, объёмные пряди у висков и щёк, многосекционные волосы до спины, ледяную корону, открытую объёмную грудь с нижними чашами корсета, узкую талию, восемь подвижных панелей платья, сапоги и кристаллический посох. Направление вперёд — `-Z`, 16 authoring units = 1 блок.

Основная иерархия: `amelia_root → body → pelvis/waist → torso/chest/head/arms`; ноги имеют отдельные бедро, голень и стопу. `head` содержит `face`, `crown`, `hair_root`; волосы разделены на передние, боковые и три задние секции. `clothing` содержит восемь костей юбки. Посох прикреплён через `staff_socket` и имеет собственные кости кристаллов.

Стабильные sockets: `staff_socket`, `staff_vfx_origin`, `vfx_chest`, `vfx_left_hand`, `vfx_right_hand`, `vfx_feet`, `vfx_head`.

## Анимации

Third-person: `amelia.idle`, `walk`, `run`, `basic`, `q_start`, `q_loop`, `q_end`, `e`, `r`, `hit`, `stun`, `root`, `knockback`, `knockup`, `death`, `recall_start`, `recall_loop`, `recall_end`.

First-person: `amelia.fp.idle`, `walk_sway`, `run_sway`, `basic`, `q_start`, `q_loop`, `q_end`, `e`, `r`, `hit`, `recall_start`, `recall_loop`, `recall_end`.

Q-клипы являются upper-body overlay для смешивания с locomotion. Волосы и панели платья имеют keyframed secondary motion. Сервер управляет trajectory knockback/knockup и моментом завершения recall.

## События, VFX и SFX

Event markers: `amelia.basic.projectile_spawn` 0,25 с; `amelia.q.channel_start`; `amelia.q.tick` 0,5 с; `amelia.q.channel_end`; `amelia.e.impact` 0,35 с; `amelia.r.telegraph_start`; `amelia.r.impact` 0,55 с; recall start/end. Полный контракт находится в `amelia.events.json`.

VFX IDs используют `amelia.vfx.*`: idle/staff glow, basic charge/spawn/hit, Q start/loop/tick/end, E charge/ring/hit, R telegraph/crack/spikes/hit, recall start/loop/end. SFX IDs: basic cast/travel/hit, Q cast/loop/hit/end, E cast/hit, R cast/telegraph/impact, hit, death, recall, level-up.

Маркеры отвечают только за presentation. Damage, Mana, cooldown, CC и movement остаются server-authoritative.

## Проверка и ограничения

Валидатор проверяет JSON/UUID, риг, pivots, UV, встроенные PNG, обязательные клипы, loop seams, Q overlay, glTF buffers, sockets, тайминги и область прицела. На 273 first-person позах область ±12% X / ±10% Y при FOV 70° и 16:9 свободна. Ошибка контакта стоп в locomotion не превышает 0,025 authoring unit.

Blockbench GUI smoke test и полный визуальный clipping audit каждой позы требуют ручной проверки владельцем. Runtime-файлы подготовлены, но Fabric mesh renderer и AnimationStateBridge в репозитории отсутствуют, поэтому модель пока не отображается в Minecraft. Известные gameplay-расхождения оставлены без изменений: Amelia E сейчас применяет effect раньше документированного маркера 0,35 с, активный Q не проверяет stun/knock-up cancellation.
