import {uuid} from './core.mjs';
export const events={basic:[[.18,'hit_right'],[.48,'hit_left'],[.78,'hit_cross']],q:[[.15,'knife_release']],e:[[.25,'flask_release']],f:[[.05,'dash_start']],r:Array.from({length:6},(_,i)=>[.15+i*.2,'slash_'+(i+1)])};
export function animate(m,fp=false){
 const clips={idle:[3,'loop'],walk:[1,'loop'],run:[.6,'loop'],basic:[1,'once'],q:[.65,'once'],e:[.9,'once'],f:[.45,'once'],r:[1.6,'once'],hit:[.35,'once'],stun:[2,'loop'],root:[2,'loop'],death:[1.5,'hold'],recall_start:[.6,'once'],recall_loop:[2,'loop'],recall_end:[.5,'once']};
 for(const [name,[length,loop]] of Object.entries(clips)){
 const id='shelianer.'+(fp?'fp.':'')+name,a={uuid:uuid(id),name:id,length,loop,override:false,snapping:30,animators:{},markers:(events[name]||[]).map(([time])=>({time,color:2}))};
 for(let k=0;k<=Math.ceil(length*60);k++){
 const t=Math.min(length,k/60),u=t/length,p=u*Math.PI*2,env=Math.sin(Math.PI*u)**2,poses={};const rot=(n,v)=>poses[n]={rotation:v};const pos=(n,v)=>poses[n]={...poses[n],position:v};
 rot('torso',[0,0,0]);rot('head',[0,Math.sin(p)*1.2,0]);
 for(const [side,s] of [['left',-1],['right',1]]){
 rot('upper_arm_'+side,[-5,0,s*5]);rot('forearm_'+side,[12,0,0]);rot('hand_'+side,[0,0,0]);rot('leg_'+side,[0,0,0]);rot('shin_'+side,[0,0,0]);
 if(name==='idle')rot('forearm_'+side,[12+Math.sin(p)*1.6,0,0]);
 if(name==='walk'||name==='run'){
 const run=name==='run',cycle=(u+(s===1?.5:0))%1,stance=.6,stride=run?2.7:1.65;
 const smooth=v=>v*v*v*(v*(v*6-15)+10);let z,lift;
 if(cycle<stance){z=-stride+2*stride*cycle/stance;lift=0;}else{const v=(cycle-stance)/(1-stance);z=stride*(1-2*smooth(v));lift=(run?1.65:.8)*Math.sin(Math.PI*v)**2;}
 const bob=run?-.6:-.35,H=13+bob-lift,L1=7,L2=6;
 const knee=-Math.acos(Math.max(-1,Math.min(1,(H*H+z*z-L1*L1-L2*L2)/(2*L1*L2))));
 const hip=Math.atan2(-z,H)-Math.atan2(L2*Math.sin(knee),L1+L2*Math.cos(knee));
 const deg=180/Math.PI;rot('leg_'+side,[hip*deg,0,0]);rot('shin_'+side,[knee*deg,0,0]);rot('foot_'+side,[-(hip+knee)*deg,0,0]);pos('body',[0,bob,0]);
 rot('upper_arm_'+side,[-Math.sin(p)*s*(run?24:14),0,s*5]);rot('forearm_'+side,[run?45:18,0,0]);rot('torso',[run?-9:-2,Math.sin(p)*2,0]);
 }
 if(name==='basic'||name==='r'){
const hits=name==='r'?6:3,phase=u*hits,stroke=Math.sin((phase%1)*Math.PI)**2,active=name==='r'||Math.floor(phase)%2===(s===1?0:1)||phase>=2; if(active){rot('upper_arm_'+side,[85*stroke,20*s*stroke,s*(5-65*stroke)]);rot('forearm_'+side,[12+65*stroke,0,0]);rot('hand_'+side,[30*stroke,0,-s*30*stroke]);}rot('torso',[0,Math.sin(u*hits*Math.PI*2)*14,0]);}
 if(name==='q'&&s===1||name==='e'&&s===-1){const hit=name==='q'?.15:.25,raw=t<hit?t/hit:Math.max(0,1-(t-hit)/.35),v=raw*raw*(3-2*raw);rot('upper_arm_'+side,[110*v,0,-s*22*v]);rot('forearm_'+side,[12+65*Math.sin(Math.PI*v),0,0]);}
 if(name==='f'){rot('upper_arm_'+side,[-35*env,0,s*25*env]);rot('leg_'+side,[s*45*env,0,0]);rot('torso',[-35*env,0,0]);}
 if(name==='hit')rot('torso',[18*env,0,8*env]);
 if(name==='stun'){rot('head',[18,0,Math.sin(p)*5]);rot('torso',[8,0,Math.sin(p)*2]);}
 if(name.startsWith('recall')){const v=name==='recall_start'?u:name==='recall_end'?1-u:1;rot('upper_arm_'+side,[-35*v,0,s*40*v]);rot('head',[15*v,0,0]);}
 }
 for(let i=0;i<7;i++)rot('cape_'+i,[-4+Math.sin(p-i*.3)*(name==='run'?9:2),0,Math.sin(p+i)*1.4]);for(const s of ['left','right'])rot('tabard_'+s,[Math.sin(p)*4,0,0]);
 if(name==='death'){rot('body',[0,0,85*Math.min(1,u*1.4)]);pos('body',[0,-12*Math.min(1,u*1.4),0]);}
 if(fp){for(const s of ['left','right'])pos('upper_arm_'+s,[(s==='left'?-1:1)*1,-24,-9]);}
 for(const [bone,channels] of Object.entries(poses)){const b=m.bones[bone];if(!b)continue;const track=a.animators[b.uuid]??={name:bone,type:'bone',keyframes:[]};for(const [channel,values] of Object.entries(channels)){const v=values.map((n,i)=>+(n*((channel==='rotation'&&i<2)||(channel==='position'&&i===0)?-1:1)).toFixed(6));track.keyframes.push({uuid:uuid(id+bone+channel+k),channel,time:+t.toFixed(6),interpolation:'linear',data_points:[{x:v[0],y:v[1],z:v[2]}]});}}
 }m.animations.push(a);
 }
}
