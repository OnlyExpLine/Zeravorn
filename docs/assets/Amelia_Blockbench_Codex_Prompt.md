# PROMPT ДЛЯ CODEX — AMELIA BLOCKBENCH ASSET

## ЗАДАЧА: AMELIA — ПОЛНЫЙ BLOCKBENCH CHARACTER ASSET

Нужно создать полностью готовый production-ready asset pipeline для героя Amelia из проекта Zeravorn / Minecraft MOBA.

**ВАЖНО:** я не хочу самостоятельно моделировать, создавать skeleton, bones, pivots, UV, texture atlas, keyframes или animations в Blockbench. Codex должен самостоятельно создать все исходные Blockbench-файлы, геометрию, rig, текстуры, animations и необходимые integration metadata.

Blockbench с моей стороны используется прежде всего как программа, в которой я открываю уже готовую модель, визуально её проверяю и затем сообщаю Codex, что нужно изменить.

---

## 1. SOURCE OF TRUTH

Перед началом обязательно прочитай:

1. `Minecraft_MOBA_5_Heroes_Design_v1.1.md`
2. `Minecraft_MOBA_Balance_v1.1.md`
3. `Minecraft_MOBA_Technical_Specification_v1.0.md`
4. существующую реализацию Amelia в Java/Fabric
5. существующую animation/render/VFX architecture проекта
6. `docs/reference/characters/amelia_concept.png`

Не менять gameplay, damage, Mana, cooldown, range, ability rank caps или server-authoritative правила.

Если документация и существующий код конфликтуют — **НЕ исправлять это молча**. Остановиться и явно сообщить о конфликте.

Визуальный reference:

`docs/reference/characters/amelia_concept.png`

Эта картинка является основным художественным эталоном Amelia.

---

## 2. ГЛАВНАЯ ЦЕЛЬ

После выполнения задачи я должен иметь возможность:

1. открыть готовый файл Amelia в Blockbench;
2. увидеть уже полностью собранную third-person модель;
3. увидеть готовый skeleton / bone hierarchy;
4. открыть вкладку Animate;
5. выбрать готовые `idle / walk / run / basic / Q / E / R / hit / CC / death / recall` animations;
6. открыть отдельную first-person модель;
7. увидеть готовые руки и посох;
8. открыть готовые texture atlases;
9. выполнить validation/export;
10. подключить assets к существующей Fabric renderer/animation системе.

Я **НЕ должен вручную создавать**:

- cubes;
- meshes;
- bones;
- pivots;
- UV;
- textures;
- keyframes;
- animation clips.

Если прямое редактирование `.bbmodel` ненадёжно, создай детерминированный generator/script/plugin, который генерирует `.bbmodel` автоматически.

---

## 3. ВНЕШНОСТЬ AMELIA

Основываться на reference image максимально близко по художественному языку, но создать самостоятельный игровой asset, а не копировать чужую IP.

Общий образ:

**AMELIA — взрослая ледяная магичка.**

Рост: примерно `1.0 Steve` по общей высоте игрового персонажа, но геометрия значительно сложнее обычной Minecraft-модели.

Силуэт:

- женственный;
- хорошо читаемый с большого расстояния;
- длинные волосы;
- длинные элементы одежды;
- ледяная корона;
- крупный магический посох;
- более выразительная фигура, чем у стандартной Minecraft-модели;
- широкие бедра;
- выраженная большая грудь согласно текущему Hero Design;
- силуэт должен оставаться пригодным для gameplay и animation.

Не превращать Amelia в стандартного Steve со skin texture. Она должна иметь полноценную составную 3D-геометрию.

---

## 4. ГОЛОВА И ЛИЦО

Создать лицо максимально близкого художественного характера к reference:

- светло-голубая / очень холодная кожа;
- светлые ледяные глаза;
- мягкие женственные черты;
- небольшая аккуратная форма носа;
- спокойное выражение;
- визуальное ощущение холодной аристократичности.

Не делать реалистичное человеческое лицо. Оставить стилизацию совместимой с Minecraft/Blockbench.

Лицо должно хорошо читаться:

