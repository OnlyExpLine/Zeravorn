import assert from 'node:assert/strict';
import { inflateSync } from 'node:zlib';
import { evaluate } from './runtime.mjs';
import { sub, cross } from './mesh.mjs';

const THIRD=['idle','walk','run','basic_1','basic_2','basic_3','q','e_dash_1','e_dash_2','r','hit','stun','root','death','recall'];
const FIRST=['idle','basic_1','basic_2','basic_3','q','e','e_dash_1','e_dash_2','r','hit','recall'];
const EVENTS=['basic_hit','q_charge','q_hit','q_heal_return','e_dash','e2_hit','r_launch','r_flight','r_impact'];
const vec=(v,size=3)=>Array.isArray(v)&&v.length===size&&v.every(Number.isFinite);
const uuidPattern=/^[0-9a-f]{8}-[0-9a-f]{4}-5[0-9a-f]{3}-a[0-9a-f]{3}-[0-9a-f]{12}$/;

export function validateModel(model,externalTexture) {
  const first=model.zeravorn.first_person,prefix=first?'jason.fp.':'jason.';
  assert.equal(model.meta.model_format,'free'); assert.equal(model.meta.format_version,'4.10');
  const texture=model.textures[0];assert(texture.source.startsWith('data:image/png;base64,'),'Embedded PNG is required');
  const bytes=Buffer.from(texture.source.split(',')[1],'base64');
  if(externalTexture)assert.deepEqual(bytes,externalTexture,'Embedded/external atlas mismatch');
  assert.deepEqual([...bytes.subarray(0,8)],[137,80,78,71,13,10,26,10]);
  const width=bytes.readUInt32BE(16),height=bytes.readUInt32BE(20);
  assert.equal(width,model.resolution.width);assert.equal(height,model.resolution.height);
  const chunks=[];for(let p=8;p<bytes.length;){const n=bytes.readUInt32BE(p),type=bytes.toString('ascii',p+4,p+8);assert(p+12+n<=bytes.length);if(type==='IDAT')chunks.push(bytes.subarray(p+8,p+8+n));p+=12+n;}
  assert.equal(inflateSync(Buffer.concat(chunks)).length,(width*4+1)*height);
  const ids=new Set(),bones=new Map(),parents=new Map(),elements=new Map(),references=new Set();
  const unique=(id)=>{assert(uuidPattern.test(id),`Invalid UUID ${id}`);assert(!ids.has(id),`Duplicate UUID ${id}`);ids.add(id);};
  for(const e of model.elements){unique(e.uuid);assert.equal(e.type,'mesh');assert(vec(e.origin));elements.set(e.uuid,e);}
  function visit(b,parent){unique(b.uuid);assert(!bones.has(b.name),`Duplicate bone ${b.name}`);assert(vec(b.origin));bones.set(b.name,b);parents.set(b.name,parent);for(const c of b.children){if(typeof c==='object')visit(c,b.name);else{assert(elements.has(c),`Dangling outliner child ${c}`);assert(!references.has(c),`Repeated mesh ${c}`);references.add(c);}}}
  model.outliner.forEach(b=>visit(b,null));assert.equal(references.size,elements.size,'Orphan meshes');
  for(const b of ['root','arm_left','forearm_left','fist_left','arm_right','forearm_right','fist_right','socket_fist_left','socket_fist_right',...(first?['camera_anchor']:['body','pelvis','chest','head','hair','scar_chest','scar_back','scar_face','leg_left','leg_right','shin_left','shin_right','foot_left','foot_right','belt','belt_tail_left','belt_tail_right','socket_ground'])])assert(bones.has(b),`Missing ${b}`);
  for(const side of ['left','right']){assert.equal(parents.get(`fist_${side}`),`forearm_${side}`);assert.equal(parents.get(`forearm_${side}`),`arm_${side}`);}
  if(!first){assert.equal(parents.get('hair'),'head');assert.equal(parents.get('head'),'chest');assert.equal(parents.get('scar_face'),'head');assert.equal(parents.get('belt'),'pelvis');}
  let triangles=0;
  for(const e of elements.values())for(const f of Object.values(e.faces)){
    assert.equal(f.vertices.length,3);assert.equal(new Set(f.vertices).size,3);triangles++;
    const ps=f.vertices.map(k=>{assert(vec(e.vertices[k]),'Missing/nonfinite mesh vertex');assert(vec(f.uv[k],2));assert(f.uv[k][0]>=0&&f.uv[k][0]<=width&&f.uv[k][1]>=0&&f.uv[k][1]<=height,'UV out of atlas');return e.vertices[k];});
    assert(Math.hypot(...cross(sub(ps[1],ps[0]),sub(ps[2],ps[0])))>1e-7,'Degenerate face');assert.equal(f.texture,0);
  }
  assert(triangles<(first?4000:10000),'Polygon budget exceeded');
  const names=new Set(),boneIds=new Set([...bones.values()].map(b=>b.uuid));
  for(const a of model.animations){
    unique(a.uuid);assert(!names.has(a.name),'Duplicate animation ID');names.add(a.name);assert(a.length>0);assert(['once','loop','hold'].includes(a.loop));
    for(const [target,animator]of Object.entries(a.animators)){
      assert(boneIds.has(target),'Animation targets unknown bone');assert.equal(animator.type,'bone');
      const stamps=new Set();
      for(const k of animator.keyframes){unique(k.uuid);assert(['rotation','position','scale'].includes(k.channel));assert(k.time>=0&&k.time<=a.length);assert.equal(k.interpolation,'linear');assert(vec(['x','y','z'].map(ax=>k.data_points[0][ax])));const stamp=`${k.channel}/${k.time}`;assert(!stamps.has(stamp),'Repeated keyframe time');stamps.add(stamp);}
      if(a.loop==='loop')for(const channel of new Set(animator.keyframes.map(k=>k.channel))){const ks=animator.keyframes.filter(k=>k.channel===channel).sort((a,b)=>a.time-b.time);assert.deepEqual(ks[0].data_points,ks.at(-1).data_points,'Loop seam');}
    }
    for(const e of a.zeravorn_event_markers){assert(EVENTS.includes(e.id));assert.equal(e.authority,'cosmetic_only');assert(e.time>=0&&e.time<=a.length);assert(a.markers.some(m=>m.time===e.time),'Missing native timeline marker');}
    if(a.name.includes('basic_'))assert.equal(a.zeravorn_event_markers.find(e=>e.id==='basic_hit')?.time,.25,'Balance basic timing');
    if(a.name.endsWith('.q'))assert.equal(a.zeravorn_event_markers.find(e=>e.id==='q_hit')?.time,.30,'Balance Q timing');
  }
  for(const suffix of first?FIRST:THIRD)assert(names.has(prefix+suffix),`Missing ${prefix+suffix}`);
  assert.equal(model.animations.find(a=>a.name===prefix+'recall').length,5,'Recall must last 5 seconds');
  if(!first){const death=model.animations.find(a=>a.name==='jason.death');assert.equal(death.loop,'hold');const body=death.animators[bones.get('body').uuid];assert(Math.abs(body.keyframes.filter(k=>k.channel==='rotation').at(-1).data_points[0].x)>70,'Death returns to standing');}
  const bind=evaluate(model),points=bind.flatMap(t=>t.points),minY=Math.min(...points.map(p=>p[1])),maxY=Math.max(...points.map(p=>p[1]));
  if(!first){assert(minY>=-.1&&minY<=.1,'Feet must be grounded');assert(maxY>=35&&maxY<=37,'Visual height must approximate 2.25 blocks');}
  return {meshes:model.elements.length,bones:bones.size,triangles,animations:model.animations.length,boundsY:[minY,maxY]};
}

