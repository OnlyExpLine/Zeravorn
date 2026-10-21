import fs from 'node:fs';
import assert from 'node:assert/strict';
import vm from 'node:vm';
import {inflateSync} from 'node:zlib';
import {hierarchy,triangles,boneMatrices,transform} from './core.mjs';

const root='assets/source/blockbench/lane',runtime='src/main/resources/assets/zeravorn/lane',results=[];
const read=p=>JSON.parse(fs.readFileSync(p));
function decodePng(file){const b=fs.readFileSync(file);assert.equal(b.subarray(1,4).toString(),'PNG');const w=b.readUInt32BE(16),h=b.readUInt32BE(20),chunks=[];let offset=8;while(offset<b.length){const n=b.readUInt32BE(offset),type=b.toString('ascii',offset+4,offset+8);if(type==='IDAT')chunks.push(b.subarray(offset+8,offset+8+n));offset+=n+12;}const raw=inflateSync(Buffer.concat(chunks));assert.equal(raw.length,h*(w*4+1));for(let y=0;y<h;y++)assert.equal(raw[y*(w*4+1)],0);return {w,h,raw};}
for(const kind of ['warrior','ranged','siege']){
 const name='lane_'+kind,m=read(root+'/'+kind+'/'+name+'.bbmodel'),{bones,parents,owners}=hierarchy(m),ids=new Set();
 for(const object of [...bones,...m.elements,...m.animations]){assert(!ids.has(object.uuid),'Duplicate UUID');ids.add(object.uuid);}
 assert.equal(bones[0].name,'root');assert.equal(m.elements.length,Object.keys(owners).length);
 for(const b of bones){assert(b.origin.every(Number.isFinite));if(parents[b.uuid])assert(bones.includes(parents[b.uuid]));}
 for(const e of m.elements){assert.equal(e.type,'cube');assert(owners[e.uuid]);for(let i=0;i<3;i++)assert(e.to[i]>e.from[i]);assert.equal(Object.keys(e.faces).length,6);for(const f of Object.values(e.faces)){assert.equal(f.texture,0);assert(f.uv.every(v=>Number.isFinite(v)&&v>=0&&v<=128));}}
 assert(m.elements.length<=128,'Lane cube budget exceeded');
 const required=['idle',kind==='siege'?'move':'walk','attack','hit','death'];for(const c of required)assert(m.animations.some(a=>a.name==='animation.zeravorn.'+name+'.'+c));
 let sampled=0,minY=Infinity;
 for(const a of m.animations){assert(a.length>0);for(const [id,tr] of Object.entries(a.animators)){assert(bones.some(b=>b.uuid===id));for(const k of tr.keyframes){assert(k.time>=0&&k.time<=a.length);assert(['rotation','position'].includes(k.channel));assert(Object.values(k.data_points[0]).every(v=>Number.isFinite(Number(v))));}}
  for(let i=0;i<=80;i++){const tris=triangles(m,a.name,a.length*i/80);sampled++;for(const t of tris)for(const v of t.points){assert(v.every(Number.isFinite));minY=Math.min(minY,v[1]);assert(v[1]>=-.025,'Animated geometry penetrates ground: '+a.name);}}
  for(const event of a.zeravorn_event_markers){assert(event.time>=0&&event.time<=a.length);assert.equal(event.authority,'visual_only');if(event.socket)assert(bones.some(b=>b.name===event.socket));}
 }
 if(kind==='siege'){
  const move=m.animations.find(a=>a.name.endsWith('.move'));for(const b of bones.filter(b=>b.name.startsWith('wheel_'))){const keys=move.animators[b.uuid].keyframes;assert.equal(Number(keys.at(-1).data_points[0].x)-Number(keys[0].data_points[0].x),-360);}
 }
 const rest=triangles(m),bounds=[0,1,2].map(i=>[Math.min(...rest.flatMap(t=>t.points.map(v=>v[i]))),Math.max(...rest.flatMap(t=>t.points.map(v=>v[i])))]);
 assert(bounds[1][0]>=-.05,'Bind pose below ground');
 const cub=read(runtime+'/'+name+'.cubes.json');assert.equal(cub.cubes.length,m.elements.length);assert.deepEqual(cub.cubes.map(e=>e.name),m.elements.map(e=>e.name));
 let geometry=null;
 for(const team of ['blue','red']){
  const tex=decodePng(runtime+'/'+name+'_'+team+'.png'),em=decodePng(runtime+'/'+name+'_'+team+'_emissive.png');assert.equal(tex.w,128);assert.equal(tex.h,128);assert.equal(em.w,128);let lit=0;for(let y=0;y<128;y++)for(let x=0;x<128;x++)lit+=em.raw[y*513+1+x*4+3]>0?1:0;assert(lit>0&&lit<128*128*.2);
  const g=read(runtime+'/'+name+'_'+team+'.gltf');assert.equal(g.asset.version,'2.0');assert.equal(g.animations.length,m.animations.length);const binary=Buffer.from(g.buffers[0].uri.split(',')[1],'base64');assert.equal(binary.length,g.buffers[0].byteLength);
  const geo=JSON.stringify([g.nodes,g.meshes,g.animations,g.accessors,g.buffers]);if(geometry)assert.equal(geo,geometry,'Team geometry/rig differs');geometry=geo;
  for(const v of g.bufferViews)assert(v.byteOffset+v.byteLength<=binary.length);
  for(const ac of g.accessors){assert(ac.count>0);const view=g.bufferViews[ac.bufferView];assert(view);const n={SCALAR:1,VEC2:2,VEC3:3,VEC4:4}[ac.type];assert.equal(ac.count*n*4,view.byteLength);for(let i=0;i<view.byteLength;i+=4)assert(Number.isFinite(binary.readFloatLE(view.byteOffset+i)));}
  for(const a of g.animations)for(const c of a.channels){assert(g.nodes[c.target.node]);assert(a.samplers[c.sampler]);}
  for(const view of ['front','three_quarter','side','back','combat'])assert(fs.existsSync(root+'/'+kind+'/previews/'+team+'_'+view+'.png'));
 }
 results.push({name,bones:bones.length,cubes:m.elements.length,triangles:rest.length,clip_count:m.animations.length,pose_samples:sampled,bind_bounds:bounds,minimum_animated_y:minY,technical_checks:'pass',clipping:'not certified; see motion sheets',in_game:'not run; renderer integration absent'});
}
const html=fs.readFileSync(root+'/previews/lane_review.html','utf8');new vm.Script(html.match(/<script>([\s\S]*)<\/script>/)[1]);
const report={status:'structural_validation_passed',scope:'JSON hierarchy, UVs, PNG decoding, budgets, keyframes, sampled transforms, team export equality, glTF buffer/accessor integrity, viewer JavaScript syntax',not_acceptance:['Blockbench import UI','visual parity with reference','clipping clearance','Minecraft rendering','performance'],results};fs.writeFileSync(root+'/validation.json',JSON.stringify(report,null,2));console.log(JSON.stringify(report,null,2));