- спереди;
- 3/4;
- сбоку.

Создать возможность нескольких facial states через texture/geometry, если существующая runtime animation architecture это позволяет:

- `amelia.face.neutral`
- `amelia.face.confident`
- `amelia.face.serious`
- `amelia.face.surprised`
- `amelia.face.smile`
- `amelia.face.eyes_closed`

Если facial animation технически не поддерживается текущим renderer, создать source-ready варианты, но не ломать renderer ради этой задачи.

---

## 5. ВОЛОСЫ

Очень важная часть silhouette.

Reference: длинные бело-серебристые волосы почти до нижней части спины.

Сделать волосы многослойными:

- `hair_root`
- `hair_front_left`
- `hair_front_right`
- `hair_side_left`
- `hair_side_right`
- `hair_back_upper`
- `hair_back_mid`
- `hair_back_lower`
- `hair_strands_left`
- `hair_strands_right`

Не делать волосы одним огромным кубом.

Нужны:

- отдельные передние локоны;
- боковые пряди;
- крупная масса сзади;
- несколько более тонких нижних секций.

Hair bones должны позволять:

- небольшое secondary motion при ходьбе;
- движение при беге;
- движение при Q/E/R;
- лёгкий подъём при сильном магическом cast;
- движение после landing / knockback.

Не использовать чрезмерно много bones. Нужен разумный performance budget.

---

## 6. ЛЕДЯНАЯ КОРОНА

Создать ледяную корону/диадему как отдельный attachment.

Reference: центральный крупный голубой кристалл + несколько меньших ледяных кристаллов.

Структура примерно:

```text
head
└── crown
    ├── crown_center_crystal
    ├── crown_left_crystal_01
    ├── crown_left_crystal_02
    ├── crown_right_crystal_01
    └── crown_right_crystal_02
```

Кристаллы должны выглядеть:

- острыми;
- визуально ледяными;
- яркими;
- хорошо читаемыми ночью.

Не делать literal transparency, если выбранный Minecraft renderer плохо её поддерживает. При необходимости имитировать лед цветом + highlights + emissive texture.

---

## 7. ТЕЛО / ПРОПОРЦИИ

Тело не должно использовать стандартную прямоугольную Steve-анатомию.

Создать составные формы:

- `torso_upper`
- `torso_mid`
- `waist`
- `pelvis`
- `chest geometry`
- `shoulders`
- `upper arms`
- `forearms`
- `hands`
- `thighs`
- `lower legs`
- `feet`

Основные особенности:

- узкие плечи относительно Jason/Loki;
- выраженный женственный torso;
- большая грудь согласно reference;
- относительно узкая талия;
- широкие бедра;
- стройные ноги;
- силуэт не должен становиться карикатурным.

**IMPORTANT:** геометрия груди и одежды должна корректно деформироваться/вращаться вместе с torso и не пересекаться с руками при:

- walk;
- run;
- Q channel;
- E;
- R;
- recall.

Проверить clipping во всех animations.

---

## 8. ОДЕЖДА

Создать одежду максимально близкой к reference.

Основные цвета:

- белый;
- ледяной голубой;
- глубокий синий;
- серебро;
- очень небольшие золотисто-бежевые accents.

Верх:

- бело-серебристый магический корсет/броня;
- голубые вставки;
- центральный голубой crystal/gem;
- shoulder decorations;
- открытая/облегченная верхняя конструкция согласно reference.

Рукава:

- синие/голубые;
- белые меховые/ледяные cuffs;
- металлические/кристаллические accents.

Нижняя часть: многослойное платье/robe.

Не делать её одним элементом.

Структура:

- `skirt_front_center`
- `skirt_front_left`
- `skirt_front_right`
- `skirt_side_left`
- `skirt_side_right`
- `skirt_back_left`
- `skirt_back_center`
- `skirt_back_right`

Каждая крупная секция должна иметь bone/pivot, чтобы одежда могла слегка двигаться независимо.

Внешняя юбка: темно-сине-голубая.

Внутренние слои: белые / серебристые.

Добавить:

