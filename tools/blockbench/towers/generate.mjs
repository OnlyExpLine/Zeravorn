import fs from 'node:fs';
import path from 'node:path';
import {Model, atlas as laneAtlas, png, hierarchy, cubeVertices, faceIndices, uuid, triangles} from '../lane/core.mjs';
import {exportGltf} from '../lane/export_gltf.mjs';
import {render, join} from '../lane/preview.mjs';
import {writeViewer} from '../world/viewer.mjs';

const root = 'assets/source/blockbench/towers';
const runtime = 'src/main/resources/assets/zeravorn/towers';
const textures = 'assets/source/textures/towers';
const write = (file, data) => { fs.mkdirSync(path.dirname(file), {recursive:true}); fs.writeFileSync(file, data); };
const json = (file, value) => write(file, JSON.stringify(value, null, 2));

// Preserve authored files before rebuilding generated output.
if (fs.existsSync(root)) {
  const backup='assets/backups/towers/'+new Date().toISOString().replace(/[:.]/g,'-');
  for(const [source,name] of [[root,'blockbench'],[runtime,'runtime'],[textures,'textures']])
    if(fs.existsSync(source)) fs.cpSync(source,`${backup}/${name}`,{recursive:true});
}

// The existing 16-tile team atlas remains compatible with the lane previewer.
// These colors emphasize stone, iron and gilded reinforcement over cloth.
function towerAtlas(team, emissive=false) {
  const pixels = laneAtlas(team, emissive);
  const colors = team === 'blue'
    ? ['#2555a1','#427ac5','#173451','#737887','#a6a9b0','#303745','#c7a15d','#80643c','#878995','#525967','#574c44','#68594e','#55c7ff','#a9eaff','#151d2c','#244d9b']
    : ['#9b334a','#ca5365','#572334','#737887','#a6a9b0','#303745','#c7a15d','#80643c','#878995','#525967','#574c44','#68594e','#ff4269','#ffc0ce','#151d2c','#902c48'];
  for (let tile=0; tile<16; tile++) for (let y=0; y<32; y++) for (let x=0; x<32; x++) {
    const i=(((Math.floor(tile/4)*32+y)*128)+(tile%4*32+x))*4;
    const rgb=colors[tile].match(/[0-9a-f]{2}/gi).map(h=>parseInt(h,16));
    const variation=.91+(((Math.floor(x/4)*7+Math.floor(y/4)*11+tile*13)%9)-4)*.009 + (x<2||y<2?.08:0);
    const glow=tile===12||tile===13;
    for (let c=0;c<3;c++) pixels[i+c]=emissive?(glow?rgb[c]:0):Math.min(255,Math.round(rgb[c]*variation));
    pixels[i+3]=emissive&&!glow?0:255;
  }
  return pixels;
}

