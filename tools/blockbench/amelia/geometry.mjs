import {Model,mix} from './core.mjs';
export const defaults={scale:1,headWidth:5.2,headHeight:5.5,torsoWidth:4.5,chestWidth:6.1,chestDepth:3.9,waistWidth:3.5,hipWidth:5.4,hairLength:18,hairVolume:1,skirtLength:13.6,staffLength:28,crystalSize:1};

function embroidery(m,name,bone,x,y,z,size,back=false){
 const r=size/2;
 for(let j=0;j<4;j++){
  const a=Math.PI*j/4,dx=Math.cos(a)*r,dy=Math.sin(a)*r;
  m.rod(name+'_ray_'+j,bone,[x-dx,y-dy,z],[x+dx,y+dy,z],.055,'whiteLight',4);
  for(const s of [-1,1])for(const k of [-1,1]){const px=x+s*dx*.65,py=y+s*dy*.65;m.rod(name+'_branch_'+j+'_'+s+'_'+k,bone,[px,py,z],[px-s*dx*.2-k*dy*.18,py-s*dy*.2+k*dx*.18,z],.04,'silver',4);}
 }
}
function trimPath(m,name,bone,path,material='gold',r=.055){for(let i=0;i<path.length-1;i++)m.rod(name+'_'+i,bone,path[i],path[i+1],r,material,5);}
function gem(m,name,bone,x,y,z,s=.6){m.crystal(name,bone,[x,y-s/2,z],[x,y+s/2,z],s*.65,s*.45);}

export function staff(m,parent,c=[6.2,0,-1.6],p=defaults){
 const [x,y,z]=c,ratio=p.staffLength/28, at=(a,b,d)=>[x+a,y+b*ratio,z+d];
 m.bone('staff_root',parent,at(0,12.4,0));
 for(const n of ['staff_grip','staff_lower','staff_upper','crystal_mount'])m.bone(n,'staff_root',at(0,n==='crystal_mount'?22:12.4,0));
 m.rod('staff_dark_shaft','staff_lower',at(0,.55,0),at(0,22,0),.19,'wood',8);
 m.rod('staff_grip_wrap','staff_grip',at(0,11.5,0),at(0,14.4,0),.25,'navy',8);
 for(const [i,h] of [.6,1.1,3.3,10.9,11.4,14.5,15,19.9,20.4,21.4].entries()){
  m.rod('staff_silver_band_'+i,'staff_lower',at(0,h,0),at(0,h+.23,0),.30,'silver',8);
  m.rod('staff_band_inlay_'+i,'staff_lower',at(0,h+.23,0),at(0,h+.32,0),.31,'gold',8);
 }
 for(const s of [-1,1]){
  trimPath(m,'crystal_silver_branch_'+s,'crystal_mount',[at(s*.2,21.5,0),at(s*.8,22.5,0),at(s*1.7,23.6,0)],'silver',.15);
  for(let j=0;j<2;j++){
   const name='crystal_'+(s<0?'left':'right')+'_'+String(j+1).padStart(2,'0');m.bone(name,'crystal_mount',at(s*(.7+j*.4),22.3+j*.7,0));
   m.crystal(name+'_facets',name,at(s*(.5+j*.55),22.1+j*.9,0),at(s*(1.7+j*.45),24.2+j*1.5,0),(.8-j*.17)*p.crystalSize,.52);
  }
 }
 m.bone('crystal_center','crystal_mount',at(0,22.3,0));m.crystal('staff_center_ice','crystal_center',at(0,22,0),at(0,28,0),1.45*p.crystalSize,1.05*p.crystalSize);
 m.bone('staff_vfx_origin','crystal_center',at(0,26.8,0));
 for(let i=0;i<3;i++)gem(m,'staff_socket_gem_'+i,'crystal_mount',x,y+(20.6+i*.55)*ratio,z-.34,.5);
}