- снежинки;
- ледяные геометрические узоры;
- серебряные линии;
- кристаллические accents.

Не перегружать маленькими деталями, которые невозможно увидеть на игровой дистанции.

---

## 9. НОГИ И ОБУВЬ

Сделать закрытые ледяные сапоги.

Reference: бело-синие сапоги с кристаллическими элементами.

Никакой стандартной Minecraft foot geometry.

Добавить:

- heel-like silhouette без чрезмерно тонкой геометрии;
- передние armor/crystal accents;
- симметричный ледяной орнамент.

Animation должна сохранять устойчивую постановку стоп.

---

## 10. ПОСОХ

Создать отдельную полноценную модель:

`amelia_staff.bbmodel`

или attachment внутри Amelia source, если текущий pipeline лучше работает так.

Посох должен быть максимально близок к reference.

Основные элементы:

- `staff_root`
- `staff_grip`
- `staff_lower`
- `staff_upper`
- `crystal_mount`
- `crystal_center`
- `crystal_left_01`
- `crystal_left_02`
- `crystal_right_01`
- `crystal_right_02`

Силуэт: длинный магический посох, примерно от земли до уровня головы/выше.

Материалы:

- тёмное дерево / металл;
- серебряные крепления;
- голубые кристаллы.

Верх: большой центральный ледяной кристалл + несколько боковых кристаллов.

Создать attachment point:

`staff_vfx_origin`

Он будет использоваться для:

- Basic Icicle;
- Q;
- некоторых cast particles;
- idle frost.

---

## 11. THIRD-PERSON SKELETON

Минимальная рекомендуемая hierarchy:

```text
amelia_root
├── body
│   ├── pelvis
│   │   ├── leg_left
│   │   │   ├── lower_leg_left
│   │   │   └── foot_left
│   │   └── leg_right
│   │       ├── lower_leg_right
│   │       └── foot_right
│   │
│   ├── waist
│   └── torso
│       ├── chest
│       ├── head
│       │   ├── face
│       │   ├── crown
│       │   └── hair_root
│       │       ├── hair_front_left
│       │       ├── hair_front_right
│       │       ├── hair_side_left
│       │       ├── hair_side_right
│       │       ├── hair_back_upper
│       │       ├── hair_back_mid
│       │       └── hair_back_lower
│       │
│       ├── shoulder_left
│       │   └── upper_arm_left
│       │       └── forearm_left
│       │           └── hand_left
│       │
│       └── shoulder_right
│           └── upper_arm_right
│               └── forearm_right
│                   └── hand_right
│
├── clothing
│   ├── skirt_front_left
│   ├── skirt_front_center
│   ├── skirt_front_right
│   ├── skirt_side_left
│   ├── skirt_side_right
│   ├── skirt_back_left
│   ├── skirt_back_center
│   └── skirt_back_right
│
└── attachments
    ├── staff_socket
    ├── vfx_chest
    ├── vfx_left_hand
    ├── vfx_right_hand
    ├── vfx_feet
    └── vfx_head
```

Можно изменить hierarchy, если это необходимо для существующего runtime renderer.

Bone names после интеграции считаются **STABLE API**. Не переименовывать их случайно в будущих задачах.

---

## 12. SCALE И HITBOX

Visual scale Amelia: около `1.0 Steve`.

При этом модель может быть шире Steve из-за одежды/волос.

Не менять gameplay hitbox только ради визуального skirt/hair.

Использовать hitbox из Balance как source of truth.

Декоративная одежда и волосы **НЕ должны** искусственно увеличивать gameplay hitbox.

---

## 13. TEXTURE ATLAS

Создать полный texture atlas.

Желательное разрешение source texture: `128x128` или `256x256`, если текущий renderer позволяет.

Не использовать чрезмерно большой atlas.

Texture groups:

- face;
- skin;
- eyes;
- lips;
- hair;
- crown;
- upper clothes;
- sleeves;
- cuffs;
- skirt;
- boots;
- metallic accents;
- crystals;
- staff.

Палитра:

