import {Model} from './core.mjs';
import {refine} from './refine.mjs';
export function dagger(m,b,x,y,z,s=1,label='dagger'){
 m.rod(label+'_grip',b,[x,y,z],[x,y-1.9*s,z],.25*s,'wood');
 for(let i=0;i<4;i++)m.box(label+'_wrap'+i,b,[x,y-i*.43*s,z],[.57*s,.12*s,.57*s],'gold');
 m.rod(label+'_guard',b,[x-1*s,y-1.8*s,z],[x+.8*s,y-1.8*s,z],.17*s,'gold');
 m.crystal(label+'_gem',b,[x,y-1.35*s,z-.32*s],[x,y-2.2*s,z-.32*s],.5*s);
 m.ribbon(label+'_blade',b,[[x,y-2*s,z],[x+.2*s,y-3*s,z],[x+1*s,y-4.4*s,z],[x+1.7*s,y-5.9*s,z]],[.8*s,1.15*s,.65*s,.015],.25*s,['silver','whiteLight','iceDark']);
 m.ribbon(label+'_edge',b,[[x+.38*s,y-2.1*s,z-.14*s],[x+.74*s,y-3.1*s,z-.14*s],[x+1.3*s,y-4.45*s,z-.14*s],[x+1.7*s,y-5.9*s,z-.14*s]],[.14*s,.24*s,.2*s,.012],.035,'ice');
}
export function character(){
 const m=new Model('shelianer');m.bone('root',null);m.bone('body','root',[0,15,0]);m.bone('torso','body',[0,17,0]);m.bone('head','torso',[0,24,0]);
 m.loft('fitted_tunic','torso',[[0,15,0,2.4,1.5],[0,18,0,2.05,1.35],[0,22,0,3.25,1.8],[0,24,0,2.6,1.5]],'navy');
 for(let i=0;i<4;i++)m.loft('abdominal_armor'+i,'torso',[[0,17+i*1.3,-.2,2.25+i*.16,1.45],[0,18+i*1.3,-.2,2.3+i*.16,1.5]],['white','whiteLight','navy']);
 m.loft('belt','body',[[0,15.5,0,2.65,1.65],[0,16.4,0,2.65,1.65]],'wood');m.box('buckle','body',[0,16,-1.8],[1.15,.9,.25],'gold');m.box('buckle_inset','body',[0,16,-1.96],[.72,.5,.08],'navy');
 m.ribbon('diagonal_harness','torso',[[-2.8,23,-1.55],[0,20,-1.94],[2,17,-1.5]],.65,.18,'wood');
 for(let i=0;i<6;i++)m.box('harness_stud'+i,'torso',[-2.5+i*.76,22.7-i*.9,-2.02],[.18,.18,.12],'gold');
 m.loft('head_under_hood','head',[[0,24.2,0,1.5,1.25],[0,26,0,2,1.6],[0,28.8,0,1.9,1.6]],'skinShade');
 m.loft('mask','head',[[0,24.2,-.35,1.45,1.3],[0,26,-.45,1.95,1.45],[0,26.4,-.35,1.9,1.35]],['navy','white','hair']);
 for(const s of [-1,1]){
 m.box('eye_socket'+s,'head',[s*.9,26.85,-1.59],[1.34,.6,.12],'lash');m.box('eye'+s,'head',[s*.9,26.87,-1.68],[1.06,.29,.06],'skin');m.box('iris'+s,'head',[s*.79,26.87,-1.73],[.35,.29,.05],'eye');m.box('pupil'+s,'head',[s*.79,26.87,-1.77],[.12,.27,.025],'lash');
 m.rod('brow'+s,'head',[s*.3,27.18,-1.75],[s*1.5,27.45,-1.6],.13,'hair');
 m.ribbon('hair_lock'+s,'head',[[s*.8,29,-1.5],[s*1.65,27.9,-1.8],[s*1.62,25.7,-1.65]],[1.3,.85,.03],.28,'hair');
 // Open hood arch, leaving the face visible from the front.
 m.ribbon('hood_rim'+s,'head',[[0,31.3,-.5],[s*1.65,30.6,-1.15],[s*2.65,28.5,-1.6],[s*2.9,25.4,-1.35],[s*2.15,24.4,-.2]],[.7,1.2,1.25,1.1,.7],[1.4,1.6,1.7,1.7,1.2],['cloth','navy','white']);
 m.rod('hood_piping'+s,'head',[s*.7,30.65,-1.88],[s*2.47,27,-2.45],.065,'gold');
 const side=s<0?'left':'right',x=s*4.2;
 m.bone('upper_arm_'+side,'torso',[x,23,0]);m.bone('forearm_'+side,'upper_arm_'+side,[s*4.65,19,0]);m.bone('hand_'+side,'forearm_'+side,[s*4.85,15.6,-.15]);m.bone('dagger_'+side,'hand_'+side,[s*4.85,14.5,-.4]);
 m.ribbon('sleeve_'+side,'upper_arm_'+side,[[x,23,0],[s*4.65,19,0]],[2.4,1.75],2,'navy');m.ribbon('forearm_'+side,'forearm_'+side,[[s*4.65,19,0],[s*4.85,15.7,-.15]],[1.8,1.3],1.65,'wood');
 for(let j=0;j<3;j++){
 m.loft('pauldron_'+side+j,'upper_arm_'+side,[[x+s*j*.15,21.2+j*.8,0,1.45,1.55],[x,22.2+j*.8,0,1.65,1.65],[x,22.7+j*.8,0,.65,.85]],['white','whiteLight','navy']);
 m.rod('pauldron_rim_'+side+j,'upper_arm_'+side,[x-1.15,21.3+j*.8,-1.05],[x+1.15,21.3+j*.8,-1.05],.075,'gold');
 m.box('bracer_plate_'+side+j,'forearm_'+side,[s*4.8,16.5+j*.85,-.85],[1.55,.65,.3],'whiteLight');
 }
 m.crystal('shoulder_spike_'+side,'upper_arm_'+side,[s*4.8,24,.2],[s*6.15,24.6,.1],.65);
 m.box('glove_'+side,'hand_'+side,[s*4.9,14.8,-.25],[1.35,1.65,1.35],'navy');for(let j=0;j<3;j++)m.box('knuckle_'+side+j,'hand_'+side,[s*4.45+s*j*.35,14.9,-.99],[.24,.35,.12],'gold');
 dagger(m,'dagger_'+side,s*5.15,14.6,-.75,1,'dagger_'+side);
 m.bone('leg_'+side,'body',[s*1.45,15,0]);m.bone('shin_'+side,'leg_'+side,[s*1.5,8,0]);m.bone('foot_'+side,'shin_'+side,[s*1.5,2,0]);
 m.ribbon('trousers_'+side,'leg_'+side,[[s*1.4,15,0],[s*1.6,8,0]],[2.4,1.7],2.5,'navy');m.ribbon('shin_armor_'+side,'shin_'+side,[[s*1.6,8,0],[s*1.5,2,0]],[1.9,1.4],1.9,['white','navy']);m.box('boot_'+side,'foot_'+side,[s*1.5,1,-.55],[2,2,3],'wood');
 for(let j=0;j<3;j++){m.box('leg_strap_'+side+j,'shin_'+side,[s*1.55,2.6+j*1.6,-.1],[1.85,.35,2.05],'wood');m.box('boot_rivet_'+side+j,'shin_'+side,[s*1.55,2.6+j*1.6,-1.17],[.25,.25,.14],'gold');}
 m.crystal('knee_plate_'+side,'shin_'+side,[s*1.6,6.6,-1.14],[s*1.6,8.8,-1.14],1.2); 
 for(let j=0;j<3;j++)dagger(m,'body',s*(2.35+j*.49),15.45-j*.15,-1.15+j*.28,.4,'throwing_'+side+j);
 m.bone('tabard_'+side,'body',[s*1.7,15.5,-1.5]);m.ribbon('front_tattered_'+side,'tabard_'+side,[[s*1.6,15.6,-1.65],[s*2.05,10,-1.85],[s*1.75,5.8+s*.5,-1.65]],[1.6,1.45,.07],.15,['cloth','blue','navy']);
 }
 m.loft('hood_back','head',[[0,24.6,1.2,2.25,.8],[0,28.8,1,2.65,1.6],[0,30.4,.65,1.7,1.25],[0,31.4,.1,.15,.2]],['cloth','navy','white']);
 m.loft('scarf','torso',[[0,23,0,2.15,1.8],[0,24.5,0,2.25,1.85]],['cloth','blue','navy']);
 for(let i=0;i<7;i++){const x=(i-3)*.77,b='cape_'+i;m.bone(b,'torso',[x,23,1.9]);m.ribbon('cape_strip_'+i,b,[[x,23,1.9],[x*1.2,17,2.35],[x*1.45,10,2.8],[x*1.5,3.4+(i%3)*1.1,2.65]],[.95,1.05,1.16,.05],.18,['cloth','navy','blue']);}
 // Symmetric violet sigil, raised on the back of the cape.
 for(const s of [-1,1])for(let j=0;j<3;j++)m.rod('sigil_branch'+s+j,'cape_3',[0,19.4-j*.7,2.63],[s*(1.7-j*.35),21.3-j*.75,2.63],.13,'iceDark');
 m.crystal('sigil_core','cape_3',[0,17.4,2.68],[0,21.7,2.68],.7);
 m.bone('flask_socket','body',[-2.5,15,1]);m.loft('poison_flask','flask_socket',[[-2.6,13.6,.8,.45,.45],[-2.6,14.5,.8,.6,.6],[-2.6,15,.8,.22,.22]],'blue');m.box('flask_cork','flask_socket',[-2.6,15.15,.8],[.5,.3,.5],'wood');
 refine(m);return m;
}
export function firstPerson(){const m=character();m.name='shelianer_first_person';const keep=new Set();function walk(b){keep.add(b.uuid);for(const c of b.children)typeof c==='object'?walk(c):keep.add(c);}for(const s of ['left','right'])walk(m.bones['upper_arm_'+s]);m.elements=m.elements.filter(e=>keep.has(e.uuid));m.outliner=['left','right'].map(s=>m.bones['upper_arm_'+s]);m.bones=Object.fromEntries(Object.entries(m.bones).filter(([,b])=>keep.has(b.uuid)));return m;}
export function weapon(){const m=new Model('shelianer_dagger');m.bone('dagger',null);dagger(m,'dagger',0,4,0,1.6);return m;}
