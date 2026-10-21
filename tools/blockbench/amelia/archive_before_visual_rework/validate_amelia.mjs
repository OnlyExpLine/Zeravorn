import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
import {inflateSync} from 'node:zlib';
import {hierarchy,evaluated,triangleList} from './runtime.mjs';
import {thirdClips,fpClips} from './animations.mjs';
import {motionChecks} from './motion_checks.mjs';

export function validateModel(m,required){
 assert.equal(m.meta.model_format,'free');assert.deepEqual(m.resolution,{width:256,height:256});
 const {bones,owners}=hierarchy(m),ids=new Set();
 for(const entry of [...bones,...m.elements]){assert(!ids.has(entry.uuid),'Duplicate UUID '+entry.name);ids.add(entry.uuid);assert(!/^(cube_\d|group_\d)/.test(entry.name));}
 for(const bone of bones)assert(bone.origin.length===3&&bone.origin.every(Number.isFinite),'Invalid pivot '+bone.name);
 for(const e of m.elements){assert(owners[e.uuid],'Unbound mesh '+e.name);for(const v of Object.values(e.vertices))assert(v.length===3&&v.every(Number.isFinite),'Nonfinite vertex');for(const f of Object.values(e.faces)){assert(f.vertices.length>=3);assert.equal(f.texture,0);for(const v of f.vertices){assert(e.vertices[v]);assert(f.uv[v]?.every(n=>Number.isFinite(n)&&n>=0&&n<=256));}}}
 const used=[];function collect(ns){for(const n of ns)if(typeof n==='string')used.push(n);else collect(n.children);}collect(m.outliner);assert.equal(used.length,m.elements.length);assert.equal(new Set(used).size,used.length);
 assert(m.textures[0].source.startsWith('data:image/png;base64,'));const bytes=Buffer.from(m.textures[0].source.split(',')[1],'base64');assert.equal(bytes.readUInt32BE(16),256);assert.equal(bytes.readUInt32BE(20),256);
 const idat=[];for(let offset=8;offset<bytes.length;){const len=bytes.readUInt32BE(offset);if(bytes.toString('ascii',offset+4,offset+8)==='IDAT')idat.push(bytes.subarray(offset+8,offset+8+len));offset+=len+12;}assert.equal(inflateSync(Buffer.concat(idat)).length,256*(256*4+1));
 const prefix=m.name==='amelia_first_person'?'amelia.fp.':'amelia.';
 for(const name of Object.keys(required))assert(m.animations.some(a=>a.name===prefix+name),'Missing clip '+name);
 for(const a of m.animations){assert(a.length>0);for(const [id,b] of Object.entries(a.animators)){assert(bones.some(n=>n.uuid===id),'Missing animated bone');const times={};for(const k of b.keyframes){assert(k.time>=0&&k.time<=a.length+1e-6);assert.equal(k.interpolation,'linear');assert(Object.values(k.data_points[0]).every(Number.isFinite));(times[k.channel]??=[]).push(k);}
  if(a.loop==='loop')for(const ks of Object.values(times)){ks.sort((x,y)=>x.time-y.time);for(const axis of ['x','y','z'])assert(Math.abs(ks[0].data_points[0][axis]-ks.at(-1).data_points[0][axis])<.0001,'Loop seam '+a.name+' '+b.name);}
 }}
 for(const b of ['staff_socket','staff_vfx_origin'])if(m.name!=='amelia_staff')assert(bones.some(n=>n.name===b),'Missing socket '+b);
 return {model:m.name,elements:m.elements.length,bones:bones.length,triangles:triangleList(m.elements).length,clips:m.animations.length};
}
export function validateAll(root){
 const folder=path.join(root,'assets/source/blockbench/heroes/amelia'),runtime=path.join(root,'src/main/resources/assets/zeravorn/amelia'),results=[];
 for(const name of ['amelia','amelia_first_person','amelia_staff']){
  const m=JSON.parse(fs.readFileSync(path.join(folder,name+'.bbmodel'),'utf8'));results.push(validateModel(m,name==='amelia'?thirdClips:name==='amelia_first_person'?fpClips:{}));
  assert.deepEqual(Buffer.from(m.textures[0].source.split(',')[1],'base64'),fs.readFileSync(path.join(root,'assets/source/textures/heroes/amelia',name+'.png')));
  for(const suffix of ['.mesh.json','.animations.json','.gltf'])assert(fs.existsSync(path.join(runtime,name+suffix)));
  const gltf=JSON.parse(fs.readFileSync(path.join(runtime,name+'.gltf'),'utf8')),buffer=Buffer.from(gltf.buffers[0].uri.split(',')[1],'base64');assert.equal(buffer.length,gltf.buffers[0].byteLength);
  for(const v of gltf.bufferViews)assert(v.byteOffset+v.byteLength<=buffer.length);for(const a of gltf.animations)for(const s of a.samplers)assert.equal(gltf.accessors[s.input].count,gltf.accessors[s.output].count);
  if(name==='amelia'){
   for(const q of m.animations.filter(a=>a.name.includes('.q_')))for(const b of Object.values(q.animators))assert(!/^(leg_|lower_leg_|foot_|body$|pelvis$)/.test(b.name),'Q overrides locomotion');
   assert.equal(m.animations.find(a=>a.name==='amelia.death').loop,'hold');
   const bind=evaluated(m),points=bind.flatMap(e=>Object.values(e.vertices));const height=Math.max(...points.map(p=>p[1]));const scale=JSON.parse(fs.readFileSync(path.join(folder,'parameters.json'),'utf8')).scale;assert(height>28*scale&&height<33*scale,'Unexpected character scale');
   // Regression probes must reject broken artifacts, not merely count generated fields.
   const broken=structuredClone(m);broken.elements[1].uuid=broken.elements[0].uuid;assert.throws(()=>validateModel(broken,thirdClips));
   const missing=structuredClone(m);delete missing.outliner[0].children[0];assert.throws(()=>validateModel(missing,thirdClips));
  }
 }
 const events=JSON.parse(fs.readFileSync(path.join(folder,'amelia.events.json'),'utf8'));assert.equal(new Set(events.markers.map(e=>e.id)).size,events.markers.length);assert.equal(events.markers.find(e=>e.id==='amelia.e.impact').time,.35);assert.equal(events.markers.find(e=>e.id==='amelia.r.impact').time,.55);assert.equal(events.markers.find(e=>e.id==='amelia.basic.projectile_spawn').time,.25);
 const motion=motionChecks(JSON.parse(fs.readFileSync(path.join(folder,'amelia.bbmodel'),'utf8')),JSON.parse(fs.readFileSync(path.join(folder,'amelia_first_person.bbmodel'),'utf8')));
 assert.equal(motion.reticle.failures.length,0,'First-person geometry intersects reticle');
 assert(motion.feet.maxContactError<.13,'Locomotion contact error exceeds 0.13 authoring units');
 const report={structural:'PASS',runtimeExport:'PASS',loopContinuity:'PASS',qOverlay:'PASS',negativeRegressionTests:2,motion,models:results,blockbenchGui:'NOT RUN',fabricIntegration:'NOT IMPLEMENTED',visualLikeness:'NOT APPROVED',fullAnimationClippingAudit:'PENDING'};
 fs.writeFileSync(path.join(folder,'validation.json'),JSON.stringify(report,null,2));console.log(JSON.stringify(report,null,2));return report;
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url))validateAll(path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../../..'));