- Skin: cold pale blue.
- Hair: white / silver / very pale blue shadows.
- Eyes: bright icy blue.
- Primary cloth: white.
- Secondary: medium icy blue.
- Dark cloth: deep desaturated blue.
- Metal: silver.
- Crystal: bright cyan / ice blue.
- Minor accents: muted beige/gold.

Создать shading вручную/процедурно так, чтобы модель не выглядела как flat-color prototype.

Добавить:

- highlights;
- subtle cold shadows;
- cloth variation;
- crystal highlights;
- hair strand variation.

Не делать photorealistic PBR. Стиль должен оставаться совместимым с Minecraft.

---

## 14. EMISSIVE

Если существующая rendering architecture поддерживает emissive texture, создать:

`amelia_emissive.png`

Emissive areas:

- eyes очень слабо;
- crown crystals;
- staff crystals;
- chest gem.

Emission не должен превращать всю Amelia в источник света.

---

## 15. ОБЯЗАТЕЛЬНЫЕ THIRD-PERSON ANIMATIONS

Создать:

- `amelia.idle`
- `amelia.walk`
- `amelia.run`
- `amelia.basic`
- `amelia.q_start`
- `amelia.q_loop`
- `amelia.q_end`
- `amelia.e`
- `amelia.r`
- `amelia.hit`
- `amelia.stun`
- `amelia.root`
- `amelia.knockback`
- `amelia.knockup`
- `amelia.death`
- `amelia.recall_start`
- `amelia.recall_loop`
- `amelia.recall_end`

При необходимости дополнительно:

- `amelia.victory`
- `amelia.spawn`
- `amelia.level_up`

Но optional animations не должны задерживать обязательные.

---

## 16. IDLE

Характер: спокойная, уверенная, холодная.

Loop: примерно 3–5 секунд.

Добавить:

- легкое дыхание;
- минимальное движение плеч;
- небольшое движение staff;
- hair secondary motion;
- небольшое движение skirt;
- едва заметный наклон головы.

---

## 17. WALK

Amelia должна двигаться плавнее остальных героев.

Добавить:

- умеренный swing рук;
- staff следует движению;
- robe/skirt слегка отстают от ног;
- волосы имеют небольшой lag;
- torso остается относительно вертикальным.

Избегать чрезмерно exaggerated hip animation. Главная идентичность — ледяная аристократичная магичка.

---

## 18. RUN

Более динамичная версия walk:

- torso слегка вперед;
- robe активнее;
- волосы сильнее отклоняются назад;
- staff удерживается стабильно;
- ноги должны ясно читаться через skirt.

Не допускать clipping staff через body/hair.

---

## 19. BASIC — ICICLE

LMB / Basic: Amelia выпускает ледяную сосульку из посоха.

Gameplay numbers **НЕ менять**.

Последовательность:

- `0.00` — neutral;
- `0.05` — правое плечо/рука начинают поднимать staff;
- `0.10–0.18` — staff направляется в aim direction;
- около server projectile spawn/hit-timing marker создать event:

`amelia.basic.projectile_spawn`

В этот момент:

- crystal на staff направлен вперед;
- левая рука может слегка поддерживать cast;
- небольшой recoil после выпуска.

Затем recovery обратно в locomotion.

Не наносить damage animation callback.

VFX hooks:

- `amelia.basic_charge`
- `amelia.basic_projectile_spawn`
- `amelia.basic_muzzle_frost`
- `amelia.basic_hit`

SFX hooks:

- `amelia.basic_cast`
- `amelia.basic_projectile`
- `amelia.basic_hit`

---

## 20. Q — FROST BREATH

Q является channel-конусом мороза.

Движение во время channel разрешено.

Разделить на:

- `amelia.q_start`
- `amelia.q_loop`
- `amelia.q_end`

Q_START:

- Amelia быстро переводит staff вперед;
- левая рука раскрывается;
- плечи слегка разворачиваются;
- кристалл начинает сиять.

Event:

`amelia.q.channel_start`

Q_LOOP:

- верхняя часть тела держит cast;
- ноги продолжают использовать movement locomotion;
- руки/torso используют upper-body overlay;
- staff направлен вперед;
- волосы и одежда реагируют на магический поток.