function makeTower(tier) {
  const m=new Model(`tower_t${tier}`), h=[0,68,82,98][tier], w=[0,31,39,47][tier];
  const b=(name,parent,pivot=[0,0,0])=>m.bone(name,parent,pivot);
  const box=(name,bone,center,size,mat,rot=[0,0,0])=>m.box(name,bone,center,size,mat,rot);
  b('root'); b('base','root'); b('body','base'); b('upper_body','body',[0,h-30,0]);
  b('crystal_mount','upper_body',[0,h-14,0]); b('crystal','crystal_mount',[0,h-9,0]);
  b('rotating_element','upper_body',[0,h-18,0]); b('weapon_core','crystal',[0,h-7,-2]);
  b('vfx_origin','weapon_core',[0,h-7,-7]); b('projectile_origin','weapon_core',[0,h-7,-10]);
  b('banner_left','upper_body',[-w*.62,h-25,0]); b('banner_right','upper_body',[w*.62,h-25,0]);
  // Layered square plinth, stepped stone shaft, and an open crystal cage.
  for (const [n,y,wide,deep,mat] of [
    ['foot',2,w+7,w+7,'dark'],['foundation',5,w+3,w+3,'steel'],['molding',8,w+5,w+5,'goldDark'],
    ['lower',13,w-5,w-5,'steel'],['socket',19,w-9,w-9,'dark']])
    box(n,'base',[0,y,0],[wide,y===13?10:4,deep],mat);
  box('shaft','body',[0,(h+20)/2,0],[w*.34,h-26,w*.34],'dark');
  // Separate masonry courses and projecting ribs keep the shaft architectural.
  for(let y=23;y<h-25;y+=8)for(const s of [-1,1]){
    box(`stone_course_x_${y}_${s}`,'body',[s*w*.20,y,0],[w*.12,7,w*.48],'steel');
    box(`stone_course_z_${y}_${s}`,'body',[0,y,s*w*.20],[w*.27,7,w*.12],'steel');
  }
  for (const y of [23,h-34,h-24]) {
    box(`band_${y}`,'body',[0,y,0],[w*.72,2,w*.72],'goldDark');
    box(`front_crest_${y}`,'body',[0,y,-w*.37],[w*.20,3,1.5],'gold');
  }
  for (const s of [-1,1]) for (const front of [-1,1]) {
    const x=s*w*.34,z=front*w*.34;
    box(`corner_pier_${s}_${front}`,'body',[x,(h-12)/2,z],[4.5,h-20,4.5],tier===1?'steel':'dark');
    box(`pier_cap_${s}_${front}`,'upper_body',[x,h-22,z],[6,4,6],'goldDark');
    box(`foot_guard_${s}_${front}`,'base',[x,12,z],[7,6,7],'steel');
  }
  box('parapet','upper_body',[0,h-24,0],[w*.92,5,w*.92],'steel');
  box('parapet_trim','upper_body',[0,h-20,0],[w*.95,2,w*.95],'gold');
  box('crystal_socket','crystal_mount',[0,h-14,0],[12+tier*2,5,12+tier*2],'goldDark');
  box('crystal_lower','crystal',[0,h-8,0],[7+tier*2,9+tier*2,7+tier*2],'crystal',[0,45,0]);
  box('crystal_upper','crystal',[0,h-1,0],[6+tier*2,9+tier*2,6+tier*2],'crystal',[0,45,45]);
  box('crystal_spine','crystal',[0,h+3,0],[3,8+tier*2,3],'glow',[0,45,0]);
  for(const s of [-1,1]) {
    box(`crystal_facet_${s}`,'crystal',[s*3,h-2,-3],[2,12+tier*2,2],'glow',[0,0,-s*15]);
    box(`orbit_arm_${s}`,'rotating_element',[s*(10+tier),h-16,0],[3,7,3],'gold',[0,0,-s*25]);
  }
  for (const s of [-1,1]) {
    box(`banner_rod_${s}`,'banner_'+(s<0?'left':'right'),[s*w*.62,h-26,-2],[1.5,19+tier*3,1.5],'gold');
    box(`banner_${s}`,'banner_'+(s<0?'left':'right'),[s*w*.62,h-33,-2.2],[7+tier,15+tier*3,.8],'team');
    box(`banner_trim_${s}`,'banner_'+(s<0?'left':'right'),[s*w*.62,h-40-tier*1.5,-2.3],[7+tier,1,.9],'gold');
    box(`banner_sigil_${s}`,'banner_'+(s<0?'left':'right'),[s*w*.62,h-32,-2.8],[2.6,2.6,.5],'gold',[0,0,45]);
    for(const side of [-1,1])box(`banner_edge_${s}_${side}`,'banner_'+(s<0?'left':'right'),[s*w*.62+side*(3+tier*.5),h-33,-2.8],[.6,15+tier*3,.5],'goldDark');
  }
  if (tier>=2) {
    for (const s of [-1,1]) {
      box(`buttress_${s}`,'body',[s*w*.62,26,0],[8,27,11],'steel',[0,0,-s*10]);
      box(`buttress_gold_${s}`,'body',[s*w*.62,34,-6],[7,3,2],'gold');
      box(`side_spire_${s}`,'upper_body',[s*w*.43,h-12,0],[6,16,6],'dark');
      box(`side_spire_cap_${s}`,'upper_body',[s*w*.43,h-2,0],[5,6,5],'gold');
    }
    box('front_shield','upper_body',[0,h-27,-w*.49],[w*.30,12,3],'team');
    box('front_shield_gem','upper_body',[0,h-26,-w*.50-2],[5,5,2],'crystal',[0,0,45]);
  }
  if (tier===3) {
    for (const s of [-1,1]) for (const f of [-1,1]) {
      box(`crown_arm_${s}_${f}`,'upper_body',[s*w*.38,h-9,f*w*.38],[8,18,8],'steel',[s*f*8,0,-s*8]);
      box(`crown_tip_${s}_${f}`,'upper_body',[s*w*.38,h+2,f*w*.38],[5,10,5],'gold',[0,45,0]);
      box(`shard_${s}_${f}`,'crystal_mount',[s*w*.28,h-2,f*w*.28],[4,13,4],'crystal',[s*12,45,f*12]);
    }
    for(const z of [-w*.5,w*.5])box(`armor_plate_${z}`,'body',[0,34,z],[w*.55,29,3],'dark');
    for(const s of [-1,1])box(`royal_finial_${s}`,'upper_body',[s*11,h+6,0],[3,9,3],'gold',[0,0,-s*15]);
  }
  return m;
}

