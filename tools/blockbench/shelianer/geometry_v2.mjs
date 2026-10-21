import {Model,sub,cross,dot} from './core.mjs';

// All coordinates authored in world bind space; forward -Z, feet Y=0.
// Closed convex faceted armor with a raised ridge, not flat decoration.
function plate(m,name,b,outline,depth,mat='metal',ridge=.15){
 const n=outline.length,c=outline.reduce((a,p)=>a.map((v,i)=>v+p[i]/n),[0,0,0]);
 const v=[...outline,...outline.map(p=>[p[0],p[1],p[2]+depth]),[c[0],c[1],c[2]-ridge],[c[0],c[1],c[2]+depth]];
 const faces=[];for(let i=0;i<n;i++){const j=(i+1)%n;faces.push([2*n,i,j],[2*n+1,n+j,n+i],[i,n+i,n+j,j]);}
 return m.mesh(name,b,v,faces,mat);
}
function seam(m,n,b,points,r=.055,mat='trim'){for(let i=1;i<points.length;i++)m.rod(n+'_'+i,b,points[i-1],points[i],r,mat,5);}
function straps(m,n,b,rows,mat='leather'){return m.loft(n,b,rows,mat,10,.85);}
function cloth(m,n,b,rows,mat='cloth'){
 // Each row is [leftX,rightX,y,z]; folded cross-section with closed back.
 const v=[];for(const [l,r,y,z] of rows)for(let i=0;i<5;i++)v.push([l+(r-l)*i/4,y+(i===1?.08:0),z+Math.sin(i*Math.PI)*.1+(i%2?-.13:.12)]);
 const count=v.length;v.push(...v.map(p=>[p[0],p[1],p[2]+.12]));const f=[];
 for(let j=0;j<rows.length-1;j++)for(let i=0;i<4;i++){const a=j*5+i;f.push([a,a+5,a+6,a+1],[a+count,a+count+1,a+count+6,a+count+5]);}
 const edge=[0,1,2,3,4,...Array.from({length:rows.length-1},(_,i)=>(i+2)*5-1),...Array.from({length:4},(_,i)=>(rows.length-1)*5+3-i),...Array.from({length:rows.length-2},(_,i)=>(rows.length-2-i)*5)];
 for(let i=0;i<edge.length;i++){const a=edge[i],c=edge[(i+1)%edge.length];f.push([a,c,c+count,a+count]);}
 return m.mesh(n,b,v,f,mat);
}
function blade(m,b,n,x,y,z,s=1,hand=1){
 // The palm wraps around the middle of the grip. Curved cutting edge faces out.
 const P=(dx,dy,dz=0)=>[x+hand*dx*s,y+dy*s,z+dz*s];
 m.rod(n+'_grip',b,P(0,.72),P(0,-1.05),.22*s,'leather',8);
 for(let i=0;i<6;i++)m.rod(n+'_grip_wrap_'+i,b,P(-.23,.5-i*.23,-.08),P(.23,.38-i*.23,-.08),.055*s,'trim',5);
 m.loft(n+'_pommel',b,[[x,y+.64*s,z,.28*s,.28*s],[x,y+.95*s,z,.14*s,.18*s]],'metal');
 seam(m,n+'_guard',b,[P(-.82,-.75),P(-.35,-1.05),P(.4,-1.05),P(.93,-.65)],.13*s,'trim');
 const edge=[P(-.38,-1.05),P(.58,-1.12),P(.92,-2.4),P(1.8,-4.2),P(2.65,-5.8),P(1.25,-4.65),P(.05,-3.1),P(-.5,-1.9)];
 plate(m,n+'_blade_body',b,edge,.19*s,'blade',.13*s);
 plate(m,n+'_cutting_edge',b,[P(.61,-1.22,-.16),P(.93,-2.42,-.16),P(1.8,-4.2,-.16),P(2.65,-5.8,-.16),P(1.58,-4.08,-.17),P(.62,-2.35,-.17)],.04*s,'magic',.025);
 seam(m,n+'_blade_spine',b,[P(-.45,-1.85,-.08),P(.08,-3.08,-.08),P(1.25,-4.62,-.08)],.055*s,'metalEdge');
 m.crystal(n+'_crystal',b,P(0,-.84,-.38),P(0,-1.65,-.38),.5*s,.3*s);
}
function throwing(m,b,n,x,y,z){
 m.rod(n+'_handle',b,[x,y,z],[x,y-.8,z],.13,'leather');
 seam(m,n+'_guard',b,[[x-.32,y-.8,z],[x+.32,y-.8,z]],.065,'trim');
 plate(m,n,b,[[x-.26,y-.85,z],[x+.26,y-.85,z],[x+.29,y-1.65,z],[x,y-2.9,z],[x-.29,y-1.65,z]],.13,'metalEdge',.08);
 m.crystal(n+'_jewel',b,[x,y-.35,z-.14],[x,y-.95,z-.14],.3,.17);
}
function rig(m){
 m.bone('root',null);m.bone('body','root',[0,15,0]);m.bone('torso','body',[0,17,0]);m.bone('head','torso',[0,24,0]);
 for(const [side,s] of [['left',-1],['right',1]]){
 m.bone('upper_arm_'+side,'torso',[s*4.2,23,0]);m.bone('forearm_'+side,'upper_arm_'+side,[s*4.65,19,0]);m.bone('hand_'+side,'forearm_'+side,[s*4.85,15.6,-.15]);m.bone('dagger_'+side,'hand_'+side,[s*5.1,14.8,-.55]);
 m.bone('leg_'+side,'body',[s*1.45,15,0]);m.bone('shin_'+side,'leg_'+side,[s*1.5,8,0]);m.bone('foot_'+side,'shin_'+side,[s*1.5,2,0]);m.bone('tabard_'+side,'body',[s*1.7,15.5,-1.5]);
 m.bone('weapon_socket_'+side,'dagger_'+side,[s*5.1,14.8,-.55]);m.bone('shoulder_armor_'+side,'upper_arm_'+side,[s*4.2,23,0]);
 }
 for(let i=0;i<7;i++)m.bone('cape_'+i,'torso',[(i-3)*.77,23,1.9]);
 m.bone('flask_socket','body',[-2.5,15,1]);m.bone('q_throw_origin','hand_right',[5.1,14.8,-.8]);m.bone('poison_flask_origin','hand_left',[-5.1,14.8,-.8]);
}
export function character(){
 const m=new Model('shelianer');rig(m);
 // Sculpted rib cage: convex front/back, broad clavicles and a narrow waist.
 m.loft('torso_base','torso',[[0,16,0,2.12,1.4],[0,18.3,.1,2.05,1.48],[0,20.6,.1,2.95,1.88],[0,22.4,.05,3.45,1.87],[0,23.7,.15,2.55,1.45],[0,24,.2,1.25,1]],'undercloth',12);
 m.loft('pelvis_underlayer','body',[[0,13.8,.1,2.4,1.35],[0,15.8,.15,2.6,1.6],[0,17,0,2.2,1.45]],'undercloth',12);
 m.loft('waist_wrap','torso',[[0,16.1,0,2.22,1.53],[0,17.4,0,2.19,1.56],[0,18.2,.1,2.12,1.51]],'cloth',12);
 for(const [side,s] of [['left',-1],['right',1]]){
 const P=(x,y,z)=>[s*x,y,z],arm='upper_arm_'+side,fore='forearm_'+side,hand='hand_'+side,leg='leg_'+side,shin='shin_'+side,foot='foot_'+side;
 plate(m,'chest_underlayer_'+side,'torso',[P(.12,22.9,-1.67),P(2.7,23.25,-1.48),P(3.14,21.8,-1.48),P(2.45,20.2,-1.77),P(.15,20.65,-1.98)],.38,'leather',.3);
 plate(m,'chest_armor_'+side,'torso',[P(.35,22.75,-1.91),P(2.5,23.1,-1.73),P(2.92,22,-1.76),P(2.1,21.2,-2.14),P(.32,21.4,-2.25)],.22,'metal',.19);
 seam(m,'chest_border_'+side,'torso',[P(.35,22.75,-1.95),P(2.5,23.1,-1.77),P(2.92,22,-1.8),P(2.1,21.2,-2.18)],.05);
 for(let j=0;j<3;j++)plate(m,'rib_leather_'+side+j,'torso',[P(.12,20.5-j*.9,-1.86),P(2.55-j*.18,20.6-j*.9,-1.55),P(2.32-j*.2,19.72-j*.9,-1.63),P(.2,19.85-j*.9,-1.82)],.22,j%2?'leather':'metal',.13);
 // Upper arm: taper, biceps, sleeve fold, layered protection and elbow cap.
 m.loft('upper_arm_anatomy_'+side,arm,[[s*4.65,19,0,.67,.75],[s*4.55,20.4,.04,.94,1.04],[s*4.35,22,.05,1.06,1.08],[s*4.05,23.2,.12,1.05,.97]],'undercloth',10);
 m.loft('sleeve_cuff_'+side,arm,[[s*4.6,19.6,0,.79,.85],[s*4.57,20,0,.89,.92]],'cloth',10);
 for(let j=0;j<2;j++)straps(m,'bicep_strap_'+side+j,arm,[[s*4.45,20.6+j*1.15,.04,.95,1.07],[s*4.43,20.86+j*1.15,.04,.96,1.08]]);
 m.loft('elbow_joint_'+side,fore,[[s*4.65,18.45,0,.58,.65],[s*4.65,19,0,.75,.81],[s*4.65,19.45,0,.61,.69]],'undercloth',10);
 plate(m,'elbow_guard_'+side,fore,[P(4.15,19.55,.6),P(5.1,19.5,.6),P(5.37,18.95,.62),P(4.6,18.4,.58)],.38,'metal',.12);
 m.loft('forearm_base_'+side,fore,[[s*4.86,15.55,-.15,.55,.65],[s*4.83,16.6,-.09,.84,.88],[s*4.7,18.3,0,.95,.94],[s*4.65,18.8,0,.72,.74]],'leather',10);
 plate(m,'bracer_main_'+side,fore,[P(4,18.4,-.72),P(5.45,18.6,-.58),P(5.8,17.6,-.56),P(5.28,15.8,-.73),P(4.42,15.7,-.84),P(3.93,17,-.87)],.35,'metal',.3);
 seam(m,'bracer_trim_'+side,fore,[P(4,18.4,-.77),P(5.45,18.6,-.63),P(5.8,17.6,-.61)],.065);
 for(let j=0;j<2;j++)straps(m,'bracer_strap_'+side+j,fore,[[s*4.8,16+j*1.55,-.05,.88,.96],[s*4.8,16.27+j*1.55,-.05,.9,.98]]);
 for(let j=0;j<2;j++)plate(m,'bracer_lamella_'+side+j,fore,[P(4.25,17.7-j*.7,-1.02),P(5.4,17.9-j*.7,-.96),P(5.35,17.35-j*.7,-1.03),P(4.4,17.15-j*.7,-1.06)],.15,'metalEdge',.08);
 m.loft('glove_palm_'+side,hand,[[s*4.95,14.05,-.25,.5,.5],[s*4.94,14.8,-.25,.69,.58],[s*4.85,15.6,-.15,.5,.58]],'leather',10);
 for(let j=0;j<4;j++){
 const xx=4.44+j*.3;m.rod('finger_proximal_'+side+j,hand,P(xx,14.6,-.67),P(xx,14.04,-.82),.16,'leather',6);m.rod('finger_curl_'+side+j,hand,P(xx,14.04,-.82),P(xx,13.94,-.32),.155,'leather',6);m.loft('knuckle_'+side+j,hand,[[s*xx,14.46,-.79,.15,.13],[s*xx,14.69,-.79,.15,.13]],'metalEdge',6);
 }
 m.rod('thumb_'+side,hand,P(4.34,15,-.2),P(4.4,14.45,-.68),.22,'leather',7);
 // Thick overlapping shoulder shells, front ridge and a swept rear tip.
 const sh='shoulder_armor_'+side;
 m.loft('shoulder_base_'+side,sh,[[s*4.12,22.1,.14,.99,1.1],[s*4.14,23.3,.2,1.28,1.4],[s*3.97,23.9,.2,.72,1.05]],'leather',10);
 for(let j=0;j<3;j++){
 const x=4.12+j*.19,y=24.05-j*.8;
 plate(m,'shoulder_plate_'+side+'_'+j,sh,[P(x-1.05,y,-.78),P(x-.3,y+.42,-1.22),P(x+.68,y+.18,-1.18),P(x+1.42,y-.8,-.52),P(x+.7,y-1.02,-.12),P(x-.92,y-.66,-.32)],1.9-j*.18,'metal',.23);
 seam(m,'shoulder_trim_'+side+j,sh,[P(x-1.05,y,-.82),P(x-.3,y+.42,-1.26),P(x+.68,y+.18,-1.22),P(x+1.42,y-.8,-.56)],.068);
 plate(m,'shoulder_rear_tip_'+side+j,sh,[P(x+.45,y-.4,1.1),P(x+1.55,y-.93,1.8),P(x+.45,y-1.1,1.05)],.2,'metalEdge',.1);
 }
 // Tapered thigh, knee, calf and asymmetrical straps.
 m.loft('thigh_base_'+side,leg,[[s*1.52,8,0,.76,.83],[s*1.65,10.5,.15,1.08,1.18],[s*1.5,13.2,.15,1.26,1.32],[s*1.4,15,0,1.13,1.26]],'undercloth',12);
 m.loft('thigh_cloth_'+side,leg,[[s*1.6,10.9,.14,1.09,1.2],[s*1.55,12.25,.12,1.3,1.4],[s*1.42,14.5,.05,1.24,1.36]],'charcoal',10);
 for(let j=0;j<(s===-1?2:3);j++)straps(m,'thigh_strap_'+side+j,leg,[[s*1.6,10.3+j*1.1,.05,1.1,1.25],[s*1.6,10.57+j*1.1,.05,1.12,1.27]]);
 plate(m,'thigh_side_plate_'+side,leg,[P(2.25,13.9,-.55),P(3.05,13.4,-.25),P(2.88,11.15,-.3),P(2.03,11.8,-.69)],.45,'metal',.1);
 m.loft('knee_joint_'+side,shin,[[s*1.5,7.45,0,.67,.71],[s*1.5,8,0,.77,.85],[s*1.5,8.5,0,.68,.77]],'undercloth',10);
 plate(m,'knee_guard_'+side,shin,[P(.9,8.65,-.69),P(1.48,9,-.95),P(2.17,8.6,-.72),P(2.1,7.75,-.81),P(1.48,7.15,-1),P(.9,7.75,-.83)],.35,'metal',.3);
 seam(m,'knee_rim_'+side,shin,[P(.9,8.65,-.72),P(1.48,9,-.99),P(2.17,8.6,-.75)],.065);
 m.loft('calf_base_'+side,shin,[[s*1.5,2,.05,.6,.69],[s*1.5,3.5,.18,.68,.85],[s*1.5,5.65,.35,.92,1.14],[s*1.5,7.5,.1,.72,.85]],'leather',12);
 plate(m,'shin_guard_'+side,shin,[P(.85,6.95,-.67),P(1.5,7.4,-.9),P(2.17,6.95,-.66),P(2.07,3.1,-.62),P(1.5,2.55,-.91),P(.99,3.3,-.73)],.3,'metal',.2);
 for(let j=0;j<2;j++)straps(m,'shin_strap_'+side+j,shin,[[s*1.5,3.35+j*2.4,.06,.89,1.06],[s*1.5,3.64+j*2.4,.06,.9,1.07]]);
 // Boot has outsole, a projecting toe, heel, ankle collar and an instep.
 m.loft('boot_sole_'+side,foot,[[s*1.5,.1,-.56,.91,1.63],[s*1.5,.39,-.56,1.02,1.73],[s*1.5,.58,-.55,.99,1.67]],'rubber',10);
 m.loft('boot_upper_'+side,foot,[[s*1.5,.5,-.6,.95,1.58],[s*1.5,1,-.62,.96,1.58],[s*1.5,1.6,-.27,.81,1.05],[s*1.5,2.6,.07,.68,.75]],'leather',10);
 m.loft('boot_heel_'+side,foot,[[s*1.5,.35,.65,.73,.47],[s*1.5,1.38,.59,.76,.56]],'rubber',8);
 plate(m,'boot_toe_cap_'+side,foot,[P(.76,1.08,-1.8),P(1.5,1.47,-1.9),P(2.24,1.08,-1.8),P(2.15,.55,-2.07),P(.85,.55,-2.07)],.55,'metal',.1);
 straps(m,'boot_ankle_collar_'+side,foot,[[s*1.5,2.05,.04,.79,.88],[s*1.5,2.48,.04,.8,.89]]);
 seam(m,'boot_instep_strap_'+side,foot,[P(.76,1.12,-.9),P(1.5,1.58,-.95),P(2.24,1.12,-.9)],.14,'leather');
 m.box('boot_buckle_'+side,foot,P(2.1,1.35,-.88),[.33,.35,.16],'trim');
 blade(m,'weapon_socket_'+side,'dagger_'+side,s*5.13,14.6,-.55,1.28,s);
 }
 // Two belts, hardware, pouches and six independent knife sockets.
 straps(m,'belt_main','body',[[0,15.3,0,2.65,1.76],[0,16.05,0,2.6,1.74]]);
 straps(m,'belt_secondary','body',[[0,16.2,.04,2.45,1.63],[0,16.48,.04,2.43,1.63]],'trimDark');
 plate(m,'belt_buckle','body',[[-.66,16.04,-1.84],[.66,16.04,-1.84],[.6,15.26,-1.86],[-.6,15.26,-1.86]],.2,'trim',.02);
 m.box('belt_buckle_inset','body',[0,15.64,-1.9],[.88,.44,.13],'leather');
 seam(m,'chest_strap_01','torso',[[-2.53,23.38,-1.64],[-1.5,21.25,-2.47],[.4,19.5,-2.12],[2.2,17.18,-1.44]],.24,'leather');
 seam(m,'chest_strap_02','torso',[[2.77,23.32,-1.58],[2.1,21.2,-2.21],[1.98,19.65,-1.83]],.18,'leather');
 for(let i=0;i<4;i++)m.box('harness_rivet_'+i,'torso',[-1.5+i*.66,21.15-i*.68,-2.58+i*.14],[.14,.14,.13],'trim');
 for(const [side,s] of [['l',-1],['r',1]]){
 m.loft('pouch_'+side,'body',[[s*2.74,13.6,.8,.63,.63],[s*2.82,14.1,.8,.74,.76],[s*2.75,15.45,.74,.64,.69]],'leather',8,.65);
 plate(m,'pouch_flap_'+side,'body',[[s*2.18,15.49,-.02],[s*3.35,15.49,.1],[s*3.33,14.65,.04],[s*2.75,14.35,-.08],[s*2.18,14.68,-.09]],.18,'trimDark',.07);
 m.box('pouch_clasp_'+side,'body',[s*2.76,14.72,-.15],[.27,.36,.16],'trim');
 for(let j=0;j<3;j++){
 const x=s*(1.32+j*.69),y=15.23-j*.12,z=-2.07+j*.24,b='throwing_knife_'+side+'_0'+(j+1);
 m.bone(b,'body',[x,y,z]);m.bone(b+'_socket',b,[x,y,z]);
 plate(m,b+'_sheath','body',[[x-.31,y-.55,z+.16],[x+.31,y-.55,z+.16],[x+.33,y-1.8,z+.16],[x,y-2.52,z+.16],[x-.32,y-1.8,z+.16]],.26,'leather',.04);
 throwing(m,b,b,x,y,z-.16);
 }
 }
 m.loft('poison_flask_belt','flask_socket',[[-2.95,13.2,1.5,.33,.36],[-2.95,13.6,1.5,.51,.52],[-2.95,14.32,1.5,.43,.44],[-2.95,14.65,1.5,.17,.18]],'poison',8);m.box('flask_stopper','flask_socket',[-2.95,14.77,1.5],[.38,.26,.38],'leather');
 buildHead(m);buildCloak(m);artFinish(m);orient(m);return m;
}

