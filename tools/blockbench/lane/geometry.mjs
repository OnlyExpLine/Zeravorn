import {Model} from './core.mjs';

function infantry(kind){
 const m=new Model('lane_'+kind),ranged=kind==='ranged',w=ranged?4.3:5.8;
 m.bone('root');m.bone('body','root',[0,8,0]);m.bone('head','body',[0,16,0]);m.bone('helmet','head',[0,19,0]);m.bone('armor_chest','body',[0,12,0]);
 m.box('gambeson','body',[0,11,0],[w,6,3.5],'teamDark');
 m.box('breastplate','armor_chest',[0,12.6,-1.6],[w+.6,4.7,1.1],'team');
 m.box('backplate','armor_chest',[0,12.5,1.8],[w,4.4,.8],'steel');
 m.box('collar','armor_chest',[0,15.1,-.1],[w+1,1,4.1],'goldDark');
 m.box('chest_crest','armor_chest',[0,13.3,-2.25],[1.5,1.5,.4],'gold',[0,0,45]);
 m.box('crest_inlay','armor_chest',[0,13.3,-2.5],[.65,.65,.25],'crystal',[0,0,45]);
 m.box('belt','body',[0,8.8,0],[w+.6,1,4.2],'leather');m.box('buckle','body',[0,8.8,-2.25],[1.5,1.1,.5],'gold');
 for(const s of [-1,1]){
  const side=s<0?'right':'left',legX=s*(ranged?1.4:1.8),armX=s*(ranged?3.5:4.5);
  m.bone('leg_'+side,'root',[legX,8,0]);m.bone('arm_'+side,'body',[armX,14,0]);m.bone('forearm_'+side,'arm_'+side,[armX,10.8,-.2]);m.bone('armor_shoulder_'+side,'arm_'+side,[armX,14,0]);
  m.box('thigh_'+side,'leg_'+side,[legX,6.6,0],[2.3,3.2,2.8],'leather');
  m.box('greave_'+side,'leg_'+side,[legX,3.8,-.4],[2.6,3.4,2.9],'steel');
  m.box('shin_plate_'+side,'leg_'+side,[legX,3.9,-1.9],[2.2,2.5,.55],'team');
  m.box('knee_'+side,'leg_'+side,[legX,5.5,-1.65],[2.7,1.3,1],'goldDark',[12,0,0]);
  m.box('boot_'+side,'leg_'+side,[legX,1.2,-.7],[2.9,2.3,4.1],'dark');
  m.box('toe_cap_'+side,'leg_'+side,[legX,1.4,-2.15],[3,1.5,1.5],'steel');
  m.box('tasset_'+side,'body',[s*(w/2-.6),7.7,-2],[2.2,2.8,.8],'team',[0,0,-s*10]);
  m.box('tasset_trim_'+side,'body',[s*(w/2-.4),6.5,-1.8],[2.2,.45,.9],'goldDark');
  m.box('upper_arm_'+side,'arm_'+side,[armX,12.2,0],[2,3.7,2.7],'teamDark');
  m.box('bracer_'+side,'forearm_'+side,[armX,10,-.3],[2.5,2.1,3],'steel');
  m.box('bracer_team_'+side,'forearm_'+side,[armX,10,-1.9],[1.7,1.4,.5],'team');
  m.box('fist_'+side,'forearm_'+side,[armX,8.5,-.4],[2.3,1.7,2.5],'skinDark');
  m.box('shoulder_'+side,'armor_shoulder_'+side,[armX,14.4,0],[ranged?3:4.4,1.9,4.5],'team',[0,0,-s*12]);
  m.box('shoulder_rim_'+side,'armor_shoulder_'+side,[armX,14.6,-2.32],[ranged?3.1:4.5,.45,.5],'gold',[0,0,-s*12]);
  if(!ranged){m.box('shoulder_lamella_'+side,'armor_shoulder_'+side,[armX+s*.9,13.4,-.1],[2.8,1.5,4.3],'steel',[0,0,-s*20]);m.box('shoulder_stud_'+side,'armor_shoulder_'+side,[armX,14.5,-2.4],[.6,.6,.5],'gold');}
 }
 // Broad helmet, protruding muzzle, pointed ears and slanted brows break the Steve silhouette.
 m.box('face','head',[0,17.9,-.3],[4.7,4.4,3.9],'skin');
 m.box('muzzle','head',[0,16.8,-2.1],[3.6,1.8,1.5],'skinDark');
 m.box('nose','head',[0,18,-2.6],[1.2,1.25,1.2],'skin',[16,0,0]);
 m.box('mouth','head',[0,16.5,-2.9],[2.4,.4,.25],'black');
 for(const s of [-1,1]){
  m.box('ear_'+s,'head',[s*2.7,18.3,.1],[1.8,1.15,1.9],'skinDark',[0,0,s*28]);
  m.box('eye_socket_'+s,'head',[s*1.35,18.7,-2.32],[1.6,.9,.3],'black');
  m.box('eye_'+s,'head',[s*1.35,18.55,-2.52],[.95,.4,.2],'glow');
  m.box('brow_'+s,'helmet',[s*1.3,19.15,-2.5],[2.2,.65,.65],'steel',[0,0,s*16]);
  m.box('cheek_guard_'+s,'helmet',[s*2.35,17.7,-.8],[.7,3.7,3.4],'team',[0,0,-s*8]);
  m.box('fang_'+s,'head',[s*.95,16.75,-3.02],[.4,.9,.4],'edge',[0,0,s*12]);
 }
 m.box('helmet_dome','helmet',[0,20,.1],[5.5,2.1,4.7],'team');
 m.box('helmet_ridge','helmet',[0,20.7,-.1],[1,1.4,5],'steel');
 m.box('helmet_brow_band','helmet',[0,19.45,-2.35],[5.4,.6,.65],'gold');
 m.box('helmet_back','helmet',[0,18.7,2],[5.3,3.2,.8],'teamDark');
 for(let i=0;i<3;i++)m.box('plume_'+i,'helmet',[0,22+i*.75,.1+i*.65],[1.1,2.6-i*.45,1.8],'teamLight',[-18-i*10,0,0]);
 m.box('plume_mount','helmet',[0,21.4,0],[1.8,.7,2.4],'goldDark');
 if(!ranged){
  m.bone('weapon_socket','forearm_right',[-4.5,8.7,-.8]);m.bone('shield_socket','forearm_left',[4.5,9,-1]);
  m.box('sword_grip','weapon_socket',[-4.5,9,-1.3],[.8,3,.8],'leather',[0,0,-24]);
  m.box('sword_guard','weapon_socket',[-5.05,10.2,-1.3],[3.8,.65,1.1],'gold',[0,0,-24]);
  m.box('sword_blade','weapon_socket',[-6.4,13.3,-1.3],[1.8,6.3,.55],'steel',[0,0,-24]);
  m.box('sword_edge','weapon_socket',[-7.25,13.7,-1.3],[.45,5.9,.62],'edge',[0,0,-24]);
  m.box('sword_tip','weapon_socket',[-7.9,16.4,-1.3],[1.3,1.6,.55],'edge',[0,0,21]);
  m.box('shield_body','shield_socket',[6.1,8.6,-2.8],[5.1,8.3,1.2],'team');
  m.box('shield_lower','shield_socket',[6.1,4.6,-2.8],[3.8,1.3,1.2],'teamDark');
  for(const s of [-1,1])m.box('shield_rail_'+s,'shield_socket',[6.1+s*2.5,8.6,-3.4],[.65,8.6,.8],'gold');
  for(const y of [4.6,12.7])m.box('shield_cross_'+y,'shield_socket',[6.1,y,-3.45],[5.4,.65,.8],'gold');
  m.box('shield_spine','shield_socket',[6.1,8.7,-3.6],[.65,7.7,.6],'steel');
  m.box('shield_boss','shield_socket',[6.1,9.2,-4],[1.8,1.8,.65],'gold',[0,0,45]);
  for(const s of [-1,1])for(const y of [5.2,12])m.box('shield_rivet_'+s+y,'shield_socket',[6.1+s*1.8,y,-3.5],[.45,.45,.4],'edge');
 } else {
  m.bone('weapon','forearm_left',[3.5,9,-1]);m.bone('weapon_string','weapon',[5.1,10,-2]);m.bone('string_top','weapon_string',[5.1,16,-1.6]);m.bone('string_bottom','weapon_string',[5.1,4,-1.6]);m.bone('projectile_origin','weapon',[5.1,10,-4.5]);
  const path=[[5.1,3.9,-1.6],[5.1,5.5,-3.4],[5.1,8,-4.2],[5.1,10,-3.6],[5.1,12,-4.2],[5.1,14.5,-3.4],[5.1,16.1,-1.6]];
  for(let i=1;i<path.length;i++)m.rod('bow_limb_'+i,'weapon',path[i-1],path[i],.8,.85,i===3||i===4?'leather':'wood');
  for(const y of [5,8,12,15])m.box('bow_binding_'+y,'weapon',[5.1,y,y===5||y===15?-2.9:-4.1],[1.15,.65,1.2],'gold');
  m.rod('bow_string_top','string_top',[5.1,16,-1.6],[5.1,10,-1.6],.14,.14,'edge');
  m.rod('bow_string_bottom','string_bottom',[5.1,10,-1.6],[5.1,4,-1.6],.14,.14,'edge');
  m.box('quiver','body',[-1.4,11.8,3.1],[2.2,5.8,2.2],'leather',[0,0,-15]);
  m.box('quiver_rim','body',[-.8,14.5,3.1],[2.5,.65,2.5],'goldDark',[0,0,-15]);
  for(let i=0;i<3;i++){m.box('arrow_shaft_'+i,'body',[-1.5+i*.7,15.7,3.15],[.22,3.7,.22],'wood',[0,0,-15]);m.box('arrow_fletch_'+i,'body',[-1.3+i*.7,17,3.15],[.7,1,.3],'edge',[0,0,-15]);}
  m.box('cloak','body',[0,10.5,2.1],[4,6.5,.4],'cloth',[-7,0,0]);
 }
 // Layered chest and helmet accents keep broad surfaces from reading as a vanilla skin.
 for(const s of [-1,1]){
  m.box('chest_flank_'+s,'armor_chest',[s*(w/2-.45),12.5,-2.15],[1.2,3.3,.5],'steel',[0,0,-s*12]);
  m.box('helmet_temple_'+s,'helmet',[s*2.1,20,-2.4],[1.1,1.5,.45],'teamLight',[0,0,s*10]);
  m.box('temple_stud_'+s,'helmet',[s*2.1,19.9,-2.7],[.4,.4,.25],'gold');
  m.box('chest_stud_'+s,'armor_chest',[s*1.5,14.3,-2.25],[.4,.4,.35],'gold');
 }
 for(let i=0;i<2;i++)m.box('abdominal_lamella_'+i,'armor_chest',[0,10.7-i*.85,-2.18],[w-.5,.65,.5],i?'goldDark':'steel');
 // Compress the lower legs and move the complete rig consistently with the geometry.
 const remap=y=>y<8?y*.8:y-1.6;
 for(const e of m.elements){e.from[1]=remap(e.from[1]);e.to[1]=remap(e.to[1]);e.origin[1]=remap(e.origin[1]);}
 for(const b of Object.values(m.bones))b.origin[1]=remap(b.origin[1]);
 return m;
}

