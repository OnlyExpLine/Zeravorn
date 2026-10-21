import {uuid,mix} from './core.mjs';
const tau=2*Math.PI, deg=x=>x*180/Math.PI, smooth=t=>t*t*(3-2*t);
export const thirdClips={idle:[4,'loop'],walk:[1.2,'loop'],run:[.78,'loop'],basic:[1,'once'],q_start:[.25,'once'],q_loop:[1,'loop'],q_end:[.30,'once'],e:[.85,'once'],r:[1.1,'once'],hit:[.4,'once'],stun:[2,'loop'],root:[2,'loop'],knockback:[.6,'once'],knockup:[.8,'once'],death:[1.8,'hold'],recall_start:[.6,'once'],recall_loop:[1.2,'loop'],recall_end:[.4,'once']};
export const fpClips={idle:[4,'loop'],walk_sway:[1.2,'loop'],run_sway:[.78,'loop'],basic:[1,'once'],q_start:[.25,'once'],q_loop:[1,'loop'],q_end:[.3,'once'],e:[.85,'once'],r:[1.1,'once'],hit:[.4,'once'],recall_start:[.6,'once'],recall_loop:[1.2,'loop'],recall_end:[.4,'once']};
const R=(p,n,v)=>p[n]={...(p[n]||{}),rotation:v};
const P=(p,n,v)=>p[n]={...(p[n]||{}),position:v};
function envelope(t,keys){if(t<=keys[0][0])return keys[0][1];for(let i=1;i<keys.length;i++)if(t<=keys[i][0])return keys[i-1][1]+(keys[i][1]-keys[i-1][1])*smooth((t-keys[i-1][0])/(keys[i][0]-keys[i-1][0]));return keys.at(-1)[1];}
function secondary(p,phase,strength=1,lift=0){
 for(const [i,b] of ['hair_front_left','hair_front_right','hair_side_left','hair_side_right','hair_back_upper','hair_back_mid','hair_back_lower','hair_strands_left','hair_strands_right'].entries())R(p,b,[-lift+Math.sin(phase-i*.32)*strength,Math.sin(phase-i*.25)*strength*.38,Math.cos(phase-i*.32)*strength*.5]);
 for(const [i,b] of ['front_center','front_left','side_left','back_left','back_center','back_right','side_right','front_right'].entries())R(p,'skirt_'+b,[Math.sin(phase-i*.6)*strength*.75-lift*.4,0,Math.cos(phase-i*.6)*strength*.5]);
}
export function pose(name,t,length,fp=false){
 const p={},phase=t/length*tau;
 R(p,'upper_arm_left',[-3,0,-4]);R(p,'upper_arm_right',[-2,0,3]);R(p,'forearm_left',[-4,0,0]);R(p,'forearm_right',[-5,0,0]);
 if(!fp){R(p,'head',[.3*Math.sin(phase),.55*Math.sin(phase*.0+phase),-.65]);P(p,'torso',[0,.035*Math.sin(phase),0]);secondary(p,phase,.65);}
 if(name==='idle'){R(p,'upper_arm_left',[-3+Math.sin(phase)*.6,0,-4]);R(p,'forearm_right',[-5+Math.sin(phase-.4)*.5,0,0]);}
 if(['walk','run','walk_sway','run_sway'].includes(name)){
  const run=name.startsWith('run'),stride=run?3.1:2.2,bob=(run?-.60:-.43)+(run?.10:.065)*Math.cos(phase*2);
  P(p,'body',[0,bob,0]);R(p,'torso',[run?-5:-1.0,Math.sin(phase)*(run?2.5:1.7),Math.sin(phase)*.8]);
  R(p,'head',[run?3:.7,-Math.sin(phase)*1.5,-.65]);
  for(const [side,off] of [['left',0],['right',.5]]){
   const u=(t/length+off)%1,stance=.60;let z,lift;
   if(u<stance){z=-stride+2*stride*u/stance;lift=0;}else{const a=(u-stance)/(1-stance);z=stride*(1-2*smooth(a));lift=(run?1.6:.8)*Math.sin(Math.PI*a)**2;}
   const vertical=14.3+bob-(1.75+lift),L1=6.2,L2=Math.hypot(6.35,.12),restAnkleAngle=Math.atan2(.12,6.35);
   const knee=-Math.acos(Math.max(-1,Math.min(1,(vertical*vertical+z*z-L1*L1-L2*L2)/(2*L1*L2))));
   const hip=Math.atan2(-z,vertical)-Math.atan2(L2*Math.sin(knee),L1+L2*Math.cos(knee));
   R(p,'leg_'+side,[deg(hip),0,0]);R(p,'lower_leg_'+side,[deg(knee-restAnkleAngle),0,0]);R(p,'foot_'+side,[-deg(hip+knee-restAnkleAngle),0,0]);
  }
  R(p,'upper_arm_left',[-3-Math.sin(phase)*(run?16:9),0,-5]);R(p,'forearm_left',[-8-(1+Math.sin(phase))*(run?9:3),0,0]);
  R(p,'upper_arm_right',[-2+Math.sin(phase)*(run?5:3),0,4]);R(p,'forearm_right',[-5-Math.sin(phase)*(run?3:1),0,0]);
  secondary(p,phase,run?2.4:1.2,run?4:0);
 }
 if(name==='basic'){
  const a=envelope(t,[[0,0],[.12,.75],[.25,1],[.32,.85],[.48,.55],[.8,0],[1,0]]);
  R(p,'upper_arm_right',[34*a,8*a,3+9*a]);R(p,'forearm_right',[-5+18*a,0,0]);R(p,'hand_right',[-137*a,0,0]);
  R(p,'upper_arm_left',[20*a,-12*a,-4-14*a]);R(p,'forearm_left',[-4+16*a,0,0]);R(p,'torso',[0,-6*a,0]);secondary(p,phase,1,a*2);
 }
 if(name.startsWith('q_')){
  const a=name==='q_start'?smooth(t/length):name==='q_end'?1-smooth(t/length):1;
  R(p,'torso',[-1*a,-7*a,0]);R(p,'upper_arm_right',[40*a,8*a,3+9*a]);R(p,'forearm_right',[-5+21*a,0,0]);R(p,'hand_right',[-146*a,0,0]);
  R(p,'upper_arm_left',[48*a,-18*a,-4-17*a]);R(p,'forearm_left',[-4+24*a,0,0]);R(p,'hand_left',[0,-20*a,-10*a]);secondary(p,phase,1.0,4*a);
 }
 if(name==='e'){
  const charge=envelope(t,[[0,0],[.20,1],[.35,0],[.85,0]]),release=envelope(t,[[0,0],[.20,0],[.35,1],[.50,.7],[.85,0]]);
  R(p,'upper_arm_right',[-16*charge+12*release,0,3+25*release]);R(p,'forearm_right',[-5-15*charge+14*release,0,0]);
  R(p,'upper_arm_left',[15*charge+24*release,-15*release,-4-48*release]);R(p,'forearm_left',[-4+25*charge,0,0]);R(p,'torso',[-3*charge+3*release,0,0]);secondary(p,phase,1,release*10);
 }
 if(name==='r'){
  const a=envelope(t,[[0,0],[.24,1],[.42,1],[.55,.28],[.74,.12],[1.1,0]]),b=envelope(t,[[0,0],[.38,0],[.55,1],[.7,.5],[1.1,0]]);
  R(p,'upper_arm_right',[20*a+12*b,0,3+20*a]);R(p,'forearm_right',[-5+65*a,0,0]);R(p,'upper_arm_left',[56*a+22*b,-14*a,-4-22*a]);R(p,'forearm_left',[-4+18*a,0,0]);R(p,'torso',[-3*a+5*b,-5*a,0]);secondary(p,phase,1,6*a+4*b);
 }
 if(['hit','knockback','knockup'].includes(name)){
  const a=envelope(t,[[0,0],[length*.2,1],[length*.48,.65],[length,0]]);
  R(p,'torso',[(name==='hit'?9:16)*a,0,3*a]);R(p,'head',[7*a,0,0]);R(p,'upper_arm_left',[-8*a,0,-4-14*a]);secondary(p,phase,2,4*a);
  if(name==='knockup'){R(p,'leg_left',[14*a,0,0]);R(p,'lower_leg_left',[-28*a,0,0]);R(p,'leg_right',[7*a,0,0]);R(p,'lower_leg_right',[-18*a,0,0]);}
 }
 if(name==='stun'){R(p,'torso',[-7+Math.sin(phase)*1.2,0,2]);R(p,'head',[-12,Math.sin(phase)*2,3]);R(p,'upper_arm_left',[-8,0,-7]);R(p,'forearm_right',[-8,0,0]);}
 if(name==='root'){R(p,'torso',[0,Math.sin(phase),0]);R(p,'upper_arm_left',[10,0,-12]);R(p,'forearm_left',[12,0,0]);}
 if(name==='death'){
  const a=envelope(t,[[0,0],[.30,.1],[.8,.65],[1.3,1],[1.8,1]]),angle=60*a;
  P(p,'body',[0,12.55*(Math.cos(angle*Math.PI/180)-1),0]);
  for(const side of ['left','right']){R(p,'leg_'+side,[angle,0,0]);R(p,'lower_leg_'+side,[-2*angle,0,0]);R(p,'foot_'+side,[angle,0,0]);}
  R(p,'torso',[-37*a,0,-8*a]);R(p,'head',[-24*a,0,0]);R(p,'upper_arm_right',[-12*a,0,3]);R(p,'hand_right',[24*a,0,0]);secondary(p,phase,1-a,9*a);
  for(const [i,b] of ['front_center','front_left','side_left','back_left','back_center','back_right','side_right','front_right'].entries()){const theta=[0,.95,1.62,2.37,Math.PI,3.91,4.66,5.33][i];R(p,'skirt_'+b,[Math.cos(theta)*55*a,0,Math.sin(theta)*55*a]);}
 }
 if(name.startsWith('recall_')){
  const a=name==='recall_start'?smooth(t/length):name==='recall_end'?1-smooth(t/length):1;
  R(p,'upper_arm_left',[43*a,-10*a,-4-31*a]);R(p,'forearm_left',[40*a,0,0]);R(p,'hand_left',[0,0,-12*a]);R(p,'head',[-4*a,0,-.65]);R(p,'upper_arm_right',[-2,0,3]);R(p,'forearm_right',[-5+8*a,0,0]);secondary(p,phase,.85,6*a);
 }
 if(fp){
  // Authoring camera at origin looking -Z; arms below and outside the reticle.
  P(p,'camera_anchor',[0,-28,-6]);
  R(p,'shoulder_left',[0,0,0]);R(p,'shoulder_right',[0,0,0]);
  for(const n of ['upper_arm_left','upper_arm_right']){const r=p[n]?.rotation||[0,0,0];R(p,n,[r[0]*.12+125,-r[1]*.15,-r[2]*.2]);}
  for(const n of ['forearm_left','forearm_right']){const r=p[n]?.rotation||[0,0,0];R(p,n,[r[0]*.15+12,-r[1]*.15,-r[2]*.2]);}
  const aim=name==='basic'?envelope(t,[[0,0],[.25,1],[.5,.5],[1,0]]):name.startsWith('q_')?(name==='q_start'?smooth(t/length):name==='q_end'?1-smooth(t/length):1):0;
  R(p,'hand_right',[0,0,0]);R(p,'staff_root',[-p.upper_arm_right.rotation[0]-p.forearm_right.rotation[0]-aim*24,0,0]);
  if(name.includes('sway'))P(p,'camera_anchor',[.07*Math.sin(phase),-28+.07*Math.cos(phase*2),-6]);
 }
 return p;
}
export const markers={basic:[['amelia.basic.projectile_spawn',.25,'ProjectileEvent.SPAWN']],q_start:[['amelia.q.channel_start',0,'AbilityEvent.Q.START']],q_loop:[['amelia.q.tick',.5,'AbilityEvent.Q.HIT']],q_end:[['amelia.q.channel_end',0,'AbilityEvent.Q.END_OR_CANCEL']],e:[['amelia.e.impact',.35,'AbilityEvent.E.HIT']],r:[['amelia.r.telegraph_start',0,'AbilityEvent.R.START'],['amelia.r.impact',.55,'AbilityEvent.R.HIT']],recall_start:[['amelia.recall.start',0,'Recall.START']],recall_end:[['amelia.recall.end',.4,'Recall.COMPLETE']]};
export function animate(m,fp=false){
 for(const [name,[length,loop]] of Object.entries(fp?fpClips:thirdClips)){
  const id='amelia.'+(fp?'fp.':'')+name,clip={name:id,uuid:uuid(id),loop,length,override:false,snapping:30,animators:{},markers:(markers[name]||[]).map(([id,time])=>({time,color:2})),zeravorn_event_markers:(markers[name]||[]).map(([id,time,gate])=>({id,time,gate})),zeravorn_layer:name.startsWith('q_')?'upper_body':'full_body'};
  const times=[...new Set([0,length,...Array.from({length:Math.ceil(length*30)},(_,i)=>Math.min(length,i/30)),...(markers[name]||[]).map(r=>r[1])])].sort((a,b)=>a-b);
  for(const time of times){const frame=pose(name,time,length,fp);for(const [bone,channels] of Object.entries(frame)){
   if(!m.bones[bone])continue;
   if(name.startsWith('q_')&&['body','pelvis','leg_left','leg_right','lower_leg_left','lower_leg_right','foot_left','foot_right'].includes(bone))continue;
   const b=m.bones[bone],a=clip.animators[b.uuid]??={name:bone,type:'bone',keyframes:[]};
   for(const [channel,value] of Object.entries(channels)){
    // Blockbench's animation channels invert X/Y rotations and X translation.
    const v=value.map((n,i)=>+(n*(channel==='rotation'&&i<2||channel==='position'&&i===0?-1:1)).toFixed(6));
    a.keyframes.push({uuid:uuid(id+'.'+bone+'.'+channel+'.'+time),channel,time:+time.toFixed(6),interpolation:'linear',data_points:[{x:v[0],y:v[1],z:v[2]}]});
   }
  }}
  m.animations.push(clip);
 }
}
