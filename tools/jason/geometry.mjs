import { Rig, add, mul, sub, noise } from './mesh.mjs';

function arms(rig, side) {
  const s=side==='left'?1:-1, arm=`arm_${side}`, forearm=`forearm_${side}`, fist=`fist_${side}`;
  rig.bone(arm,[s*6.25,28.2,0],'chest');
  rig.bone(forearm,[s*8.25,23.7,0],arm);
  rig.bone(fist,[s*9.15,18.2,-.15],forearm);
  rig.bone(`socket_fist_${side}`,[s*9.4,16.7,-2.0],fist);
  rig.ellipsoid(`deltoid_${side}`,arm,[s*6.15,27.5,0],[2.6,2.38,2.15],'skin',12,6,s*16);
  rig.ellipsoid(`biceps_${side}`,arm,[s*7.65,25.1,-.4],[1.75,3.12,1.67],'skin',12,6,s*18);
  rig.ellipsoid(`triceps_${side}`,arm,[s*7.6,25.3,.85],[1.7,2.6,1.3],'skin',10,5,s*15);
  rig.loft(`upper_arm_core_${side}`,arm,[[s*8.25,22.8,0,1.32,1.22],[s*7.75,24.3,.05,1.61,1.56],[s*7,26.3,.05,1.98,1.72],[s*6.35,28.0,0,1.95,1.73]],'skin',12);
  rig.ellipsoid(`elbow_${side}`,forearm,[s*8.35,23.4,.15],[1.35,1.35,1.35],'skinDark');
  rig.loft(`forearm_shape_${side}`,forearm,[[s*9.2,18.0,-.15,1.18,1.13],[s*9.1,19.2,-.1,1.45,1.3],[s*8.75,21.3,0,1.75,1.45],[s*8.3,23.4,0,1.4,1.3]],'bandage',12);
  // Overlapping helical strips, with a darker seam along each exposed edge.
  for(let turn=0;turn<8;turn++) {
    const positions=[],faces=[],edge=[],ef=[];
    for(let j=0;j<=14;j++) {
      const a=j/14*Math.PI*2, y=18.05+turn*.61+j/14*.61;
      const t=(y-18)/5.7,cx=s*(9.2-t*.95),rx=1.22+Math.sin(Math.min(1,t)*Math.PI)*.52,rz=1.16+Math.sin(t*Math.PI)*.27;
      for(const dy of [0,.49]) positions.push([cx+Math.cos(a)*rx,y+dy,-.05+Math.sin(a)*rz]);
      for(const dy of [0,.055]) edge.push([cx+Math.cos(a)*(rx+.018),y+dy,-.05+Math.sin(a)*(rz+.018)]);
      if(j) {const k=j*2;faces.push([k-2,k,k+1],[k-2,k+1,k-1]);ef.push([k-2,k,k+1],[k-2,k+1,k-1]);}
    }
    rig.mesh(`wrap_${side}_${turn}`,forearm,positions,faces,'bandage');
    rig.mesh(`wrap_edge_${side}_${turn}`,forearm,edge,ef,'bandageEdge');
  }
  rig.ellipsoid(`palm_${side}`,fist,[s*9.45,16.65,-.3],[1.65,1.9,1.4],'skin',10,5,s*7);
  rig.ellipsoid(`hand_wrap_${side}`,fist,[s*9.35,17.9,.15],[1.51,.66,1.4],'bandage',10,4);
  for(let i=0;i<4;i++) {
    const x=s*(8.3+i*.72);
    rig.ellipsoid(`knuckle_${side}_${i}`,fist,[x,16.55,-1.35],[.48,.75,.56],'skinLight',6,4);
    rig.ellipsoid(`finger_fold_${side}_${i}`,fist,[x,15.8,-.8],[.44,.62,.75],'skin',6,4);
    rig.ribbon(`knuckle_crease_${side}_${i}`,fist,[[x-.25,16.1,-1.69],[x+.23,16.1,-1.69]],.045,'skinDark');
  }
  rig.ellipsoid(`thumb_${side}`,fist,[s*8.05,16.8,-.82],[.65,1.0,.72],'skinLight',8,4,s*29);
  for(const id of rig.bones.get(fist).children.filter(c=>typeof c==='string')) {
    const mesh=rig.elements.find(e=>e.uuid===id);
    for(const key of Object.keys(mesh.vertices)) mesh.vertices[key]=mul(mesh.vertices[key],1.18);
  }
}

