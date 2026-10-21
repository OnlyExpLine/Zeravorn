import fs from 'node:fs';
import assert from 'node:assert/strict';
import {png} from '../lane/core.mjs';
import {publish,json,write,atlas} from './pipeline.mjs';
import {throne,fountain,projectiles,environment} from './geometry.mjs';
import {validate} from './validate.mjs';

await import('../towers/generate.mjs');
await import('../towers/validate.mjs');
const phases=[['throne',[throne()],'D','Трон'],['fountain',[fountain()],'E','Фонтан'],['projectiles',projectiles(),'F','Снаряды и VFX'],['environment',environment(),'G','Окружение']];
for(const [category,entries,phase,title]of phases){
 const stats=publish(category,entries,`ZERAVORN / PHASE ${phase} — ${title}`),report=validate(category);
 const lines=[`# ZERAVORN — Phase ${phase} / ${title}`,'','Дата: 2026-10-20. Первая версия для отложенного визуального ревью.','',`Создано моделей: **${stats.length}**. Статус: **исходники и экспорты подготовлены; к игровому рендереру не подключены**.`,'','| Модель | Варианты | Кубоиды | Клипы | Высота, блоки |','|---|---|---:|---:|---:|',...stats.map(s=>`| ${s.name} | ${s.variants.join(', ')} | ${s.cuboids} | ${s.clips.length} | ${s.height_blocks.toFixed(2)} |`),'','Для каждой модели сохранены `.bbmodel`, PNG 128×128 и emissive PNG, cuboid/animation JSON, glTF, четыре ракурса, силуэты и motion sheets для клипов.','',`- [Просмотрщик](../../assets/source/blockbench/${category}/previews/review.html)`,`- [Общий рендер](../../assets/source/blockbench/${category}/previews/lineup.png)`,`- [Отчёт валидации](../../assets/source/blockbench/${category}/validation.json)`,`- [Статус интеграции](../../assets/source/blockbench/${category}/integration.json)`,'',`Техническая проверка: PASS; ${report.results.reduce((n,r)=>n+r.sampled_poses,0)} sampled poses. Проверены обязательный набор, UUID, иерархия, UV, бюджеты, ключевые кадры, замыкание циклов, source/export parity, PNG, glTF buffers и синтаксис просмотрщика.`,'','## Ограничения','', 'Визуальное соответствие референсам и проверка в GUI Blockbench/Minecraft отложены. Просмотрщик показывает реальную геометрию и ключевые кадры, но не частицы и динамический emissive. Не подтверждены отсутствие взаимных пересечений деталей и производительность в игре.','', 'Gameplay/config, collision, hitboxes, damage и серверные правила не изменены. Наличие экспортов в resources не означает автоматическое отображение в Fabric. Требуются регистрация рендерера, выбор состояний и обработка визуальных событий.','', 'Повторная сборка: `node tools/blockbench/world/build.mjs`. Перед генерацией предыдущие файлы сохраняются в `assets/backups/<category>/<timestamp>/`.'];
 if(category==='throne')lines.push('','Состояния: idle/protected/vulnerable/attack/hit/low_hp/destroy. Защитный щит, искры, трещины и финальная вспышка описаны событиями; частицы/видимость щита требуют runtime. Фрагменты основного кристалла остаются в разрушенной позе до очистки сервером.');
 if(category==='fountain')lines.push('','Состояния: idle/heal/danger; четыре плавающих осколка. Центральный луч и частицы лечения требуют runtime.');
 if(category==='projectiles')lines.push('','Локальное направление полёта: -Z. Основная геометрия — 3–12 кубоидов; trail, impact, shield/heal/danger sprites и параметры визуального lifetime лежат в `world.vfx.json`. Скорость, урон, радиус и столкновения задаются сервером.');
 if(category==='environment')lines.push('','21 модуль: четыре куста, три дерева, четыре камня, семь руин и три фонаря. Фонари имеют blue/red варианты и idle sway. Декоративные модели сами по себе не задают collision и границы gameplay-кустов.');
 write(`docs/assets/${category.toUpperCase()}_PHASE_${phase}.md`,lines.join('\n')+'\n');
}

