import fs from 'node:fs';
import {hierarchy,sample,rotation,multiply,transform,sub,boneMatrices,cubeVertices,triangles,faceIndices} from './core.mjs';
export function writeViewer(models,file){
 const helpers=[hierarchy,sample,rotation,multiply,transform,sub,boneMatrices,cubeVertices,triangles].map(f=>`const ${f.name}=${f.toString()};`).join('\n');
 fs.writeFileSync(file,`<!doctype html><html lang="ru"><meta charset="utf-8"><title>ZERAVORN — Lane asset review</title><style>body{margin:0;background:#101927;color:#d4e1ef;font:16px system-ui}header{padding:18px 24px;background:#182538}h1{font-size:22px;margin:0 0 12px}select,button,input{margin:4px;padding:7px;background:#25374d;color:white;border:1px solid #60748c}canvas{display:block;width:100%;height:70vh}p{margin:12px 24px;max-width:1100px}a{color:#80c6ff}</style><header><h1>ZERAVORN / PHASE A — проверка моделей</h1><select id="model"></select><select id="team"><option>blue</option><option>red</option></select><select id="clip"></select><button id="play">Пауза</button><button id="front">Спереди</button><button id="side">Сбоку</button><button id="back">Сзади</button><label>Время <input type="range" id="time" min="0" max="1" step=".001" value="0"></label><label><input id="sil" type="checkbox">Силуэт</label></header><canvas id="view"></canvas><p id="status"></p><p>Перетащите мышью для вращения. Колесо — масштаб. Это локальная проверка реальной геометрии и анимаций, не Minecraft. PNG-превью используют Z-buffer; интерактивный canvas сортирует грани по глубине и может показывать артефакты пересечения. 16 единиц = 1 блок.</p><p><a href="lineup.png">Общий lineup: сверху Blue, снизу Red; Warrior → Ranged → Siege</a></p><script>
const models=${JSON.stringify(models)};const faceIndices=${JSON.stringify(faceIndices)};
${helpers}
const $=id=>document.getElementById(id),canvas=$('view'),ctx=canvas.getContext('2d');let index=0,yaw=-.65,pitch=.18,span=34,playing=true,time=0,last=0,drag=null;
models.forEach((m,i)=>$('model').add(new Option(m.name,i)));
const images=models.map(m=>m.textures.map(t=>{const im=new Image();im.src=t.source;return im;}));
function clips(){ $('clip').innerHTML='';$('clip').add(new Option('Bind pose',''));models[index].animations.forEach(a=>$('clip').add(new Option(a.name.split('.').at(-1),a.name)));time=0; }
clips();$('model').onchange=()=>{index=Number($('model').value);clips()};$('clip').onchange=()=>time=0;
$('play').onclick=()=>{playing=!playing;$('play').textContent=playing?'Пауза':'Играть'};
$('front').onclick=()=>{yaw=0;pitch=.04};$('side').onclick=()=>{yaw=-Math.PI/2;pitch=.04};$('back').onclick=()=>{yaw=Math.PI;pitch=.04};
$('time').oninput=()=>{time=Number($('time').value);playing=false;$('play').textContent='Играть'};
canvas.onpointerdown=e=>{drag=[e.clientX,e.clientY];canvas.setPointerCapture(e.pointerId)};canvas.onpointerup=()=>drag=null;
canvas.onpointermove=e=>{if(drag){yaw+=(e.clientX-drag[0])*.01;pitch=Math.max(-1,Math.min(1,pitch+(e.clientY-drag[1])*.008));drag=[e.clientX,e.clientY]}};
canvas.onwheel=e=>{e.preventDefault();span=Math.max(18,Math.min(65,span+e.deltaY*.025))};
function frame(ms){const dt=Math.min(.05,(ms-last)/1000);last=ms;const m=models[index],clip=m.animations.find(a=>a.name===$('clip').value),length=clip?.length||1;if(playing){time+=dt/length;if(time>1)time=clip?.loop==='loop'?time%1:1;}$('time').value=time;
 const w=canvas.clientWidth,h=canvas.clientHeight;if(canvas.width!==w||canvas.height!==h){canvas.width=w;canvas.height=h;}ctx.fillStyle=$('sil').checked?'#e8ebf0':'#101927';ctx.fillRect(0,0,w,h);
 const cy=Math.cos(yaw),sy=Math.sin(yaw),cp=Math.cos(pitch),sp=Math.sin(pitch),scale=h/span;const proj=v=>{const x=v[0],y=v[1]-12,z=v[2],a=cy*x+sy*z,b=-sy*x+cy*z;return [w/2+a*scale,h/2-(cp*y+sp*b)*scale,cp*b-sp*y]};
 const tris=triangles(m,clip?.name||'',time*length).map(t=>({...t,p:t.points.map(proj)}));tris.sort((a,b)=>b.p.reduce((s,p)=>s+p[2],0)-a.p.reduce((s,p)=>s+p[2],0));const im=images[index][$('team').value==='red'?1:0];
 for(const t of tris){const p=t.p,u=t.uv;ctx.save();ctx.beginPath();ctx.moveTo(p[0][0],p[0][1]);ctx.lineTo(p[1][0],p[1][1]);ctx.lineTo(p[2][0],p[2][1]);ctx.closePath();ctx.clip();
 if($('sil').checked){ctx.fillStyle='#000';ctx.fillRect(0,0,w,h);}else if(im.complete){const det=u[0][0]*(u[1][1]-u[2][1])+u[1][0]*(u[2][1]-u[0][1])+u[2][0]*(u[0][1]-u[1][1]);if(Math.abs(det)>1e-6){const coef=k=>[(p[0][k]*(u[1][1]-u[2][1])+p[1][k]*(u[2][1]-u[0][1])+p[2][k]*(u[0][1]-u[1][1]))/det,(p[0][k]*(u[2][0]-u[1][0])+p[1][k]*(u[0][0]-u[2][0])+p[2][k]*(u[1][0]-u[0][0]))/det,(p[0][k]*(u[1][0]*u[2][1]-u[2][0]*u[1][1])+p[1][k]*(u[2][0]*u[0][1]-u[0][0]*u[2][1])+p[2][k]*(u[0][0]*u[1][1]-u[1][0]*u[0][1]))/det];const a=coef(0),b=coef(1);ctx.setTransform(a[0],b[0],a[1],b[1],a[2],b[2]);ctx.imageSmoothingEnabled=false;ctx.drawImage(im,0,0);}}
 ctx.restore();}
 $('status').textContent=m.name+' | '+(clip?clip.name.split('.').at(-1):'bind')+' | '+(time*length).toFixed(2)+' s | '+m.elements.length+' cuboids | '+hierarchy(m).bones.length+' bones';requestAnimationFrame(frame);
}requestAnimationFrame(frame);
</script></html>`);
}