Loop должен корректно смешиваться с walk/run.

Events/VFX:

- `amelia.q_frost_start`
- `amelia.q_frost_loop`
- `amelia.q_tick`
- `amelia.q_frost_end`

Не создавать damage из animation. Server создает gameplay ticks.

Visual:

- frost cone;
- snow particles;
- small ice shards;
- cold fog;
- glowing staff crystal.

SFX:

- wind build-up;
- continuous frost;
- periodic ice crack;
- channel end.

Q должен корректно прерываться stun/knock-up согласно gameplay.

---

## 21. E — BLIZZARD BURST

Кольцевой снежный взрыв вокруг Amelia.

Последовательность:

1. Amelia слегка отводит staff назад.
2. Левая рука открывается.
3. Быстро опускает/разводит руки.
4. Вокруг персонажа происходит burst.

Создать impact event:

`amelia.e.impact`

VFX hooks:

- `amelia.e_charge`
- `amelia.e_ring_spawn`
- `amelia.e_snow_burst`
- `amelia.e_hit`

Visual:

- круг мороза от ног;
- снег;
- ice mist;
- мелкие кристаллы;
- outward motion, подчеркивающий knockback.

Animation recovery должна быть быстрой.

---

## 22. R — ICE SPIKES

R — ground-target ability.

Обязательный telegraph до появления шипов.

Последовательность:

- `0.00` — staff поднимается;
- `0.10` — свободная рука направляется к выбранной области;
- `0.20–0.35` — staff crystal ярко светится.

Event:

`amelia.r.telegraph_start`

Перед server impact Amelia резко опускает staff / делает casting gesture.

Event:

`amelia.r.impact`

После server confirmation из земли выходят spikes.

VFX:

- `amelia.r_ground_telegraph`
- `amelia.r_cracks`
- `amelia.r_spikes`
- `amelia.r_frost_burst`
- `amelia.r_after_frost`

Telegraph должен быть хорошо заметен противнику.

SFX:

- `amelia.r_charge`
- `amelia.r_ground_freeze`
- `amelia.r_crystal_rise`
- `amelia.r_impact`

---

## 23. HIT / STUN / ROOT / KNOCKBACK / KNOCKUP

HIT:
- torso назад;
- плечо/голова слегка отклоняются;
- staff не выпадает.

STUN:
- читаемая потерянная стойка;
- locomotion отсутствует;
- staff остаётся в руке.

ROOT:
- нижняя часть тела неподвижна;
- upper-body animations остаются возможными, если gameplay позволяет.

KNOCKBACK / KNOCKUP:
не управлять trajectory через Blockbench — сервер двигает героя, animation лишь показывает реакцию.

---

## 24. DEATH

Death должна быть серьёзной, не комедийной.

Последовательность:

- staff опускается;
- Amelia теряет силы;
- колени/тело уходят вниз;
- волосы/robe продолжают движение;
- финальная pose.

Optional VFX:

- несколько снежинок;
- слабое погасание crystal emission.

---

## 25. RECALL

Recall = 5-секундный channel.

Разделить:

- `amelia.recall_start`
- `amelia.recall_loop`
- `amelia.recall_end`

Concept:

START:
Amelia ставит/держит staff вертикально.

LOOP:
- вокруг ног образуется небольшой ледяной круг;
- снежинки поднимаются вверх;
- волосы слегка поднимаются;
- crown/staff crystals усиливают glow;
- Amelia поднимает свободную руку;
- несколько ice shards медленно вращаются вокруг.

END:
- сильный frost flash;
- animation заканчивается перед teleport.

Cancel должен мгновенно прекращать recall-loop без teleport end.

---

## 26. FIRST-PERSON MODEL

Создать отдельный Blockbench проект:

`amelia_first_person.bbmodel`

Не использовать стандартные Minecraft руки.

Игрок должен видеть:

- левую руку Amelia;
- правую руку / staff.

First-person visual:

- голубая кожа;
- длинный синий/серебристый рукав;
- белый cuff;
- staff с crystal.