function head(rig) {
  rig.bone('head',[0,30.8,0],'chest'); rig.bone('hair',[0,34.3,0],'head');
  rig.bone('scar_face',[0,32.8,-1.9],'head');
  rig.loft('neck','chest',[[0,28.85,.15,2.1,1.58],[0,30.0,.25,1.67,1.45],[0,31.35,.2,1.35,1.27]],'skin',12);
  rig.loft('skull','head',[[0,30.8,-.1,.95,1.05],[0,31.3,.05,1.53,1.35],[0,32.2,.13,1.86,1.64],[0,33.2,.18,1.84,1.71],[0,34.3,.2,1.73,1.6],[0,35.1,.25,1.35,1.25]],'skin',12,Math.PI/12);
  // Explicit facial planes: chin, jaw, temples, brow ridge, nose and inset eyes.
  const faceRows=[
    [30.86,.97,1.32,1.05,.45],[31.2,1.38,1.72,1.46,.62],
    [31.55,1.61,1.88,1.53,.72],[31.84,1.68,1.92,1.55,.78],
    [32.19,1.81,1.71,1.61,.81],[32.54,1.82,1.65,1.84,.84],
    [32.86,1.81,1.61,1.63,.77],[33.14,1.78,1.62,1.51,.67],
    [33.48,1.78,1.76,1.65,.67],[33.84,1.73,1.60,1.52,.64],
    [34.21,1.67,1.48,1.41,.61],
  ],fv=[],ff=[];
  for(const [y,w,center,cheek,edge]of faceRows)for(let i=0;i<9;i++){
    const t=(i-4)/4,u=Math.abs(t),depth=u<.6?center+(cheek-center)*u/.6:cheek+(edge-cheek)*(u-.6)/.4;
    fv.push([t*w,y,-depth]);
  }
  for(let j=0;j<faceRows.length-1;j++)for(let i=0;i<8;i++){const k=j*9+i;ff.push([k,k+9,k+10],[k,k+10,k+1]);}
  rig.mesh('face_planes','head',fv,ff,'skin');
  for(const s of [-1,1]) {
    rig.ellipsoid(`ear_${s}`,'head',[s*1.87,32.85,.04],[.34,.66,.45],'skin',8,4);
    rig.ellipsoid(`ear_inner_${s}`,'head',[s*2.02,32.82,-.14],[.13,.36,.23],'skinDark',6,4);
    const eye=[ [s*.27,33.13,-1.67],[s*1.37,33.4,-1.38],[s*1.26,32.98,-1.6],[s*.39,32.95,-1.81] ];
    const eyeFaces=s>0?[[0,1,2],[0,2,3]]:[[2,1,0],[3,2,0]];
    rig.mesh(`eye_socket_${s}`,'head',eye,eyeFaces,'black');
    const white=eye.map(p=>[p[0]*.81+s*.09,(p[1]-33.13)*.36+33.13,p[2]-.035]);
    rig.mesh(`eye_white_${s}`,'head',white,eyeFaces,'eyeWhite');
    rig.ellipsoid(`iris_${s}`,'head',[s*.73,33.13,-1.78],[.12,.09,.045],'iris',8,4);
    rig.ellipsoid(`pupil_${s}`,'head',[s*.73,33.13,-1.817],[.049,.072,.03],'black',6,4);
    rig.ribbon(`brow_${s}`,'head',[[s*.22,33.34,-1.88],[s*.81,33.52,-1.82],[s*1.42,33.54,-1.46]],.17,'hair');
    rig.ribbon(`frown_${s}`,'head',[[s*.24,33.55,-1.68],[s*.30,33.84,-1.58]],.06,'skinDark');
  }
  rig.mesh('nose','head',[[0,33.43,-1.67],[-.3,32.5,-1.84],[0,32.54,-2.32],[.3,32.5,-1.84],[-.43,32.35,-1.73],[0,32.28,-2.1],[.43,32.35,-1.73]],[[0,1,2],[0,2,3],[1,4,5],[1,5,2],[2,5,6],[2,6,3],[4,6,5]],'skinLight');
  rig.ribbon('mouth','head',[[-.69,31.78,-1.84],[0,31.86,-2.0],[.68,31.78,-1.84]],.083,'black');
  rig.ribbon('lower_lip','head',[[-.5,31.63,-1.87],[0,31.58,-1.98],[.5,31.63,-1.87]],.115,'lip');
  rig.ribbon('face_scar','scar_face',[[1.28,33.79,-1.33],[1.36,33.5,-1.51],[1.4,32.82,-1.61],[1.23,32.31,-1.74]],.095,'scarLight');
  rig.loft('hair_underlayer','hair',[[0,33.83,.37,1.87,1.68],[0,34.65,.31,1.88,1.67],[0,35.27,.24,1.47,1.34],[0,35.52,.2,.6,.6]],'hair',12);
  for(let i=0;i<25;i++) {
    const a=i*2.399963,cx=Math.cos(a)*(i<14?1.32:.78),cz=.15+Math.sin(a)*(i<14?1.12:.65);
    const y=34.35+(i<14?.05:.47)+noise(`hair${i}`)*.25;
    const tip=[cx-.45,y+.57+noise(`tip${i}`)*.25,cz-.6];
    rig.mesh(`hair_lock_${i}`,'hair',[[cx-.55,y,cz-.35],[cx+.56,y,cz-.29],[cx+.42,y+.02,cz+.47],[cx-.41,y+.03,cz+.43],tip],[[0,1,4],[1,2,4],[2,3,4],[3,0,4]],i%4===0?'hairLight':'hair');
  }
  for(const s of [-1,1]) rig.ribbon(`sideburn_${s}`,'hair',[[s*1.75,34,-.38],[s*1.87,33.1,-.45]],.30,'hair');
  const remap=p=>[p[0]*1.10,30.55+(p[1]-30.8)*1.12,p[2]*1.08];
  function reshape(b) {
    const old=b.origin.slice(); b.origin=remap(old);
    for(const child of b.children) {
      if(typeof child==='object')reshape(child);
      else {const e=rig.elements.find(e=>e.uuid===child),o=e.origin.slice();e.origin=remap(o);for(const k of Object.keys(e.vertices))e.vertices[k]=sub(remap(add(o,e.vertices[k])),e.origin);}
    }
  }
  reshape(rig.bones.get('head'));
}

