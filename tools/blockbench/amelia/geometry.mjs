import {Model,mix} from './core.mjs';
import {rework,reworkArms} from './visual_rework.mjs';
export const defaults={scale:1,headWidth:4.75,headHeight:5.75,torsoWidth:4.5,chestWidth:6.85,chestDepth:4.45,waistWidth:3.5,hipWidth:5.4,hairLength:18,hairVolume:1.08,skirtLength:13.6,staffLength:30.5,crystalSize:1};

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
function bustCup(m,name,bone,c,rx,ry,rz,s){
 const vertices=[],faces=[],rows=6,segments=12;
 for(let j=0;j<=rows;j++){
  const theta=-Math.PI/2+(Math.PI*.46)*j/rows;
  for(let i=0;i<=segments;i++){
   const phi=-Math.PI/2+Math.PI*i/segments;
   vertices.push([c[0]+Math.sin(phi)*Math.cos(theta)*rx,c[1]+Math.sin(theta)*ry,c[2]-Math.cos(phi)*Math.cos(theta)*rz]);
  }
 }
 for(let j=0;j<rows;j++)for(let i=0;i<segments;i++){const a=j*(segments+1)+i,b=a+1,c=a+segments+1,d=c+1;faces.push([a,b,d,c]);}
 m.mesh(name,bone,vertices,faces,'white');
}

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
 m.loft('silver_hair_cap','hair_root',[[0,25.75,.22,2.72,2.08],[0,27.3,.15,2.75,2.12],[0,28.15,.1,2.48,1.94],[0,28.45,.15,2.04,1.72]],['hair','hairLight','hairShade','hair'],12,.32);
 // Split curtain fringe: diagonally swept separate stepped locks.
 for(const s of [-1,1])for(let i=0;i<4;i++){
  const side=s===1?'left':'right',b='hair_front_'+side,w=.48+i*.08;
  const path=[[s*(.25+i*.34),28.05-i*.14,-1.77],[s*(.40+i*.40),27.05-i*.27,-2.12],[s*(.83+i*.46),25.65-i*.32,-2.30],[s*(1.65+i*.36),24.80-i*.32,-2.13],[s*(2.04+i*.36),24.72-i*.32,-1.88]];
  m.ribbon('swept_fringe_'+side+'_'+i,b,path,[w,w+.13,w+.16,w*.64,.09],[.39,.40,.35,.25,.08],i%2?'hair':'hairLight');
 }
 m.ribbon('center_fringe_tip','hair_root',[[0,27.55,-2.10],[-.1,26.45,-2.24],[.03,25.5,-2.3]],[.61,.63,.15],[.33,.32,.13],'hair');
 for(const s of [-1,1]){
  const side=s===1?'left':'right';
  for(let j=0;j<5;j++){
   const x=s*(1.86+j*.31),z=-1.18+j*.46;
   m.ribbon('temple_curl_'+side+'_'+j,'hair_side_'+side,[[x,27.05,z],[x+s*(.18+j*.03),25.4,z-.43],[x-s*.06,23.55,z-.72],[x+s*(.55+j*.05),22.15,z-.67],[x+s*.22,20.65,z-.89],[x+s*.75,19.45,z-.62],[x+s*.48,18.65,z-.78]],[.68,.73,.76,.69,.55,.38,.09],[.64,.62,.58,.51,.42,.29,.10],j%3===1?'hairLight':j%3===2?'hairShade':'hair');
   trimPath(m,'temple_curl_highlight_'+side+'_'+j,'hair_side_'+side,[[x-s*.12,26.8,z-.35],[x+s*.04,24.5,z-.72],[x+s*.35,22.2,z-.94],[x+s*.12,20.7,z-1.08]],'hairLight',.03);
  }
  m.ribbon('profile_face_frame_'+side,'hair_side_'+side,[[s*1.74,27.15,-1.75],[s*2.18,25.7,-2.15],[s*2.32,23.95,-2.22],[s*2.48,22.25,-2.05],[s*2.78,20.75,-1.78],[s*2.55,19.25,-1.62]],[.72,.78,.74,.62,.40,.08],[.50,.48,.43,.38,.28,.08],'hairLight');
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
 m.loft('face_sculpt','face',[[0,22.32,-.03,w*.43,1.12],[0,22.63,-.08,w*.68,1.38],[0,23.45,-.02,w*.88,1.61],[0,25.25,.02,w,1.72],[0,26.62,.04,w*.91,1.66],[0,22.32+p.headHeight,.02,w*.78,1.49]],'skin',12,.42);
 for(const s of [-1,1]){
  m.box('ear_'+s,'face',[s*2.31,24.25,.06],[.38,1.20,.54],'skin');
  gem(m,'earring_'+s,'head',s*2.34,23.31,-.08,.48);
  const x=s*.98;
  m.box('eye_white_'+s,'face',[x,24.38,-1.76],[1.08,.60,.055],'whiteLight');
  m.box('iris_'+s,'face',[x-s*.06,24.34,-1.80],[.54,.54,.04],'eye');
  m.box('iris_dark_'+s,'face',[x-s*.07,24.48,-1.83],[.49,.22,.03],'blue');
  m.box('pupil_'+s,'face',[x-s*.07,24.37,-1.855],[.18,.36,.025],'navy');
  m.box('eye_glint_'+s,'face',[x-.11,24.49,-1.87],[.14,.14,.022],'whiteLight');
  m.ribbon('upper_lash_'+s,'face',[[x-s*.57,24.62,-1.85],[x,24.68,-1.89],[x+s*.60,24.77,-1.84]],[.12,.15,.10],[.06,.06,.05],'lash');
  m.ribbon('lower_lash_'+s,'face',[[x-s*.48,24.12,-1.82],[x,24.06,-1.84],[x+s*.47,24.14,-1.81]],[.055,.045,.035],[.035,.03,.025],'lash');
  m.ribbon('eyebrow_'+s,'face',[[x-s*.52,25.12,-1.77],[x,25.25,-1.80],[x+s*.52,25.38,-1.75]],[.16,.19,.12],[.055,.055,.045],'lash');
 }
 m.loft('small_nose','face',[[0,23.40,-1.79,.17,.14],[0,23.75,-1.84,.11,.12],[0,24.05,-1.76,.075,.05]],'skin',6,.65);
 m.ribbon('confident_mouth','face',[[-.34,23.05,-1.70],[0,22.99,-1.74],[.34,23.07,-1.70]],[.085,.11,.075],[.04,.045,.035],'lip');
 m.ribbon('defined_lower_lip','face',[[-.22,22.93,-1.68],[0,22.88,-1.70],[.22,22.94,-1.68]],[.06,.075,.05],[.03,.035,.025],'skinShade');
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
 m.loft('upper_body_skin','torso',[[0,17.9,.05,2.02,1.25],[0,19.4,0,2.55,1.36],[0,20.45,-.02,2.88,1.10],[0,21.2,.05,2.25,.96],[0,21.72,.1,1.30,.78],[0,22.3,.1,.66,.63]],'skin',12,.6);
 m.loft('neck','torso',[[0,21.4,0,.66,.6],[0,22.65,0,.70,.63]],'skin',8,.6);
 m.loft('dark_choker','torso',[[0,21.86,0,.71,.65],[0,22.2,0,.73,.67]],'navy',10,.55);
 for(const s of [-1,1]){
  const cx=s*p.chestWidth*.218;
  // The bust is its own body volume; the corset cups cover only the lower half,
  // preserving the deep open neckline and separated silhouette from the reference.
  m.loft('exposed_bust_'+s,'chest',[[cx*.82,18.05,-.42,.52,.48],[cx*.93,18.42,-.62,.92,.86],[cx*.99,18.90,-.76,1.34,1.25],[cx,19.48,-.82,p.chestWidth*.245,p.chestDepth*.335],[cx*.98,20.03,-.70,1.53,1.40],[cx*.90,20.48,-.45,1.18,1.02],[cx*.72,20.82,-.18,.66,.54],[cx*.52,21.00,-.03,.28,.24]],['skinShade','skin','skin','skin','skin','skin','skin'],16,.96);
  bustCup(m,'open_lower_corset_cup_'+s,'chest',[cx,19.42,-.84],1.72,1.54,1.53,s);
  trimPath(m,'open_cup_top_edge_'+s,'chest',[[s*.12,19.25,-1.43],[s*.55,19.46,-1.79],[s*1.18,19.73,-2.13],[s*1.88,20.02,-2.03],[s*2.55,20.25,-1.48]],'gold',.075);
  for(let j=0;j<2;j++)trimPath(m,'cup_seam_'+s+'_'+j,'chest',[[s*(.69+j*.70),19.22,-1.75],[s*(1.00+j*.62),18.91,-2.10],[s*(1.20+j*.46),18.44,-1.98],[s*(1.04+j*.36),17.98,-1.43]],'silver',.031);
  trimPath(m,'bodice_gold_edge_'+s,'chest',[[s*.10,17.98,-1.25],[s*.40,18.48,-1.74],[s*1.18,19.64,-2.14],[s*1.88,20.00,-2.04],[s*2.59,20.24,-1.49]],'gold',.06);
  trimPath(m,'corset_boning_'+s,'waist',[[s*1.82,18.23,-1.28],[s*1.16,17.35,-1.10],[s*.90,16.4,-1.02],[s*2.15,15.34,-1.37]],'gold',.075);
  trimPath(m,'waist_chevron_'+s,'waist',[[s*2.4,15.65,-1.22],[s*1.31,15.11,-1.57],[0,14.46,-1.77]],'gold',.11);
  trimPath(m,'shoulder_chain_'+s,'torso',[[s*.45,21.58,-.66],[s*1.45,21.34,-.84],[s*2.46,20.98,-.88],[s*3.15,20.48,-.67]],'gold',.085);
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
 rework(m);return m;
}
export function firstPerson(p=defaults){
 const m=new Model('amelia_first_person');m.bone('amelia_root',null);m.bone('camera_anchor','amelia_root');arm(m,'left','camera_anchor',p,true);arm(m,'right','camera_anchor',p,true);m.bone('staff_socket','hand_right',[-4.56,13.1,-.68]);staff(m,'staff_socket',[-4.70,.7,-.66],p);
 reworkArms(m);
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
