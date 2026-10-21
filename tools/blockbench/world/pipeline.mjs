import fs from 'node:fs';
import path from 'node:path';
import {Model,png,hierarchy,cubeVertices,faceIndices,triangles,uuid} from '../lane/core.mjs';
import {exportGltf} from '../lane/export_gltf.mjs';
import {render,join} from '../lane/preview.mjs';
import {writeViewer} from './viewer.mjs';
export {Model};
export const write=(file,data)=>{fs.mkdirSync(path.dirname(file),{recursive:true});fs.writeFileSync(file,data);};
export const json=(file,data)=>write(file,JSON.stringify(data,null,2));
export function atlas(variant='blue'){
 const colors=['2857a0','5f86bd','1e324d','727987','b4bdc6','303943','c4a064','796442','849087','4c564a','624c3d','74634b','48c7ff','c4f4ff','151d25','29549a'];
 if(variant==='red')for(const [i,c] of [[0,'a3324c'],[1,'cf596a'],[2,'4f2234'],[12,'ff3a60'],[13,'ffd1d9'],[15,'8f2940']])colors[i]=c;
 if(variant==='forest')for(const [i,c] of [[0,'456a38'],[1,'71994f'],[2,'263f2d'],[8,'82905c'],[9,'475e3e'],[15,'4c6839']])colors[i]=c;
 if(variant==='fire'||variant==='basic'){colors[12]='ff701e';colors[13]='fff0a5';colors[0]='ad341c';}
 if(variant==='poison'){colors[12]='70e936';colors[13]='dcff9a';colors[0]='3d871f';}
 const pixels=new Uint8Array(128*128*4),emissive=new Uint8Array(pixels.length);
 for(let tile=0;tile<16;tile++)for(let y=0;y<32;y++)for(let x=0;x<32;x++){
  const i=((Math.floor(tile/4)*32+y)*128+tile%4*32+x)*4,rgb=colors[tile].match(/../g).map(v=>parseInt(v,16));
  let shade=.92+(((Math.floor(x/4)*7+Math.floor(y/4)*11+tile*3)%11)-5)*.012;
  if(x<2||y<2)shade+=.09;if(x>29||y>29)shade-=.1;
  if(tile===10||tile===11)shade+=(Math.floor(x/2)+Math.floor(y/8))%5===0?-.18:0;
  if(tile===12)shade+=x<16-y*.3?.10:-.10;
  for(let c=0;c<3;c++){pixels[i+c]=Math.min(255,Math.round(rgb[c]*shade));emissive[i+c]=tile===12||tile===13?rgb[c]:0;}
  pixels[i+3]=255;emissive[i+3]=tile===12||tile===13?255:0;
 }
 return {pixels,emissive};
}
export function clip(m,name,length,loop,tracks,events=[]){
 const full=`animation.zeravorn.${m.name}.${name}`,a={uuid:uuid(full),name:full,length,loop,override:false,snapping:40,animators:{},zeravorn_event_markers:events};
 for(const [bone,channels] of Object.entries(tracks)){
  if(!m.bones[bone])throw Error('Missing bone '+bone);
  const tr={name:bone,type:'bone',keyframes:[]};
  for(const [channel,keys] of Object.entries(channels))for(const [time,v] of keys){if(v.length!==3||!v.every(Number.isFinite))throw Error(full+' invalid vector');tr.keyframes.push({uuid:uuid(full+bone+channel+time),channel,time,interpolation:'linear',data_points:[Object.fromEntries(['x','y','z'].map((c,i)=>[c,String(v[i])]))]});}
  a.animators[m.bones[bone].uuid]=tr;
 }m.animations.push(a);
}
export const R=rotation=>({rotation}),P=position=>({position});
export const event=(name,time,socket)=>({name,time,socket,authority:'visual_only'});
export function crystal(m,name,bone,[x,y,z],width,height){
 for(let i=0;i<9;i++){const t=(i+.5)/9,wide=width*(.18+.82*(1-Math.abs(t*2-1)));m.box(`${name}_${i}`,bone,[x,y-height/2+height*t,z],[wide,height/9+.025,wide],i%3===0?'glow':'crystal',[0,45,0]);}
}
export function ring(m,name,bone,y,radius,width,mat='gold',segments=12){
 for(let i=0;i<segments;i++){const a=i*2*Math.PI/segments;m.box(`${name}_${i}`,bone,[Math.sin(a)*radius,y,Math.cos(a)*radius],[width,2,radius*2*Math.sin(Math.PI/segments)+1],mat,[0,a*180/Math.PI+90,0]);}
}
export function banner(m,name,parent,x,y,z,w=9,h=24){
 m.bone(name,parent,[x,y,z]);m.box(name+'_pole',name,[x,y+1,z],[w+4,1.4,2],'gold');m.box(name+'_cloth',name,[x,y-h/2,z],[w,h,.9],'team');
 for(const s of [-1,1])m.box(name+'_edge_'+s,name,[x+s*w/2,y-h/2,z-.6],[.7,h,.5],'gold');m.box(name+'_sigil',name,[x,y-h*.32,z-.8],[w*.32,w*.32,.6],'gold',[0,0,45]);
}