function siege(){
 const m=new Model('lane_siege');m.bone('root');m.bone('body','root',[0,6,0]);m.bone('turret','body',[0,10,0]);m.bone('barrel','turret',[0,11,-3]);m.bone('crystal','turret',[0,15,1]);m.bone('projectile_origin','barrel',[0,11,-17]);
 m.box('chassis','body',[0,5.5,0],[12,2.4,16],'dark');
 m.box('deck','body',[0,7.2,0],[12.6,1,15.5],'goldDark');
 m.box('engine','body',[0,9.2,3],[9.5,4,8],'teamDark');
 m.box('front_armor','body',[0,7.8,-6],[11.5,2.8,2.5],'team',[18,0,0]);
 m.box('bumper','body',[0,5.7,-8],[12.6,1.5,1.6],'steel');
 for(const s of [-1,1]){
  m.box('side_armor_'+s,'body',[s*5.5,9,1],[1.3,3.5,12.5],'team',[0,0,s*10]);
  m.box('side_trim_'+s,'body',[s*6,10.8,1],[1,.6,12.6],'gold');
  for(let i=0;i<3;i++)m.box('side_plate_'+s+i,'body',[s*6.4,8.5,-3+i*3.5],[.6,2.4,2.3],'steel');
  for(const [end,z] of [['front',-4.8],['back',5.3]]){
   const b='wheel_'+end+'_'+(s<0?'right':'left');m.bone(b,'body',[s*7,4.2,z]);
   // Three crossing cuboids form an octagonal wheel, with separately readable spokes and hub.
   for(const a of [0,45])m.box(b+'_tire_'+a,b,[s*7,4.2,z],[2.6,5.8,5.8],'dark',[a,0,0]);
   for(const a of [0,45])m.box(b+'_rim_'+a,b,[s*8.35,4.2,z],[.45,5.2,5.2],'steel',[a,0,0]);
   for(const a of [0,45,90,135])m.box(b+'_spoke_'+a,b,[s*8.65,4.2,z],[.5,.75,6.2],'goldDark',[a,0,0]);
   m.box(b+'_hub',b,[s*9,4.2,z],[1,2.3,2.3],'team');
   m.box(b+'_axle',b,[s*9.55,4.2,z],[.35,1,1],'gold');
  }
 }
 m.box('turret_base','turret',[0,9.4,0],[8,1.8,8],'steel');
 m.box('turret_housing','turret',[0,11.4,0],[7.4,3.6,7.4],'team');
 m.box('breech','barrel',[0,11,-2.6],[5.8,4.8,5.5],'dark');
 for(let i=0;i<8;i++){const a=i*Math.PI/4;m.box('barrel_facet_'+i,'barrel',[2.25*Math.sin(a),11+2.25*Math.cos(a),-9.3],[2,.8,11.5],i<3?'steel':'dark',[0,0,-i*45]);}
 for(const z of [-5,-10.4,-14.8]){
  for(let i=0;i<8;i++){const a=i*Math.PI/4;m.box('barrel_band_'+z+'_'+i,'barrel',[2.8*Math.sin(a),11+2.8*Math.cos(a),z],[2.5,.55,1],'goldDark',[0,0,-i*45]);}
 }
 m.box('muzzle_dark','barrel',[0,11,-12.8],[3.3,3.3,.6],'black');
 m.box('muzzle_core','barrel',[0,11,-13.2],[1.5,1.5,.4],'crystal');
 m.box('crystal_mount','turret',[0,14,1],[4.5,1,4.5],'gold');
 m.box('power_crystal','crystal',[0,16.4,1],[2.6,4.8,2.6],'crystal',[0,0,16]);
 m.box('crystal_tip','crystal',[-.6,19,1],[1.6,1.8,1.6],'glow',[0,0,35]);
 for(const s of [-1,1]){m.box('crystal_clamp_'+s,'turret',[s*1.7,15,1],[.75,2.6,3.4],'gold',[0,0,s*15]);m.box('rear_fin_'+s,'body',[s*3.4,12.5,6],[1.4,5,3],'team',[15,0,-s*12]);}
 m.box('rear_banner','body',[0,6.5,8.4],[5,4,.5],'cloth');m.box('rear_crest','body',[0,6.8,8.75],[1.7,1.7,.3],'gold',[0,0,45]);
 return m;
}
export const createModels=()=>[infantry('warrior'),infantry('ranged'),siege()];
