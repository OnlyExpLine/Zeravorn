#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { output } from './generate_jason_blockbench.mjs';
import { repository } from './generate_jason_blockbench.mjs';
import { render, sheet } from './jason/preview.mjs';

const third=JSON.parse(fs.readFileSync(path.join(output,'jason.bbmodel'),'utf8'));
const first=JSON.parse(fs.readFileSync(path.join(output,'jason_first_person.bbmodel'),'utf8'));
const preview=path.join(output,'previews');fs.mkdirSync(preview,{recursive:true});
fs.writeFileSync(path.join(preview,'jason_three_quarter.png'),render(third).buffer);
fs.writeFileSync(path.join(preview,'jason_turnaround.png'),sheet(third,[{angle:0},{angle:35},{angle:90},{angle:180}]));
fs.writeFileSync(path.join(preview,'jason_face.png'),render(third,{center:[0,32.8,0],span:9,width:800,height:800,angle:10}).buffer);
fs.writeFileSync(path.join(preview,'jason_animations.png'),sheet(third,third.animations.map(a=>({animation:a.name,time:a.name==='jason.death'?1.6:a.name==='jason.r'?.86:a.name==='jason.q'?.30:a.name.includes('basic')?.25:a.length*.35})),{span:45}));
fs.writeFileSync(path.join(preview,'jason_r_phases.png'),sheet(third,[.18,.45,.86,1.3].map(time=>({animation:'jason.r',time})),{span:44}));
fs.writeFileSync(path.join(preview,'jason_first_person.png'),render(first,{firstPerson:true,width:1280,height:720,animation:'jason.fp.idle'}).buffer);
const runtime=fs.readFileSync(path.join(repository,'tools/jason/runtime.mjs'),'utf8').replace(/^export /gm,'');
const viewer=fs.readFileSync(path.join(repository,'tools/jason/viewer.js'),'utf8');
fs.writeFileSync(path.join(preview,'jason_viewer.html'),`<!doctype html><html lang="ru"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Jason · Source model inspector</title><style>
*{box-sizing:border-box}body{margin:0;background:#171b21;color:#e8dfce;font:14px system-ui}header{padding:16px 24px;background:#242830;display:flex;gap:18px;align-items:center}header strong{letter-spacing:3px;color:#d0a377}select,button{background:#323943;color:#eee;border:1px solid #51545a;border-radius:4px;padding:8px}button{cursor:pointer}main{position:relative;height:calc(100vh - 172px)}canvas{display:block;width:100%;height:100%;touch-action:none}#crosshair{position:absolute;left:50%;top:50%;color:#eee;pointer-events:none}footer{padding:12px 24px}nav{display:flex;gap:14px;align-items:center}input{flex:1}#events{font-size:12px;color:#bcab96;margin-top:10px}small{color:#aaa;margin-left:auto}
</style><header><strong>JASON / ZERAVORN</strong><select id="rig"><option value="0">Third person</option><option value="1">First person</option></select><select id="clip"></select><button id="front">Спереди</button><button id="quarter">¾</button><button id="back">Сзади</button><small>Поворот — мышь · Масштаб — колесо</small></header><main><canvas></canvas><span id="crosshair" hidden>+</span></main><footer><nav><button id="play">▶</button><input id="scrub" aria-label="Animation time" type="range" min="0" step="0.005"><span id="readout"></span></nav><div id="events"></div></footer><script>const MODELS=${JSON.stringify([third,first])};\n${runtime}\n${viewer}</script></html>`);
console.log('Rendered actual .bbmodel geometry, embedded atlas UVs and animation transforms to previews/.');
