import {Model} from './core.mjs';

// Broad overlapping rock volumes, readable seams, no microscopic geometry.
function rock(m,name,bone,c,size,mat='rock',rot=[0,0,0]){
 m.box(name,bone,c,size,mat,rot);
 const s=size;
 m.box(name+'_facet',bone,[c[0]+s[0]*.12,c[1]+s[1]*.28,c[2]-s[2]*.12],[s[0]*.83,s[1]*.48,s[2]*.87],mat==='basalt'?'rockDark':'rockLight',[rot[0]+9,rot[1]+8,rot[2]-8]);
}
function shard(m,name,bone,base,length,width,tilt=[0,0,0],material='crystal'){
 // The tips are stepped rotated cuboids, so the source stays compatible with a cuboid renderer.
 const a=tilt[2]*Math.PI/180,b=tilt[0]*Math.PI/180,dir=[-Math.sin(a)*Math.cos(b),Math.cos(a)*Math.cos(b),Math.sin(b)];
 for(let i=0;i<3;i++){const u=[.29,.66,.88][i],sz=[1,.68,.34][i];m.box(name+'_'+i,bone,base.map((v,j)=>v+dir[j]*length*u),[width*sz,length*[.58,.26,.2][i],width*.72*sz],i===2?'glow':material,tilt);}
}
function rig(name,low=false){
 const m=new Model('jungle_'+name);m.bone('root');m.bone('pelvis','root',[0,11,4]);m.bone('torso','pelvis',[0,17,2]);m.bone('head','torso',[0,low?20:26,-5]);
 for(const s of [-1,1]){const side=s<0?'right':'left';m.bone('upper_arm_'+side,'torso',[s*(low?10:11),low?20:25,-1]);m.bone('forearm_'+side,'upper_arm_'+side,[s*(low?13:13),low?11:17,-3]);m.bone('hand_'+side,'forearm_'+side,[s*14,low?5:9,-5]);m.bone('leg_'+side,'pelvis',[s*5,11,4]);m.bone('foot_'+side,'leg_'+side,[s*5,3,3]);}
 m.bone('energy_core','torso',[0,20,-2]);m.bone('vfx_origin','root',[0,.5,-10]);m.bone('attack_origin','hand_right',[-14,low?4:8,-8]);
 return m;
}
function feet(m,kind){
 for(const s of [-1,1]){const side=s<0?'right':'left',mat=kind==='red_buff'?'basalt':'rockDark';
  rock(m,'haunch_'+side,'leg_'+side,[s*5,9,4],[6,8,7],mat,[0,0,s*7]);
  rock(m,'shin_'+side,'leg_'+side,[s*5.6,4.7,2.7],[5.8,5.7,5.8],kind.startsWith('green')?'wood':'rock',[7,0,-s*5]);
  m.box('sole_'+side,'foot_'+side,[s*5.6,1.4,1.6],[7,2.6,9],mat);
  for(let i=0;i<3;i++)m.box('toe_'+side+i,'foot_'+side,[s*5.6+(i-1)*2.1,1.6,-2.5],[1.7,2.4,3],kind.startsWith('green')?'woodLight':'rockLight',[8,0,(i-1)*5]);
 }
}
function face(m,{y=26,z=-6,width=6,wood=false}={}){
 rock(m,'skull','head',[0,y,z],[width,5.5,5.7],wood?'woodLight':'rockDark',[-8,0,0]);
 m.box('face_shadow','head',[0,y-.3,z-3],[width*.83,2.5,.45],'black');
 for(const s of [-1,1]){m.box('eye_'+s,'head',[s*width*.23,y+.1,z-3.3],[width*.2,.65,.35],'glow');m.box('brow_'+s,'head',[s*width*.23,y+1,z-3.3],[width*.52,1.1,1.3],wood?'wood':'rock',[0,0,s*16]);m.box('jaw_'+s,'head',[s*width*.27,y-2,z-2.8],[width*.34,1.8,2.1],wood?'woodLight':'rockDark',[0,0,-s*12]);}
 m.box('snout','head',[0,y-1,z-3.4],[width*.3,1.7,1.5],wood?'wood':'basalt');
}
function red(){
 const m=rig('red_buff');m.bone('lava_core','energy_core',[0,20,-2]);feet(m,'red_buff');
 m.box('molten_torso','lava_core',[0,20,0],[11.5,13,8.5],'core',[-9,0,0]);
 rock(m,'belly','torso',[0,16,1],[12,7,10],'basalt',[-8,0,5]);
 rock(m,'hunched_back','torso',[0,24,4],[14,12,8],'rockDark',[-20,0,0]);
 for(const s of [-1,1]){
  const side=s<0?'right':'left';
  rock(m,'chest_plate_'+side,'torso',[s*3.8,23,-4.1],[6.3,6.4,3.6],'basalt',[-12,s*7,s*12]);
  rock(m,'rib_'+side,'torso',[s*4.8,18.3,-3.7],[4,3.1,3.4],'rockDark',[8,0,-s*12]);
  rock(m,'shoulder_'+side,'upper_arm_'+side,[s*10,26,-.2],[9,8.5,9],'basalt',[5,0,s*17]);
  rock(m,'arm_'+side,'upper_arm_'+side,[s*12,21,-2],[6.2,8.4,6.5],'rockDark',[0,0,-s*12]);
  m.box('elbow_heat_'+side,'forearm_'+side,[s*13,16,-3],[5.8,4,5.5],'core');
  rock(m,'forearm_'+side,'forearm_'+side,[s*13.5,13.2,-4],[7.2,7.8,7],'basalt',[10,0,s*8]);
  rock(m,'fist_'+side,'hand_'+side,[s*14,8,-5.6],[8.5,7.4,8],'rockDark',[-8,0,s*8]);
  for(let i=0;i<3;i++){m.box('knuckle_glow_'+side+i,'hand_'+side,[s*14+(i-1)*2.5,8,-9.6],[1.7,2.7,.45],'crystal',[0,0,8]);m.box('finger_'+side+i,'hand_'+side,[s*14+(i-1)*2.6,5.7,-8.6],[2.2,3.4,3],'basalt',[12,0,0]);}
  shard(m,'shoulder_ember_'+side,'upper_arm_'+side,[s*11,29,1],4,2,[5,0,-s*28],'core');
  for(let j=0;j<3;j++)m.box('rib_fissure_'+side+j,'lava_core',[s*(2.8+j*.8),18+j*2,-5.3],[.55,2.2,.45],'crystal',[0,0,s*(24+j*7)]);
  // Exposed channels on the forward rock faces remain visible at gameplay distance.
  branches(m,'chest_lava_'+side,'torso',[[s*2.6,25,-6.4],[s*3.3,23,-6.5],[s*2.8,21,-6.4]],.6,'crystal');
  branches(m,'forearm_lava_'+side,'forearm_'+side,[[s*14.4,15,-8],[s*13.4,13,-8.2],[s*14,11,-8.3]],.55,'crystal');
  branches(m,'back_lava_'+side,'torso',[[s*3,28,9.7],[s*2,25,10],[s*3.6,22,9.7]],.7,'core');
  rock(m,'broken_shoulder_'+side,'upper_arm_'+side,[s*13,27.5,-2],[3.7,4.2,4.4],'basalt',[20,s*18,s*25]);
 }
 face(m,{y:27,z:-5.5,width:6.3});
 for(let i=0;i<4;i++)rock(m,'back_rock_'+i,'torso',[(i%2?1:-1)*3,24+Math.floor(i/2)*4,7],[5.5,5.5,4.5],'basalt',[15,i*12,10-i*8]);
 return m;
}
function blue(){
 const m=rig('blue_buff',true);m.bone('crystal_crown','head',[0,24,-7]);feet(m,'blue_buff');
 rock(m,'low_body','torso',[0,17.5,3.5],[15,12,16],'rockDark',[12,0,0]);
 m.box('arcane_core','energy_core',[0,18,-4.7],[7,8,2],'core');
 for(const s of [-1,1]){
  const side=s<0?'right':'left';rock(m,'chest_'+side,'torso',[s*4.7,18,-4.4],[5.4,8,4.5],'rock',[0,0,s*20]);
  rock(m,'shoulder_'+side,'upper_arm_'+side,[s*10.4,21,0],[9,9,10],'rock',[12,0,s*12]);
  rock(m,'front_arm_'+side,'upper_arm_'+side,[s*12.8,15,-3],[6.5,10,7],'rockDark',[-15,0,s*7]);
  rock(m,'brace_'+side,'forearm_'+side,[s*13.7,9,-5],[7.5,7,8.8],'rock',[12,0,0]);
  rock(m,'front_paw_'+side,'hand_'+side,[s*14,4.1,-6.4],[8,5.7,10],'rockDark');
  for(let i=0;i<3;i++)m.box('claw_'+side+i,'hand_'+side,[s*14+(i-1)*2.3,2,-11.3],[1.5,2.1,3.7],'crystal',[15,0,0]);
  shard(m,'shoulder_spire_'+side,'upper_arm_'+side,[s*10,24,0],9,3.6,[12,0,-s*22]);
  shard(m,'outer_spire_'+side,'upper_arm_'+side,[s*13,23,2],6.5,3,[25,0,-s*38]);
  shard(m,'forearm_shard_'+side,'forearm_'+side,[s*15,10,-4],5,2.5,[30,0,-s*25]);
  rock(m,'shoulder_scale_'+side,'upper_arm_'+side,[s*13.8,20.3,-3.5],[3.8,4.7,4],'rockDark',[15,20,s*26]);
 }
 face(m,{y:21,z:-8,width:7.4});
 for(let i=0;i<4;i++)shard(m,'spinal_crystal_'+i,'torso',[i%2?2:-2,23-i*.5,1+i*3],9-i*.7,3.2,[18+i*5,0,i%2?10:-10]);
 shard(m,'crown_central','crystal_crown',[0,24,-7],6.5,2.6,[-8,0,0]);
 for(const s of [-1,1])shard(m,'crown_side_'+s,'crystal_crown',[s*2.8,23,-6],4.7,1.8,[0,0,-s*25]);
 return m;
}
function branches(m,name,bone,path,width,mat='wood'){
 for(let i=1;i<path.length;i++)m.rod(name+'_'+i,bone,path[i-1],path[i],width*(1-i*.12),width*(1-i*.12),mat);
}
function foliage(m,name,bone,c,size){m.box(name,bone,c,size,'moss',[0,15,8]);m.box(name+'_light',bone,[c[0]-.5,c[1]+size[1]*.35,c[2]-.5],[size[0]*.65,size[1]*.7,size[2]*.65],'mossLight',[0,-15,-10]);}
function greenOne(){
 const m=rig('green_1',true);m.bone('horns','head',[0,24,-6]);feet(m,'green_1');
 rock(m,'root_body','torso',[0,17,3],[14,12,13],'wood',[-10,0,0]);rock(m,'stone_back','torso',[0,21,5],[12,8,10],'rockDark',[16,0,-8]);
 m.box('forest_heart','energy_core',[0,18,-4.1],[5,6,1.5],'core',[0,0,45]);
 for(const s of [-1,1]){
  const side=s<0?'right':'left';
  rock(m,'rib_stone_'+side,'torso',[s*4,18,-3],[4.5,8,5],'rock',[0,0,s*20]);
  rock(m,'ram_shoulder_'+side,'upper_arm_'+side,[s*10,21,-1.7],[10,9,10],'rock',[0,0,-s*16]);
  foliage(m,'shoulder_moss_'+side,'upper_arm_'+side,[s*10,25,-1],[7.5,2.5,7]);
  branches(m,'upper_root_'+side,'upper_arm_'+side,[[s*10,22,1],[s*13,15,-2],[s*14,10,-3]],4.5);
  rock(m,'forearm_boulder_'+side,'forearm_'+side,[s*14,9.5,-4.5],[8,9,8],'rockDark',[15,0,s*10]);
  rock(m,'ram_fist_'+side,'hand_'+side,[s*14,4.8,-6],[9,6.2,10],'rock',[0,0,s*5]);
  for(let i=0;i<3;i++)branches(m,'finger_root_'+side+i,'hand_'+side,[[s*14+(i-1)*2.7,6,-7],[s*14+(i-1)*2.7,3,-10],[s*14+(i-1)*2.7,1.4,-10]],1.7,'root');
  branches(m,'swept_horn_'+side,'horns',[[s*2,23,-6],[s*5,26,-6],[s*8,29,-4],[s*8.7,32,-2]],2.2);
  branches(m,'horn_fork_'+side,'horns',[[s*5,26,-6],[s*7,27,-8],[s*8,29,-9]],1.3);
  shard(m,'green_spine_'+side,'torso',[s*4,24,3],5.2,2.4,[10,0,-s*20]);
  foliage(m,'hip_moss_'+side,'leg_'+side,[s*6,12,4],[4.8,2,4.5]);
  shard(m,'shoulder_thorn_'+side,'upper_arm_'+side,[s*10,25,-1],5.5,2,[10,0,-s*30]);
  foliage(m,'outer_leaves_'+side,'upper_arm_'+side,[s*14,23,2],[3.8,2,4]);
  branches(m,'chest_root_'+side,'torso',[[s*6,22,-5],[s*3.5,19,-6],[s*4.5,14,-5]],1.3,'root');
 }
 face(m,{y:21,z:-7.4,width:7,wood:true});
 foliage(m,'back_canopy','torso',[0,25,5.5],[9,2.8,7]);
 return m;
}
function greenTwo(){
 const m=rig('green_2');m.bone('antlers','head',[0,29,-4]);feet(m,'green_2');
 rock(m,'ancient_trunk','torso',[0,20,1.5],[12,16,11],'woodLight',[-6,0,4]);
 m.box('hollow_heart','energy_core',[0,21,-4.2],[5,6,1.2],'black');m.box('heart_rune','energy_core',[0,21,-4.9],[1.2,3.3,.5],'glow');
 for(const s of [-1,1]){
  const side=s<0?'right':'left';rock(m,'stone_rib_'+side,'torso',[s*4.3,19,-3.4],[5,8,4],'rock',[0,0,-s*12]);
  rock(m,'old_shoulder_'+side,'upper_arm_'+side,[s*10.5,26,0],[9,8,9],'woodLight',[0,0,s*14]);
  foliage(m,'shoulder_crown_'+side,'upper_arm_'+side,[s*10,29.5,0],[7,2.8,6]);
  branches(m,'arm_trunk_'+side,'upper_arm_'+side,[[s*10,26,0],[s*13,20,-1],[s*13.6,15,-2]],4.5,'woodLight');
  rock(m,'forearm_stone_'+side,'forearm_'+side,[s*13.7,13,-3],[6.8,8,6.6],'rock',[10,0,s*8]);
  rock(m,'ancient_hand_'+side,'hand_'+side,[s*14,8,-4.5],[7.8,7,7],'woodLight');
  for(let i=0;i<3;i++)branches(m,'root_claw_'+side+i,'hand_'+side,[[s*14+(i-1)*2.2,8,-6],[s*14+(i-1)*2.2,5,-8],[s*14+(i-1)*2.2,3.7,-7]],1.5,'root');
  branches(m,'trunk_root_'+side,'torso',[[s*2.6,27,-4],[s*5.2,22,-5.7],[s*2.7,16,-5],[s*4,13,-3]],1.6,'bark');
  const main=[[s*2.5,29,-4],[s*6,34,-3],[s*8,40,-2],[s*9,46,-1],[s*8,50,0]];
  branches(m,'antler_main_'+side,'antlers',main,2.8,'woodLight');
  branches(m,'antler_outer_'+side,'antlers',[[s*6.4,35,-3],[s*12,37,-3],[s*15,40,-2],[s*15.5,43,-1]],1.9,'woodLight');
  branches(m,'antler_mid_'+side,'antlers',[[s*8,40,-2],[s*12,42,-2],[s*13,46,-1]],1.6,'woodLight');
  branches(m,'antler_inner_'+side,'antlers',[[s*8.5,44,-1],[s*4.5,46,-2],[s*4,49,-2]],1.4,'woodLight');
  foliage(m,'antler_leaves_'+side,'antlers',[s*12.5,38,-3],[3,1.4,3]);
  foliage(m,'knee_moss_'+side,'leg_'+side,[s*5.7,8.5,1],[4.5,1.6,4]);
 }
 face(m,{y:28,z:-5.3,width:6,wood:true});
 for(let i=0;i<3;i++)branches(m,'back_root_'+i,'torso',[[(i-1)*3,27,6],[(i-1)*3+1,21,7.6],[(i-1)*3,14,7]],1.7,'bark');
 foliage(m,'back_moss','torso',[1,24,7],[7,3,2.5]);
 return m;
}
export const createModels=()=>[red(),blue(),greenOne(),greenTwo()];
