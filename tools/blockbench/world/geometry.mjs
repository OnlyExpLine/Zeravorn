import {Model,clip,R,P,event,crystal,ring,banner} from './pipeline.mjs';

export function throne(){
 const m=new Model('throne');m.bone('root');m.bone('platform','root');m.bone('pedestal','platform');m.bone('central_mount','pedestal',[0,24,0]);m.bone('core_crystal','central_mount',[0,56,0]);
 for(const [n,p,v]of[['crystal_left','central_mount',[-17,40,0]],['crystal_right','central_mount',[17,40,0]],['pillar_left','platform',[-35,10,8]],['pillar_right','platform',[35,10,8]],['arch_left','pillar_left',[-35,60,8]],['arch_right','pillar_right',[35,60,8]],['energy_ring_01','central_mount',[0,34,0]],['energy_ring_02','central_mount',[0,56,0]],['vfx_origin','core_crystal',[0,56,0]],['projectile_origin','core_crystal',[0,56,-17]],['shield_origin','central_mount',[0,48,0]]])m.bone(n,p,v);
 for(const [i,w,d]of[[0,92,70],[1,84,64],[2,76,58]])m.box('platform_step_'+i,'platform',[0,2+i*4,0],[w,4,d],i===1?'steel':'dark');
 for(let i=0;i<3;i++)m.box('front_stair_'+i,'platform',[0,2+i*3,-40+i*4],[44-i*5,4,9],'steel');
 m.box('pedestal_stone','pedestal',[0,15,0],[36,10,30],'steel');m.box('pedestal_trim','pedestal',[0,21,0],[40,3,34],'gold');m.box('core_socket','central_mount',[0,25,0],[25,7,24],'goldDark');
 crystal(m,'nexus_core','core_crystal',[0,55,0],25,55);crystal(m,'left_shard','crystal_left',[-18,41,0],8,28);crystal(m,'right_shard','crystal_right',[18,41,0],8,28);
 for(const s of [-1,1]){
  const side=s<0?'left':'right',bone='pillar_'+side;
  m.box('pillar_foot_'+side,bone,[s*35,15,8],[17,9,18],'goldDark');
  for(let i=0;i<5;i++)m.box('pillar_course_'+side+i,bone,[s*35,23+i*8,8],[12,7.5,13],'steel');
  for(const z of [1,15])m.box('pillar_rib_'+side+z,bone,[s*35,39,z],[3,40,2.5],'goldDark');
  m.box('pillar_cap_'+side,bone,[s*35,62,8],[18,4,18],'gold');
  m.rod('arch_sweep_'+side,'arch_'+side,[s*35,61,8],[s*21,77,8],6,7,'steel');m.rod('arch_gold_'+side,'arch_'+side,[s*35,65,4],[s*22,80,4],2,2,'gold');
  m.box('arch_crown_'+side,'arch_'+side,[s*22,80,8],[6,5,8],'gold');
  banner(m,'banner_'+side,bone,s*44,58,3,12,34);
 }
 ring(m,'lower_energy','energy_ring_01',34,19,1.5,'crystal');ring(m,'upper_energy','energy_ring_02',56,21,1.2,'crystal');
 clip(m,'idle',4,'loop',{core_crystal:P([[0,[0,0,0]],[2,[0,1.2,0]],[4,[0,0,0]]]),energy_ring_01:R([[0,[0,0,0]],[2,[0,15,0]],[4,[0,0,0]]])},[event('CORE_PULSE',2,'vfx_origin')]);
 clip(m,'protected',3,'loop',{energy_ring_01:R([[0,[0,-8,0]],[1.5,[0,8,0]],[3,[0,-8,0]]]),energy_ring_02:R([[0,[0,8,0]],[1.5,[0,-8,0]],[3,[0,8,0]]])},[event('SHIELD_ACTIVE',0,'shield_origin')]);
 clip(m,'vulnerable',.8,'hold',{energy_ring_01:P([[0,[0,0,0]],[.8,[0,-8,0]]]),energy_ring_02:P([[0,[0,0,0]],[.8,[0,-25,0]]])},[event('SHIELD_COLLAPSE',.15,'shield_origin')]);
 clip(m,'attack',1.2,'once',{core_crystal:R([[0,[0,0,0]],[.6,[-6,0,0]],[.8,[4,0,0]],[1.2,[0,0,0]]]),crystal_left:P([[0,[0,0,0]],[.6,[2,3,0]],[1.2,[0,0,0]]]),crystal_right:P([[0,[0,0,0]],[.6,[-2,3,0]],[1.2,[0,0,0]]])},[event('PROJECTILE_RELEASE',.72,'projectile_origin')]);
 clip(m,'hit',.3,'once',{core_crystal:R([[0,[0,0,0]],[.08,[3,0,4]],[.3,[0,0,0]]])});
 clip(m,'low_hp',1.5,'loop',{core_crystal:R([[0,[0,0,0]],[.3,[2,3,2]],[.7,[-2,-4,-3]],[1.1,[2,3,1]],[1.5,[0,0,0]]])},[event('CRACK_SPARKS',.3,'vfx_origin')]);
 clip(m,'destroy',3,'hold',{core_crystal:{rotation:[[0,[0,0,0]],[.8,[0,25,0]],[1.3,[8,40,12]],[3,[20,50,35]]],position:[[0,[0,0,0]],[1,[0,6,0]],[3,[0,-9,0]]]},crystal_left:P([[0,[0,0,0]],[1,[0,2,0]],[2,[10,5,2]],[3,[13,-10,4]]]),crystal_right:P([[0,[0,0,0]],[1,[0,2,0]],[2,[-10,5,2]],[3,[-13,-10,4]]])},[event('CRYSTAL_CRACK',.5,'vfx_origin'),event('NEXUS_BURST',1.2,'vfx_origin'),event('CORE_EXTINGUISH',2.8,'vfx_origin')]);
 return {model:m,phase:'D'};
}

