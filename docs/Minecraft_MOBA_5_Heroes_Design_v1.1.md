# MINECRAFT MOBA - 5 HEROES DESIGN v1.1

          Финальные механики, актуальные бинды, анимации, VFX и SFX пяти стартовых героев

Общее правило: сервер определяет попадание, damage, ability rank, cooldown, Mana и CC.
Клиент отображает first-person/third-person animation, VFX, SFX и HUD.

Механика способности определяется этим документом; точные числа каждого rank определяет Minecraft_MOBA_Balance_v1.1.md. F как геройская способность существует только у Shelianer.

## 1. JASON - БОЕЦ

Массивный рукопашный боец, масштаб \~1.25 Steve. Голый торс, широкие
штаны, красный пояс, бинты/шрамы. First-person - крупные кулаки.

**Skill rank caps:** Q=4, E=4, R=2. F отсутствует. На LVL1 можно изучить Q или E; R доступен с LVL4.

Кнопка Навык Механика Числа

ЛКМ Basic правый хук -\> левый хук -\> тяжелый прямой 100% Attack; 1с;
range2

Q Вампирический удар красный charge -\> punch -\> энергия возвращается
90+100% Attack; CD8; heal40%

E Double Dash dash2; окно второго 5с; E2 подбрасывает и станит E2 70+60%
Attack; CD8; stun1с

R Meteor Crash прыжок, полет5, radial impact 140+80% Attack; CD20;
radius3; stun1.5с

### Визуальная и звуковая подача

• VFX: пыль, ударные кольца, темно-красная энергия. • SFX: THUMP/BOOM,
heartbeat на Q, ветер на R. • Animation: тяжелые плечи/корпус, заметный
landing pose после R.

### Обязательные animation states

idle, walk, run, basic, Q, E, F (если есть), R, hit reaction, stun/root
pose, death, recall.

### First-person

Собственные руки/оружие героя заменяют стандартную Minecraft-руку.
Анимация не должна закрывать прицел и центр экрана.

## 2. SHELIANER - УБИЙЦА

Стройный ассасин размера Steve: темная многослойная одежда, два ножа, по
три метательных клинка на поясе. First-person - два ножа.

**Skill rank caps:** Q=3, E=3, F=2, R=2. На LVL1 можно изучить Q, E или F; R доступен с LVL4.

Кнопка Навык Механика Числа

ЛКМ Basic правый -\> левый -\> X-slash 100% Attack; 1с; range2

Q Throwing Knife быстрый вращающийся knife projectile 45+75% Attack; CD2
fixed; Mana35; range7

E Poison Flask зеленая колба -\> cloud -\> poison (25+10% Attack)x4
magic; CD12; Mana70; slow20

F Dash быстрый afterimage dash 2 блока; CD3; Mana40

R Death Dance target до3; 6 slash; leap back3 (30+23% Attack)x6; CD30;
Mana120; stun1.5с

### Визуальная и звуковая подача

• VFX: тонкие slash trails, afterimage, зеленый poison cloud. • SFX:
SHIK/SLASH, glass crack, hiss. • Animation: R должен иметь 6 четко
читаемых синхронных hits.

### Обязательные animation states

idle, walk, run, basic, Q, E, F (если есть), R, hit reaction, stun/root
pose, death, recall.

### First-person

Собственные руки/оружие героя заменяют стандартную Minecraft-руку.
Анимация не должна закрывать прицел и центр экрана.

## 3. ESAKI - СТРЕЛОК ЗЕМЛИ

Geomancer обычного роста: смуглая кожа, каменные наручи,
песочно-коричневая одежда, янтарные глаза. First-person - руки и
формирующиеся камни.

**Skill rank caps:** Q=4, E=4, R=2. F отсутствует. На LVL1 можно изучить Q или E; R доступен с LVL4.

Кнопка Навык Механика Числа

ЛКМ Stone Shot 2с подготовка: рука вниз -\> камень -\> бросок; movement
lock 100% Attack; range5; speed13

Q Boulder двумя руками запускает большой валун 100+110% Attack; CD8;
Mana75; range7; speed8

E Earth Repulse удар по земле и земляное кольцо 60+50% Attack; CD12;
Mana60; radius3; knockbac

R Earthquake 3 ground pulses за 3с (70+35% Attack)x3; CD40; Mana140;
radius6; nau

### Визуальная и звуковая подача

• VFX: камни, пыль, трещины, ground pulses. • SFX: RUMBLE/CRACK/stone
impact. • Hard CC до 2.0с прерывает незавершенный Basic и камень не
выпускается.

### Обязательные animation states

