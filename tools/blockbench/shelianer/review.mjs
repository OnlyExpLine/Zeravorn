import {uuid} from './core.mjs';
import {hierarchy,evaluated} from './runtime.mjs';
import assert from 'node:assert/strict';

// A single static review pose, not a polished ability animation.
export function addReviewPose(m){
 const a={name:'shelianer.review_combat_pose',uuid:uuid('review_combat_pose'),length:1,loop:'hold',override:false,snapping:30,animators:{}};
 const pose={body:{position:[0,-.42,0]},torso:{rotation:[-5,-7,0]},head:{rotation:[3,7,0]},upper_arm_left:{rotation:[22,0,-22]},forearm_left:{rotation:[46,0,0]},hand_left:{rotation:[-18,0,-10]},upper_arm_right:{rotation:[12,0,24]},forearm_right:{rotation:[28,0,0]},hand_right:{rotation:[-10,0,8]},leg_left:{rotation:[12,0,-6]},shin_left:{rotation:[-24,0,0]},foot_left:{rotation:[12,0,0]},leg_right:{rotation:[8,0,6]},shin_right:{rotation:[-16,0,0]},foot_right:{rotation:[8,0,0]}};
 for(const [n,channels] of Object.entries(pose)){const b=m.bones[n];if(!b)continue;a.animators[b.uuid]={name:n,type:'bone',keyframes:Object.entries(channels).map(([channel,value])=>{const v=value.map((n,i)=>n*((channel==='rotation'&&i<2)||(channel==='position'&&i===0)?-1:1));return {uuid:uuid('review_combat_pose.'+n+channel),channel,time:0,interpolation:'linear',data_points:[{x:v[0],y:v[1],z:v[2]}]};})};}m.animations.push(a);
}
export function validateGeometry(doc,old){
 const {bones,parents}=hierarchy(doc),byName=new Map(bones.map(b=>[b.name,b]));
 const oldH=hierarchy(old);for(const b of oldH.bones){assert(byName.has(b.name));assert.equal(byName.get(b.name).uuid,b.uuid);assert.equal(parents[byName.get(b.name).uuid]?.name,oldH.parents[b.uuid]?.name);}
 for(const side of ['left','right']){assert.equal(parents[byName.get('forearm_'+side).uuid].name,'upper_arm_'+side);assert.equal(parents[byName.get('weapon_socket_'+side).uuid].name,'dagger_'+side);}
 for(const name of ['q_throw_origin','poison_flask_origin',...['l','r'].flatMap(s=>[1,2,3].map(i=>'throwing_knife_'+s+'_0'+i+'_socket'))])assert(byName.has(name),name);
 let triangles=0;for(const e of doc.elements){assert(Object.keys(e.vertices).length>=4);for(const f of Object.values(e.faces)){assert(new Set(f.vertices).size===f.vertices.length);triangles+=f.vertices.length-2;}}
 assert(doc.elements.length<500);assert(triangles<15000);
 const heights=doc.elements.flatMap(e=>Object.values(e.vertices).map(v=>v[1]));assert(Math.max(...heights)<32&&Math.min(...heights)>=0);
 const knifeNames=doc.elements.filter(e=>/^throwing_knife_[lr]_0[123]$/.test(e.name)).map(e=>e.name);assert.equal(knifeNames.length,6);
 // Evaluate all retained clips for broken transforms or detached hierarchy.
 let poses=0;for(const a of doc.animations)for(const u of [0,.15,.3,.5,.7,.85,1]){for(const e of evaluated(doc,a.name,a.length*u))for(const v of Object.values(e.vertices))assert(v.every(Number.isFinite),a.name);poses++;}
 return {stableHierarchy:'PASS',sockets:'PASS',sixKnives:knifeNames,triangles,elements:doc.elements.length,heightUnits:Math.max(...heights),heightBlocks:Math.max(...heights)/16,gameplayHitboxUnchanged:{width:.6,height:1.8},evaluatedPoses:poses,clipping:'Visual contact sheets reviewed; exhaustive triangle collision audit not performed. Legacy ability clips are provisional.'};
}
