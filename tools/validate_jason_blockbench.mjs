#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import { buildAssets, output } from './generate_jason_blockbench.mjs';
import { validateModel, validateFirstPersonFraming } from './jason/validation.mjs';
import { evaluate } from './jason/runtime.mjs';
import vm from 'node:vm';

const generated=buildAssets();
for(const expected of generated.models){
  const model=JSON.parse(fs.readFileSync(path.join(output,`${expected.name}.bbmodel`),'utf8'));
  assert(JSON.stringify(model)===JSON.stringify(expected),'Regeneration is not deterministic or output is stale');
  const png=fs.readFileSync(path.join(output,'textures',model.textures[0].name));
  console.log(`${model.name}: ${JSON.stringify(validateModel(model,png))}`);
  const exported=JSON.parse(fs.readFileSync(path.join(output,'exports',`${expected.name}.gltf`),'utf8'));
  assert.equal(exported.asset.version,'2.0');assert.deepEqual(exported.animations.map(a=>a.name),model.animations.map(a=>a.name));
  const binary=Buffer.from(exported.buffers[0].uri.split(',')[1],'base64');assert.equal(binary.length,exported.buffers[0].byteLength);
  for(const v of exported.bufferViews)assert(v.byteOffset%4===0&&v.byteOffset+v.byteLength<=binary.length,'Invalid glTF buffer view');
  for(const a of exported.animations)for(const c of a.channels){assert(exported.nodes[c.target.node]);const s=a.samplers[c.sampler];assert.equal(exported.accessors[s.input].count,exported.accessors[s.output].count);}
  if(model.zeravorn.first_person)console.log(`First-person reticle: ${validateFirstPersonFraming(model)} sampled poses passed at 70° vertical FOV, 16:9.`);
  else for(const [name,time]of [['jason.r',.18],['jason.r',.86],['jason.r',1.03],['jason.death',1.6]]) {
    const points=evaluate(model,name,time).flatMap(t=>t.points);assert(Math.abs(Math.min(...points.map(p=>p[1])))<.05,`${name}: floor contact`);
  }
}
assert.deepEqual(JSON.parse(fs.readFileSync(path.join(output,'jason.events.json'),'utf8')),generated.contract);

// Regressions corresponding to the defects in the original asset generator.
for(const [name,mutate] of [
  ['UUID collision',m=>{m.outliner[0].uuid=m.elements[0].uuid;}],
  ['missing embedded texture',m=>{m.textures[0].source='';}],
  ['unknown keyframe target',m=>{m.animations[0].animators.bad=Object.values(m.animations[0].animators)[0];}],
  ['incorrect Q timing',m=>{m.animations.find(a=>a.name==='jason.q').zeravorn_event_markers.find(e=>e.id==='q_hit').time=.68;}],
  ['standing death',m=>{m.animations.find(a=>a.name==='jason.death').loop='once';}],
]){const broken=structuredClone(generated.models[0]);mutate(broken);assert.throws(()=>validateModel(broken),name);}
console.log('PASS: source structure, textures, hierarchy, timings, glTF, deterministic regeneration and 5 defect regressions.');
const preview=path.join(output,'previews/jason_viewer.html');
if(fs.existsSync(preview)) {
  const html=fs.readFileSync(preview,'utf8'),script=html.match(/<script>([\s\S]*)<\/script>/)[1];
  new vm.Script(script,{filename:'jason_viewer.html'});
  const prefix='const MODELS=',end=script.indexOf(';\n');
  assert(JSON.stringify(JSON.parse(script.slice(prefix.length,end)))===JSON.stringify(generated.models),'Preview contains stale model data');
  console.log('PASS: offline viewer JavaScript parses and its model payload matches the .bbmodel assets.');
}
