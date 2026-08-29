# MINECRAFT MOBA - BALANCE v1.1

                     Итоговая стартовая математика для разработки и первого плейтеста

Статус: стартовая версия баланса для реализации и первого плейтеста. Все числа должны храниться в конфигурациях и после плейтеста могут меняться без переписывания механик.

Правило v1.1: каждый уровень героя дает 1 Skill Point. Обычные герои имеют Q=4, E=4, R=2; Shelianer имеет Q=3, E=3, F=2, R=2. R доступен для прокачки с LVL4.

## 1. Общие параметры и XP

Параметр Значение

Max Level 10

Ultimate LVL 4

Start Gold 500

Passive Gold 3/сек с 00:30

Первая волна 00:30

Wave interval 30 сек

Первый jungle spawn 00:35

Inventory 5 обычных + boots

Target match \~20 минут

      LVL                          XP с прошлого                   Суммарный XP

      1                            -                               0

      2                            180                             180

      3                            270                             450

      4                            400                             850

      5                            450                             1300

      6                            500                             1800

      7                            550                             2350

      8                            600                             2950

      9                            650                             3600

      10                           700                             4300


### Skill Point progression

| Hero Level | New Skill Points | Ultimate rule |
|---|---:|---|
| 1 | 1 | R locked |
| 2 | 1 | R locked |
| 3 | 1 | R locked |
| 4 | 1 | R may be learned |
| 5 | 1 | normal caps |
| 6 | 1 | normal caps |
| 7 | 1 | normal caps |
| 8 | 1 | normal caps |
| 9 | 1 | normal caps |
| 10 | 1 | normal caps |

Обычные герои: Q max4 / E max4 / R max2.  
Shelianer: Q max3 / E max3 / F max2 / R max2.  
Skill Point можно сохранять нераспределенным.

## 2. Характеристики героев 1-10

### Jason - Боец / Physical

Attack interval 1.0с; Move 4.3; без Mana

      LVL           HP            HP Regen/s   Mana   Mana Regen/s   Attack

      1             1250          6.0          -      -              65

      2             1320          6.4          -      -              69

      3             1390          6.8          -      -              73

      4             1460          7.2          -      -              77

      5             1530          7.6          -      -              81

      6             1600          8.0          -      -              85

      7             1670          8.4          -      -              89

      8             1740          8.8          -      -              93

      9             1810          9.2          -      -              97

      10            1880          9.6          -      -              101

### Shelianer - Убийца / Mixed

Attack interval 1.0с; Move 4.5

      LVL           HP            HP Regen/s   Mana   Mana Regen/s   Attack

      1             950           4.5          400    5.0            60

      2             995           4.8          425    5.25           64

      3             1040          5.1          450    5.5            68

      4             1085          5.4          475    5.75           72

      5             1130          5.7          500    6.0            76

      6             1175          6.0          525    6.25           80

      7             1220          6.3          550    6.5            84

      8             1265          6.6          575    6.75           88

      9             1310          6.9          600    7.0            92

      10            1355          7.2          625    7.25           96

### Esaki - Стрелок / Physical

Attack interval 2.0с; Move 4.3

      LVL           HP            HP Regen/s   Mana   Mana Regen/s   Attack

      1             1000          4.5          450    5.0            90

      2             1050          4.8          475    5.25           95

      3             1100          5.1          500    5.5            100

      4             1150          5.4          525    5.75           105

      5             1200          5.7          550    6.0            110

      6             1250          6.0          575    6.25           115

      7             1300          6.3          600    6.5            120

      8             1350          6.6          625    6.75           125

      9             1400          6.9          650    7.0            130

      10            1450          7.2          675    7.25           135

### Amelia - Маг / Magical

Attack interval 1.0с; Move 4.3

      LVL           HP            HP Regen/s   Mana   Mana Regen/s   Attack

      1             900           4.0          550    6.0            55

      2             940           4.25         580    6.35           58

      3             980           4.5          610    6.7            61

      4             1020          4.75         640    7.05           64

      5             1060          5.0          670    7.4            67

      6             1100          5.25         700    7.75           70

      7             1140          5.5          730    8.1            73

      8             1180          5.75         760    8.45           76

      9             1220          6.0          790    8.8            79

      10            1260          6.25         820    9.15           82