function arm(m,side,parent,p,fp=false){
 const s=side==='left'?1:-1,x=s*3.65,shoulderY=21.05;
 m.bone('shoulder_'+side,parent,[x,shoulderY,0]);m.bone('upper_arm_'+side,'shoulder_'+side,[x,20.65,0]);
 m.bone('forearm_'+side,'upper_arm_'+side,[s*4.25,17.55,0]);m.bone('hand_'+side,'forearm_'+side,[s*4.65,14.45,-.3]);
 m.loft('bare_upper_arm_'+side,'upper_arm_'+side,[[s*4.18,17.4,0,.53,.60],[s*3.95,19.45,0,.62,.68],[x,20.95,0,.67,.67]],'skin',8,.6);
 m.loft('detached_sleeve_'+side,'forearm_'+side,[[s*4.7,14.15,-.22,.91,.82],[s*4.48,15.65,0,.91,.83],[s*4.25,17.6,0,.69,.68],[s*4.17,18.05,0,.72,.7]],['blue','cloth','blueLight','blue'],8,.45);
 for(let j=0;j<2;j++){
  const y=16.85-j*.32;
  for(let i=0;i<11;i++){const a=i*Math.PI*2/11;m.box('cuff_tuft_'+side+'_'+j+'_'+i,'forearm_'+side,[s*4.37+Math.cos(a)*.86,y+Math.sin(i*2.1)*.10,Math.sin(a)*.84],[.46,.52,.46],i%3===0?'white':'fur');}
 }
 m.loft('gloved_palm_'+side,'hand_'+side,[[s*4.73,12.9,-.38,.53,.34],[s*4.65,14.6,-.24,.60,.42]],'navy',8,.3);
 for(let i=0;i<4;i++){
  const fx=s*(4.30+i*.27),fy=12.65+(i===0||i===3?.22:0);
  m.ribbon('finger_'+side+'_'+i,'hand_'+side,[[fx,13.6,-.48],[fx+s*.12,fy,-.65],[fx+s*.06,fy-.32,-.41]],[.22,.21,.17],[.24,.23,.20],'navy');
  m.box('finger_silver_tip_'+side+'_'+i,'hand_'+side,[fx+s*.08,fy-.17,-.49],[.22,.32,.23],'silver');
 }
 m.ribbon('thumb_'+side,'hand_'+side,[[s*4.1,14,-.30],[s*3.91,13.55,-.55],[s*4.0,13.25,-.68]],[.29,.26,.22],.27,'navy');
 trimPath(m,'sleeve_front_gold_'+side,'forearm_'+side,[[s*4.35,17.95,-.67],[s*4.53,16,-.86],[s*4.7,14.2,-1.0]],'gold',.07);
 gem(m,'arm_diamond_'+side,'upper_arm_'+side,s*4.04,18.75,-.65,.95);
 gem(m,'sleeve_diamond_'+side,'forearm_'+side,s*4.66,14.9,-1.06,.83);
 m.bone('vfx_'+side+'_hand','hand_'+side,[s*4.65,13.5,-.7]);
}

