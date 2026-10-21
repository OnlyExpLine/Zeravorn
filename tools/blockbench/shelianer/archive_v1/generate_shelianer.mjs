import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';
import vm from 'node:vm';
import {checkMotion} from './check_motion.mjs';
import {atlas,png} from './core.mjs';
import {character,firstPerson,weapon} from './geometry.mjs';
import {animate,events} from './animations.mjs';
import {sheet} from './preview.mjs';
import {exportGltf} from './export_gltf.mjs';
const dir=path.dirname(fileURLToPath(import.meta.url)),root=path.resolve(dir,'../../..'),out=path.join(root,'assets/source/blockbench/heroes/shelianer');
for(const d of [out,out+'/previews',out+'/textures',out+'/exports'])fs.mkdirSync(d,{recursive:true});
const models=[character(),firstPerson(),weapon()];animate(models[0]);animate(models[1],true);const texture=png(256,256,atlas()),docs=models.map(m=>m.save(texture));
const stats=[];
for(const d of docs){
 const ids=new Set(),used=new Set(),bones=new Set();for(const e of d.elements){assert(!ids.has(e.uuid));ids.add(e.uuid);for(const f of Object.values(e.faces)){assert(f.vertices.length>=3);for(const v of f.vertices){assert(e.vertices[v]?.every(Number.isFinite));assert(f.uv[v].every(n=>n>=0&&n<=256));}}}
 function walk(b){assert(!bones.has(b.uuid));bones.add(b.uuid);for(const c of b.children)if(typeof c==='string'){assert(ids.has(c));assert(!used.has(c));used.add(c);}else walk(c);}d.outliner.forEach(walk);assert.equal(used.size,ids.size);
 for(const a of d.animations)for(const [id,b] of Object.entries(a.animators)){assert(bones.has(id));for(const k of b.keyframes){assert(k.time>=0&&k.time<=a.length+.00001);assert(Object.values(k.data_points[0]).every(Number.isFinite));}if(a.loop==='loop'){for(const channel of new Set(b.keyframes.map(k=>k.channel))){const keys=b.keyframes.filter(k=>k.channel===channel);for(const c of ['x','y','z'])assert(Math.abs(keys[0].data_points[0][c]-keys.at(-1).data_points[0][c])<.001);}}}
 fs.writeFileSync(out+'/'+d.name+'.bbmodel',JSON.stringify(d));exportGltf(d,out+'/exports/'+d.name+'.gltf');stats.push({name:d.name,meshes:d.elements.length,bones:bones.size,animations:d.animations.length});
}
fs.writeFileSync(out+'/textures/shelianer.png',texture);fs.writeFileSync(out+'/textures/shelianer_emissive.png',png(256,256,atlas(true)));
fs.writeFileSync(out+'/shelianer.events.json',JSON.stringify({authority:'server',markers:events,notes:'Presentation timings only. R: six hits; movement and damage controlled by server. Basic clip contains three combo stages; runtime must select stages for individual attacks.',vfx:['violet_slash','knife_trail','green_poison_cloud','dash_afterimage','death_dance'],runtimeIntegration:false},null,2));
sheet(docs[0],[0,.65,Math.PI/2,Math.PI].map(yaw=>({yaw,width:500,height:850,target:[0,15.7,0],span:35})),out+'/previews/shelianer_turnaround.png');
sheet(docs[0],[['basic',.18],['q',.15],['e',.25],['f',.22],['r',.35]].map(([clip,time])=>({clip:'shelianer.'+clip,time,yaw:.4,width:450,height:700})),out+'/previews/shelianer_abilities.png');
sheet(docs[0],Array.from({length:8},(_,i)=>({clip:'shelianer.walk',time:i/8,yaw:Math.PI/2,width:280,height:620})),out+'/previews/shelianer_walk_side.png');
sheet(docs[0],Array.from({length:6},(_,i)=>({clip:'shelianer.q',time:i*.1,yaw:Math.PI/2,width:320,height:620})),out+'/previews/shelianer_elbow_check.png');
// Reuse the project offline inspector shell, with the actual Shelianer documents.
let shell=fs.readFileSync(path.join(dir,'../amelia/inspector.mjs'),'utf8');let html=shell.slice(shell.indexOf('const html=`')+12,shell.indexOf('`;\nfs.writeFileSync'));
const runtime=fs.readFileSync(dir+'/runtime.mjs','utf8').replaceAll('export ',''),viewer=fs.readFileSync(dir+'/viewer.js','utf8');
html=html.replace('${JSON.stringify(models).replaceAll(\'<\',\'\\\\u003c\')}',JSON.stringify(docs).replaceAll('<','\\u003c')).replace('${runtime}',runtime).replace('${viewer}',viewer).replaceAll('\\n','\n').replaceAll('AMELIA','SHELIANER').replaceAll('Amelia','Shelianer').replaceAll('Ледяная магичка','Теневой убийца').replaceAll('посох','кинжал').replaceAll('Посох','Кинжал');
assert(html.includes('const MODELS=[{'));new vm.Script(html.match(/<script>([\s\S]*)<\/script>/)[1]);fs.writeFileSync(out+'/previews/shelianer_viewer.html',html);
fs.writeFileSync(out+'/validation.json',JSON.stringify({structural:'PASS',loopSeams:'PASS',motion:checkMotion(docs[0]),models:stats,blockbenchGui:'NOT RUN',runtimeIntegration:false,visualApproval:'pending'},null,2));console.log(JSON.stringify(stats,null,2));