const sections=[[19.9,3.9,2.0],[21.3,4.0,1.9],[23.1,4.45,2.08],[25.2,5.55,2.44],[27.35,6.1,2.53],[28.6,5.3,2.1],[29.4,2.0,1.4]];
function section(y) {
  let a=sections[0],b=sections.at(-1);
  for(const next of sections) {if(next[0]<=y)a=next;if(next[0]>=y){b=next;break;}}
  const t=a===b?0:(y-a[0])/(b[0]-a[0]);
  return [a[1]*(1-t)+b[1]*t,a[2]*(1-t)+b[2]*t];
}
function surface(x,y,front=true) {
  const [width,depth]=section(y),edge=Math.sqrt(Math.max(0,1-(x/width)**2));
  const gaussian=(v,c,w)=>Math.exp(-(((v-c)/w)**2));
  if(!front)return depth*edge+.34*gaussian(Math.abs(x),2.7,1.9)*gaussian(y,27.1,1.65);
  const pec=.88*gaussian(Math.abs(x),2.7,2.0)*gaussian(y,26.93,1.22);
  const abs=.62*gaussian(Math.abs(x),1.10,.86)*[24.12,22.91,21.70].reduce((s,c)=>s+gaussian(y,c,.58),0);
  const oblique=.17*gaussian(Math.abs(x),3.23,.9)*[24.3,23.2,22.1].reduce((s,c)=>s+gaussian(y,c,.40),0);
  return -(depth*edge+pec+abs+oblique);
}