idle, walk, run, basic, Q, E, F (если есть), R, hit reaction, stun/root
pose, death, recall.

### First-person

Собственные руки/оружие героя заменяют стандартную Minecraft-руку.
Анимация не должна закрывать прицел и центр экрана.

## 4. AMELIA - МАГ

Элегантная ледяная магичка размера Steve: голубая кожа, белые глаза, большая грудь, широкие бедра
серебряно-бело-голубая одежда, посох с ледяным кристаллом.

**Skill rank caps:** Q=4, E=4, R=2. F отсутствует. На LVL1 можно изучить Q или E; R доступен с LVL4.

Кнопка Навык Механика Числа

ЛКМ Icicle сосулька из посоха 100% Attack magical; 1с; range5; speed20

Q Frost Breath channel-конус мороза; движение разрешено 35+12% AP
каждые0.5с x6; CD12; Mana90; slow4

E Blizzard Burst кольцевая снежная буря 90+45% AP; CD12; Mana75;
radius4; knockback4

R Ice Spikes ground target до4; telegraph -\> spikes 220+90% AP; CD30;
Mana130; radius2; delay0.55с

### Визуальная и звуковая подача

• VFX: иней, снег, кристаллы, холодный туман. • SFX: crystal crack, wind
build-up, magical boom. • Q прерывается stun/knock-up. R имеет четкую
ground-telegraph область.

### Обязательные animation states

idle, walk, run, basic, Q, E, F (если есть), R, hit reaction, stun/root
pose, death, recall.

### First-person

Собственные руки/оружие героя заменяют стандартную Minecraft-руку.
Анимация не должна закрывать прицел и центр экрана.

## 5. LOKI - ТАНК

Очень крупный (\~1.5 Steve) бородатый тюремщик/портовый громила: кожа,
металл, цепи, огромный крюк. First-person - hook + chain.

**Skill rank caps:** Q=4, E=4, R=2. F отсутствует. На LVL1 можно изучить Q или E; R доступен с LVL4.

Кнопка Навык Механика Числа

ЛКМ Hook Swing широкий боковой взмах 100% Attack; 1с; range2.5

Q Hook outbound hook + chain; hero hit -\> pull; miss/wall -\> return
70+70% Attack; CD10; Mana70; range9; speed15/

E Rampage удар в грудь, рык, тяжелый ускоренный бег CD12; Mana50;
duration4с

R Iron Prison цепи вырываются из земли и фиксируют врагов 100+40%
Attack; CD30; Mana110; radius4; root2с

### Визуальная и звуковая подача

• VFX: настоящая chain между рукой и hook; chain cage на R. • SFX:
CLANK/THUMP/chain rattle. • Iron Prison - окончательный ультимейт v1.1.
Chain не должна закрывать центр экрана.

### Обязательные animation states

idle, walk, run, basic, Q, E, F (если есть), R, hit reaction, stun/root
pose, death, recall.

### First-person

Собственные руки/оружие героя заменяют стандартную Minecraft-руку.
Анимация не должна закрывать прицел и центр экрана.

## 6. Общая визуальная идентичность

Герой Движение VFX SFX Ощущение

Jason тяжелое пыль, красная энергия BOOM/THUMP сила

Shelianer быстрое afterimage, яд SHIK/SLASH скорость

Esaki тяжелые касты камни, трещины RUMBLE/CRACK масса

Amelia плавное лед, снег CRYSTAL/WIND магия

Loki грузное цепи, пыль CLANK/THUMP контроль

## 7. Asset checklist

Тип Нужно

Third-person hero model + weapon/attachments

First-person arms/weapon model

Texture hero/weapon texture atlas

Animation idle/walk/run/basic/Q/E/F/R/hit/CC/death/recall

VFX cast/travel/hit/area

SFX swing/cast/travel/hit + hero identity

UI portrait, skill icons, status icons

## 8. Правила синхронизации

Animation/VFX/SFX запускаются по подтвержденным server events. Реальный
damage не применяется client animation callback. Multi-hit имеет
отдельные server damage ticks. Сначала допускаются placeholder-модели и
простые анимации; финальные Blockbench assets подключаются после
проверки механики.


## 9. Skill progression presentation

- При наличии нераспределенного Skill Point HUD показывает `+` над способностью, которую разрешено улучшить.
- Неизученная Q/E/F имеет rank 0 и визуально помечается как NOT LEARNED.
- R до LVL4 визуально помечается `LOCKED LVL 4`; после LVL4 может получить rank 1, если есть Skill Point.
- Анимация и VFX не должны зависеть от числового rank; rank меняет gameplay numbers из Balance, а не идентичность способности.
- У героев без F слот F в hero ability HUD не отображается.
