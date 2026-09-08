import { uuid } from './mesh.mjs';

const r = (x=0,y=0,z=0) => ({rotation:[x,y,z]});
const p = (x=0,y=0,z=0) => ({position:[x,y,z]});
const pose = (time, bones={}) => ({time,bones});
const event = (id,time,gate='server_confirmed') => ({id,time,gate,authority:'cosmetic_only'});

// Choreography uses ordinary right-handed XYZ degrees. Serialization below converts
// X/Y rotation and X translation to Blockbench's animation coordinate convention.
function clip(rig, suffix, length, loop, frames, events=[]) {
  const name=`${rig.name==='jason'?'jason':'jason.fp'}.${suffix}`, animators={};
  const names=new Set(frames.flatMap(f=>Object.keys(f.bones)));
  for(const bone of names) {
    const b=rig.bones.get(bone);
    if(!b) throw Error(`${name}: missing ${bone}`);
    const channels=new Set(frames.flatMap(f=>Object.keys(f.bones[bone]||{})));
    const keyframes=[];
    for(const channel of channels) frames.forEach((f,i)=>{
      const v=(f.bones[bone]?.[channel] || (channel==='scale'?[1,1,1]:[0,0,0])).slice();
      if(channel==='rotation') {v[0]*=-1;v[1]*=-1;}
      if(channel==='position') v[0]*=-1;
      keyframes.push({uuid:uuid(`${name}/${bone}/${channel}/${i}`),channel,time:f.time,
        interpolation:'linear',data_points:[{x:v[0],y:v[1],z:v[2]}]});
    });
    animators[b.uuid]={name:bone,type:'bone',keyframes};
  }
  return {name,uuid:uuid(`animation/${name}`),length,loop,override:false,snapping:100,
    animators,markers:events.map((e,i)=>({time:e.time,color:i%8})),
    zeravorn_event_markers:events};
}