// Small transparent sprites are authored independently from model geometry.
const effects={};
for(const variant of ['blue','red','fire','poison','basic'])for(const kind of ['trail','impact','ring','spark']){
 const size=32,pixels=new Uint8Array(size*size*4),color=atlas(variant).pixels.slice((96*128)*4,(96*128)*4+3);
 for(let y=0;y<size;y++)for(let x=0;x<size;x++){
  const dx=(x-15.5)/15.5,dy=(y-15.5)/15.5,r=Math.hypot(dx,dy);let a=0;
  if(kind==='ring')a=Math.max(0,1-Math.abs(r-.76)/.12);
  if(kind==='impact')a=Math.max(0,1-r)*Math.max(.2,1-Math.abs(dx*dy)*7);
  if(kind==='trail')a=Math.max(0,1-Math.abs(dy)*4)*Math.pow((1-dx)/2,1.5);
  if(kind==='spark')a=Math.max(0,1-Math.abs(dx)*5)*Math.max(0,1-Math.abs(dy));
  const i=(y*size+x)*4;pixels.set([...color,Math.round(a*230)],i);
 }
 const alphas=Array.from({length:size*size},(_,i)=>pixels[i*4+3]);assert(alphas.some(a=>a===0)&&alphas.some(a=>a>0&&a<255));
 const file=`${variant}_${kind}.png`,bytes=png(size,size,pixels);write(`assets/source/textures/projectiles/vfx/${file}`,bytes);write(`src/main/resources/assets/zeravorn/projectiles/vfx/${file}`,bytes);assert.deepEqual(fs.readFileSync(`assets/source/textures/projectiles/vfx/${file}`),fs.readFileSync(`src/main/resources/assets/zeravorn/projectiles/vfx/${file}`));effects[variant+'_'+kind]={texture:'vfx/'+file,render:kind==='ring'?'horizontal_billboard':'camera_billboard',visual_lifetime_seconds:kind==='trail'?.22:.5};
}
json('src/main/resources/assets/zeravorn/projectiles/world.vfx.json',{schema:'zeravorn.world.vfx.v1',authority:'presentation_only',status:'assets_only_not_registered',effects,projectiles:{projectile_basic:{variant:'basic',trail:'basic_trail',impact:'basic_impact'},projectile_ice:{variant:'blue',trail:'blue_trail',impact:'blue_spark'},projectile_fire:{variant:'fire',trail:'fire_trail',impact:'fire_impact'},projectile_poison:{variant:'poison',trail:'poison_trail',impact:'poison_ring'},tower_shot:{variant:'server_team',trail:'{team}_trail',impact:'{team}_impact'},throne_shot:{variant:'server_team',trail:'{team}_trail',impact:'{team}_ring',additional_shards:true}},event_mapping:{CORE_PULSE:'emissive intensity cycle',PROJECTILE_RELEASE:'spawn core with colored trail',CORE_OVERLOAD:'impact',DEBRIS_BURST:'bounded stone fragments',SHIELD_ACTIVE:'ring shell around shield_origin',SHIELD_COLLAPSE:'ring fade',CRYSTAL_CRACK:'core crack material and sparks',CRACK_SPARKS:'spark',NEXUS_BURST:'impact and core shards',CORE_EXTINGUISH:'emissive fade',HEAL_PARTICLES:'rising spark',DANGER_BEAM:'beam to server target'}});
json('assets/source/blockbench/projectiles/vfx.validation.json',{status:'pass',sprites:Object.keys(effects).length,checks:['transparent and visible pixels','source/runtime byte parity'],runtime:'not registered'});
const doc=['# ZERAVORN — Asset phases C–G','', 'Дата: 2026-10-20. Создание продолжено по указанию пользователя; визуальная приёмка отложена.','', '| Фаза | Набор | Статус |','|---|---|---|','| C | T1/T2/T3, blue/red | Исходники, экспорты, анимации, превью и техническая проверка |','| D | Трон, blue/red | То же; щит/вспышки требуют runtime |','| E | Фонтан, blue/red | То же; луч/лечение требуют runtime |','| F | 6 видов снарядов / 8 вариантов + 20 VFX sprites | Экспорты и визуальные metadata; частицы не зарегистрированы |','| G | 21 модуль окружения / 24 варианта | Экспорты и превью; размещение на карте отдельно |','', 'Новые фазы не подключены к Minecraft. Текущие временные башни, трон и декорации карты этим проходом не заменяются. Техническая валидация проверяет файлы и трансформации, а не художественную приёмку или FPS.','', '- [Башни](TOWERS_PHASE_C.md)','- [Трон](THRONE_PHASE_D.md)','- [Фонтан](FOUNTAIN_PHASE_E.md)','- [Снаряды/VFX](PROJECTILES_PHASE_F.md)','- [Окружение](ENVIRONMENT_PHASE_G.md)','', 'Генератор: `node tools/blockbench/world/build.mjs`. Повторная генерация сохраняет предыдущие результаты в assets/backups.'];
write('docs/assets/PHASES_C_G_STATUS.md',doc.join('\n')+'\n');
console.log('C–G asset build complete. Runtime integration and visual acceptance remain open.');
