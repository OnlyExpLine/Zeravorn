// Art pass: human proportions, closed anatomical joints and layered armor.
export function refine(m){
 const head=m.elements.filter(e=>m.bones.head.children.includes(e.uuid));
 for(const e of head)for(const v of Object.values(e.vertices)){v[0]*=.84;v[1]=24+(v[1]-24)*.79;v[2]*=.9;}
 for(let i=0;i<5;i++){const x=(i-2)*.52;m.ribbon('fringe_'+i,'head',[[x,28.1,-1.38],[x+.2,27.35,-1.64],[x-.12,26.6+(i%2)*.35,-1.57]],[.72,.59,.025],.16,i%2?'hair':'hairLight');}
 // Replace the repetitive cylindrical pauldrons with overlapping pointed plates.
 const remove=m.elements.filter(e=>/^(pauldron_|shoulder_spike_|knee_plate_)/.test(e.name));const ids=new Set(remove.map(e=>e.uuid));m.elements=m.elements.filter(e=>!ids.has(e.uuid));for(const b of Object.values(m.bones))b.children=b.children.filter(c=>!ids.has(c));
 for(const [side,s] of [['left',-1],['right',1]]){
 const arm='upper_arm_'+side,fore='forearm_'+side,leg='leg_'+side,shin='shin_'+side;
 m.loft('deltoid_'+side,arm,[[s*4.2,21.8,0,.85,.9],[s*4.2,23,0,1.2,1.2],[s*3.7,24,0,.6,.7]],'navy',12);
 m.loft('elbow_joint_'+side,fore,[[s*4.65,18.35,0,.65,.67],[s*4.65,19,0,.85,.8],[s*4.65,19.6,0,.65,.67]],'navy',12);
 m.loft('knee_joint_'+side,shin,[[s*1.6,7.4,0,.65,.76],[s*1.6,8,0,.83,.94],[s*1.6,8.6,0,.65,.76]],'navy',12);
 for(let j=0;j<3;j++){
 const x=s*(4.1+j*.16),y=23.8-j*.8;
 m.mesh('pointed_pauldron_'+side+j,arm,[[x-s*1.1,y,-1],[x+s*.5,y+.3,-1.3],[x+s*1.7,y-.65,-.4],[x+s*.7,y-.9,1.05],[x-s*.9,y,1],[x,y+.5,0]],[[0,1,5],[1,2,5],[2,3,5],[3,4,5],[4,0,5],[4,3,2,0],[0,2,1]],['whiteLight','white','navy']);
 m.rod('bronze_pauldron_edge_'+side+j,arm,[x-s*1.1,y,-1],[x+s*.5,y+.3,-1.3],.09,'gold');m.rod('bronze_pauldron_tip_'+side+j,arm,[x+s*.5,y+.3,-1.3],[x+s*1.7,y-.65,-.4],.09,'gold');
 m.ribbon('thigh_lamella_'+side+j,leg,[[s*(2.1+j*.1),14.8-j*.85,-.75],[s*(2.6+j*.1),13.5-j*.85,-.9]],[1.2,.85],.28,['white','wood']);
 }
 m.loft('patella_'+side,shin,[[s*1.6,7.1,-.85,.35,.28],[s*1.6,8,-1,.8,.45],[s*1.6,8.7,-.9,.45,.25]],['white','whiteLight','gold']);
 m.loft('calf_shape_'+side,shin,[[s*1.5,3,.1,.68,.75],[s*1.55,5.6,.25,.94,1.1],[s*1.6,7.5,0,.78,.87]],'navy',12);
 m.loft('boot_toe_'+side,'foot_'+side,[[s*1.5,.25,-.7,1,1.7],[s*1.5,.9,-.85,1,1.8],[s*1.5,1.65,-.45,.85,1.2]],['white','wood','whiteLight']);
 for(let j=0;j<4;j++)m.rod('finger_'+side+j,'hand_'+side,[s*(4.45+j*.29),14.5,-.65],[s*(4.45+j*.29),13.85,-.46],.16,'wood');
 m.rod('thumb_'+side,'hand_'+side,[s*4.28,15,-.4],[s*4.25,14.25,-.85],.23,'wood');
 m.ribbon('chest_leather_'+side,'torso',[[s*2.4,23,-1.3],[s*1.6,21.5,-1.83],[s*.2,20.5,-1.93]],[1.05,1.5,.6],.25,['wood','white']);
 m.rod('chest_trim_'+side,'torso',[s*2.85,22.9,-1.57],[s*1.4,21,-2.06],.075,'gold');
 m.box('belt_pouch_'+side,'body',[s*2.65,15.2,.3],[1.2,1.7,1.2],'wood');m.box('pouch_flap_'+side,'body',[s*2.65,15.65,-.37],[1.25,.65,.18],'gold');
 for(let j=0;j<5;j++)m.ribbon('cloth_shred_'+side+j,'tabard_'+side,[[s*(1.2+j*.28),13-j*.5,-1.85],[s*(1.4+j*.4),6-j*.4,-1.98],[s*(1.5+j*.42),4.8-j*.35,-1.9]],[.48,.3,.01],.08,j%2?'cloth':'blue');
 }
 // Keep both blades handed, curving away from the legs.
 for(const e of m.elements.filter(e=>e.name.startsWith('dagger_left')))for(const v of Object.values(e.vertices))v[0]=-10.3-v[0];
}