Не показывать тело/грудь, если это закрывает обзор.

Crosshair area должна всегда оставаться свободной.

---

## 27. FIRST-PERSON ANIMATIONS

Создать:

- `amelia.fp.idle`
- `amelia.fp.walk_sway`
- `amelia.fp.run_sway`
- `amelia.fp.basic`
- `amelia.fp.q_start`
- `amelia.fp.q_loop`
- `amelia.fp.q_end`
- `amelia.fp.e`
- `amelia.fp.r`
- `amelia.fp.hit`
- `amelia.fp.recall_start`
- `amelia.fp.recall_loop`
- `amelia.fp.recall_end`

---

## 28. SECONDARY MOTION

Hair и skirt требуют дополнительного движения.

Не использовать сложную physics simulation, если runtime её не поддерживает.

Использовать keyframed secondary motion.

Hair:

- небольшое delayed movement;
- сильнее при run;
- вверх/назад при магическом burst.

Skirt:

- small sway при walk;
- больше при run;
- outward movement при E;
- небольшое движение при R.

---

## 29. VFX SYSTEM — НЕ BLOCKBENCH GEOMETRY

Создать stable event IDs.

GENERAL:

- `amelia.vfx.idle_frost`
- `amelia.vfx.staff_glow`

BASIC:

- `amelia.vfx.basic_charge`
- `amelia.vfx.basic_spawn`
- `amelia.vfx.basic_hit`

Q:

- `amelia.vfx.q_start`
- `amelia.vfx.q_loop`
- `amelia.vfx.q_tick`
- `amelia.vfx.q_end`

E:

- `amelia.vfx.e_charge`
- `amelia.vfx.e_ring`
- `amelia.vfx.e_hit`

R:

- `amelia.vfx.r_telegraph`
- `amelia.vfx.r_ground_crack`
- `amelia.vfx.r_spikes`
- `amelia.vfx.r_hit`

RECALL:

- `amelia.vfx.recall_start`
- `amelia.vfx.recall_loop`
- `amelia.vfx.recall_end`

---

## 30. SFX HOOKS

Создать stable sound event IDs:

- `amelia.basic.cast`
- `amelia.basic.travel`
- `amelia.basic.hit`
- `amelia.q.cast`
- `amelia.q.loop`
- `amelia.q.hit`
- `amelia.q.end`
- `amelia.e.cast`
- `amelia.e.hit`
- `amelia.r.cast`
- `amelia.r.telegraph`
- `amelia.r.impact`
- `amelia.hit`
- `amelia.death`
- `amelia.recall`
- `amelia.level_up`

Финальные `.ogg` можно оставить отдельной задачей, но hooks и IDs подготовить.

---

## 31. UI ASSETS

Подготовить:

- `amelia_portrait.png`
- `amelia_basic.png`
- `amelia_q.png`
- `amelia_e.png`
- `amelia_r.png`
- `amelia_hero_icon.png`

Skill icons:

- Basic — летящая сосулька;
- Q — поток/конус ледяного дыхания;
- E — кольцевой снежный взрыв;
- R — ледяные шипы из земли.

Создать самостоятельную иконографию Zeravorn.

---

## 32. FILE STRUCTURE

Желательно:

```text
assets/source/blockbench/heroes/amelia/
    amelia.bbmodel
    amelia_first_person.bbmodel
    amelia_staff.bbmodel

assets/source/textures/heroes/amelia/
    amelia.png
    amelia_emissive.png
    amelia_first_person.png
    amelia_staff.png
```

Runtime assets размещать в существующей структуре:

```text
src/main/resources/assets/<modid>/
```

Если проект использует другую convention — следовать существующей архитектуре.

---

## 33. SOURCE VS RUNTIME ASSETS

Не смешивать editable source и runtime export.

Pipeline:

```text
source .bbmodel
        ↓
validation
        ↓
runtime geometry/animation resources
        ↓
Fabric renderer
```

---

## 34. АВТОМАТИЧЕСКИЙ GENERATOR

Я хочу иметь возможность попросить Codex:

> сделай волосы Amelia длиннее