### Loki - Танк / Physical

Attack interval 1.0с; Move 4.1

      LVL           HP            HP Regen/s   Mana   Mana Regen/s   Attack

      1             1600          8.0          400    4.0            45

      2             1690          8.5          420    4.2            48

      3             1780          9.0          440    4.4            51

      4             1870          9.5          460    4.6            54

      5             1960          10.0         480    4.8            57

      6             2050          10.5         500    5.0            60

      7             2140          11.0         520    5.2            63

      8             2230          11.5         540    5.4            66

      9             2320          12.0         560    5.6            69

      10            2410          12.5         580    5.8            72

## 3. Формулы, caps и hitbox

AttackInterval = BaseAttackInterval / (1 + BonusAttackSpeed) 1.0 / 1.25
= 0.8с при +25% AS Esaki: 2.0 / 1.25 = 1.6с

Attack Speed складывается аддитивно. Cap +100%. Basic Lifesteal cap 35%;
Spell Vamp cap 30%; item Move Speed cap +30%. Slow не складываются -
применяется сильнейший. В стартовой версии Armor/Magic Resistance
отсутствуют.

Герой Basic Q E F R

Jason Физ. Физ. Физ. - Физ.

Shelianer Физ. Физ. Маг. DoT Move Физ.

Esaki Физ. Физ. Физ. - Физ.

Amelia Маг. Маг. Маг. - Маг.

Loki Физ. Физ. Utility - Физ.

Герой Hitbox width Hitbox height

Shelianer 0.60 1.80

Amelia 0.60 1.80

Esaki 0.65 1.85

Jason 0.75 2.25

Loki 0.95 2.70

## 4. Предметы

Физические Цена Статы

Железный клинок 1800 +35 Attack; +150 HP

Клинок берсерка 2100 +25 Attack; +25% AS

Кровопийца 2300 +30 Attack; +15% Basic Lifesteal

Клык охотника 2000 +20 Attack; +300 HP; +10% Move

Крушитель 2500 +55 Attack

Боевые наручи 2200 +20 Attack; +20% AS; +200 HP

Защитные Цена Статы

Броня титана 2100 +650 HP

Стальной нагрудник 1900 +450 HP; +10 Attack

Сердце великана 2400 +850 HP

Боевой панцирь 2100 +500 HP; +10% AS

Кровавая броня 2300 +450 HP; +10% Basic Lifesteal

Доспех преследователя 2200 +400 HP; +8% Move

Магические Цена Статы

Ледяной кристалл 1900 +30 AP; +150 Max Mana

Посох мудрости 2200 +35 AP; +250 Max Mana; +1 Mana/s

Сфера архимага 2500 +60 AP; +200 Max Mana

Кристалл вечности 2100 +350 Max Mana; +3 Mana/s

Кровавый гримуар 2400 +35 AP; +12% Spell Vamp

Магические Цена Статы

Сфера боевого мага 2200 +30 AP; +300 HP; +150 Max Mana

Сапоги Цена Статы

Сапоги путешественника 900 +15% Move

Боевые сапоги 1200 +10% Move; +15% AS

## 5. Навыки

### Общие правила rank scaling

- Значения v1.0 сохранены как ориентир максимальной силы способности и переразложены по рангам.
- Scaling от Attack/AP, range, projectile speed, hit timing, CC type и основная механика не меняются между рангами, если таблица прямо не говорит обратное.
- Mana/CD могут меняться только по таблице.
- Rank 0 означает, что способность не изучена и не может быть использована.
- Q/E обычных героев имеют maxRank 4; R maxRank 2.
- Shelianer: Q=3, E=3, F=2, R=2.

### Jason

Basic: 100% Attack; interval 1.0с; range2.

#### Q - Вампирический удар

| Rank | Base Damage | Scaling | CD | Mana | Heal |
|---|---:|---:|---:|---:|---:|
| 1 | 60 | 100% Attack | 8с | - | 30% нанесенного урона |
| 2 | 70 | 100% Attack | 8с | - | 33% |
| 3 | 80 | 100% Attack | 8с | - | 36% |
| 4 | 90 | 100% Attack | 8с | - | 40% |