function torso(rig) {
  rig.bone('root',[0,0,0]); rig.bone('body',[0,19.8,0],'root');
  rig.bone('pelvis',[0,19.8,0],'body'); rig.bone('chest',[0,22.7,0],'body');
  rig.bone('scar_chest',[0,25.8,-2],'chest'); rig.bone('scar_back',[0,26,2],'chest');
  const vertices=[],polygons=[],count=32,rows=25;
  for(let j=0;j<rows;j++) for(let i=0;i<count;i++) {
    const y=19.9+j/(rows-1)*9.5+(j>0&&j<rows-1?(noise(`torso-${j}-${i}`)-.5)*.06:0);
    const a=(i+(j%2)*.07)/count*Math.PI*2,[width]=section(y),x=Math.cos(a)*width;
    vertices.push([x,y,surface(x,y,Math.sin(a)<0)]);
  }
  for(let j=0;j<rows-1;j++) for(let i=0;i<count;i++) {
    const a=j*count+i,b=j*count+(i+1)%count,c=b+count,d=a+count;
    polygons.push([a,d,c],[a,c,b]);
  }
  rig.mesh('torso_sculpt','chest',vertices,polygons,'skin');
  for(const s of [-1,1]) {
    rig.ellipsoid(`trapezius_${s}`,'chest',[s*2.9,28.65,.3],[2.75,.66,1.42],'skin',10,4,-s*13);
    rig.ribbon(`clavicle_${s}`,'chest',[[s*.7,28.55],[s*2.9,28.3],[s*4.6,28.1]].map(([x,y])=>[x,y,surface(x,y)-.035]),.085,'skinLight');
  }
  rig.ribbon('sternum','chest',[[0,27.7],[0,26.3],[0,25.15]].map(([x,y])=>[x,y,surface(x,y)-.025]),.055,'skinDark');
  rig.ribbon('navel','chest',[[-.13,20.85,-2.025],[.13,20.85,-2.025]],.11,'skinDark');
  const scar=[[4.4,28.7,-1.64],[3.4,28.0,-2.72],[2.4,27.1,-3.23],[1.55,26.35,-3.05],[.7,25.65,-2.56],[-.1,24.9,-2.11],[-.85,24.1,-2.43],[-1.55,23.23,-2.22],[-2.1,22.45,-2.07]];
  const mapped=scar.map(([x,y])=>[x,y,surface(x,y)-.075]);
  rig.ribbon('diagonal_scar','scar_chest',mapped,.14,'scar');
  rig.ribbon('scar_raised_edge','scar_chest',scar.map(([x,y])=>[x+.10,y+.025,surface(x+.10,y+.025)-.075]),.05,'scarLight');
  rig.ribbon('chest_slash','scar_chest',[[-4.55,27.09],[-3.5,26.7],[-2.45,26.3],[-1.25,26.01]].map(([x,y])=>[x,y,surface(x,y)-.075]),.11,'scar');
  rig.ribbon('back_scar','scar_back',[[-3.8,28],[-2.6,26.8],[-.8,25.6],[1,24.5],[2,23.5]].map(([x,y])=>[x,y,surface(x,y,false)+.075]),.14,'scarLight');
}