function skirt(m,p){
 m.bone('clothing','pelvis',[0,15.2,0]);
 const names=['front_center','front_left','side_left','back_left','back_center','back_right','side_right','front_right'];
 // Separate articulated panels, open in front so the legs remain visible.
 const angles=[0,.95,1.62,2.37,Math.PI,3.91,4.66,5.33];
 for(let i=0;i<8;i++){
  const name='skirt_'+names[i],a=angles[i],front=i===0,top=15.25,bottom=front?4.7:15.25-p.skirtLength;
  m.bone(name,'clothing',[Math.sin(a)*2.3,top,-Math.cos(a)*1.35]);
  const rows=[],left=[],right=[],n=front?6:7;
  for(let j=0;j<n;j++){
   const t=j/(n-1),y=top+(bottom-top)*t,rx=2.6+3.15*t,rz=1.5+1.65*t;
   const width=front?.53+.10*t:(i===1||i===7)?.64+.76*t:1.15+1.00*t;
   rows.push([Math.sin(a)*rx,y,-Math.cos(a)*rz,width]);
  }
  const v=[],f=[];
  for(let j=0;j<n;j++)for(let k=0;k<3;k++){
   const [x,y,z,w]=rows[j],u=(k-1)*w;
   v.push([x+Math.cos(a)*u,y+(j===n-1?(k===1?-.5:.12):0),z+Math.sin(a)*u-(k===1?.11:0)*Math.cos(a)]);
  }
  // Cloth thickness, inside lining, front and back have distinct materials.
  const len=v.length;v.push(...v.map(q=>[q[0]-Math.sin(a)*.12,q[1],q[2]+Math.cos(a)*.12]));
  const mats=[];for(let j=0;j<n-1;j++)for(let k=0;k<2;k++){let o=j*3+k;f.push([o,o+1,o+4,o+3]);mats.push(front?'white':k===0?'blue':'cloth');f.push([o+len+3,o+len+4,o+len+1,o+len]);mats.push('navy');}
  for(let j=0;j<n-1;j++)for(const k of [0,2]){let o=j*3+k;f.push([o,o+3,o+3+len,o+len]);mats.push('gold');}
  f.push([0,1,1+len,len],[1,2,2+len,1+len]);mats.push('gold','gold');
  for(let k=0;k<2;k++){const o=(n-1)*3+k;f.push([o,o+len,o+len+1,o+1]);mats.push('gold');}
  m.mesh(name+'_layered_fabric',name,v,f,mats);
  for(const k of [0,2])trimPath(m,name+'_border_'+k,name,Array.from({length:n},(_,j)=>v[j*3+k]),'gold',.075);
  for(let j=1;j<n-1;j++){
   const [x,y,z,w]=rows[j];gem(m,name+'_ice_inlay_'+j,name,x,y,z-Math.cos(a)*.18,front?.50:.40);
  }
  // Embroidery follows panel plane, including side and back surfaces.
  for(const t of [.55,.80]){
   const y=top+(bottom-top)*t,rx=2.6+3.15*t,rz=1.5+1.65*t,c=[Math.sin(a)*(rx+.08),y,-Math.cos(a)*(rz+.10)];
   for(let ray=0;ray<4;ray++){
    const angle=ray*Math.PI/4,dx=Math.cos(angle)*(front?.40:.86),dy=Math.sin(angle)*(front?.40:.86),to=(u,h)=>[c[0]+Math.cos(a)*u,c[1]+h,c[2]+Math.sin(a)*u];
    m.rod(name+'_snow_'+t+'_'+ray,name,to(-dx,-dy),to(dx,dy),.06,'white',4);
    for(const s of [-1,1])for(const k of [-1,1])m.rod(name+'_snow_branch_'+t+'_'+ray+'_'+s+'_'+k,name,to(s*dx*.7,s*dy*.7),to(s*dx*.45-k*dy*.25,s*dy*.45+k*dx*.25),.045,'silver',4);
   }
  }
  // Stepped metallic hem and scattered icy applique, matching the concept's pixel edges.
  for(let j=0;j<5;j++){
   const u=(j-2)*(front?.21:.40),y=bottom+.28+(j%2)*.34,rx=2.6+3.15,rz=1.5+1.65;
   const x=Math.sin(a)*rx+Math.cos(a)*u,z=-Math.cos(a)*rz+Math.sin(a)*u;
   m.box(name+'_hem_tab_'+j,name,[x,y,z],[front?.20:.35,.38+(j%2)*.28,.18],'gold');
  }
  for(let j=0;j<7;j++){
   const t=.30+j*.085,rx=2.6+3.15*t,rz=1.5+1.65*t,u=Math.sin(j*1.7)*(front?.28:.52),x=Math.sin(a)*(rx+.1)+Math.cos(a)*u,z=-Math.cos(a)*(rz+.13)+Math.sin(a)*u;
   gem(m,name+'_applique_'+j,name,x,top+(bottom-top)*t,z,front?.36:.48);
  }
 }
 for(const s of [-1,1]){
  const b=s>0?'skirt_front_left':'skirt_front_right';
  m.ribbon('white_inner_robe_'+s,b,[[s*1.22,15.0,-1.68],[s*1.91,12,-1.91],[s*2.40,9,-2.09],[s*2.9,6.8,-2.36]],[.41,.49,.54,.13],[.17,.16,.14,.09],'whiteLight');
  trimPath(m,'inner_robe_gold_'+s,b,[[s*1.05,15,-1.8],[s*1.68,12,-2.02],[s*2.15,9,-2.20],[s*2.88,6.8,-2.41]],'gold',.055);
 }
 embroidery(m,'large_back_snowflake','skirt_back_center',0,4.55,3.22,3.6,true);
}