и получить обновленный `.bbmodel` без ручной работы.

Если возможно, создать generator/tooling:

```text
tools/blockbench/amelia/
    generate_amelia.py / js / ts
    validate_amelia.*
    README.md
```

Параметризовать минимум:

- scale;
- head dimensions;
- torso dimensions;
- chest dimensions;
- waist;
- hip width;
- hair length;
- hair volume;
- skirt length;
- staff length;
- crystal size.

---

## 35. VALIDATION

Проверить автоматически:

- `.bbmodel` JSON валиден;
- нет duplicate bone IDs;
- parent bones существуют;
- animation references используют существующие bones;
- texture references существуют;
- required animations существуют;
- first-person animations существуют;
- staff socket существует;
- VFX sockets существуют;
- event IDs уникальны;
- no NaN/Infinity transforms;
- reasonable scale bounds;
- runtime export создан.

---

## 36. PERFORMANCE

Это Minecraft, а не offline renderer.

Приоритет детализации:

1. silhouette;
2. лицо/волосы;
3. crown;
4. верхняя часть одежды;
5. staff;
6. skirt;
7. boots;
8. мелкий декор.

Создать отчёт:

- cube/element count;
- bone count;
- animation count;
- texture resolution.

Если asset слишком тяжелый — предложить оптимизацию.

---

## 37. INTEGRATION С FABRIC

Использовать существующий renderer и abstractions.

Схема:

```text
server gameplay state
        ↓
network event
        ↓
client animation state
        ↓
Blockbench animation / VFX / SFX
```

Никогда:

```text
Blockbench animation
        ↓
apply damage
```

Damage остается server-authoritative.

---

## 38. ANIMATION SYNCHRONIZATION

Использовать server-confirmed markers.

Пример Basic:

```text
Client press LMB
↓
C2S BasicAttackRequest
↓
server validates
↓
server BasicAttackEvent
↓
client starts amelia.basic
↓
projectile_spawn marker совпадает с server spawn
↓
Icicle projectile
↓
server collision
↓
damage
↓
S2C hit event
↓
ice hit VFX/SFX
```

То же правило для Q/E/R.

---

## 39. SKILL RANKS

Не создавать отдельные animation variants для rank 1/2/3/4.

Rank меняет gameplay numbers, а не визуальную идентичность способности.

Использовать одну animation identity на ability.

---

## 40. BLOCKBENCH USABILITY

После генерации файл должен нормально открываться в Blockbench.

OUTLINER:
понятная hierarchy.

ANIMATE:
понятные названия animations.

TEXTURES:
корректно подключены.

PIVOTS:
логично расположены.

Не оставлять:

- `cube_1`
- `cube_2`
- `group_17`
- `animation.untitled`

Использовать semantic naming.

---

## 41. ДОКУМЕНТАЦИЯ

Создать:

`docs/assets/AMELIA_ASSET.md`

Включить:

1. visual reference;
2. source files;
3. runtime files;
4. skeleton hierarchy;
5. texture atlases;
6. bone names;
7. animation IDs;
8. VFX sockets;
9. event markers;
10. SFX IDs;
11. export process;
12. validation;
13. Fabric integration;
14. как открыть Amelia в Blockbench;
15. как изменить модель через Codex/generator;
16. known limitations.

---

## 42. README ДЛЯ МЕНЯ

Отдельно написать простую инструкцию:

1. открыть Blockbench;
2. `File -> Open Model`;
3. выбрать путь к Amelia;
4. где найти Outliner;
5. где открыть Animate;
6. как выбрать `amelia.walk`;
7. где нажать Play;
8. как посмотреть модель спереди/сбоку/сзади;
9. как открыть first-person file;
10. как ничего случайно не сломать.

---

## 43. ACCEPTANCE CRITERIA — ВНЕШНОСТЬ

Asset **НЕ принимается**, если Amelia:

- выглядит как обычный Steve;
- имеет только texture skin без 3D silhouette;
- не имеет длинных многослойных волос;
- не имеет ледяной crown;
- не имеет сложного layered dress;
- не имеет большого crystal staff;
- не соответствует white / ice-blue / silver palette;
- визуально не похожа по общей идентичности на reference image;
- имеет много clipping;
- выглядит как generic Minecraft NPC.