function buildHead(m){
 m.bone('face','head',[0,26.1,-.1]);
 m.loft('male_face','face',[[0,24.3,-.1,.87,.87],[0,25,-.1,1.25,1.07],[0,26.3,-.05,1.57,1.22],[0,27.5,0,1.5,1.18],[0,28.3,.12,1.1,1.03]],'skinShade',10);
 m.loft('neck','head',[[0,23.7,.14,.88,.8],[0,25,.14,.88,.85]],'skinShade');
 plate(m,'mask','face',[[-1.41,26,-1.19],[-.43,26.2,-1.43],[0,26.45,-1.64],[.43,26.2,-1.43],[1.41,26,-1.19],[1.16,24.94,-1.13],[.52,24.43,-1.25],[-.52,24.43,-1.25],[-1.16,24.94,-1.13]],.22,'mask',.12);
 for(const s of [-1,1]){
 const P=(x,y,z)=>[s*x,y,z];
 plate(m,'eye_shadow_'+s,'face',[P(.15,26.92,-1.25),P(1.3,27.03,-1.05),P(1.26,26.42,-1.17),P(.19,26.39,-1.38)],.1,'black',.01);
 plate(m,'eye_'+s,'face',[P(.27,26.74,-1.43),P(1.18,26.86,-1.24),P(1.11,26.59,-1.29),P(.33,26.56,-1.44)],.025,'eyeWhite',.005);
 m.box('iris_'+s,'face',P(.66,26.7,-1.48),[.22,.23,.04],'eye');m.box('pupil_'+s,'face',P(.66,26.7,-1.51),[.075,.22,.03],'black');
 seam(m,'angular_brow_'+s,'face',[P(.19,26.96,-1.45),P(.91,27.13,-1.36),P(1.28,27.09,-1.17)],.105,'hair');
 seam(m,'mask_seam_'+s,'face',[P(.13,25.7,-1.61),P(.85,25.48,-1.47),P(1.23,25.57,-1.33)],.023,'clothLight');
 }
 for(let i=0;i<5;i++){const x=(i-2)*.48;m.ribbon('hair_fringe_'+i,'face',[[x,28.1,-1.08],[x+.21,27.55,-1.34],[x-.06,26.98+(i%2)*.2,-1.39]],[.65,.48,.015],.18,'hair');}
 // Seven distinct hood sections; deep side walls and an overhanging brow.
 for(const [n,o] of Object.entries({hood_top:[0,28.3,0],hood_front_left:[-1.5,27.5,-1.25],hood_front_right:[1.5,27.5,-1.25],hood_side_left:[-1.7,27,.2],hood_side_right:[1.7,27,.2],hood_back_upper:[0,28,1.2],hood_back_lower:[0,25.5,1.6]}))m.bone(n,'head',o);
 plate(m,'hood_top','hood_top',[[-1.9,28.1,-1.1],[-1.18,29.07,-1.3],[.1,29.88,-.68],[1.35,29, -.85],[1.94,28.07,-1.04],[.1,28.64,-1.9]],2.15,'hood',.13);
 for(const [side,s] of [['left',-1],['right',1]]){
 const P=(x,y,z)=>[s*x,y,z],front='hood_front_'+side,wall='hood_side_'+side;
 plate(m,front,front,[P(.12,29.14,-1.73),P(1.26,28.77,-1.56),P(1.97,27.13,-1.3),P(2.15,24.8,-.79),P(1.28,24.18,-.45),P(1.53,25.85,-1.6),P(1.39,27.35,-1.83)],.55,'hood',.08);
 plate(m,wall,wall,[P(1.8,28.15,-.97),P(2.12,27.03,-.88),P(2.25,24.5,-.29),P(1.75,24.01,.4),P(1.55,26.4,.42)],2.05,'hood',.1);
 seam(m,'hood_inner_binding_'+side,front,[P(.12,29.14,-1.79),P(1.39,27.35,-1.88),P(1.53,25.85,-1.65),P(1.28,24.18,-.51)],.045,'trimDark');
 }
 m.loft('hood_back_upper','hood_back_upper',[[0,26.8,1.24,1.95,.97],[0,28.2,1.07,1.85,1.04],[.12,29.49,.83,.6,.55]],'hood',10);
 m.loft('hood_back_lower','hood_back_lower',[[0,23.7,1.22,1.65,.7],[0,25,1.35,2.05,.95],[0,26.85,1.24,1.95,.97]],'hood',10);
 m.loft('scarf_cowl','torso',[[0,23.2,.15,1.67,1.14],[0,23.9,.15,1.78,1.29],[0,24.4,.22,1.27,1.07]],'cloth',12);
 cloth(m,'scarf_fold','torso',[[-1.75,1.25,24,-1.14],[-1.28,.8,23.3,-1.49],[-.8,.35,22.9,-1.46]],'clothLight');
}
function buildCloak(m){
 // Old cape bones remain as stable animation drivers, broad panels are children.
 const panels=[
 ['cloak_back_center','cape_3',[0,23.3,1.75],[[-1.65,1.65,23.4,1.85],[-1.85,1.85,18.5,2.15],[-1.92,1.92,11,2.8],[-1.48,1.56,5.1,3.12],[-.32,.18,3.7,3.2]],'hood'],
 ['cloak_back_left','cape_2',[-1.6,23,1.8],[[-2.7,-.55,23,1.7],[-3.05,-.7,17.5,2.18],[-3.92,-1.08,10.1,2.69],[-3.35,-1.46,6.5,3.04],[-2.95,-2.05,5.8,3.1]],'charcoal'],
 ['cloak_back_right','cape_4',[1.6,23,1.8],[[.55,2.7,23,1.72],[.7,3.06,17.2,2.17],[1.08,4.02,10.8,2.62],[1.43,3.52,7.2,2.99],[2.55,3.2,5.9,3.04]],'cloth'],
 ['cloak_side_left','cape_0',[-2.45,16,1.15],[[-2.82,-1.95,16.2,.5],[-3.58,-2.05,12.1,.55],[-4,-2.65,8,.87],[-3.57,-3.09,5.75,1.15]],'charcoal'],
 ['cloak_side_right','cape_6',[2.45,16,1.15],[[1.95,2.82,16.2,.5],[2.05,3.48,12.1,.55],[2.65,3.85,8.9,.87],[3.04,3.51,6.8,1.15]],'hood'],
 ['front_tabard_left','tabard_left',[-1.7,15.5,-1.5],[[-2.4,-.6,15.2,-1.72],[-2.8,-.76,12.1,-1.8],[-3.08,-1.42,8.4,-1.7],[-2.62,-1.69,6.8,-1.5]],'charcoal'],
 ['front_tabard_right','tabard_right',[1.7,15.5,-1.5],[[.6,2.4,15.2,-1.71],[.76,2.81,12,-1.84],[1.42,3.08,9.1,-1.74],[2.02,2.67,7.2,-1.65]],'hood'],
 ['front_tabard_center','body',[0,15.2,-1.55],[[-.72,.72,15.1,-1.81],[-.87,.8,12,-1.95],[-.51,.58,9.3,-1.97],[-.14,.35,8.6,-1.98]],'cloth']];
 for(const [n,parent,o,rows,mat] of panels){m.bone(n,parent,o);const mesh=cloth(m,n,n,rows,mat);const bottom=(rows.length-1)*5;for(let i=0;i<5;i++){mesh.vertices['v'+(bottom+i)][1]+=[.15,.7,-.25,.45,.1][i];mesh.vertices['v'+(bottom+i+rows.length*5)][1]+=[.15,.7,-.25,.45,.1][i];}}
 // Torn overlapping lower patches: broad wedges, not identical ribbons.
 cloth(m,'cloak_rear_patch_left','cloak_back_left',[[-3.45,-1.6,11.2,2.75],[-3.25,-1.55,8,3.01],[-2.45,-1.72,5.5,3.22]],'hood');
 cloth(m,'cloak_rear_patch_right','cloak_back_right',[[1.4,3.44,12.5,2.71],[1.5,3.2,9,3.02],[2.2,2.9,7,3.16]],'charcoal');
 // Thorned violet sigil follows the broad central panel; mostly dark purple.
 const b='cloak_back_center';
 for(const s of [-1,1]){
 for(let j=0;j<3;j++)plate(m,'sigil_wing_'+s+j,b,[[s*.13,20.4-j*.75,2.18+j*.085],[s*(1.48-j*.23),21.55-j*.68,2.17+j*.085],[s*(1.08-j*.17),20.45-j*.7,2.21+j*.085],[s*.23,19.55-j*.74,2.26+j*.085]],.075,'sigil',-.04);
 }
 plate(m,'sigil_spine',b,[[0,21.55,2.22],[.38,19.5,2.35],[0,17.7,2.52],[-.38,19.5,2.35]],.08,'sigil',-.06);
}
function artFinish(m){
 // Keep the openings between the legs readable: side cloth finishes above the
 // knees, while the central back tail retains the long triangular silhouette.
 for(const e of m.elements){
 if(e.name==='front_tabard_left'||e.name==='front_tabard_right')for(const v of Object.values(e.vertices)){if(v[1]<12)v[1]=12+(v[1]-12)*.65;}
 if(e.name==='cloak_side_left'||e.name==='cloak_side_right')for(const v of Object.values(e.vertices)){if(v[1]<12)v[1]=12+(v[1]-12)*.72;}
 if(e.name==='cloak_back_left'||e.name==='cloak_back_right'||e.name.startsWith('cloak_rear_patch'))for(const v of Object.values(e.vertices)){if(v[1]<12){v[0]*=1.08;v[2]+=(12-v[1])*.08;}}
 if(e.name.startsWith('eye_shadow_'))for(const v of Object.values(e.vertices))v[1]=26.73+(v[1]-26.73)*.68;
 if(e.name.startsWith('eye_')&&!e.name.startsWith('eye_shadow_'))for(const v of Object.values(e.vertices))v[1]=26.71+(v[1]-26.71)*.74;
 if(e.name.startsWith('iris_')||e.name.startsWith('pupil_'))for(const v of Object.values(e.vertices))v[1]=26.7+(v[1]-26.7)*.72;
 }
 m.loft('hair_under_hood','face',[[0,27.63,.13,1.45,1.15],[0,28.1,.17,1.23,1.12],[0,28.5,.23,.7,.83]],'hair',10);
 cloth(m,'scarf_center_overlap','torso',[[-.66,.67,24.04,-1.44],[-.45,.58,23.36,-1.6],[-.24,.32,23.02,-1.53]],'cloth');
 // Thorn-like rather than a filled chevron: leave dark negative space.
 const removed=new Set(m.elements.filter(e=>e.name.startsWith('sigil_')).map(e=>e.uuid));m.elements=m.elements.filter(e=>!removed.has(e.uuid));for(const b of Object.values(m.bones))b.children=b.children.filter(c=>!removed.has(c));
 const b='cloak_back_center';
 for(const s of [-1,1]){
 seam(m,'sigil_thorn_'+s,b,[[0,18.3,2.58],[s*.36,19.2,2.51],[s*.87,20.06,2.41],[s*1.43,21.22,2.32]],.105,'sigil');
 for(let i=0;i<3;i++)seam(m,'sigil_barb_'+s+i,b,[[s*.17,19.5+i*.52,2.51-i*.09],[s*(.76+i*.23),19.73+i*.55,2.48-i*.09],[s*(1.02+i*.21),20.16+i*.55,2.43-i*.09]],.085,'sigil');
 }
 plate(m,'sigil_center',b,[[0,21.5,2.37],[.2,20.27,2.46],[0,18.05,2.68],[-.2,20.27,2.46]],.08,'sigil',-.025);
}
// Standardize outward winding of closed elements; cloth remains double-sided
// in software preview, but exported geometry has consistent closed boundaries.
function orient(m){for(const e of m.elements){const vs=Object.values(e.vertices),c=vs.reduce((a,p)=>a.map((v,i)=>v+p[i]/vs.length),[0,0,0]);for(const f of Object.values(e.faces)){const p=f.vertices.map(k=>e.vertices[k]),n=cross(sub(p[1],p[0]),sub(p[2],p[0])),fc=p.reduce((a,q)=>a.map((v,i)=>v+q[i]/p.length),[0,0,0]);if(dot(n,sub(fc,c))<0)f.vertices.reverse();}}}
export function firstPerson(){const m=character();m.name='shelianer_first_person';const keep=new Set();function walk(b){keep.add(b.uuid);for(const c of b.children)typeof c==='object'?walk(c):keep.add(c);}for(const side of ['left','right'])walk(m.bones['upper_arm_'+side]);m.elements=m.elements.filter(e=>keep.has(e.uuid));m.outliner=['left','right'].map(s=>m.bones['upper_arm_'+s]);m.bones=Object.fromEntries(Object.entries(m.bones).filter(([,b])=>keep.has(b.uuid)));return m;}
export function weapon(){const m=new Model('shelianer_dagger');m.bone('dagger',null);blade(m,'dagger','dagger',0,4,0,1.28);orient(m);return m;}
