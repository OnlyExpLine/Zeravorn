import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {atlas,png} from './core.mjs';
import {character,firstPerson,staffOnly,defaults} from './geometry.mjs';
import {animate,markers} from './animations.mjs';
import {sheet} from './preview.mjs';
import {hierarchy} from './runtime.mjs';
import {exportGltf} from './export_gltf.mjs';
import {validateAll} from './validate_amelia.mjs';
import {extras} from './extras.mjs';
import {createHash} from 'node:crypto';
import assert from 'node:assert/strict';

export const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../../..');
const source=path.join(root,'assets/source/blockbench/heroes/amelia'),textures=path.join(root,'assets/source/textures/heroes/amelia'),runtime=path.join(root,'src/main/resources/assets/zeravorn/amelia');
for(const d of [source,textures,runtime,path.join(source,'previews')])fs.mkdirSync(d,{recursive:true});
const paramsFile=process.argv.slice(2).find(a=>!a.startsWith('--'));
const params=paramsFile?{...defaults,...JSON.parse(fs.readFileSync(paramsFile,'utf8'))}:defaults;
for(const [key,value] of Object.entries(params)){assert(key in defaults,'Unknown parameter '+key);assert(Number.isFinite(value)&&value>0,'Invalid parameter '+key);}
const hash=file=>createHash('sha256').update(fs.readFileSync(file)).digest('hex');
const verifyFiles=['amelia','amelia_first_person','amelia_staff'].flatMap(n=>[path.join(source,n+'.bbmodel'),path.join(runtime,n+'.gltf'),path.join(textures,n+'.png')]);
const before=process.argv.includes('--verify')?Object.fromEntries(verifyFiles.map(f=>[f,hash(f)])):null;
const texture=png(256,256,atlas()),emissive=png(256,256,atlas(true));
const models=[character(params),firstPerson(params),staffOnly(params)];animate(models[0]);animate(models[1],true);
const saved=[];
for(const m of models){
 if(params.scale!==1){for(const b of Object.values(m.bones))b.origin=b.origin.map(v=>v*params.scale);for(const e of m.elements)for(const k in e.vertices)e.vertices[k]=e.vertices[k].map(v=>v*params.scale);for(const a of m.animations)for(const b of Object.values(a.animators))for(const k of b.keyframes)if(k.channel==='position')for(const axis of ['x','y','z'])k.data_points[0][axis]*=params.scale;}
 const doc=m.save(texture);saved.push(doc);fs.writeFileSync(path.join(source,m.name+'.bbmodel'),JSON.stringify(doc));fs.writeFileSync(path.join(textures,m.name+'.png'),texture);fs.writeFileSync(path.join(textures,m.name+'_emissive.png'),emissive);
 exportGltf(doc,path.join(runtime,m.name+'.gltf'));
 // Exact mesh/bone/clip payload, ready for a mesh-capable adapter. Not a vanilla cube model.
 fs.writeFileSync(path.join(runtime,m.name+'.mesh.json'),JSON.stringify({schema:'zeravorn.rigid_mesh.v1',unitsPerBlock:16,front:'-Z',geometry:doc.elements,bones:doc.outliner,texture:'zeravorn:amelia/'+m.name+'.png'}));
 fs.writeFileSync(path.join(runtime,m.name+'.animations.json'),JSON.stringify({schema:'zeravorn.blockbench_animation.v1',clips:doc.animations}));fs.writeFileSync(path.join(runtime,m.name+'.png'),texture);fs.writeFileSync(path.join(runtime,m.name+'_emissive.png'),emissive);
}
fs.writeFileSync(path.join(source,'parameters.json'),JSON.stringify(params,null,2));
const contract={schema:'zeravorn.amelia.presentation.v1',gameplayAuthority:'server',runtimeIntegration:'pending: renderer/network bridge not implemented in repository',markers:Object.entries(markers).flatMap(([clip,rows])=>rows.map(([id,time,gate])=>({id,clip:'amelia.'+clip,time,gate}))),recall:{durationSeconds:5,startSeconds:.6,loopUntilServerEnd:true,endSeconds:.4,cancelPlaysEnd:false},layers:{q:'upper_body_overlay_on_locomotion',root:'stationary_lower_body',knockback:'server_trajectory',knockup:'server_trajectory'},vfx:['idle_frost','staff_glow','basic_charge','basic_spawn','basic_hit','q_start','q_loop','q_tick','q_end','e_charge','e_ring','e_hit','r_telegraph','r_ground_crack','r_spikes','r_hit','recall_start','recall_loop','recall_end'].map(id=>'amelia.vfx.'+id),sfx:['basic.cast','basic.travel','basic.hit','q.cast','q.loop','q.hit','q.end','e.cast','e.hit','r.cast','r.telegraph','r.impact','hit','death','recall','level_up'].map(id=>'amelia.'+id),sockets:['staff_socket','staff_vfx_origin','vfx_chest','vfx_left_hand','vfx_right_hand','vfx_feet','vfx_head'],knownGameplayConflicts:['E currently applies damage immediately; documented impact is 0.35s.','Active Q currently does not cancel on stun/knock-up.'],reference:{source:'User-attached Amelia concept sheet, 2026-09-08',requestedLocalPath:'docs/reference/characters/amelia_concept.png',localFilePresent:fs.existsSync(path.join(root,'docs/reference/characters/amelia_concept.png')),likenessApproved:false}};
for(const folder of [source,runtime])fs.writeFileSync(path.join(folder,'amelia.events.json'),JSON.stringify(contract,null,2));
const counts=saved.map(m=>({model:m.name,elements:m.elements.length,triangles:m.elements.reduce((s,e)=>s+Object.values(e.faces).reduce((n,f)=>n+f.vertices.length-2,0),0),bones:hierarchy(m).bones.length,animations:m.animations.length,texture:[256,256]}));
fs.writeFileSync(path.join(source,'statistics.json'),JSON.stringify(counts,null,2));
sheet(saved[0],[0,.65,Math.PI/2,Math.PI,-Math.PI/2].map(yaw=>({yaw,width:420,height:850,hideStaff:true})),path.join(source,'previews/amelia_turnaround.png'));
sheet(saved[0],[{yaw:.55,pitch:.04,width:850,height:1000}],path.join(source,'previews/amelia_with_staff.png'));
sheet(saved[0],[0,.6,Math.PI/2].map(yaw=>({yaw,width:450,height:500,target:[0,25.4,0],span:10,hideStaff:true})),path.join(source,'previews/amelia_face.png'));
sheet(saved[0],[0,.15,.30,.45,.60,.75,.9,1.05].map(time=>({clip:'amelia.walk',time,yaw:.65,width:300,height:650})),path.join(source,'previews/amelia_walk.png'));
sheet(saved[0],[['basic',.25],['q_loop',.5],['e',.35],['r',.55],['death',1.8],['recall_loop',.6]].map(([clip,time])=>({clip:'amelia.'+clip,time,yaw:.5,width:360,height:700})),path.join(source,'previews/amelia_poses.png'));
console.log(JSON.stringify(counts,null,2));
extras(saved[0],source,runtime);
await import('./inspector.mjs');
validateAll(root);
if(before){for(const file of verifyFiles)assert.equal(hash(file),before[file],'Non-deterministic output '+file);console.log('Deterministic regeneration: PASS (9 model/texture/export artifacts).');}
