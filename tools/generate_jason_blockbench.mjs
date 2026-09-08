#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { atlas, ATLAS_SIZE, uuid } from './jason/mesh.mjs';
import { thirdPerson, firstPerson } from './jason/geometry.mjs';
import { thirdAnimations, firstAnimations } from './jason/animations.mjs';
import { gltf } from './jason/gltf.mjs';
import { evaluate } from './jason/runtime.mjs';

export const repository=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
export const output=path.join(repository,'assets/source/blockbench');

export function buildAssets() {
  const third=thirdPerson(),first=firstPerson(third),png=atlas();
  const thirdClips=thirdAnimations(third);
  // Ground contact is solved from the actual mesh, avoiding floating crouches and
  // corpses intersecting the floor after a proportion edit.
  for(const [name,times,feetOnly]of [['jason.r',[.18,.86,1.03,1.3],true],['jason.death',[.85,1.03,1.16,1.6],false]]) {
    const animation=thirdClips.find(a=>a.name===name),body=animation.animators[third.bones.get('body').uuid];
    for(const time of times) {
      const triangles=evaluate({elements:third.elements,outliner:third.outliner,animations:thirdClips},name,time);
      const points=triangles.filter(t=>!feetOnly||/^(foot_body|toe|toenail)_/.test(t.mesh)).flatMap(t=>t.points);
      const ground=Math.min(...points.map(p=>p[1]));
      body.keyframes.find(k=>k.channel==='position'&&k.time===time).data_points[0].y-=ground;
    }
  }
  function project(rig,animations,texture) {
    return {meta:{format_version:'4.10',model_format:'free',box_uv:false},name:rig.name,
      model_identifier:`zeravorn:${rig.name}`,visible_box:[4,4,0],
      resolution:{width:ATLAS_SIZE,height:ATLAS_SIZE},elements:rig.elements,outliner:rig.outliner,
      textures:[{name:texture,id:'0',uuid:uuid(`texture/${texture}`),relative_path:`textures/${texture}`,
        source:`data:image/png;base64,${png.toString('base64')}`,internal:true,mode:'bitmap',
        uv_width:ATLAS_SIZE,uv_height:ATLAS_SIZE,width:ATLAS_SIZE,height:ATLAS_SIZE,
        render_mode:'default',render_sides:'double',visible:true}],animations,
      zeravorn:{pipeline_version:2,units_per_block:16,forward:'-Z',up:'+Y',
        geometry:'triangulated rigid bone meshes',authority:'cosmetic_only',
        runtime_requires:'mesh-capable hero renderer; not vanilla Java cube export',
        first_person:rig===first,bones:[...rig.bones.keys()]}};
  }
  const models=[project(third,thirdClips,'jason_atlas.png'),project(first,firstAnimations(first),'jason_first_person_atlas.png')];
  const contract={schema:'zeravorn.jason.animation-events/v2',authoritativeDamage:false,
    coordinate_system:{units_per_block:16,forward:'-Z',up:'+Y'},
    events:['basic_hit','q_charge','q_hit','q_heal_return','e_dash','e2_hit','r_launch','r_flight','r_impact'],
    timing_policy:'basic_hit=0.25s and q_hit=0.30s from Balance v1.1; E2 waits for collision and R waits for landing. Other times are authored cosmetic poses, not gameplay deadlines.',
    root_motion:'in-place; entity movement is owned by the server',
    animations:Object.fromEntries(models.flatMap(m=>m.animations.map(a=>[a.name,{length:a.length,loop:a.loop,markers:a.zeravorn_event_markers}])))};
  return {models,png,contract};
}

if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)) {
  const {models,png,contract}=buildAssets();
  fs.mkdirSync(path.join(output,'textures'),{recursive:true});
  fs.mkdirSync(path.join(output,'exports'),{recursive:true});
  for(const model of models) {
    fs.writeFileSync(path.join(output,`${model.name}.bbmodel`),JSON.stringify(model,null,2)+'\n');
    fs.writeFileSync(path.join(output,'textures',model.textures[0].name),png);
    fs.writeFileSync(path.join(output,'exports',`${model.name}.gltf`),JSON.stringify(gltf(model))+'\n');
    const faces=model.elements.reduce((n,m)=>n+Object.keys(m.faces).length,0);
    console.log(`${model.name}: ${model.elements.length} meshes / ${faces} triangles / ${model.animations.length} animations`);
  }
  fs.writeFileSync(path.join(output,'jason.events.json'),JSON.stringify(contract,null,2)+'\n');
}