export function publish(category,entries,title){
 const root=`assets/source/blockbench/${category}`,runtime=`src/main/resources/assets/zeravorn/${category}`,textures=`assets/source/textures/${category}`;
 if(fs.existsSync(root)){const backup=`assets/backups/${category}/`+new Date().toISOString().replace(/[:.]/g,'-');for(const [source,name]of[[root,'blockbench'],[runtime,'runtime'],[textures,'textures']])if(fs.existsSync(source))fs.cpSync(source,`${backup}/${name}`,{recursive:true});}
 const models=[],stats=[];
 for(const entry of entries){
  const {model:m,variants=['blue','red']}=entry,name=m.name,model=m.save(),dir=`${root}/${name}`,{bones,parents,owners}=hierarchy(model);
  model.zeravorn={...model.zeravorn,category,variants,phase:entry.phase,authoring_status:'first_pass_pending_visual_review'};
  model.textures=variants.map((v,i)=>({name:`${name}_${v}.png`,id:String(i),uuid:uuid(name+v),mode:'bitmap',source:'data:image/png;base64,'+png(128,128,atlas(v).pixels).toString('base64'),internal:true,saved:true,width:128,height:128,uv_width:128,uv_height:128}));
  json(`${dir}/${name}.bbmodel`,model);models.push(model);
  const animations={schema:'zeravorn.world.animation.v1',authority:'presentation_only',units_per_block:16,rotation_convention:'Blockbench: negate X/Y for right-handed XYZ matrices',clips:model.animations.map(a=>({name:a.name,length:a.length,loop:a.loop,tracks:Object.fromEntries(Object.values(a.animators).map(t=>[t.name,t.keyframes.map(k=>({channel:k.channel,time:k.time,value:['x','y','z'].map(c=>Number(k.data_points[0][c])),interpolation:k.interpolation}))])),events:a.zeravorn_event_markers}))};
  json(`${dir}/${name}.animations.json`,animations);json(`${runtime}/${name}.animations.json`,animations);
  json(`${runtime}/${name}.cubes.json`,{schema:'zeravorn.world.cuboids.v1',authority:'presentation_only',units_per_block:16,front:'-Z',bones:bones.map(b=>({name:b.name,parent:parents[b.uuid]?.name??null,pivot:b.origin})),cubes:model.elements.map(e=>({name:e.name,bone:owners[e.uuid].name,from:e.from,to:e.to,pivot:e.origin,rotation:e.rotation,faces:e.faces})),textures:Object.fromEntries(variants.map(v=>[v,`${name}_${v}.png`])),emissive:Object.fromEntries(variants.map(v=>[v,`${name}_${v}_emissive.png`]))});
  const mesh={...model,elements:model.elements.map(e=>{const verts=cubeVertices(e);return {...e,type:'mesh',vertices:Object.fromEntries(verts.map((v,i)=>['v'+i,v])),faces:Object.fromEntries(Object.entries(faceIndices).map(([f,ids])=>{const [u,v,U,V]=e.faces[f].uv,uv=[[u,v],[U,v],[U,V],[u,V]];return [f,{vertices:ids.map(i=>'v'+i),uv:Object.fromEntries(ids.map((id,i)=>['v'+id,uv[i]]))}];}))};})};
  const points=triangles(model).flatMap(t=>t.points),bounds=[0,1,2].map(i=>[Math.min(...points.map(p=>p[i])),Math.max(...points.map(p=>p[i]))]),target=bounds.map(b=>(b[0]+b[1])/2),span=Math.max(...bounds.map(b=>b[1]-b[0]))*1.5;
  model.zeravorn.preview={target,span};
  for(const [i,variant]of variants.entries()){
   const tex=atlas(variant);
   for(const [suffix,pixels]of[['',tex.pixels],['_emissive',tex.emissive]]){const file=`${name}_${variant}${suffix}.png`,bytes=png(128,128,pixels);write(`${textures}/${name}/${file}`,bytes);write(`${runtime}/${file}`,bytes);}
   const file=`${runtime}/${name}_${variant}.gltf`;exportGltf({...mesh,textures:[model.textures[i]]},file);const g=JSON.parse(fs.readFileSync(file));g.asset.generator='Zeravorn world asset pipeline';g.materials[0].name=category+'_atlas';if(!g.animations.length)delete g.animations;for(const n of g.nodes)if(n.children?.length===0)delete n.children;g.images.push({uri:`${name}_${variant}_emissive.png`});g.textures.push({sampler:0,source:1});g.materials[0].emissiveTexture={index:1};g.materials[0].emissiveFactor=[1,1,1];json(file,g);
   const panels=[];
   for(const [view,yaw,pitch]of[['front',0,.07],['three_quarter',-.65,.22],['side',-Math.PI/2,.07],['back',Math.PI,.07]]){const p=render(model,{width:300,height:360,target,span,texture:tex.pixels,yaw,pitch});panels.push(p);write(`${dir}/previews/${variant}_${view}.png`,png(p.width,p.height,p.pixels));}
   write(`${dir}/previews/${variant}_turnaround.png`,join(panels));
  }
  write(`${dir}/previews/silhouette.png`,join([0,-.65,-Math.PI/2,Math.PI].map(yaw=>render(model,{width:240,height:300,target,span,yaw,silhouette:true}))));
  for(const a of model.animations)write(`${dir}/previews/${a.name.split('.').at(-1)}_motion.png`,join([0,.25,.5,.75,1].map(t=>render(model,{width:220,height:280,target,span:span*1.15,texture:atlas(variants[0]).pixels,yaw:-.65,pitch:.2,clip:a.name,time:a.length*t}))));
  stats.push({name,variants,bones:bones.length,cuboids:model.elements.length,triangles:model.elements.length*12,clips:model.animations.map(a=>a.name),bounds,height_blocks:(bounds[1][1]-bounds[1][0])/16});
 }
 write(`${root}/previews/lineup.png`,join(models.flatMap(m=>m.zeravorn.variants.map(v=>render(m,{width:300,height:360,...m.zeravorn.preview,texture:atlas(v).pixels,yaw:-.65,pitch:.22}))),Math.min(4,models.reduce((n,m)=>n+m.zeravorn.variants.length,0))));
 writeViewer(models,`${root}/previews/review.html`,title);json(`${root}/statistics.json`,stats);
 json(`${root}/integration.json`,{status:'exported_not_connected_to_game_renderer',authority:'presentation_only',units_per_block:16,front:'-Z',not_verified:['Blockbench GUI import','Minecraft renderer and state sync','in-game scale','clipping','runtime VFX','performance'],models:stats.map(s=>({name:s.name,variants:s.variants}))});
 console.log(`${category}: ${models.length} models, ${stats.reduce((n,s)=>n+s.cuboids,0)} cuboids`);
 return stats;
}