Asset принимается, если при открытии Blockbench сразу визуально понятно:

> это Amelia с нашего concept sheet.

---

## 44. ACCEPTANCE CRITERIA — ANIMATIONS

Обязательны:

- [ ] idle
- [ ] walk
- [ ] run
- [ ] basic
- [ ] q_start
- [ ] q_loop
- [ ] q_end
- [ ] e
- [ ] r
- [ ] hit
- [ ] stun
- [ ] root
- [ ] knockback
- [ ] knockup
- [ ] death
- [ ] recall_start
- [ ] recall_loop
- [ ] recall_end

First-person:

- [ ] idle
- [ ] movement sway
- [ ] basic
- [ ] Q
- [ ] E
- [ ] R
- [ ] hit
- [ ] recall

---

## 45. ACCEPTANCE CRITERIA — GAMEPLAY

После подключения в Minecraft:

- Amelia визуально имеет около 1.0 Steve height;
- gameplay hitbox остается из Balance;
- модель видна другим игрокам;
- first-person модель видна владельцу;
- vanilla hand скрыта;
- staff корректно следует руке;
- Basic projectile появляется из нужного socket;
- Q визуально работает во время движения;
- E burst синхронизирован с server event;
- R telegraph появляется до spikes;
- stun/root/death/recall states работают;
- волосы/robe не вызывают gameplay collision;
- hidden enemy Amelia корректно скрывается VisionSystem;
- модель не влияет на server gameplay calculations.

---

## 46. TESTING

После завершения:

1. выполнить asset validation;
2. выполнить relevant unit tests;
3. выполнить:

```powershell
.\gradlew.bat clean build
```

4. если доступно:

```powershell
.\gradlew.bat runClient
```

Проверить отсутствие:

- missing texture;
- missing animation;
- broken model;
- renderer exception;
- invalid bone;
- network crash.

Не считать задачу завершенной, если Gradle build падает.

---

## 47. НЕ ДЕЛАТЬ

Не:

- менять способности Amelia;
- менять Balance;
- менять Mana/CD/damage;
- менять hitbox без explicit instruction;
- копировать copyrighted character;
- использовать чужие textures/models;
- делать runtime damage из animation callback;
- создавать God-class renderer только для Amelia;
- mass-format unrelated Java files;
- переписывать working gameplay systems;
- удалять placeholder pipeline до успешной интеграции Amelia.

---

## 48. ПОРЯДОК РАБОТЫ

Выполнять этапами:

**PHASE 1** — проанализировать project architecture и документы.  
**PHASE 2** — создать Amelia geometry + skeleton.  
**PHASE 3** — создать texture atlas.  
**PHASE 4** — создать third-person locomotion.  
**PHASE 5** — создать Basic/Q/E/R animations.  
**PHASE 6** — создать CC/death/recall animations.  
**PHASE 7** — создать first-person asset.  
**PHASE 8** — создать VFX/SFX event hooks.  
**PHASE 9** — создать validation/export pipeline.  
**PHASE 10** — подключить к Fabric renderer.  
**PHASE 11** — build/tests.

Не переходить к полной переделке существующей архитектуры, если для текущей phase это не требуется.

---

## 49. ФИНАЛЬНЫЙ ОТЧЕТ CODEX

После выполнения обязательно сообщить:

1. какие файлы созданы;
2. какие файлы изменены;
3. путь к `amelia.bbmodel`;
4. путь к `amelia_first_person.bbmodel`;
5. путь к staff asset;
6. texture paths;
7. bone count;
8. element/cube count;
9. список animations;
10. список event markers;
11. список VFX hooks;
12. список SFX hooks;
13. что уже интегрировано в Minecraft;
14. что осталось placeholder;
15. какие tests выполнены;
16. результат `gradlew clean build`;
17. точную инструкцию: **КАК МНЕ ОТКРЫТЬ ГОТОВУЮ AMELIA В BLOCKBENCH**.
