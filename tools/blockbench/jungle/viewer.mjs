import fs from 'node:fs';
import {hierarchy,sample,rotation,multiply,transform,sub,boneMatrices,cubeVertices,triangles,faceIndices,norm,cross,dot} from './core.mjs';
import {render} from './preview.mjs';
export function writeViewer(models,file){
 const helpers=[hierarchy,sample,rotation,multiply,transform,sub,boneMatrices,cubeVertices,triangles,norm,cross,dot,render].map(f=>`const ${f.name}=${f.toString()};`).join('\n');
 fs.writeFileSync(file,`<!doctype html><html lang="ru"><meta charset="utf-8"><title>ZERAVORN — Jungle review</title><style>body{margin:0;background:#101927;color:#d4e1ef;font:16px system-ui}header{padding:18px 24px;background:#182538}h1{font-size:22px;margin:0 0 12px}select,button,input{margin:4px;padding:7px;background:#25374d;color:white;border:1px solid #60748c}canvas{display:block;width:100%;height:70vh;image-rendering:pixelated}p{margin:12px 24px;max-width:1100px}a{color:#80c6ff}</style><header><h1>ZERAVORN / PHASE B — проверка моделей</h1><select id="model"></select><select id="clip"></select><button id="play">Пауза</button><button id="front">Спереди</button><button id="side">Сбоку</button><button id="back">Сзади</button><label>Время <input type="range" id="time" min="0" max="1" step=".001" value="0"></label><label><input id="sil" type="checkbox">Силуэт</label></header><canvas id="view"></canvas><p id="status">Загрузка текстур…</p><p>Перетащите мышью для вращения. Колесо — масштаб. Реальная геометрия и ключевые кадры, software Z-buffer. Это локальная проверка моделей, не Minecraft. VFX и пульсация emissive здесь не проигрываются. 16 единиц = 1 блок.</p><p><a href="lineup.png">Lineup: Red Buff → Blue Buff → Green 1 → Green 2</a> · <a href="scale_comparison.png">Сравнение с фигурой 1.8 блока</a></p><script>
const models=${JSON.stringify(models)};const faceIndices=${JSON.stringify(faceIndices)};
${helpers}
const $=id=>document.getElementById(id),canvas=$('view'),ctx=canvas.getContext('2d'),atlasCache={};let index=0,yaw=-.65,pitch=.18,span=62,playing=true,time=0,last=0,drag=null;
const atlas=kind=>atlasCache[kind];
models.forEach((m,i)=>{$('model').add(new Option(m.name,i));const im=new Image();im.onload=()=>{const c=document.createElement('canvas');c.width=128;c.height=128;const x=c.getContext('2d');x.drawImage(im,0,0);atlasCache[m.zeravorn.kind]=x.getImageData(0,0,128,128).data};im.src=m.textures[0].source;});
function clips(){ $('clip').innerHTML='';$('clip').add(new Option('Bind pose',''));models[index].animations.forEach(a=>$('clip').add(new Option(a.name.split('.').at(-1),a.name)));time=0; }
clips();$('model').onchange=()=>{index=Number($('model').value);clips()};$('clip').onchange=()=>time=0;
$('play').onclick=()=>{playing=!playing;$('play').textContent=playing?'Пауза':'Играть'};
$('front').onclick=()=>{yaw=0;pitch=.04};$('side').onclick=()=>{yaw=-Math.PI/2;pitch=.04};$('back').onclick=()=>{yaw=Math.PI;pitch=.04};
$('time').oninput=()=>{time=Number($('time').value);playing=false;$('play').textContent='Играть'};
canvas.onpointerdown=e=>{drag=[e.clientX,e.clientY];canvas.setPointerCapture(e.pointerId)};canvas.onpointerup=()=>drag=null;
canvas.onpointermove=e=>{if(drag){yaw+=(e.clientX-drag[0])*.01;pitch=Math.max(-1,Math.min(1,pitch+(e.clientY-drag[1])*.008));drag=[e.clientX,e.clientY]}};
canvas.onwheel=e=>{e.preventDefault();span=Math.max(30,Math.min(95,span+e.deltaY*.025))};
function frame(ms){const dt=Math.min(.2,(ms-last)/1000);last=ms;const m=models[index],clip=m.animations.find(a=>a.name===$('clip').value),length=clip?.length||1;if(playing&&clip){time+=dt/length;if(time>1)time=clip.loop==='loop'?time%1:1;}$('time').value=time;
 const w=Math.min(960,canvas.clientWidth),h=Math.round(w*canvas.clientHeight/canvas.clientWidth);if(canvas.width!==w||canvas.height!==h){canvas.width=w;canvas.height=h;}
 if(atlasCache[m.zeravorn.kind]){const p=render(m,{width:w,height:h,yaw,pitch,span,target:[0,24,0],clip:clip?.name||'',time:time*length,silhouette:$('sil').checked});ctx.putImageData(new ImageData(new Uint8ClampedArray(p.pixels),w,h),0,0);$('status').textContent=m.name+' | '+(clip?clip.name.split('.').at(-1):'bind')+' | '+(time*length).toFixed(2)+' s | '+m.elements.length+' cuboids | '+hierarchy(m).bones.length+' bones';}
 requestAnimationFrame(frame);
}requestAnimationFrame(frame);
</script></html>`);
}
