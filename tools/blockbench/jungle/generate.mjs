import fs from 'node:fs';
import path from 'node:path';
import {createModels} from './geometry.mjs';
import {animate} from './animations.mjs';
import {atlas,png,hierarchy,cubeVertices,faceIndices,triangles,uuid,Model} from './core.mjs';
import {render,join} from './preview.mjs';
import {exportGltf} from './export_gltf.mjs';
import {writeViewer} from './viewer.mjs';
const root='assets/source/blockbench/jungle',runtime='src/main/resources/assets/zeravorn/jungle';
const write=(p,data)=>{fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,data);};
const json=(p,data)=>write(p,JSON.stringify(data,null,2));
if(fs.existsSync(root)){
 const backup='assets/backups/jungle/'+new Date().toISOString().replace(/[:.]/g,'-');fs.mkdirSync(backup,{recursive:true});
 for(const [src,dst] of [[root,'blockbench'],['assets/source/textures/jungle','textures'],[runtime,'runtime']])if(fs.existsSync(src))fs.cpSync(src,backup+'/'+dst,{recursive:true});
 console.log('Backup: '+backup);
}
const models=createModels().map(m=>{
 animate(m);const model=m.save();
 for(const a of model.animations){const keys=[],count=Math.ceil(a.length*60);for(let i=0;i<=count;i++){
  const time=i===count?a.length:a.length*i/count,minY=Math.min(...triangles(model,a.name,time).flatMap(t=>t.points.map(v=>v[1])));
  keys.push({uuid:uuid(a.name+'.ground.'+i),channel:'position',time,interpolation:'linear',data_points:[{x:'0',y:String(Math.max(0,.04-minY)),z:'0'}]});
 }a.animators[m.bones.root.uuid]={name:'root',type:'bone',keyframes:keys};}
 return model;
});
const stats=[];
for(const m of models){
 const kind=m.zeravorn.kind,dir=root+'/'+kind,{bones,parents,owners}=hierarchy(m);
 json(dir+'/'+m.name+'.bbmodel',m);
 const clips={schema:'zeravorn.jungle.animation.v1',authority:'presentation_only',units_per_block:16,rotation_convention:'Blockbench: negate X/Y for right-handed XYZ matrices',clips:m.animations.map(a=>({name:a.name,length:a.length,loop:a.loop,tracks:Object.fromEntries(Object.values(a.animators).map(tr=>[tr.name,tr.keyframes.map(k=>({channel:k.channel,time:k.time,value:['x','y','z'].map(c=>Number(k.data_points[0][c])),interpolation:k.interpolation}))])),events:a.zeravorn_event_markers}))};
 json(dir+'/'+m.name+'.animations.json',clips);json(runtime+'/'+m.name+'.animations.json',clips);
 json(runtime+'/'+m.name+'.cubes.json',{schema:'zeravorn.jungle.cuboids.v1',units_per_block:16,front:'-Z',authority:'presentation_only',bones:bones.map(b=>({name:b.name,parent:parents[b.uuid]?.name??null,pivot:b.origin})),cubes:m.elements.map(e=>({name:e.name,bone:owners[e.uuid].name,from:e.from,to:e.to,pivot:e.origin,rotation:e.rotation,faces:e.faces})),texture:m.name+'.png',emissive:m.name+'_emissive.png'});
 for(const em of [false,true]){const filename=m.name+(em?'_emissive':'')+'.png',bytes=png(128,128,atlas(kind,em));write('assets/source/textures/jungle/'+kind+'/'+filename,bytes);write(runtime+'/'+filename,bytes);}
 const mesh={...m,elements:m.elements.map(e=>{const verts=cubeVertices(e);return {...e,type:'mesh',vertices:Object.fromEntries(verts.map((v,i)=>['v'+i,v])),faces:Object.fromEntries(Object.entries(faceIndices).map(([f,ids])=>{const [u,v,U,V]=e.faces[f].uv,uv=[[u,v],[U,v],[U,V],[u,V]];return [f,{vertices:ids.map(i=>'v'+i),uv:Object.fromEntries(ids.map((id,i)=>['v'+id,uv[i]]))}];}))};})};
 exportGltf(mesh,runtime+'/'+m.name+'.gltf');const gltf=JSON.parse(fs.readFileSync(runtime+'/'+m.name+'.gltf'));
 gltf.images.push({uri:m.name+'_emissive.png'});gltf.textures.push({sampler:0,source:1});gltf.materials[0].emissiveTexture={index:1};gltf.materials[0].emissiveFactor=[1,1,1];json(runtime+'/'+m.name+'.gltf',gltf);
 const camera={width:580,height:660,span:kind==='green_2'?60:46,target:[0,kind==='green_2'?24:18,0]};
 const panels=[];
 for(const [name,yaw,pitch] of [['front',0,.08],['three_quarter',-.65,.2],['side',-Math.PI/2,.08],['back',Math.PI,.1]]){const p=render(m,{...camera,yaw,pitch});panels.push(p);write(dir+'/previews/'+name+'.png',png(p.width,p.height,p.pixels));}
 write(dir+'/previews/turnaround.png',join(panels));
 write(dir+'/previews/silhouettes.png',join([0,-.65,-Math.PI/2,Math.PI].map(yaw=>render(m,{...camera,yaw,silhouette:true}))));
 for(const a of m.animations){const short=a.name.split('.').at(-1);write(dir+'/previews/'+short+'_motion.png',join([0,.25,.5,.75,1].map(t=>render(m,{...camera,width:340,height:420,span:camera.span+22,yaw:-.65,pitch:.17,clip:a.name,time:a.length*t}))));if(['attack','special'].includes(short)){const p=render(m,{...camera,span:camera.span+12,yaw:-.65,pitch:.17,clip:a.name,time:short==='special'?.45:.4});write(dir+'/previews/'+short+'_pose.png',png(p.width,p.height,p.pixels));}}
 const tri=triangles(m),bounds=[0,1,2].map(i=>[Math.min(...tri.flatMap(t=>t.points.map(v=>v[i]))),Math.max(...tri.flatMap(t=>t.points.map(v=>v[i])))]);
 stats.push({name:m.name,source:dir+'/'+m.name+'.bbmodel',bones:bones.length,elements:m.elements.length,triangles:tri.length,texture_resolution:'128x128',meshes_in_source:0,team_variants:'none; neutral jungle monster',animations:m.animations.map(a=>a.name),sockets:bones.filter(b=>/origin|core/.test(b.name)).map(b=>b.name),bounds,height_blocks:(bounds[1][1]-bounds[1][0])/16});
}
write(root+'/previews/lineup.png',join(models.map(m=>render(m,{width:500,height:600,target:[0,24,0],span:60,yaw:-.55,pitch:.16})),4));
write(root+'/previews/silhouette_lineup.png',join(models.map(m=>render(m,{width:500,height:600,target:[0,24,0],span:60,yaw:0,pitch:.05,silhouette:true})),4));
// A neutral 1.8-block measurement mannequin; no existing hero assets are read or changed.
const scale=new Model('scale_reference');scale.bone('root');
for(const [n,c,s] of [['head',[0,25.2,0],[7.2,7.2,7.2]],['body',[0,17.4,0],[8,8.4,4]],['left_arm',[5.5,17.4,0],[3,8.4,4]],['right_arm',[-5.5,17.4,0],[3,8.4,4]],['left_leg',[2,6.6,0],[3.5,13.2,4]],['right_leg',[-2,6.6,0],[3.5,13.2,4]]])scale.box(n,'root',c,s,'rockLight');
write(root+'/previews/scale_comparison.png',join([...models,scale.save()].map(m=>render(m,{width:500,height:600,target:[0,24,0],span:60,yaw:-.55,pitch:.16})),5));
// Lightweight independent VFX sprites, no gameplay radius/damage/CC values.
for(const effect of ['shockwave','root_eruption']){
 const size=32,pixels=new Uint8Array(size*size*4);
 for(let y=0;y<size;y++)for(let x=0;x<size;x++){const dx=(x-15.5)/15.5,dy=(y-15.5)/15.5,r=Math.hypot(dx,dy),i=(y*size+x)*4;let alpha=0;if(effect==='shockwave')alpha=r>.65&&r<.94?Math.max(0,1-Math.abs(r-.8)/.15)*255:0;else alpha=y>2&&y<30&&Math.abs(x-16)<(y/5+1)?(x%3===0?230:180):0;pixels.set(effect==='shockwave'?[134,235,84,Math.round(alpha)]:[139,170,91,Math.round(alpha)],i);}
 const bytes=png(size,size,pixels);write('assets/source/textures/jungle/vfx/'+effect+'.png',bytes);write(runtime+'/vfx/'+effect+'.png',bytes);
}
json(runtime+'/jungle.vfx.json',{schema:'zeravorn.jungle.vfx.v1',status:'assets_only_not_registered',authority:'presentation_only',effects:{KNOCKBACK_SHOCKWAVE:{texture:'vfx/shockwave.png',socket:'vfx_origin',render:'horizontal expanding billboard',visual_lifetime_seconds:.45},ROOT_ERUPTION:{texture:'vfx/root_eruption.png',socket:'vfx_origin',render:'upward billboard burst',visual_lifetime_seconds:.6},CORE_PULSE:{render:'emissive intensity modulation',socket:'energy_core'},CORE_EXTINGUISH:{render:'fade emissive to zero',socket:'energy_core'}}});
json(root+'/statistics.json',stats);
json(root+'/integration.json',{status:'exported_not_connected_to_game_renderer',mapping:{red_buff:'RED',blue_buff:'BLUE',green_1:'GREEN_A',green_2:'GREEN_B'},config_source:'src/main/resources/config_defaults/jungle.json',excluded:['CONTESTED_A','CONTESTED_B','bosses'],authority:'server owns damage, CC, movement, hitboxes and leash; clip events are visual only',open_requirements:['Blockbench GUI import','Minecraft renderer and render-state sync','debug gallery','VFX registration','emissive pulse/fade','in-game animation/clipping/scale/performance validation'],phase_a_status:'unchanged; integration remains open'});
writeViewer(models,root+'/previews/jungle_review.html');
console.log(JSON.stringify(stats.map(({name,bones,elements,height_blocks})=>({name,bones,elements,height_blocks})),null,2));
