import fs from 'node:fs';
import path from 'node:path';
import {createModels} from './geometry.mjs';
import {animate} from './animations.mjs';
import {atlas,png,hierarchy,cubeVertices,faceIndices,triangles,uuid} from './core.mjs';
import {render,join} from './preview.mjs';
import {exportGltf} from './export_gltf.mjs';
import {writeViewer} from './viewer.mjs';

const root='assets/source/blockbench/lane',runtime='src/main/resources/assets/zeravorn/lane';
const write=(file,data)=>{fs.mkdirSync(path.dirname(file),{recursive:true});fs.writeFileSync(file,data);};
const json=(file,data)=>write(file,JSON.stringify(data,null,2));
// Preserve the entire previous lane output before regeneration, including manual edits.
if(fs.existsSync(root)){
 const backup='assets/backups/lane/'+new Date().toISOString().replace(/[:.]/g,'-');
 fs.mkdirSync(backup,{recursive:true});fs.cpSync(root,backup+'/blockbench',{recursive:true});
 for(const [src,dst] of [['assets/source/textures/lane','textures'],[runtime,'runtime']])if(fs.existsSync(src))fs.cpSync(src,backup+'/'+dst,{recursive:true});
 console.log('Previous assets backed up to '+backup);
}
const models=createModels().map(m=>{
 animate(m);const model=m.save();
 // Author a grounded root track from the actual posed cuboids. This is visual lift,
 // never entity movement, collision or gameplay root motion.
 for(const clip of model.animations){const keys=[],count=Math.ceil(clip.length*80);for(let i=0;i<=count;i++){
  const time=i===count?clip.length:clip.length*i/count,minY=Math.min(...triangles(model,clip.name,time).flatMap(t=>t.points.map(p=>p[1])));
  keys.push({uuid:uuid(clip.name+'.ground.'+i),channel:'position',time,interpolation:'linear',data_points:[{x:'0',y:String(Math.max(0,-minY+.015)),z:'0'}]});
 }clip.animators[m.bones.root.uuid]={name:'root',type:'bone',keyframes:keys};}
 return model;
}),stats=[];
for(const model of models){
 const kind=model.name.replace('lane_',''),dir=root+'/'+kind;
 json(dir+'/'+model.name+'.bbmodel',model);
 const {bones,parents,owners}=hierarchy(model);
 const animationExport={schema:'zeravorn.lane.animation.v1',units:'Blockbench authoring units; 16 = 1 block',rotation_convention:'Blockbench: negate X/Y for right-handed XYZ matrices',authority:'presentation_only',clips:model.animations.map(a=>({name:a.name,length:a.length,loop:a.loop,tracks:Object.fromEntries(Object.entries(a.animators).map(([id,v])=>[v.name,v.keyframes.map(({channel,time,data_points,interpolation})=>({channel,time,value:['x','y','z'].map(k=>Number(data_points[0][k])),interpolation}))])),events:a.zeravorn_event_markers}))};
 json(dir+'/'+model.name+'.animations.json',animationExport);json(runtime+'/'+model.name+'.animations.json',animationExport);
 json(runtime+'/'+model.name+'.cubes.json',{schema:'zeravorn.lane.cuboids.v1',units_per_block:16,front:'-Z',authority:'presentation_only',bones:bones.map(b=>({name:b.name,parent:parents[b.uuid]?.name??null,pivot:b.origin})),cubes:model.elements.map(e=>({name:e.name,bone:owners[e.uuid].name,from:e.from,to:e.to,pivot:e.origin,rotation:e.rotation,faces:e.faces})),textures:{blue:model.name+'_blue.png',red:model.name+'_red.png'},emissive:{blue:model.name+'_blue_emissive.png',red:model.name+'_red_emissive.png'}});
 // Export render triangles from the exact editable cuboids; no mesh elements in the source.
 const meshModel={...model,elements:model.elements.map(e=>{const verts=cubeVertices(e);return {...e,type:'mesh',vertices:Object.fromEntries(verts.map((v,i)=>['v'+i,v])),faces:Object.fromEntries(Object.entries(faceIndices).map(([name,ids])=>{const [u,v,U,V]=e.faces[name].uv,uv=[[u,v],[U,v],[U,V],[u,V]];return [name,{vertices:ids.map(i=>'v'+i),uv:Object.fromEntries(ids.map((id,i)=>['v'+id,uv[i]]))}];}))};})};
 for(const team of ['blue','red']){
  for(const emissive of [false,true]){const filename=model.name+'_'+team+(emissive?'_emissive':'')+'.png',data=png(128,128,atlas(team,emissive));write('assets/source/textures/lane/'+kind+'/'+filename,data);write(runtime+'/'+filename,data);}
  exportGltf({...meshModel,textures:[model.textures[team==='red'?1:0]]},runtime+'/'+model.name+'_'+team+'.gltf');
  const gltf=JSON.parse(fs.readFileSync(runtime+'/'+model.name+'_'+team+'.gltf'));
  gltf.images.push({uri:model.name+'_'+team+'_emissive.png'});gltf.textures.push({sampler:0,source:1});gltf.materials[0].emissiveTexture={index:1};gltf.materials[0].emissiveFactor=[1,1,1];
  json(runtime+'/'+model.name+'_'+team+'.gltf',gltf);
  const views=[['front',0,.04],['three_quarter',-.65,.2],['side',-Math.PI/2,.06],['back',Math.PI,.06]];
  const panels=[];
  for(const [name,yaw,pitch] of views){const p=render(model,{yaw,pitch,team,span:32});panels.push(p);write(dir+'/previews/'+team+'_'+name+'.png',png(p.width,p.height,p.pixels));}
  write(dir+'/previews/'+team+'_turnaround.png',join(panels));
  const combat=render(model,{team,yaw:-.65,pitch:.2,span:35,clip:'animation.zeravorn.'+model.name+'.attack',time:kind==='warrior'?.22:kind==='ranged'?.55:.44});write(dir+'/previews/'+team+'_combat.png',png(combat.width,combat.height,combat.pixels));
 }
 write(dir+'/previews/silhouettes.png',join([0,-.65,-Math.PI/2,Math.PI].map(yaw=>render(model,{yaw,span:32,silhouette:true}))));
 for(const clip of model.animations){write(dir+'/previews/'+clip.name.split('.').at(-1)+'_motion.png',join([0,.25,.5,.75,1].map(t=>render(model,{width:300,height:380,span:36,yaw:-.65,pitch:.15,clip:clip.name,time:clip.length*t}))));}
 stats.push({name:model.name,bones:bones.length,elements:model.elements.length,triangles:model.elements.length*12,texture_resolution:'128x128',variants:['blue','red'],meshes_in_source:0,animations:model.animations.map(a=>a.name),sockets:bones.filter(b=>/socket|origin/.test(b.name)).map(b=>b.name),source:dir+'/'+model.name+'.bbmodel'});
}
write(root+'/previews/lineup.png',join(['blue','red'].flatMap(team=>models.map(model=>render(model,{team,width:480,height:530,yaw:-.55,pitch:.18,span:32}))),3));
json(root+'/statistics.json',stats);
json(root+'/integration.json',{status:'exported_not_connected_to_game_renderer',scale:'16 authoring units = 1 block',front:'-Z',clips_are_visual_only:true,server_controls:['attack start','release timing','damage','hitbox','movement'],required_renderer_features:['hierarchical cuboid transforms','per-face UV','team texture selection','emissive pass','linear keyframe interpolation','socket world transforms','death hold and despawn'],not_verified:['Blockbench GUI import','Minecraft dev-client rendering','runtime clipping','in-game performance'],notes:'No lane entity renderer or render-state synchronization exists in this checkout. glTF and cuboid JSON are exports, not automatic Fabric integration. Playback rate must be driven by existing server attack cadence, not clip duration.'});
writeViewer(models,root+'/previews/lane_review.html');
console.log(JSON.stringify(stats.map(({name,bones,elements,triangles})=>({name,bones,elements,triangles})),null,2));