export function thirdAnimations(rig) {
  const result=[];
  const a=(...args)=>result.push(clip(rig,...args));
  a('idle',3,'loop',[pose(0),pose(.75,{chest:{...r(-.6),scale:[1.009,1.012,1.016]},arm_left:r(1,0,-.8),arm_right:r(1,0,.8)}),pose(1.5,{chest:{...r(-1.2),scale:[1.018,1.02,1.027]},head:r(.6),arm_left:r(2,0,-1.4),arm_right:r(2,0,1.4)}),pose(2.25,{chest:{...r(-.4),scale:[1.006,1.008,1.01]},belt_tail_left:r(1.5)}),pose(3)]);
  for(const [name,length,swing] of [['walk',1.12,25],['run',.72,42]]) {
    const frames=[];
    for(let i=0;i<=8;i++) {
      const phase=i/8*Math.PI*2, v=Math.cos(phase), k=Math.sin(phase),run=name==='run';
      frames.push(pose(i*length/8,{
        body:{...r(run?-7:-1,0,(run?2:1.4)*v),position:[0,(run?.38:.16)*(Math.cos(phase*2)-1),0]},
        chest:r(0,-(run?7:4)*v,0), head:r(run?5:0,(run?3:2)*v),
        leg_left:r(swing*v),leg_right:r(-swing*v),
        shin_left:r(Math.max(0,k)*(run?66:34)),shin_right:r(Math.max(0,-k)*(run?66:34)),
        foot_left:r(-Math.max(0,k)*15),foot_right:r(-Math.max(0,-k)*15),
        arm_left:r(-swing*.72*v,0,-3),arm_right:r(swing*.72*v,0,3),
        forearm_left:r(run?42:12),forearm_right:r(run?42:12),
        belt_tail_left:r(swing*.35*k,0,3*k),belt_tail_right:r(-swing*.3*k,0,-3*k),
      }));
    }
    // Exact endpoint values avoid floating point residuals at loop seams.
    frames[8].bones=structuredClone(frames[0].bones); a(name,length,'loop',frames);
  }
  for(const [suffix,side] of [['basic_1','right'],['basic_2','left']]) {
    const s=side==='right'?-1:1,arm=`arm_${side}`,forearm=`forearm_${side}`,other=`arm_${side==='right'?'left':'right'}`;
    a(suffix,1,'once',[pose(0),pose(.12,{chest:r(-3,s*20),[arm]:r(38,-s*30,s*32),[forearm]:r(78),[other]:r(22),body:r(0,-s*5)}),
      pose(.20,{chest:r(-4,s*6),[arm]:r(74,-s*18,s*48),[forearm]:r(68),[other]:r(28)}),
      pose(.25,{chest:r(-5,-s*24),[arm]:r(84,s*38,s*20),[forearm]:r(68),[other]:r(30),head:r(3,s*14),body:r(0,-s*8)}),
      pose(.32,{chest:r(-5,-s*32),[arm]:r(80,s*52,s*11),[forearm]:r(65),[other]:r(24)}),
      pose(.52,{chest:r(-2,-s*10),[arm]:r(40,s*12,s*8),[forearm]:r(42),[other]:r(14)}),pose(.82),pose(1)], [event('basic_hit',.25)]);
  }
  a('basic_3',1,'once',[pose(0),pose(.13,{chest:r(5,-18),arm_right:r(38,12,-14),forearm_right:r(94),arm_left:r(30),forearm_left:r(38)}),
    pose(.25,{body:r(-8,7),chest:r(-6,17),arm_right:r(100,-10,3),forearm_right:r(6),arm_left:r(34),forearm_left:r(50),head:r(10,-14)}),
    pose(.32,{body:r(-9,7),chest:r(-5,19),arm_right:r(106,-10,2),forearm_right:r(3),arm_left:r(31)}),
    pose(.53,{body:r(-3),arm_right:r(55),forearm_right:r(50),arm_left:r(20)}),pose(.86),pose(1)], [event('basic_hit',.25)]);
  a('q',.95,'once',[pose(0),pose(.10,{chest:r(3,-13),arm_right:r(24,12,-12),forearm_right:r(96),arm_left:r(25),forearm_left:r(45)}),
    pose(.23,{chest:r(5,-19),arm_right:r(30,19,-15),forearm_right:r(104),fist_right:r(0,0,-8),arm_left:r(30)}),
    pose(.30,{body:r(-6,4),chest:r(-7,21),arm_right:r(99,-6,4),forearm_right:r(4),arm_left:r(32),head:r(9,-12)}),
    pose(.36,{body:r(-7),chest:r(-5,16),arm_right:r(102,-6,4),forearm_right:r(2)}),
    pose(.58,{chest:r(-2,5),arm_right:r(54),forearm_right:r(78),fist_right:r(0,-20),arm_left:r(14)}),pose(.95)],
    [event('q_charge',.10,'server_start'),event('q_hit',.30),event('q_heal_return',.58,'server_heal')]);
  const dash={body:r(-19),head:r(15),arm_left:r(-27,0,-10),arm_right:r(-20,0,10),forearm_left:r(40),forearm_right:r(50),leg_left:r(35),leg_right:r(-30),shin_right:r(50),belt_tail_left:r(-32),belt_tail_right:r(-24)};
  a('e_dash_1',.48,'once',[pose(0),pose(.07,dash),pose(.23,dash),pose(.36,{body:r(-6),arm_left:r(-10),arm_right:r(10),leg_left:r(12),shin_right:r(18)}),pose(.48)],[event('e_dash',.07,'server_start')]);
  a('e_dash_2',.65,'once',[pose(0),pose(.06,{...dash,arm_right:r(45),forearm_right:r(100)}),
    pose(.20,{...dash,chest:r(-4,16),arm_right:r(102,-16,-6),forearm_right:r(36)}),
    pose(.27,{body:r(-11),chest:r(-8,22),head:r(15),arm_right:r(123,-16,-8),forearm_right:r(22),leg_left:r(20),shin_right:r(27)}),
    pose(.40,{chest:r(-4,10),arm_right:r(90),forearm_right:r(40)}),pose(.65)], [event('e_dash',.06,'server_start'),event('e2_hit',.27,'server_collision')]);
  const crouch={body:{...r(-23),position:[0,-2.6,0]},head:r(18),leg_left:r(-29),leg_right:r(-29),shin_left:r(56),shin_right:r(56),foot_left:r(-6),foot_right:r(-6),arm_left:r(-20),arm_right:r(-20),forearm_left:r(35),forearm_right:r(35)};
  const flight={body:{...r(-23),position:[0,3.5,0]},head:r(20),arm_left:r(135,0,-15),arm_right:r(135,0,15),forearm_left:r(44),forearm_right:r(44),leg_left:r(-25),leg_right:r(15),shin_left:r(60),shin_right:r(38),belt_tail_left:r(-65),belt_tail_right:r(-50)};
  const landing={body:{...r(-38),position:[0,-3.9,0]},head:r(26),leg_left:r(-42),leg_right:r(-42),shin_left:r(78),shin_right:r(78),foot_left:r(-12),foot_right:r(-12),arm_left:r(6,0,-8),arm_right:r(6,0,8),forearm_left:r(22),forearm_right:r(22),belt_tail_left:r(30),belt_tail_right:r(22)};
  a('r',1.6,'once',[pose(0),pose(.18,crouch),pose(.29,{body:r(5),arm_left:r(125,0,-13),arm_right:r(125,0,13),leg_left:r(10),leg_right:r(-10),shin_right:r(30)}),pose(.45,flight),pose(.73,flight),pose(.86,landing),pose(1.03,landing),pose(1.3,{body:{...r(-12),position:[0,-1.3,0]},head:r(10),shin_left:r(20),shin_right:r(20),leg_left:r(-10),leg_right:r(-10)}),pose(1.6)],
    [event('r_launch',.29,'server_launch'),event('r_flight',.45,'server_flight'),event('r_impact',.86,'server_landing')]);
  a('hit',.42,'once',[pose(0),pose(.07,{chest:r(12,-8),head:r(9,5),arm_left:r(-8),arm_right:r(-12)}),pose(.16,{chest:r(7,-4),head:r(5)}),pose(.30,{chest:r(-2)}),pose(.42)]);
  const daze={chest:r(-9),head:r(-10,0,7),arm_left:r(5,0,-3),arm_right:r(2,0,3),forearm_left:r(20),forearm_right:r(12)};
  a('stun',1.6,'loop',[pose(0,daze),pose(.4,{...daze,chest:r(-10,0,2),head:r(-7,4,9)}),pose(.8,{...daze,chest:r(-8,0,-2),head:r(-12,-3,4)}),pose(1.2,{...daze,head:r(-11,-2,7)}),pose(1.6,daze)]);
  const rooted={chest:r(-4),arm_left:r(27,0,-9),arm_right:r(30,0,9),forearm_left:r(44),forearm_right:r(41)};
  a('root',1.4,'loop',[pose(0,rooted),pose(.35,{...rooted,chest:r(-5,-3)}),pose(.7,{...rooted,chest:r(-3,3)}),pose(1.05,rooted),pose(1.4,rooted)]);
  const fallen={body:{...r(-87,0,8),position:[0,-17.2,0]},head:r(0,20),arm_left:r(10,0,32),arm_right:r(10,0,-39),forearm_left:r(34),forearm_right:r(20),leg_left:r(-3),leg_right:r(2)};
  a('death',1.6,'hold',[pose(0),pose(.16,{chest:r(16),head:r(14),arm_left:r(-10),arm_right:r(-14)}),pose(.4,{body:{...r(-20,0,7),position:[0,-2.5,0]},leg_left:r(-20),leg_right:r(-15),shin_left:r(43),shin_right:r(35),arm_left:r(40),arm_right:r(45)}),pose(.85,{...fallen,body:{...r(-78,0,8),position:[0,-15.9,0]}}),pose(1.03,fallen),pose(1.16,{...fallen,head:r(3,22)}),pose(1.6,fallen)]);
  const recall={chest:r(-3),head:r(7),arm_left:r(32,0,-14),arm_right:r(32,0,14),forearm_left:r(93),forearm_right:r(93),fist_left:r(0,-24),fist_right:r(0,24)};
  a('recall',5,'hold',[pose(0),pose(.5,recall),pose(1.5,{...recall,chest:r(-4)}),pose(2.5,{...recall,chest:r(-2)}),pose(3.5,{...recall,chest:r(-4)}),pose(4.4,recall),pose(5,{...recall,head:r(13),arm_left:r(38,0,-14),arm_right:r(38,0,14)})]);
  return result;
}

