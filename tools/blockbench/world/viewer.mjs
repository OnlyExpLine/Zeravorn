import fs from 'node:fs';
import {hierarchy,sample,rotation,multiply,transform,sub,boneMatrices,cubeVertices,triangles,faceIndices,norm,cross,dot} from '../lane/core.mjs';
import {render} from '../lane/preview.mjs';

export function writeViewer(models,file,title){
 const helpers=[hierarchy,sample,rotation,multiply,transform,sub,boneMatrices,cubeVertices,triangles,norm,cross,dot,render].map(f=>`const ${f.name}=${f.toString()};`).join('\n');
 const html=`<!doctype html><html lang="ru"><meta charset="utf-8"><title>${title}</title>
 <style>body{margin:0;background:#101927;color:#d4e1ef;font:16px system-ui}header{padding:18px 24px;background:#182538}h1{font-size:22px}select,button,input{margin:4px;padding:7px;background:#25374d;color:white;border:1px solid #60748c}canvas{display:block;width:100%;height:70vh;image-rendering:pixelated}p{margin:12px 24px}a{color:#80c6ff}</style>
 <header><h1>${title}</h1><select id="model"></select><select id="variant"></select><select id="clip"></select><button id="play">Играть</button><button id="front">Спереди</button><button id="side">Сбоку</button><button id="back">Сзади</button><label>Время <input type="range" id="time" min="0" max="1" step=".001" value="0"></label><label><input id="sil" type="checkbox">Силуэт</label></header>
 <canvas id="view"></canvas><p id="status">Загрузка…</p><p>Вращение мышью; колесо — масштаб. Геометрия, текстуры и ключевые кадры из сохранённых моделей. Частицы и динамическое свечение требуют игрового рендерера.</p><p><a href="lineup.png">Общий рендер</a></p><script>
 const models=${JSON.stringify(models)},faceIndices=${JSON.stringify(faceIndices)};
 ${helpers}
 const $=id=>document.getElementById(id),canvas=$('view'),ctx=canvas.getContext('2d'),cache=[];
 let index=0,yaw=-.65,pitch=.18,span=100,target=[0,45,0],playing=false,time=0,last=0,drag=null,dirty=true;
 models.forEach((m,i)=>{$('model').add(new Option(m.name,i));cache[i]=[];m.textures.forEach((t,j)=>{const im=new Image();im.onload=()=>{const c=document.createElement('canvas');c.width=c.height=128;const x=c.getContext('2d');x.drawImage(im,0,0);cache[i][j]=x.getImageData(0,0,128,128).data;dirty=true};im.src=t.source})});
 function select(){const m=models[index],points=triangles(m).flatMap(t=>t.points);const bound=[0,1,2].map(i=>[Math.min(...points.map(p=>p[i])),Math.max(...points.map(p=>p[i]))]);span=Math.max(...bound.map(b=>b[1]-b[0]))*1.45;target=bound.map(b=>(b[0]+b[1])/2);$('variant').innerHTML='';m.textures.forEach((t,j)=>$('variant').add(new Option(t.name,j)));$('clip').innerHTML='';$('clip').add(new Option('Исходная поза',''));m.animations.forEach(a=>$('clip').add(new Option(a.name.split('.').at(-1),a.name)));time=0;dirty=true;}
 select();$('model').onchange=()=>{index=Number($('model').value);select()};$('variant').onchange=()=>dirty=true;$('clip').onchange=()=>{time=0;dirty=true};$('sil').onchange=()=>dirty=true;
 $('play').onclick=()=>{playing=!playing;$('play').textContent=playing?'Пауза':'Играть';if(time===1)time=0;dirty=true};
 for(const [id,angle] of [['front',0],['side',-Math.PI/2],['back',Math.PI]])$(id).onclick=()=>{yaw=angle;pitch=.05;dirty=true};
 $('time').oninput=()=>{time=Number($('time').value);playing=false;$('play').textContent='Играть';dirty=true};
 canvas.onpointerdown=e=>{drag=[e.clientX,e.clientY];canvas.setPointerCapture(e.pointerId)};canvas.onpointerup=()=>drag=null;
 canvas.onpointermove=e=>{if(drag){yaw+=(e.clientX-drag[0])*.01;pitch=Math.max(-1,Math.min(1,pitch+(e.clientY-drag[1])*.008));drag=[e.clientX,e.clientY];dirty=true}};
 canvas.onwheel=e=>{e.preventDefault();span=Math.max(2,Math.min(400,span*Math.exp(e.deltaY*.001)));dirty=true};
 window.onresize=()=>dirty=true;
 function frame(ms){const dt=Math.min(.15,(ms-last)/1000);last=ms;const m=models[index],clip=m.animations.find(a=>a.name===$('clip').value),length=clip?.length||1;if(playing&&clip){time+=dt/length;if(time>1)time=clip.loop==='loop'?time%1:1;dirty=true;}$('time').value=time;
 const texture=cache[index][Number($('variant').value)];if(dirty&&texture){const w=Math.min(800,canvas.clientWidth),h=Math.max(1,Math.round(w*canvas.clientHeight/canvas.clientWidth));canvas.width=w;canvas.height=h;const p=render(m,{width:w,height:h,yaw,pitch,span,target,texture,clip:clip?.name||'',time:time*length,silhouette:$('sil').checked});ctx.putImageData(new ImageData(new Uint8ClampedArray(p.pixels),w,h),0,0);$('status').textContent=m.name+' | '+(clip?.name.split('.').at(-1)||'исходная поза')+' | '+(time*length).toFixed(2)+' с | '+m.elements.length+' кубоидов';dirty=false;}requestAnimationFrame(frame);}
 requestAnimationFrame(frame);
 </script></html>`;
 fs.writeFileSync(file,html);
}