#### E - Double Dash

| Rank | E2 Base Damage | Scaling | Shared CD | Dash | E2 CC |
|---|---:|---:|---:|---:|---|
| 1 | 40 | 60% Attack | 9с | 2 блока | knock-up1 + stun0.7с |
| 2 | 50 | 60% Attack | 9с | 2 | knock-up1 + stun0.8с |
| 3 | 60 | 60% Attack | 8.5с | 2 | knock-up1 + stun0.9с |
| 4 | 70 | 60% Attack | 8с | 2 | knock-up1 + stun1.0с |

Окно второго E: 5с на всех рангах.

#### R - Meteor Crash

| Rank | Base Damage | Scaling | CD | Flight | Radius | Stun |
|---|---:|---:|---:|---:|---:|---:|
| 1 | 110 | 80% Attack | 24с | 5 блоков | 3 | 1.25с |
| 2 | 140 | 80% Attack | 20с | 5 блоков | 3 | 1.5с |

### Shelianer

Basic: 100% Attack; interval 1.0с; range2.

#### Q - Throwing Knife

| Rank | Base Damage | Scaling | CD | Mana | Range |
|---|---:|---:|---:|---:|---:|
| 1 | 35 | 75% Attack | 2с fixed | 35 | 7 |
| 2 | 40 | 75% Attack | 2с fixed | 35 | 7 |
| 3 | 45 | 75% Attack | 2с fixed | 35 | 7 |

#### E - Poison Flask

| Rank | Damage per tick | Ticks | Scaling per tick | CD | Mana | Slow |
|---|---:|---:|---:|---:|---:|---:|
| 1 | 18 | 4 | 10% Attack | 13с | 70 | 15% |
| 2 | 22 | 4 | 10% Attack | 12.5с | 70 | 18% |
| 3 | 25 | 4 | 10% Attack | 12с | 70 | 20% |

Magic DoT duration: 4с.

#### F - Dash

| Rank | Damage | CD | Mana | Dash |
|---|---:|---:|---:|---:|
| 1 | 0 | 4с | 40 | 2 блока |
| 2 | 0 | 3с | 40 | 2 блока |

#### R - Death Dance

| Rank | Damage per hit | Hits | Scaling per hit | CD | Mana | Target range | Stun |
|---|---:|---:|---:|---:|---:|---:|---:|
| 1 | 24 | 6 | 23% Attack | 34с | 120 | 3 | 1.25с |
| 2 | 30 | 6 | 23% Attack | 30с | 120 | 3 | 1.5с |

### Esaki

Basic: 100% Attack; interval 2.0с; range5; movement lock до выпуска; projectile speed13.

#### Q - Boulder

| Rank | Base Damage | Scaling | CD | Mana | Range | Speed |
|---|---:|---:|---:|---:|---:|---:|
| 1 | 70 | 110% Attack | 9с | 75 | 7 | 8 |
| 2 | 80 | 110% Attack | 9с | 75 | 7 | 8 |
| 3 | 90 | 110% Attack | 8.5с | 75 | 7 | 8 |
| 4 | 100 | 110% Attack | 8с | 75 | 7 | 8 |

#### E - Earth Repulse

| Rank | Base Damage | Scaling | CD | Mana | Radius | Knockback |
|---|---:|---:|---:|---:|---:|---:|
| 1 | 40 | 50% Attack | 13с | 60 | 3 | 1.5 |
| 2 | 47 | 50% Attack | 13с | 60 | 3 | 1.7 |
| 3 | 54 | 50% Attack | 12.5с | 60 | 3 | 1.85 |
| 4 | 60 | 50% Attack | 12с | 60 | 3 | 2 |

#### R - Earthquake

| Rank | Damage per pulse | Pulses | Scaling per pulse | CD | Mana | Radius |
|---|---:|---:|---:|---:|---:|---:|
| 1 | 55 | 3 | 35% Attack | 44с | 140 | 6 |
| 2 | 70 | 3 | 35% Attack | 40с | 140 | 6 |

Duration 3с; nausea visual effect.

