import fs from 'node:fs';
import assert from 'node:assert/strict';
import vm from 'node:vm';
import {inflateSync} from 'node:zlib';
import {hierarchy,triangles,sample} from '../lane/core.mjs';

export function validate(category){
 const root=`assets/source/blockbench/${category}`,runtime=`src/main/resources/assets/zeravorn/${category}`,read=p=>JSON.parse(fs.readFileSync(p));
 const png=p=>{const b=fs.readFileSync(p);assert.equal(b.subarray(0,8).toString('hex'),'89504e470d0a1a0a');const w=b.readUInt32BE(16),h=b.readUInt32BE(20),chunks=[];for(let i=8;i<b.length;){const n=b.readUInt32BE(i);if(b.toString('ascii',i+4,i+8)==='IDAT')chunks.push(b.subarray(i+8,i+8+n));i+=n+12;}const raw=inflateSync(Buffer.concat(chunks));assert.equal(raw.length,h*(w*4+1));return {w,h,raw};};
 const expected={throne:{throne:['idle','protected','vulnerable','attack','hit','low_hp','destroy']},fountain:{fountain:['idle','heal','danger']},projectiles:Object.fromEntries(['projectile_basic','projectile_ice','projectile_fire','projectile_poison','tower_shot','throne_shot'].map(n=>[n,['flight']])),environment:Object.fromEntries(['bush_small','bush_medium','bush_large','bush_corner','tree_a','tree_b','tree_c','rock_small','rock_medium','rock_large','rock_cluster','ruin_column_full','ruin_column_broken','ruin_arch','ruin_wall_small','ruin_base','ruin_stairs','ruin_rubble','lantern_post','lantern_wall','lantern_hanging'].map(n=>[n,n.startsWith('lantern')?['idle']:[]]))}[category];
 const results=[];
 for(const [name,required]of Object.entries(expected)){
  const m=read(`${root}/${name}/${name}.bbmodel`),{bones,parents,owners}=hierarchy(m),ids=[...bones,...m.elements,...m.animations].map(o=>o.uuid);
  assert.equal(new Set(ids).size,ids.length);assert.equal(bones[0].name,'root');assert(m.elements.length<=(category==='projectiles'?16:256));assert.equal(Object.keys(owners).length,m.elements.length);
  for(const b of bones){assert(b.origin.every(Number.isFinite));if(parents[b.uuid])assert(bones.includes(parents[b.uuid]));}
  for(const e of m.elements){assert.equal(e.type,'cube');assert(owners[e.uuid]);assert(e.from.every((v,i)=>Number.isFinite(v)&&e.to[i]>v));assert(e.rotation.every(Number.isFinite));for(const f of Object.values(e.faces)){assert.equal(f.texture,0);assert(f.uv.every(v=>Number.isFinite(v)&&v>=0&&v<=128));}}
  for(const n of required)assert(m.animations.some(a=>a.name.endsWith('.'+n)),name+' missing '+n);
  let sampled=0;
  for(const a of m.animations){
   for(const [id,tr]of Object.entries(a.animators)){assert(bones.some(b=>b.uuid===id));for(const k of tr.keyframes){assert(k.time>=0&&k.time<=a.length);assert(['x','y','z'].every(c=>Number.isFinite(Number(k.data_points[0][c]))));}if(a.loop==='loop')for(const channel of ['rotation','position'])assert.deepEqual(sample(tr,channel,0),sample(tr,channel,a.length));}
   for(let i=0;i<=40;i++){for(const t of triangles(m,a.name,a.length*i/40))for(const p of t.points)assert(p.every(Number.isFinite));sampled++;}
   for(const e of a.zeravorn_event_markers){assert(e.time>=0&&e.time<=a.length);assert.equal(e.authority,'visual_only');assert(bones.some(b=>b.name===e.socket));}
   png(`${root}/${name}/previews/${a.name.split('.').at(-1)}_motion.png`);
  }
  const cubes=read(`${runtime}/${name}.cubes.json`),animations=read(`${runtime}/${name}.animations.json`);
  assert.deepEqual(cubes.cubes,m.elements.map(e=>({name:e.name,bone:owners[e.uuid].name,from:e.from,to:e.to,pivot:e.origin,rotation:e.rotation,faces:e.faces})));
  assert.deepEqual(animations,read(`${root}/${name}/${name}.animations.json`));assert.equal(animations.clips.length,m.animations.length);
  for(const a of m.animations){const out=animations.clips.find(c=>c.name===a.name);assert(out);for(const tr of Object.values(a.animators))assert.deepEqual(out.tracks[tr.name],tr.keyframes.map(k=>({channel:k.channel,time:k.time,value:['x','y','z'].map(c=>Number(k.data_points[0][c])),interpolation:k.interpolation})));}
  let geometry;
  for(const [i,v]of m.zeravorn.variants.entries()){
   const file=`${name}_${v}.png`,tex=png(`${runtime}/${file}`);assert.equal(tex.w,128);assert.equal(tex.h,128);assert.deepEqual(fs.readFileSync(`${runtime}/${file}`),Buffer.from(m.textures[i].source.split(',')[1],'base64'));png(`${runtime}/${name}_${v}_emissive.png`);
   const g=read(`${runtime}/${name}_${v}.gltf`),bytes=Buffer.from(g.buffers[0].uri.split(',')[1],'base64');assert.equal(g.asset.version,'2.0');assert.equal((g.animations??[]).length,m.animations.length);assert.equal(bytes.length,g.buffers[0].byteLength);assert(!g.animations||g.animations.length>0);for(const n of g.nodes)assert(!n.children||n.children.length>0);
   const geo=JSON.stringify([g.nodes,g.meshes,g.animations,g.buffers]);if(geometry)assert.equal(geo,geometry);geometry=geo;
   for(const ac of g.accessors){const b=g.bufferViews[ac.bufferView],n={SCALAR:1,VEC2:2,VEC3:3,VEC4:4}[ac.type];assert.equal(ac.count*n*4,b.byteLength);assert(b.byteOffset+b.byteLength<=bytes.length);for(let j=0;j<b.byteLength;j+=4)assert(Number.isFinite(bytes.readFloatLE(b.byteOffset+j)));}
   for(const view of ['front','three_quarter','side','back'])png(`${root}/${name}/previews/${v}_${view}.png`);
  }
  results.push({name,cuboids:m.elements.length,bones:bones.length,clips:m.animations.length,sampled_poses:sampled,status:'pass'});
 }
 new vm.Script(fs.readFileSync(`${root}/previews/review.html`,'utf8').match(/<script>([\s\S]*)<\/script>/)[1]);
 const report={status:'structural_validation_passed',scope:['required model inventory','hierarchy and UUIDs','UV and cuboid budgets','finite sampled transforms','loop closure','source/export parity','team geometry equality','PNG decoding','glTF buffer integrity','viewer syntax'],not_verified:['Blockbench GUI import','reference fidelity','inter-part clipping','Minecraft integration','VFX runtime','performance'],results};
 fs.writeFileSync(`${root}/validation.json`,JSON.stringify(report,null,2));console.log(`${category} validation PASS: ${results.length} models, ${results.reduce((n,r)=>n+r.sampled_poses,0)} poses`);return report;
}