// Triangle/rectangle SAT, not just vertex tests: catches a fist spanning the reticle.
function intersectsCenter(points) {
  const box=[[-.12,-.10],[.12,-.10],[.12,.10],[-.12,.10]],axes=[[1,0],[0,1]];
  for(let i=0;i<3;i++){const a=points[i],b=points[(i+1)%3];axes.push([a[1]-b[1],b[0]-a[0]]);}
  return axes.every(axis=>{const p=points.map(v=>v[0]*axis[0]+v[1]*axis[1]),q=box.map(v=>v[0]*axis[0]+v[1]*axis[1]);return Math.max(...p)>=Math.min(...q)&&Math.max(...q)>=Math.min(...p);});
}
export function validateFirstPersonFraming(model) {
  let samples=0;
  for(const a of model.animations){
    const times=[...new Set([...Object.values(a.animators).flatMap(x=>x.keyframes.map(k=>k.time)),...Array.from({length:Math.ceil(a.length*20)+1},(_,i)=>Math.min(i/20,a.length))])];
    for(const time of times){samples++;for(const t of evaluate(model,a.name,time)){
      assert(t.points.every(p=>p[2]<-.1),`${a.name}: geometry crosses camera plane`);
      const f=1/Math.tan(70*Math.PI/360),project=t.points.map(p=>[p[0]/(-p[2])*f/(16/9),p[1]/(-p[2])*f]);
      assert(!intersectsCenter(project),`${a.name}@${time}: ${t.mesh} covers protected reticle region`);
    }}
  }
  return samples;
}