### Amelia

Basic: 100% Attack magical; interval 1.0с; range5; projectile speed20.

#### Q - Frost Breath

| Rank | Base per tick | Scaling per tick | Ticks | CD | Mana | Cone | Slow |
|---|---:|---:|---:|---:|---:|---:|---:|
| 1 | 23 | 12% AP | 6 | 13с | 90 | 5 | 25% |
| 2 | 27 | 12% AP | 6 | 13с | 90 | 5 | 30% |
| 3 | 31 | 12% AP | 6 | 12.5с | 90 | 5 | 35% |
| 4 | 35 | 12% AP | 6 | 12с | 90 | 5 | 40% |

Tick interval: 0.5с.

#### E - Blizzard Burst

| Rank | Base Damage | Scaling | CD | Mana | Radius | Knockback |
|---|---:|---:|---:|---:|---:|---:|
| 1 | 60 | 45% AP | 13с | 75 | 4 | 2.5 |
| 2 | 70 | 45% AP | 13с | 75 | 4 | 3.0 |
| 3 | 80 | 45% AP | 12.5с | 75 | 4 | 3.5 |
| 4 | 90 | 45% AP | 12с | 75 | 4 | 4.0 |

#### R - Ice Spikes

| Rank | Base Damage | Scaling | CD | Mana | Target range | Radius | Delay |
|---|---:|---:|---:|---:|---:|---:|---:|
| 1 | 175 | 90% AP | 34с | 130 | 4 | 2 | 0.55с |
| 2 | 220 | 90% AP | 30с | 130 | 4 | 2 | 0.55с |

### Loki

Basic: 100% Attack; interval 1.0с; range2.5.

#### Q - Hook

| Rank | Base Damage | Scaling | CD | Mana | Range | Out/Return speed |
|---|---:|---:|---:|---:|---:|---|
| 1 | 45 | 70% Attack | 12с | 70 | 9 | 15/18 |
| 2 | 55 | 70% Attack | 11.5с | 70 | 9 | 15/18 |
| 3 | 63 | 70% Attack | 11с | 70 | 9 | 15/18 |
| 4 | 70 | 70% Attack | 10с | 70 | 9 | 15/18 |

#### E - Rampage

| Rank | CD | Mana | Duration | Move bonus |
|---|---:|---:|---:|---:|
| 1 | 14с | 50 | 4с | +12% |
| 2 | 13.5с | 50 | 4с | +14% |
| 3 | 13с | 50 | 4с | +16% |
| 4 | 12с | 50 | 4с | +18% |

#### R - Iron Prison

| Rank | Base Damage | Scaling | CD | Mana | Radius | Root |
|---|---:|---:|---:|---:|---:|---:|
| 1 | 75 | 40% Attack | 34с | 110 | 4 | 1.5с |
| 2 | 100 | 40% Attack | 30с | 110 | 4 | 2.0с |

## 6. Projectile speed, collision и hit timing

Атака Speed

Shelianer Q 24 блок/с

Shelianer E 11 блок/с

Esaki Basic 13 блок/с

Esaki Q 8 блок/с

Amelia Basic 20 блок/с

Loki Q outbound 15 блок/с

Loki Q return 18 блок/с

Союзные герои/крипы игнорируются. Первая допустимая вражеская цель
останавливает Shelianer Q, Esaki Basic/Q и Amelia Basic. Стена
уничтожает projectile; Loki Q при стене/максимальной дальности начинает
возврат. Обычные skill projectiles не повреждают здания.

Действие Hit timing

Jason Basic 0.25с

Jason Q 0.30с

Jason E2 collision

Jason R landing

Shelianer Basic 0.18с

Shelianer Q 0.15с

Shelianer E 0.25с

Shelianer R 6 damage ticks

Esaki Basic 2.0с

Esaki Q 0.65с

Esaki E 0.30с

Amelia Basic 0.25с

Amelia Q каждые 0.5с

Amelia E 0.35с

Amelia R 0.55с

Loki Basic 0.30с

Loki Q 0.35с

Loki R 0.50с

Stun/knock-up прерывают незавершенный cast. Root блокирует dash/Flash,
но не обычный cast без перемещения. Esaki Basic до 2.0с прерывается hard
CC без выпуска камня.