function hair(m,p){
 m.bone('hair_root','head',[0,27,0]);
 for(const [name,o] of Object.entries({hair_front_left:[1.1,27.4,-1.8],hair_front_right:[-1.1,27.4,-1.8],hair_side_left:[2.45,25,0],hair_side_right:[-2.45,25,0],hair_back_upper:[0,27,1.5],hair_strands_left:[2.7,21,1],hair_strands_right:[-2.7,21,1]}))m.bone(name,'hair_root',o);
 m.bone('hair_back_mid','hair_back_upper',[0,21,2]);m.bone('hair_back_lower','hair_back_mid',[0,15.5,2.3]);
 m.loft('silver_hair_cap','hair_root',[[0,25.9,.18,2.69,1.97],[0,27.3,.15,2.72,2.03],[0,28.0,.1,2.51,1.87],[0,28.22,.15,2.14,1.65]],['hair','hairLight','hairShade','hair'],12,.32);
 // Split curtain fringe: diagonally swept separate stepped locks.
 for(const s of [-1,1])for(let i=0;i<4;i++){
  const side=s===1?'left':'right',b='hair_front_'+side,w=.48+i*.08;
  const path=[[s*(.25+i*.34),28.05-i*.14,-1.77],[s*(.40+i*.40),27.05-i*.27,-2.12],[s*(.83+i*.46),25.65-i*.32,-2.30],[s*(1.65+i*.36),24.80-i*.32,-2.13],[s*(2.04+i*.36),24.72-i*.32,-1.88]];
  m.ribbon('swept_fringe_'+side+'_'+i,b,path,[w,w+.13,w+.16,w*.64,.09],[.39,.40,.35,.25,.08],i%2?'hair':'hairLight');
 }
 m.ribbon('center_fringe_tip','hair_root',[[0,27.55,-2.10],[-.1,26.45,-2.24],[.03,25.5,-2.3]],[.61,.63,.15],[.33,.32,.13],'hair');
 for(const s of [-1,1]){
  const side=s===1?'left':'right';
  for(let j=0;j<3;j++){
   const x=s*(2.15+j*.38),z=-.55+j*.65;
   m.ribbon('temple_curl_'+side+'_'+j,'hair_side_'+side,[[x,26.65,z],[x+s*.18,24.3,z-.45],[x+s*.16,22.0,z-.7],[x+s*.67,20.6,z-.6],[x+s*.37,19.6,z-.78],[x+s*.85,18.8,z-.45]],[.62,.66,.67,.51,.40,.09],[.58,.55,.48,.40,.30,.12],j===1?'hairLight':'hair');
  }
  for(let j=0;j<2;j++)m.ribbon('loose_long_lock_'+side+'_'+j,'hair_strands_'+side,[[s*(2.6+j*.3),23,1],[s*(3.1+j*.3),19,1.65],[s*(3.6+j*.4),15,1.95],[s*(3.0+j*.7),12,2.1],[s*(3.65+j*.6),10.7,1.5]],[.46,.50,.43,.35,.08],[.4,.4,.37,.28,.09],'hairShade');
 }
 const bottom=28-p.hairLength;
 for(let i=0;i<11;i++){
  const x=(i-5)*.48*p.hairVolume,z=1.72+.33*Math.sqrt(Math.max(0,1-(x/3)**2)),wave=Math.sin(i*2.7)*.30;
   const regions=[['hair_back_upper',[[x,27.85,z],[x*1.04,24.5,z+.14],[x*1.12,21,z+.14]]],['hair_back_mid',[[x*1.12,21,z+.14],[x*1.2+wave,18,z+.31],[x*1.30,15.5,z+.45]]],['hair_back_lower',[[x*1.30,15.5,z+.45],[x*1.36+wave,13.6,z+.62],[x*1.28-wave,bottom+1.9+Math.cos(i*1.4)*.5,z+.58],[x*1.35-wave,bottom+.9+Math.cos(i*1.4)*.5,z+.72],[x*1.25+wave,bottom+.3+Math.cos(i*1.4)*.5,z+.55],[x*1.18+wave,bottom+.7+Math.cos(i*1.4)*.5,z+.31]]]];
  for(const [b,path] of regions){const widths=path.map((_,j)=>b==='hair_back_lower'&&j===path.length-1?.12:.59+(i%3)*.05);m.ribbon('back_lock_'+i+'_'+b,b,path,widths,.42,i%3===0?'hairLight':i%3===1?'hair':'hairShade');
   trimPath(m,'back_lock_groove_'+i+'_'+b,b,path.map(q=>[q[0]-.13,q[1],q[2]+.235]),'hairLight',.027);
  }
 }
}