export function fountain(){
 const m=new Model('fountain');m.bone('root');m.bone('platform_outer','root');m.bone('platform_inner','root');m.bone('central_pedestal','root');m.bone('central_crystal','central_pedestal',[0,44,0]);m.bone('energy_origin','root',[0,12,0]);
 m.box('foundation','platform_inner',[0,2,0],[54,4,54],'dark');m.box('pool','platform_inner',[0,5,0],[48,3,48],'teamDark');
 for(let i=0;i<12;i++){const a=i*Math.PI/6,x=Math.sin(a)*29,z=Math.cos(a)*29;m.box('rim_'+i,'platform_outer',[x,6,z],[16,8,8],'steel',[0,a*180/Math.PI,0]);m.box('rim_cap_'+i,'platform_outer',[x,11,z],[17,2,9],'goldDark',[0,a*180/Math.PI,0]);if(i%3===0)m.box('rim_gem_'+i,'platform_outer',[x,9,z],[3,4,10],'crystal',[0,a*180/Math.PI,0]);}
 m.box('central_foot','central_pedestal',[0,9,0],[20,8,20],'steel');m.box('central_step','central_pedestal',[0,15,0],[16,4,16],'goldDark');m.box('central_stem','central_pedestal',[0,23,0],[11,12,11],'steel');m.box('core_mount','central_pedestal',[0,29,0],[17,3,17],'gold');
 crystal(m,'healing_core','central_crystal',[0,45,0],13,31);
 for(let i=0;i<4;i++){const a=i*Math.PI/2+Math.PI/4,x=Math.sin(a)*23,z=Math.cos(a)*23,n='floating_crystal_0'+(i+1);m.bone(n,'root',[x,30,z]);crystal(m,n,n,[x,30+i%2*8,z],5.5,14);}
 ring(m,'healing_ring','platform_inner',7.5,21,1.5,'crystal');
 const idle={central_crystal:P([[0,[0,0,0]],[2,[0,1,0]],[4,[0,0,0]]])};for(let i=1;i<=4;i++)idle['floating_crystal_0'+i]={position:[[0,[0,0,0]],[2,[0,i%2?2:-2,0]],[4,[0,0,0]]],rotation:[[0,[0,0,0]],[2,[0,i%2?15:-15,0]],[4,[0,0,0]]]};
 clip(m,'idle',4,'loop',idle,[event('CORE_PULSE',2,'energy_origin')]);
 clip(m,'heal',2,'loop',{central_crystal:P([[0,[0,0,0]],[1,[0,2,0]],[2,[0,0,0]]])},[event('HEAL_PARTICLES',.2,'energy_origin'),event('HEAL_PARTICLES',1.2,'energy_origin')]);
 clip(m,'danger',1,'loop',{central_crystal:R([[0,[0,0,0]],[.4,[0,25,0]],[1,[0,0,0]]])},[event('DANGER_BEAM',.4,'energy_origin')]);
 return {model:m,phase:'E'};
}