function animate(m) {
  const add=(name,length,loop,tracks,events=[])=>{
    const full=`animation.zeravorn.${m.name}.${name}`;
    const clip={uuid:uuid(full),name:full,length,loop,override:false,snapping:40,animators:{},zeravorn_event_markers:events};
    for(const [bone,channels] of Object.entries(tracks)){
      const a={name:bone,type:'bone',keyframes:[]};
      for(const [channel,keys] of Object.entries(channels)) for(const [time,v] of keys)
        a.keyframes.push({uuid:uuid(full+bone+channel+time),channel,time,interpolation:'linear',data_points:[Object.fromEntries(['x','y','z'].map((n,i)=>[n,String(v[i])]))]});
      clip.animators[m.bones[bone].uuid]=a;
    }
    m.animations.push(clip);
  };
  const R=rotation=>({rotation}),P=position=>({position});
  add('idle',3,'loop',{crystal:R([[0,[0,0,0]],[1.5,[0,12,0]],[3,[0,0,0]]]),rotating_element:R([[0,[0,0,0]],[1.5,[0,-15,0]],[3,[0,0,0]]])});
  add('target',.55,'hold',{upper_body:R([[0,[0,0,0]],[.55,[0,25,0]]]),crystal:R([[0,[0,0,0]],[.55,[-8,0,0]]])});
  add('attack',1,'once',{crystal:{rotation:[[0,[0,0,0]],[.55,[-12,0,0]],[.7,[8,0,0]],[1,[0,0,0]]],position:[[0,[0,0,0]],[.55,[0,2,0]],[.7,[0,-1,0]],[1,[0,0,0]]]},weapon_core:P([[0,[0,0,0]],[.55,[0,0,-2]],[.7,[0,0,0]],[1,[0,0,0]]])},[{name:'PROJECTILE_RELEASE',time:.67,socket:'projectile_origin',authority:'visual_only'}]);
  add('hit',.3,'once',{crystal:R([[0,[0,0,0]],[.1,[8,0,7]],[.3,[0,0,0]]])});
  add('low_hp',1.6,'loop',{crystal:R([[0,[0,0,0]],[.4,[5,-5,4]],[.8,[-4,4,-5]],[1.2,[6,0,2]],[1.6,[0,0,0]]]),banner_left:R([[0,[0,0,0]],[.8,[0,0,-8]],[1.6,[0,0,0]]])});
  add('destroy',2,'hold',{crystal:{rotation:[[0,[0,0,0]],[.55,[0,45,0]],[1.1,[15,90,20]],[2,[25,100,35]]],position:[[0,[0,0,0]],[.7,[0,4,0]],[1.3,[0,-9,1]],[2,[0,-12,2]]]},upper_body:R([[0,[0,0,0]],[1,[0,0,0]],[2,[3,0,8]]]),banner_left:R([[0,[0,0,0]],[2,[0,0,-35]]]),banner_right:R([[0,[0,0,0]],[2,[0,0,28]]])},[{name:'CORE_OVERLOAD',time:.7,socket:'vfx_origin',authority:'visual_only'},{name:'DEBRIS_BURST',time:1.25,socket:'vfx_origin',authority:'visual_only'}]);
}