function head(m,p){
 m.bone('head','torso',[0,22.1,0]);m.bone('face','head',[0,24.5,-1.85]);m.bone('crown','head',[0,27.7,0]);
 const w=p.headWidth/2;
 m.loft('face_sculpt','face',[[0,22.35,-.12,w*.65,1.30],[0,22.7,-.05,w*.86,1.58],[0,23.7,0,w*.98,1.78],[0,26.35,0,w,1.81],[0,22.35+p.headHeight,0,w*.92,1.64]],'skin',12,.28);
 for(const s of [-1,1]){
  m.box('ear_'+s,'face',[s*2.58,24.1,0],[.45,1.25,.61],'skin');
  gem(m,'earring_'+s,'head',s*2.63,23.16,-.14,.52);
  const x=s*1.12;
  m.box('eye_white_'+s,'face',[x,24.27,-1.87],[1.09,.92,.055],'whiteLight');
  m.box('iris_'+s,'face',[x-s*.05,24.2,-1.91],[.61,.78,.04],'eye');
  m.box('iris_dark_'+s,'face',[x-s*.07,24.42,-1.94],[.54,.32,.03],'blue');
  m.box('pupil_'+s,'face',[x-s*.07,24.35,-1.963],[.21,.49,.025],'navy');
  m.box('eye_glint_'+s,'face',[x-.14,24.47,-1.98],[.20,.21,.022],'whiteLight');
  m.box('upper_lash_'+s,'face',[x,24.80,-1.97],[1.26,.22,.045],'lash');
  m.box('outer_lash_'+s,'face',[x+s*.62,24.68,-1.91],[.19,.34,.04],'lash');
  m.box('eyebrow_'+s,'face',[x,25.30,-1.88],[1.01,.13,.04],'hairShade');
  m.box('cheek_blush_'+s,'face',[s*1.53,23.52,-1.83],[.53,.16,.02],'skinShade');
 }
 m.loft('small_nose','face',[[0,23.45,-1.91,.20,.17],[0,23.7,-1.94,.13,.15],[0,24,-1.86,.1,.06]],'skin',6,.6);
 m.box('quiet_mouth','face',[0,23.03,-1.76],[.55,.105,.05],'lip');
 m.box('lower_lip_light','face',[0,22.94,-1.72],[.36,.075,.03],'skin');
 hair(m,p);
 // Silver circlet and ice battlements wrap all around the head.
 for(let i=0;i<16;i++){
  const a=i*Math.PI*2/16,x=Math.sin(a)*2.70,z=-Math.cos(a)*2.0;
  m.rod('circlet_band_'+i,'crown',[x,27.35,z],[Math.sin(a+Math.PI/8)*2.70,27.35,-Math.cos(a+Math.PI/8)*2],.15,'silver',6);
  m.box('crown_battlement_'+i,'crown',[x,27.9+(i%3)*.12,z],[.30,.94+(i%3)*.23,.32],i%2?'silver':'blueLight');
 }
 for(const [name,x,h,z] of [['center',0,2.65,-2.1],['left_01',1.15,1.5,-1.91],['right_01',-1.15,1.5,-1.91],['left_02',2.19,1.35,-1.10],['right_02',-2.19,1.35,-1.10],['back',0,1.18,2.05]]){
  const b='crown_'+name+'_crystal';m.bone(b,'crown',[x,27.2,z]);m.crystal(b+'_facets',b,[x,27.0,z],[x*1.06,27+h,z],name==='center'?.98:.64,.49);
 }
 gem(m,'forehead_jewel','crown',0,26.94,-2.37,.9);m.bone('vfx_head','head',[0,28,0]);
}