export function projectiles(){
 return ['basic','ice','fire','poison','tower_shot','throne_shot'].map(kind=>{
  const name=kind.endsWith('_shot')?kind:'projectile_'+kind,m=new Model(name);m.bone('root');m.bone('core','root');m.bone('trail_origin','root',[0,0,7]);m.bone('impact_origin','root',[0,0,-8]);
  const large=kind==='throne_shot',width=large?2.4:kind==='tower_shot'?1.7:1,length=large?14:10;
  m.box('energy_spine','core',[0,0,-1],[width*.5,width*.5,length],'glow');
  if(kind==='basic')for(const s of [-1,1])m.rod('arrow_fin_'+s,'core',[0,0,-6],[s*1.8,0,-2],.6,.6,'crystal');
  else if(kind==='fire'){
   m.box('hot_core','core',[0,0,-3],[2.8,2.8,3],'glow',[25,35,20]);
   for(let i=0;i<4;i++){const a=i*Math.PI/2;m.rod('flame_'+i,'core',[Math.sin(a)*.8,Math.cos(a)*.8,-2],[Math.sin(a)*1.7,Math.cos(a)*1.7,4+i%2*2],.8,.8,'crystal');}
  }else if(kind==='poison'){
   for(let i=0;i<3;i++)m.box('toxic_core_'+i,'core',[(i-1)*.7,i%2*.5,-3+i*.7],[2,2,2],'crystal',[i*20,i*30,15]);
   for(const s of [-1,1])m.box('spore_'+s,'core',[s*2,s*.7,2],[.8,.8,.8],'glow',[0,30,30]);
  }else{
   const count=large?7:5;
   for(let i=0;i<count;i++){const t=(i+.5)/count,w=width*(.2+1.5*(1-Math.abs(t*2-1)));m.box('spear_facet_'+i,'core',[0,0,-length*.65+t*length*.8],[w,w,length*.8/count+.02],i%3?'crystal':'glow',[0,0,45]);}
   for(let i=0;i<(large?4:2);i++){const a=i*Math.PI/2;m.rod('energy_shard_'+i,'core',[Math.sin(a)*(width+1.2),Math.cos(a)*(width+1.2),-2],[Math.sin(a)*(width+2),Math.cos(a)*(width+2),3],.6,.6,'crystal');}
  }
  clip(m,'flight',.6,'loop',{core:R([[0,[0,0,0]],[.3,[0,0,12]],[.6,[0,0,0]]])});
  return {model:m,phase:'F',variants:kind==='tower_shot'||kind==='throne_shot'?['blue','red']:[kind==='ice'?'blue':kind]};
 });
}

