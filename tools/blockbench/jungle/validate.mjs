import fs from 'node:fs';
import assert from 'node:assert/strict';
import {inflateSync} from 'node:zlib';
import vm from 'node:vm';
import {hierarchy,triangles,boneMatrices,transform} from './core.mjs';
const root='assets/source/blockbench/jungle',runtime='src/main/resources/assets/zeravorn/jungle';
const read=p=>JSON.parse(fs.readFileSync(p));
function png(p,w=128,h=128){const b=fs.readFileSync(p);assert.equal(b.subarray(1,4).toString(),'PNG');assert.equal(b.readUInt32BE(16),w);assert.equal(b.readUInt32BE(20),h);let offset=8;const chunks=[];while(offset<b.length){const n=b.readUInt32BE(offset);if(b.toString('ascii',offset+4,offset+8)==='IDAT')chunks.push(b.subarray(offset+8,offset+8+n));offset+=n+12;}const data=inflateSync(Buffer.concat(chunks));assert.equal(data.length,h*(w*4+1));return data;}
const results=[],fingerprints=new Set();
for(const kind of ['red_buff','blue_buff','green_1','green_2']){
 const name='jungle_'+kind,m=read(root+'/'+kind+'/'+name+'.bbmodel'),{bones,parents,owners}=hierarchy(m),ids=new Set();
 for(const o of [...bones,...m.elements,...m.animations]){assert(!ids.has(o.uuid),'duplicate UUID');ids.add(o.uuid);}
 assert.equal(bones[0].name,'root');assert.equal(Object.keys(owners).length,m.elements.length);assert(m.elements.length<=220,'jungle cube budget');assert.equal(m.textures.length,1);
 for(const b of bones){assert(b.origin.every(Number.isFinite));if(parents[b.uuid])assert(bones.includes(parents[b.uuid]));}
 for(const e of m.elements){assert.equal(e.type,'cube');assert(owners[e.uuid]);for(let i=0;i<3;i++)assert(e.to[i]>e.from[i]);assert.equal(Object.keys(e.faces).length,6);for(const f of Object.values(e.faces)){assert.equal(f.texture,0);assert(f.uv.every(v=>v>=0&&v<=128));}}
 const fingerprint=JSON.stringify(m.elements.map(e=>[e.from,e.to,e.rotation]));assert(!fingerprints.has(fingerprint),'creature is only a recolor');fingerprints.add(fingerprint);
 for(const clip of ['idle','walk','attack','hit','death','leash_return',...(kind.startsWith('green')?['special']:[])])assert(m.animations.some(a=>a.name.endsWith('.'+clip)),'missing '+clip);
 let minY=Infinity,poseSamples=0;
 for(const a of m.animations){
  for(const [id,tr] of Object.entries(a.animators)){assert(bones.some(b=>b.uuid===id));for(const k of tr.keyframes){assert(k.time>=0&&k.time<=a.length);assert(['position','rotation'].includes(k.channel));assert(Object.values(k.data_points[0]).every(v=>Number.isFinite(Number(v))));}}
  for(const event of a.zeravorn_event_markers){assert(event.time>=0&&event.time<=a.length);assert.equal(event.authority,'visual_only');assert(bones.some(b=>b.name===event.socket));}
  for(let i=0;i<=80;i++){poseSamples++;for(const tri of triangles(m,a.name,a.length*i/80))for(const v of tri.points){assert(v.every(Number.isFinite));minY=Math.min(minY,v[1]);assert(v[1]>=-.025,'ground penetration '+a.name+' at '+i+': '+v[1]);}}
  // Loop seams must return to the same pose, including the authored root correction.
  if(a.loop==='loop'){const first=boneMatrices(m,a.name,0),last=boneMatrices(m,a.name,a.length);for(const b of bones)first[b.uuid].forEach((v,i)=>assert(Math.abs(v-last[b.uuid][i])<1e-5,'loop seam '+a.name));}
 }
 if(kind.startsWith('green')){const a=m.animations.find(a=>a.name.endsWith('.special'));assert(a.zeravorn_event_markers.some(e=>e.name===(kind==='green_1'?'KNOCKBACK_SHOCKWAVE':'ROOT_ERUPTION')));}
 const tex=png(runtime+'/'+name+'.png'),em=png(runtime+'/'+name+'_emissive.png');assert(!tex.equals(em));let emissive=0;for(let y=0;y<128;y++)for(let x=0;x<128;x++)emissive+=em[y*513+1+x*4+3]>0?1:0;assert(emissive>0&&emissive<128*128*.25);
 assert.deepEqual(Buffer.from(m.textures[0].source.split(',')[1],'base64'),fs.readFileSync(runtime+'/'+name+'.png'));
 const cub=read(runtime+'/'+name+'.cubes.json');assert.deepEqual(cub.cubes.map(e=>[e.name,e.from,e.to,e.rotation]),m.elements.map(e=>[e.name,e.from,e.to,e.rotation]));
 const g=read(runtime+'/'+name+'.gltf'),binary=Buffer.from(g.buffers[0].uri.split(',')[1],'base64');assert.equal(g.asset.version,'2.0');assert.equal(binary.length,g.buffers[0].byteLength);assert.equal(g.animations.length,m.animations.length);
 for(const view of g.bufferViews)assert(view.byteOffset+view.byteLength<=binary.length);
 for(const ac of g.accessors){const v=g.bufferViews[ac.bufferView],n={SCALAR:1,VEC2:2,VEC3:3,VEC4:4}[ac.type];assert(ac.count>0);assert.equal(ac.count*n*4,v.byteLength);for(let i=0;i<v.byteLength;i+=4)assert(Number.isFinite(binary.readFloatLE(v.byteOffset+i)));}
 for(const a of g.animations)for(const c of a.channels){assert(g.nodes[c.target.node]);assert(a.samplers[c.sampler]);}
 for(const file of ['front','three_quarter','side','back','turnaround','silhouettes','attack_pose','death_motion'])assert(fs.existsSync(root+'/'+kind+'/previews/'+file+'.png'));
 results.push({name,bones:bones.length,cubes:m.elements.length,triangles:m.elements.length*12,clips:m.animations.length,pose_samples:poseSamples,minimum_animated_y:minY,technical_status:'pass',clipping:'not certified',in_game:'not tested'});
}
for(const effect of ['shockwave','root_eruption'])png(runtime+'/vfx/'+effect+'.png',32,32);
const html=fs.readFileSync(root+'/previews/jungle_review.html','utf8');new vm.Script(html.match(/<script>([\s\S]*)<\/script>/)[1]);
const config=read('src/main/resources/config_defaults/jungle.json');for(const id of ['RED','BLUE','GREEN_A','GREEN_B'])assert(config.mobs.some(m=>m.id===id));
const report={status:'structural_validation_passed',scope:['hierarchy','cuboid UV and budget','distinct geometry','required animations and markers','81 pose samples per clip','loop seam continuity','PNG decode and emissive mask','source/export parity','glTF buffers/accessors','viewer syntax','existing gameplay ID mapping'],not_validated:['Blockbench GUI import','reference fidelity','limb clipping','Minecraft renderer','VFX runtime','performance'],results};fs.writeFileSync(root+'/validation.json',JSON.stringify(report,null,2));console.log(JSON.stringify(report,null,2));