export function character(p=defaults){
 const m=new Model('amelia');m.bone('amelia_root',null);m.bone('body','amelia_root',[0,14.6,0]);m.bone('pelvis','body',[0,14.6,0]);m.bone('waist','pelvis',[0,16.0,0]);m.bone('torso','waist',[0,17.15,0]);m.bone('chest','torso',[0,19.4,-.2]);
 m.loft('hip_foundation','pelvis',[[0,13.5,0,p.hipWidth*.41,1.28],[0,14.6,0,p.hipWidth*.50,1.42],[0,15.9,0,p.waistWidth*.56,1.05]],'navy',12,.60);
 m.loft('corset_tailored_waist','waist',[[0,15.15,0,p.hipWidth*.45,1.35],[0,16.25,0,p.waistWidth*.50,1.10],[0,17.35,0,p.waistWidth*.54,1.17],[0,18.4,0,p.torsoWidth*.49,1.45]],['blue','cloth','blue','blueLight'],12,.6);
 m.loft('upper_body_skin','torso',[[0,17.9,.05,2.02,1.25],[0,19.6,0,2.60,1.33],[0,20.8,0,2.93,1.02],[0,21.5,.1,1.49,.90],[0,22.3,.1,.66,.63]],'skin',12,.6);
 m.loft('neck','torso',[[0,21.4,0,.66,.6],[0,22.65,0,.70,.63]],'skin',8,.6);
 m.loft('dark_choker','torso',[[0,21.86,0,.71,.65],[0,22.2,0,.73,.67]],'navy',10,.55);
 for(const s of [-1,1]){
  const cx=s*p.chestWidth*.245;
  m.loft('sculpted_bodice_cup_'+s,'chest',[[cx*.85,17.95,-.71,.70,.75],[cx,18.45,-.83,1.15,1.16],[cx,19.25,-.72,p.chestWidth*.25,p.chestDepth*.36],[cx*.94,20,-.57,1.32,1.23],[cx*.85,20.5,-.33,.78,.72]],['white','whiteLight','white','white'],12,.72);
  for(let j=0;j<2;j++)trimPath(m,'cup_seam_'+s+'_'+j,'chest',[[s*(.9+j*.8),20.25,-1.68],[s*(1.2+j*.7),19.6,-2.02],[s*(1.4+j*.5),18.8,-2.09],[s*(1.2+j*.35),18.1,-1.64]],'silver',.031);
  trimPath(m,'bodice_gold_edge_'+s,'chest',[[s*.12,18.25,-1.57],[s*.42,19.18,-2.04],[s*1.20,20.27,-1.64],[s*2.01,20.35,-1.26],[s*2.75,19.7,-.99]],'gold',.055);
  trimPath(m,'corset_boning_'+s,'waist',[[s*1.82,18.23,-1.28],[s*1.16,17.35,-1.10],[s*.90,16.4,-1.02],[s*2.15,15.34,-1.37]],'gold',.075);
  trimPath(m,'waist_chevron_'+s,'waist',[[s*2.4,15.65,-1.22],[s*1.31,15.11,-1.57],[0,14.46,-1.77]],'gold',.11);
  trimPath(m,'shoulder_chain_'+s,'torso',[[s*.45,21.6,-.65],[s*1.6,21.22,-.77],[s*2.9,20.90,-.70],[s*3.6,20.2,-.56]],'gold',.085);
  gem(m,'collar_side_gem_'+s,'torso',s*2.40,21.00,-.81,.62);
  const side=s===1?'left':'right',x=s*1.3;
  m.bone('leg_'+side,'pelvis',[x,14.3,0]);m.bone('lower_leg_'+side,'leg_'+side,[x,8.1,0]);m.bone('foot_'+side,'lower_leg_'+side,[x,1.75,-.12]);
  m.loft('thigh_'+side,'leg_'+side,[[x,7.8,0,.70,.70],[x,10.5,0,.92,.91],[x,13.8,0,1.00,1.02],[x,14.6,0,.83,.94]],'skin',10,.55);
  m.loft('boot_calf_'+side,'lower_leg_'+side,[[x,1.5,.05,.61,.63],[x,3.0,.13,.66,.65],[x,5.2,.13,.86,.78],[x,7.15,.06,.79,.73],[x,8.30,.03,.66,.68]],['white','silver','blueLight','white'],10,.40);
  m.loft('boot_knee_guard_'+side,'lower_leg_'+side,[[x,6.1,-.59,.57,.17],[x,7.3,-.67,.77,.19],[x,8.45,-.55,.46,.20]],'white',8,.3);
  m.box('boot_toe_'+side,'foot_'+side,[x,.53,-.72],[1.45,1.05,1.92],'white');
  m.box('boot_heel_'+side,'foot_'+side,[x,.56,.60],[1.1,1.12,.75],'navy');
  m.box('boot_sole_'+side,'foot_'+side,[x,.12,-.73],[1.5,.24,1.98],'silver');
  m.ribbon('instep_armor_'+side,'foot_'+side,[[x,.67,-1.4],[x,1.1,-.73],[x,1.97,-.28]],[1.2,1.24,.91],[.29,.32,.32],'white');
  for(let j=0;j<3;j++)gem(m,'boot_crystal_'+side+'_'+j,'lower_leg_'+side,x,2.3+j*2.1,-.73,.67);
  trimPath(m,'boot_gold_line_'+side,'lower_leg_'+side,[[x,1.7,-.60],[x,4.0,-.74],[x,6.9,-.81]],'gold',.045);
 }
 gem(m,'sternum_ice','chest',0,18.15,-1.94,.73);gem(m,'waist_ice','waist',0,16.4,-1.2,1.0);gem(m,'collar_ice','torso',0,21.43,-.86,.94);
 m.bone('vfx_chest','chest',[0,19.5,-2]);m.bone('vfx_feet','amelia_root',[0,.1,0]);
 arm(m,'left','torso',p);arm(m,'right','torso',p);head(m,p);skirt(m,p);
 m.bone('staff_socket','hand_right',[-4.56,13.1,-.68]);staff(m,'staff_socket',[-4.70,.7,-.66],p);
 return m;
}
export function firstPerson(p=defaults){
 const m=new Model('amelia_first_person');m.bone('amelia_root',null);m.bone('camera_anchor','amelia_root');arm(m,'left','camera_anchor',p,true);arm(m,'right','camera_anchor',p,true);m.bone('staff_socket','hand_right',[-4.56,13.1,-.68]);staff(m,'staff_socket',[-4.70,.7,-.66],p);
 const staffIds=new Set();const visit=b=>{for(const c of b.children)if(typeof c==='string')staffIds.add(c);else visit(c);};visit(m.bones.staff_root);
 const compact=v=>[-4.7+(v[0]+4.7)*.8,13.1+(v[1]-13.1)*.55,-.66+(v[2]+.66)*.8];
 for(const e of m.elements)if(staffIds.has(e.uuid))for(const k in e.vertices)e.vertices[k]=compact(e.vertices[k]);
 const compactBone=b=>{b.origin=compact(b.origin);for(const c of b.children)if(typeof c!=='string')compactBone(c);};compactBone(m.bones.staff_root);
 const removed=new Set(m.elements.filter(e=>/^(bare_upper_arm_|arm_diamond_)/.test(e.name)).map(e=>e.uuid));m.elements=m.elements.filter(e=>!removed.has(e.uuid));
 for(const b of Object.values(m.bones)){b.children=b.children.filter(c=>!removed.has(c));b.origin[0]*=-1;}
 for(const e of m.elements){for(const v of Object.values(e.vertices))v[0]*=-1;for(const f of Object.values(e.faces))f.vertices.reverse();}
 return m;
}
export function staffOnly(p=defaults){const m=new Model('amelia_staff');m.bone('amelia_root',null);staff(m,'amelia_root',[0,0,0],p);return m;}