function environmentModel(name){const m=new Model(name);m.bone('root');return m;}
function rock(m,name,center,size,rotation=[0,0,0],bone='root'){
 for(let i=0;i<3;i++)m.box(name+'_layer_'+i,bone,[center[0]+(i-1)*size[0]*.08,center[1]+(i-1)*size[1]*.29,center[2]+(i%2)*size[2]*.08],[size[0]*(i===1?1:.78),size[1]*.4,size[2]*(i===1?1:.8)],i===0?'dark':'steel',[0,rotation[1]+i*13,0]);
 m.box(name+'_moss',bone,[center[0]+size[0]*.13,center[1]+size[1]*.49,center[2]],[size[0]*.5,1,size[2]*.55],'team',rotation);
 m.box(name+'_fissure',bone,[center[0]+size[0]*.13,center[1],center[2]-size[2]*.51],[.6,size[1]*.4,.3],'dark',rotation);
}
export function environment(){
 const entries=[],add=m=>entries.push({model:m,phase:'G',variants:['forest']});
 for(const [kind,w,h]of[['small',16,15],['medium',25,20],['large',34,25],['corner',28,22]]){
  const m=environmentModel('bush_'+kind);
  for(let x=-1;x<=1;x++)for(let z=-1;z<=1;z++){
   if(kind==='corner'&&x===1&&z===1)continue;
   const yy=h-(Math.abs(x)+Math.abs(z))*2,px=x*w*.28,pz=z*w*.28;
   m.box(`stem_${x}_${z}`,'root',[px,yy*.35,pz],[2,yy*.7,2],'wood');
   m.box(`leaf_mass_${x}_${z}`,'root',[px,yy*.60,pz],[w*.42,yy*.7,w*.42],(x+z)%2?'teamDark':'team');
   for(let j=0;j<3;j++)m.box(`leaf_tip_${x}_${z}_${j}`,'root',[px+(j-1)*3,yy*.96+(j%2)*2,pz+(j%2)*2],[3,4,4],'teamLight',[0,j*20,0]);
  }add(m);
 }
 for(let variant=0;variant<3;variant++){
  const m=environmentModel('tree_'+String.fromCharCode(97+variant)),h=58+variant*10;m.bone('canopy','root',[0,h,0]);
  m.rod('trunk','root',[0,5,0],[variant===1?9:-4,h-12,0],10+variant,10,'wood');
  for(let i=0;i<5;i++){const a=i*Math.PI*2/5;m.rod('root_'+i,'root',[Math.sin(a)*18,3,Math.cos(a)*18],[0,12,0],5,5,'wood');}
  for(let i=0;i<5;i++){
   const a=i*Math.PI*2/5+variant*.7,x=Math.sin(a)*(17+variant*2),z=Math.cos(a)*(17+variant*2),y=h+(i%2)*7;
   m.rod('branch_'+i,'root',[0,h-22,0],[x,y-8,z],5,5,'wood');
   m.box('canopy_dark_'+i,'canopy',[x,y,z],[23+variant*3,16,23],'teamDark',[0,i*17,0]);
   m.box('canopy_top_'+i,'canopy',[x-2,y+9,z],[19,7,18],i%2?'team':'teamLight',[0,i*17,0]);
   m.box('vine_'+i,'canopy',[x+8,y-12,z],[2,14,2],'team');
  }add(m);
 }
 for(const [kind,size]of[['small',10],['medium',18],['large',28],['cluster',19]]){
  const m=environmentModel('rock_'+kind);rock(m,'main',[0,size*.48,0],[size,size*.9,size*.85],[0,17,0]);
  if(kind==='cluster')for(const s of [-1,1])rock(m,'satellite_'+s,[s*12,6,s*5],[11,12,13],[0,s*25,0]);
  else rock(m,'broken_face',[size*.35,size*.25,-size*.2],[size*.4,size*.45,size*.7],[0,-12,0]);add(m);
 }
 for(const kind of ['column_full','column_broken','arch','wall_small','base','stairs','rubble']){
  const m=environmentModel('ruin_'+kind);
  const column=(name,x,h,z=0)=>{
   m.box(name+'_foot','root',[x,3,z],[17,6,17],'dark');
   for(let y=8;y<h-5;y+=8)m.box(name+'_course_'+y,'root',[x,y,z],[11,7.5,11],'steel');
   m.box(name+'_capital','root',[x,h-3,z],[16,6,16],'steel');
   for(const s of [-1,1])m.box(name+'_flute_'+s,'root',[x+s*4,Math.max(10,h/2),z-6],[1,h-13,1],'dark');
   m.box(name+'_moss','root',[x-4,h+.5,z],[7,1,12],'team');
  };
  if(kind==='column_full')column('column',0,56);
  if(kind==='column_broken'){column('broken',0,28);rock(m,'fragment',[13,4,4],[12,8,10],[0,28,0]);}
  if(kind==='arch'){for(const s of [-1,1]){column('pier_'+s,s*22,44);m.rod('arch_'+s,'root',[s*22,43,0],[s*7,56,0],10,12,'steel');}m.box('keystone','root',[0,56,0],[13,12,14],'dark');}
  if(kind==='wall_small')for(let y=0;y<4;y++)for(let x=0;x<4;x++)m.box('brick_'+x+'_'+y,'root',[(x-1.5)*10+(y%2)*2,3+y*6,0],[9.7,5.7,8],(x+y)%5?'steel':'dark');
  if(kind==='base')for(let i=0;i<3;i++)m.box('step_'+i,'root',[0,2+i*4,0],[32-i*5,4,32-i*5],'steel');
  if(kind==='stairs')for(let i=0;i<5;i++)m.box('stair_'+i,'root',[0,(i+1)*2,i*7-14],[28,(i+1)*4,7],'steel');
  if(kind==='rubble')for(let i=0;i<8;i++)rock(m,'rubble_'+i,[(i%3-1)*9,3+(i%2),Math.floor(i/3)*8-8],[7+i%3,6,8],[0,i*29,0]);
  add(m);
 }
 for(const kind of ['post','wall','hanging']){
  const m=environmentModel('lantern_'+kind),h=kind==='post'?48:kind==='wall'?28:24;m.bone('lamp','root',[0,h-4,0]);m.bone('light_origin','lamp',[0,h-12,0]);
  if(kind==='post'){m.box('foot','root',[-10,3,0],[14,6,14],'steel');m.box('post','root',[-10,24,0],[5,38,5],'dark');m.rod('crooked_arm','root',[-10,43,0],[0,48,0],3,3,'goldDark');}
  if(kind==='wall'){m.box('wall_mount','root',[0,h,-10],[8,13,3],'steel');m.rod('wall_bracket','root',[0,h,-10],[0,h,0],3,3,'goldDark');}
  m.box('chain','root',[0,h-3,0],[1,6,1],'goldDark');m.box('roof','lamp',[0,h-7,0],[11,3,11],'dark');m.box('lamp_base','lamp',[0,h-19,0],[10,3,10],'dark');
  for(const x of [-4,4])for(const z of [-4,4])m.box('frame_'+x+'_'+z,'lamp',[x,h-13,z],[1,10,1],'goldDark');crystal(m,'light','lamp',[0,h-13,0],4,9);
  clip(m,'idle',3,'loop',{lamp:R([[0,[0,0,-3]],[1.5,[0,0,3]],[3,[0,0,-3]]])});
  entries.push({model:m,phase:'G',variants:['blue','red']});
 }
 return entries;
}