function legsAndBelt(rig) {
  rig.loft('trouser_hips','pelvis',[[0,16.3,0,3.9,2.14],[0,18.4,.2,4.15,2.45],[0,20.0,.1,4.1,2.2]],'pants',14);
  for(const s of [-1,1]) {
    const side=s===1?'left':'right',leg=`leg_${side}`,shin=`shin_${side}`,foot=`foot_${side}`;
    rig.bone(leg,[s*2.37,18.5,0],'pelvis'); rig.bone(shin,[s*2.72,10.5,0],leg); rig.bone(foot,[s*2.94,1.35,0],shin);
    rig.loft(`trouser_thigh_${side}`,leg,[[s*2.71,10.15,.05,2.2,1.92],[s*2.75,12.7,.08,2.72,2.21],[s*2.3,16.3,.2,2.8,2.31],[s*2.14,19.3,.17,2.26,2.1]],'pants',12);
    rig.loft(`trouser_shin_${side}`,shin,[[s*2.95,1.25,.1,2.0,1.65],[s*2.93,2.7,.1,2.5,2.03],[s*2.8,5.8,0,2.38,1.96],[s*2.71,8.15,.05,2.15,1.83],[s*2.71,10.65,.05,2.27,1.96]],'pants',12,0,-.5);
    rig.ribbon(`pants_fold_${side}`,shin,[[s*3.85,9.9,-1.62],[s*4.1,6.8,-1.91],[s*3.5,3.9,-2.03],[s*3.7,1.7,-1.52]],.16,'pantsLight');
    rig.ellipsoid(`foot_body_${side}`,foot,[s*2.95,.68,-.72],[1.58,.68,2.28],'skin',10,4);
    for(let i=0;i<5;i++) {
      const x=s*(1.73+i*.60),z=-2.48+i*.12;
      rig.ellipsoid(`toe_${side}_${i}`,foot,[x,.4,z],[.38-i*.018,.39-i*.025,.71-i*.055],'skin',6,4);
      rig.ellipsoid(`toenail_${side}_${i}`,foot,[x,.67-i*.024,z-.2],[.21-i*.012,.035,.29-i*.018],'nail',6,3);
    }
    for(const id of rig.bones.get(foot).children.filter(c=>typeof c==='string')) {
      const mesh=rig.elements.find(e=>e.uuid===id);
      for(const key of Object.keys(mesh.vertices)) {
        const world=add(mesh.origin,mesh.vertices[key]);
        mesh.vertices[key]=sub([s*2.95+(world[0]-s*2.95)*1.22,world[1]*1.12,world[2]*1.16],mesh.origin);
      }
    }
  }
  rig.bone('belt',[0,20,0],'pelvis'); rig.bone('belt_tail_left',[.5,19.9,-2.44],'belt'); rig.bone('belt_tail_right',[-.5,19.9,-2.46],'belt');
  rig.loft('red_sash','belt',[[0,19.4,0,4.3,2.43],[0,20.0,0,4.34,2.48],[0,20.8,0,4.28,2.41]],'belt',16);
  rig.loft('waistband','belt',[[0,20.8,0,4.14,2.25],[0,21.15,0,4.02,2.17]],'pants',16);
  rig.ellipsoid('sash_knot','belt',[0,19.9,-2.46],[.9,.65,.62],'beltDark',8,4,20);
  for(const [name,s,length] of [['left',1,7.6],['right',-1,5.8]]) {
    const v=[],f=[];
    for(let i=0;i<5;i++) {
      const t=i/4,y=19.8-length*t,cx=s*(.42+t*.8),z=-2.56-.22*Math.sin(t*5),w=.86*(1-t*.35);
      v.push([cx-w,y,z],[cx,y-.12,z-.19],[cx+w,y-.3,z]);
      if(i) {const a=i*3;f.push([a-3,a,a+1],[a-3,a+1,a-2],[a-2,a+1,a+2],[a-2,a+2,a-1]);}
    }
    rig.mesh(`sash_tail_${name}`,`belt_tail_${name}`,v,f,i=>i%3===0?'beltDark':'belt');
  }
}

export function thirdPerson() {
  const rig=new Rig('jason'); torso(rig); head(rig); arms(rig,'left'); arms(rig,'right'); legsAndBelt(rig);
  rig.bone('socket_ground',[0,0,0],'root'); rig.bone('socket_chest',[0,26,-3.2],'chest');
  return rig;
}

export function firstPerson(source) {
  const rig=new Rig('jason_first_person');rig.bone('root',[0,0,0]);rig.bone('camera_anchor',[0,0,0],'root');
  // Camera looks -Z; bind fists stay outside a central 12-degree cone.
  for(const side of ['left','right']) {
    const s=side==='left'?1:-1, original=source.bones.get(`arm_${side}`);
    const transform=p=>add(mul(sub(p,original.origin),.95),[s*4.4,-1.4,-2.5]);
    function copy(b,parent) {
      rig.bone(b.name,transform(b.origin),parent);
      for(const child of b.children) {
        if(typeof child==='object') copy(child,b.name);
        else {
          const e=source.elements.find(e=>e.uuid===child);
          if(/^(deltoid|biceps|triceps|upper_arm_core)_/.test(e.name)) continue;
          const keys=Object.keys(e.vertices),positions=keys.map(k=>transform(add(e.origin,e.vertices[k])));
          const faces=Object.values(e.faces),polys=faces.map(f=>f.vertices.map(k=>keys.indexOf(k)));
          const copy=rig.mesh(e.name,b.name,positions,polys,'skin');
          // Retain the same per-face UV/material assignments as the third-person hands.
          Object.values(copy.faces).forEach((f,i)=>{ f.uv=Object.fromEntries(f.vertices.map((k,j)=>[k,faces[i].uv[faces[i].vertices[j]]])); });
        }
      }
    }
    copy(original,'camera_anchor');
  }
  return rig;
}