const models=[];
for(let tier=1;tier<=3;tier++){
  const m=makeTower(tier); animate(m); const model=m.save(); const dir=`${root}/t${tier}`, stem=m.name;
  model.textures=['blue','red'].map((team,i)=>({...model.textures[i],source:'data:image/png;base64,'+png(128,128,towerAtlas(team)).toString('base64')}));
  models.push(model);
  json(`${dir}/${stem}.bbmodel`,model);
  const {bones,parents,owners}=hierarchy(model);
  const animations={schema:'zeravorn.tower.animation.v1',authority:'presentation_only',units_per_block:16,clips:model.animations.map(a=>({name:a.name,length:a.length,loop:a.loop,tracks:Object.fromEntries(Object.values(a.animators).map(t=>[t.name,t.keyframes.map(k=>({channel:k.channel,time:k.time,value:['x','y','z'].map(c=>Number(k.data_points[0][c])),interpolation:k.interpolation}))])),events:a.zeravorn_event_markers}))};
  json(`${dir}/${stem}.animations.json`,animations);json(`${runtime}/${stem}.animations.json`,animations);
  json(`${runtime}/${stem}.cubes.json`,{schema:'zeravorn.tower.cuboids.v1',units_per_block:16,front:'-Z',authority:'presentation_only',bones:bones.map(b=>({name:b.name,parent:parents[b.uuid]?.name??null,pivot:b.origin})),cubes:model.elements.map(e=>({name:e.name,bone:owners[e.uuid].name,from:e.from,to:e.to,pivot:e.origin,rotation:e.rotation,faces:e.faces})),textures:{blue:`${stem}_blue.png`,red:`${stem}_red.png`},emissive:{blue:`${stem}_blue_emissive.png`,red:`${stem}_red_emissive.png`}});
  const mesh={...model,elements:model.elements.map(e=>{const verts=cubeVertices(e);return {...e,type:'mesh',vertices:Object.fromEntries(verts.map((v,i)=>['v'+i,v])),faces:Object.fromEntries(Object.entries(faceIndices).map(([f,ids])=>{const [u,v,U,V]=e.faces[f].uv,uv=[[u,v],[U,v],[U,V],[u,V]];return [f,{vertices:ids.map(i=>'v'+i),uv:Object.fromEntries(ids.map((id,i)=>['v'+id,uv[i]]))}];}))};})};
  for(const team of ['blue','red']){
    for(const emissive of [false,true]){const file=`${stem}_${team}${emissive?'_emissive':''}.png`,bytes=png(128,128,towerAtlas(team,emissive));write(`${textures}/t${tier}/${file}`,bytes);write(`${runtime}/${file}`,bytes);}
    exportGltf({...mesh,textures:[model.textures[team==='red'?1:0]]},`${runtime}/${stem}_${team}.gltf`);
    const gltf=JSON.parse(fs.readFileSync(`${runtime}/${stem}_${team}.gltf`));gltf.asset.generator='Zeravorn tower pipeline';for(const n of gltf.nodes)if(n.children?.length===0)delete n.children;gltf.images.push({uri:`${stem}_${team}_emissive.png`});gltf.textures.push({sampler:0,source:1});gltf.materials[0].emissiveTexture={index:1};gltf.materials[0].emissiveFactor=[1,1,1];json(`${runtime}/${stem}_${team}.gltf`,gltf);
    // Preview rasterization uses the saved cuboids, UVs and the actual tower atlas.
    const camera={width:320,height:410,span:125,target:[0,51,0],team,texture:towerAtlas(team)};
    for(const [view,yaw,pitch] of [['front',0,.06],['three_quarter',-.65,.16],['side',-Math.PI/2,.06],['back',Math.PI,.06]]){
      const p=render(model,{...camera,yaw,pitch});write(`${dir}/previews/${team}_${view}.png`,png(p.width,p.height,p.pixels));
    }
    const panels=[0,-.65,-Math.PI/2,Math.PI].map(yaw=>render(model,{...camera,yaw,pitch:.08}));write(`${dir}/previews/${team}_turnaround.png`,join(panels));
    const attack=render(model,{...camera,yaw:-.65,pitch:.16,clip:`animation.zeravorn.${stem}.attack`,time:.67});write(`${dir}/previews/${team}_attack.png`,png(attack.width,attack.height,attack.pixels));
  }
  write(`${dir}/previews/silhouette.png`,join([0,-.65,-Math.PI/2,Math.PI].map(yaw=>render(model,{width:320,height:410,span:125,target:[0,51,0],yaw,silhouette:true}))));
  for(const clip of model.animations)write(`${dir}/previews/${clip.name.split('.').at(-1)}_motion.png`,join([0,.25,.5,.75,1].map(t=>render(model,{width:260,height:330,span:130,target:[0,51,0],yaw:-.65,pitch:.16,texture:towerAtlas('blue'),clip:clip.name,time:clip.length*t}))));
  const points=triangles(model).flatMap(t=>t.points);
  json(`${dir}/statistics.json`,{tier,bones:bones.length,cuboids:model.elements.length,triangles:model.elements.length*12,animation_clips:model.animations.length,height_blocks:(Math.max(...points.map(p=>p[1]))-Math.min(...points.map(p=>p[1])))/16});
  console.log(`${stem}: ${bones.length} bones, ${model.elements.length} cuboids`);
}
write(`${root}/previews/lineup.png`,join(['blue','red'].flatMap(team=>models.map(model=>render(model,{width:360,height:460,span:125,target:[0,51,0],yaw:-.55,pitch:.14,texture:towerAtlas(team)}))),3));
json(`${root}/integration.json`,{status:'exported_not_connected_to_game_renderer',mapping:{T1:'tower_t1',T2:'tower_t2',T3:'tower_t3'},variants:['blue','red'],authority:'presentation_only',required_runtime:['structure renderer','server-driven state selection and aiming','emissive intensity','projectile socket and VFX events'],not_verified:['Blockbench GUI import','in-game scale and clipping','performance']});
writeViewer(models,`${root}/previews/tower_review.html`,'ZERAVORN / PHASE C — башни');
