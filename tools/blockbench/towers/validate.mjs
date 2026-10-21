import fs from 'node:fs';
import assert from 'node:assert/strict';
import {inflateSync} from 'node:zlib';
import vm from 'node:vm';
import {hierarchy,triangles,sample} from '../lane/core.mjs';

const root='assets/source/blockbench/towers',runtime='src/main/resources/assets/zeravorn/towers';
const read=file=>JSON.parse(fs.readFileSync(file));
const results=[];
function png(file){
 const b=fs.readFileSync(file);assert.equal(b.subarray(0,8).toString('hex'),'89504e470d0a1a0a');
 const width=b.readUInt32BE(16),height=b.readUInt32BE(20),parts=[];
 for(let i=8;i<b.length;){const n=b.readUInt32BE(i);if(b.toString('ascii',i+4,i+8)==='IDAT')parts.push(b.subarray(i+8,i+8+n));i+=n+12;}
 const raw=inflateSync(Buffer.concat(parts));assert.equal(raw.length,height*(1+width*4));return {width,height,raw};
}
for(let tier=1;tier<=3;tier++){
 const name=`tower_t${tier}`,m=read(`${root}/t${tier}/${name}.bbmodel`),{bones,parents,owners}=hierarchy(m);
 const ids=[...bones,...m.elements,...m.animations].map(o=>o.uuid);assert.equal(new Set(ids).size,ids.length);
 assert.equal(bones[0].name,'root');assert(m.elements.length<=160);
 for(const b of bones){assert(b.origin.every(Number.isFinite));if(parents[b.uuid])assert(bones.includes(parents[b.uuid]));}
 for(const e of m.elements){assert.equal(e.type,'cube');assert(owners[e.uuid]);assert(e.from.every((v,i)=>Number.isFinite(v)&&e.to[i]>v));assert(e.rotation.every(Number.isFinite));for(const f of Object.values(e.faces))assert(f.uv.every(v=>Number.isFinite(v)&&v>=0&&v<=128));}
 for(const required of ['idle','target','attack','hit','low_hp','destroy'])assert(m.animations.some(a=>a.name.endsWith('.'+required)));
 let samples=0;
 for(const clip of m.animations){
  for(const [id,track] of Object.entries(clip.animators)){
   assert(bones.some(b=>b.uuid===id));
   for(const k of track.keyframes){assert(k.time>=0&&k.time<=clip.length);assert(['x','y','z'].every(c=>Number.isFinite(Number(k.data_points[0][c]))));}
   if(clip.loop==='loop')for(const channel of ['rotation','position'])assert.deepEqual(sample(track,channel,0),sample(track,channel,clip.length),'Loop seam');
  }
  for(let i=0;i<=40;i++){for(const t of triangles(m,clip.name,clip.length*i/40))for(const p of t.points){assert(p.every(Number.isFinite),clip.name);assert(p[1]>=-.02,'Geometry below ground');}samples++;}
  for(const e of clip.zeravorn_event_markers){assert(e.time>=0&&e.time<=clip.length);assert(bones.some(b=>b.name===e.socket));assert.equal(e.authority,'visual_only');}
 }
 const cubes=read(`${runtime}/${name}.cubes.json`),clips=read(`${runtime}/${name}.animations.json`);
 assert.deepEqual(cubes.cubes,m.elements.map(e=>({name:e.name,bone:owners[e.uuid].name,from:e.from,to:e.to,pivot:e.origin,rotation:e.rotation,faces:e.faces})));
 assert.deepEqual(clips,read(`${root}/t${tier}/${name}.animations.json`));
 for(const a of m.animations){const out=clips.clips.find(c=>c.name===a.name);assert(out);for(const tr of Object.values(a.animators))assert.deepEqual(out.tracks[tr.name],tr.keyframes.map(k=>({channel:k.channel,time:k.time,value:['x','y','z'].map(c=>Number(k.data_points[0][c])),interpolation:k.interpolation})));}
 let teamGeometry;
 for(const [i,team] of ['blue','red'].entries()){
  const filename=`${name}_${team}.png`,tex=png(`${runtime}/${filename}`),em=png(`${runtime}/${name}_${team}_emissive.png`);assert.equal(tex.width,128);assert.equal(tex.height,128);assert.equal(em.width,128);
  assert.deepEqual(fs.readFileSync(`${runtime}/${filename}`),Buffer.from(m.textures[i].source.split(',')[1],'base64'));
  let lit=0;for(let y=0;y<128;y++)for(let x=0;x<128;x++)if(em.raw[y*513+1+x*4+3])lit++;assert(lit>0&&lit<128*128*.2);
  const g=read(`${runtime}/${name}_${team}.gltf`),bytes=Buffer.from(g.buffers[0].uri.split(',')[1],'base64');assert.equal(g.asset.version,'2.0');assert.equal(bytes.length,g.buffers[0].byteLength);assert.equal(g.animations.length,6);
  const geometry=JSON.stringify([g.nodes,g.meshes,g.animations,g.buffers]);if(teamGeometry)assert.equal(geometry,teamGeometry);teamGeometry=geometry;
  for(const ac of g.accessors){const v=g.bufferViews[ac.bufferView],size={SCALAR:1,VEC2:2,VEC3:3,VEC4:4}[ac.type];assert(v.byteOffset+v.byteLength<=bytes.length);assert.equal(ac.count*size*4,v.byteLength);for(let j=0;j<v.byteLength;j+=4)assert(Number.isFinite(bytes.readFloatLE(v.byteOffset+j)));}
  for(const view of ['front','three_quarter','side','back','attack'])png(`${root}/t${tier}/previews/${team}_${view}.png`);
 }
 for(const c of m.animations)png(`${root}/t${tier}/previews/${c.name.split('.').at(-1)}_motion.png`);
 const points=triangles(m).flatMap(t=>t.points),bounds=[0,1,2].map(i=>[Math.min(...points.map(p=>p[i])),Math.max(...points.map(p=>p[i]))]);
 results.push({name,cuboids:m.elements.length,bones:bones.length,clips:m.animations.length,sampled_poses:samples,height_blocks:(bounds[1][1]-bounds[1][0])/16,status:'pass'});
}
assert(results[0].cuboids<results[1].cuboids&&results[1].cuboids<results[2].cuboids);
assert(results[0].height_blocks<results[1].height_blocks&&results[1].height_blocks<results[2].height_blocks);
new vm.Script(fs.readFileSync(`${root}/previews/tower_review.html`,'utf8').match(/<script>([\s\S]*)<\/script>/)[1]);
const report={status:'structural_validation_passed',scope:['hierarchy','UV','finite animation transforms','ground clearance','loop closure','source/export parity','team geometry equality','PNG decoding','glTF buffers'],not_verified:['Blockbench GUI','reference fidelity','inter-part clipping','Minecraft renderer','VFX callbacks','performance'],results};
fs.writeFileSync(`${root}/validation.json`,JSON.stringify(report,null,2));console.log(JSON.stringify(report,null,2));