export function firstAnimations(rig) {
  const result=[],a=(...args)=>result.push(clip(rig,...args));
  const guard={arm_left:r(48,0,-8),arm_right:r(48,0,8),forearm_left:r(22),forearm_right:r(22)};
  const fp=(time,bones={})=>pose(time,{...guard,...bones});
  a('idle',3,'loop',[fp(0),fp(1.5,{arm_left:r(50,0,-9),arm_right:r(50,0,9)}),fp(3)]);
  for(const [suffix,side] of [['basic_1','right'],['basic_2','left'],['basic_3','right']]) {
    const s=side==='right'?-1:1,arm=`arm_${side}`,forearm=`forearm_${side}`,straight=suffix==='basic_3';
    a(suffix,1,'once',[fp(0),fp(.13,{[arm]:r(34,-s*8,s*12),[forearm]:r(54)}),
      fp(.25,{[arm]:r(straight?86:75,s*(straight?2:10),s*8),[forearm]:r(straight?4:35)}),
      fp(.33,{[arm]:r(straight?89:78,s*12,s*8),[forearm]:r(straight?3:30)}),fp(.55,{[arm]:r(59),[forearm]:r(42)}),fp(.85),fp(1)], [event('basic_hit',.25)]);
  }
  a('q',.95,'once',[fp(0),fp(.12,{arm_right:r(30,8,14),forearm_right:r(65)}),fp(.23,{arm_right:r(33,10,15),forearm_right:r(72)}),fp(.30,{arm_right:r(86,-2,8),forearm_right:r(3)}),fp(.36,{arm_right:r(89,-2,8),forearm_right:r(3)}),fp(.58,{arm_right:r(54),forearm_right:r(60),fist_right:r(0,18)}),fp(.95)],[event('q_charge',.1,'server_start'),event('q_hit',.30),event('q_heal_return',.58,'server_heal')]);
  a('e',.48,'once',[fp(0),fp(.07,{arm_left:r(20,0,-14),arm_right:r(20,0,14)}),fp(.23,{arm_left:r(20,0,-14),arm_right:r(20,0,14)}),fp(.48)], [event('e_dash',.07,'server_start')]);
  a('e_dash_1',.48,'once',[fp(0),fp(.07,{arm_left:r(20,0,-14),arm_right:r(20,0,14)}),fp(.23,{arm_left:r(20,0,-14),arm_right:r(20,0,14)}),fp(.48)], [event('e_dash',.07,'server_start')]);
  a('e_dash_2',.65,'once',[fp(0),fp(.08,{arm_right:r(35),forearm_right:r(78)}),fp(.27,{arm_right:r(86,3,10),forearm_right:r(9)}),fp(.38,{arm_right:r(91,3,10),forearm_right:r(9)}),fp(.65)],[event('e_dash',.06,'server_start'),event('e2_hit',.27,'server_collision')]);
  a('r',1.6,'once',[fp(0),fp(.18,{arm_left:r(28,0,-12),arm_right:r(28,0,12)}),fp(.29,{arm_left:r(82,0,-15),arm_right:r(82,0,15)}),fp(.45,{arm_left:r(70,0,-18),arm_right:r(70,0,18)}),fp(.73,{arm_left:r(70,0,-18),arm_right:r(70,0,18)}),fp(.86,{arm_left:r(32,0,-10),arm_right:r(32,0,10)}),fp(1.03,{arm_left:r(30,0,-10),arm_right:r(30,0,10)}),fp(1.6)],[event('r_launch',.29,'server_launch'),event('r_flight',.45,'server_flight'),event('r_impact',.86,'server_landing')]);
  a('hit',.42,'once',[fp(0),fp(.07,{arm_left:r(36,0,-13),arm_right:r(36,0,13)}),fp(.2,{arm_left:r(42,0,-10),arm_right:r(42,0,10)}),fp(.42)]);
  a('recall',5,'hold',[fp(0),fp(.5,{arm_left:r(60,0,-12),arm_right:r(60,0,12),forearm_left:r(30),forearm_right:r(30)}),fp(2.5,{arm_left:r(62,0,-12),arm_right:r(62,0,12),forearm_left:r(30),forearm_right:r(30)}),fp(4.5,{arm_left:r(60,0,-12),arm_right:r(60,0,12),forearm_left:r(30),forearm_right:r(30)}),fp(5,{arm_left:r(64,0,-12),arm_right:r(64,0,12),forearm_left:r(30),forearm_right:r(30)})]);
  return result;
}