## 7. Спеллы

Спелл CD Эффект

Вспышка 120с 5 блоков; через стену при безопасной точке

Возмездие 35с 500 + 50 x LVL по jungle mob

Очищение 90с снимает stun/root/slow

Регенерация 75с 25% Max HP

## 8. Линейные крипы и экономика

Крип HP Damage Interval Range Gold XP

Воин 300 20 1.0с 1.5 24 24

Стрелок 200 25 1.0с 6 20 20

Осадный 550 35 2.0с 7 50 40

Первая волна 00:30. Обычная волна 3+2; каждая третья + siege. HP +8% и
Damage +5% каждые 2 минуты до 20-й.

Событие Gold XP

Passive 3/с с 00:30 -

Hero Kill 300 160 + VictimLVL x20

Assist 100 50% kill XP

Tower team 150 каждому 100 каждому

Tower last hit +100 -

Lane last hit = 100% gold; союзник рядом без last hit = 25%. XP radius
10: 1 герой 100%, 2 героя 80% каждому, 3+ 60% каждому.

## 9. Jungle

Mob HP Dmg Int. Gold XP Respawn Leash

Red 1000 45 1.5с 150 220 180с 9

Blue 1000 40 1.5с 150 220 180с 9

Green1 700 35 1.5с 130 190 120с 8

Green2 700 35 1.5с 130 190 120с 8

Cont.1 500 28 1.4с 110 160 90с 7

Cont.2 500 28 1.4с 110 160 90с 7

Green1 special: 55 dmg, CD7, knockback2. Green2: 50 dmg, CD8, knock-up1.
Scaling: +10% MaxHP/+6% damage каждые 3 мин до 18:00.

### Jungle Buffs

**Red Buff**
- Назначение: прямое усиление исходящего урона героя.
- Duration: 120с.
- Эффект снимается после смерти.
- Точное ранее согласованное числовое значение отсутствует в доступных исходных файлах. Чтобы реализация не блокировалась, для первого тестового билда используется **PROVISIONAL +10% ко всему исходящему hero damage** (basic + hero abilities, но не true damage фонтана/structures). Это значение хранится только в `jungle.json` и должно быть легко заменено без изменения кода.

**Blue Buff**
- +15% Max Mana.
- +10% Cooldown Reduction для hero abilities.
- Jason не имеет Mana и получает только +10% CDR.
- Duration: 120с.
- Эффект снимается после смерти.
- CDR применяется к новым cooldown, не сокращает уже запущенный cooldown задним числом.

Green/Contested camps в v1.1 не дают постоянного hero buff, если отдельно не указано в конфиге.

## 10. Башни, трон, фонтан и vision

Объект HP Dmg creeps Attack radius Vision Interval

T1 3200 120 10 18 2с

T2 4200 145 10.5 18 2с

T3 5200 170 11 18 2с

Трон 7000 200 11.5 20 2с

По героям: 26% MaxHP за hit для не-Танка, 17% для Танка. Без вражеской
волны -80% входящего hero damage. Только basic attacks повреждают
structures. T1-\>T2-\>T3; после любой enemy T3 трон уязвим. Фонтан: +20%
MaxHP и +20% MaxMana в секунду. Enemy fountain danger-zone: 35% MaxHP
true damage каждые 0.5с.

     Vision source                                          Radius

     Hero                                                   16

     Lane creep                                             10

     Tower                                                  18

     Throne                                                 20

     Fountain                                               20

Bush attack reveal 1.0с. Атака allied creep/tower раскрывает enemy и
держит reveal еще 2с после прекращения действия.

### Respawn

       LVL                                                 Respawn

       1                                                   5с

       2                                                   6с

       3                                                   7с

       4                                                   9с

       5                                                   11с

       6                                                   13с

       7                                                   16с

       8                                                   19с

       9                                                   22с

       10                                                  25с

## 11. Метрики плейтеста

Измерять: timing LVL2/4/10, GPM, TTK всех пар героев, mana per combo,
first jungle clear, first T1, T3/throne timing, deaths to tower,
full-build timing, average match duration.
