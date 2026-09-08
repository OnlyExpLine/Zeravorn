import fs from 'node:fs';
import path from 'node:path';
import {png,uuid} from './core.mjs';
import {hierarchy} from './runtime.mjs';
import {render,sheet} from './preview.mjs';

export function extras(model,source,runtime){
 const facesDir=path.join(source,'faces'),ui=path.join(runtime,'ui');fs.mkdirSync(facesDir,{recursive:true});fs.mkdirSync(ui,{recursive:true});
 const states=['neutral','confident','serious','surprised','smile','eyes_closed'];
 const faceModels=[];
 for(const state of states){
  const doc=structuredClone(model),head=hierarchy(doc).bones.find(b=>b.name==='head');doc.name='amelia_face_'+state;doc.animations=[];doc.outliner=[head];const ids=new Set();function walk(b){for(const c of b.children)if(typeof c==='string')ids.add(c);else walk(c);}walk(head);doc.elements=doc.elements.filter(e=>ids.has(e.uuid));
  const alter=(e,sy=1,dy=0,rz=0)=>{const vs=Object.values(e.vertices),cx=vs.reduce((s,p)=>s+p[0],0)/vs.length,cy=vs.reduce((s,p)=>s+p[1],0)/vs.length;for(const v of vs){const x=v[0]-cx,y=(v[1]-cy)*sy;v[0]=cx+x*Math.cos(rz)-y*Math.sin(rz);v[1]=cy+x*Math.sin(rz)+y*Math.cos(rz)+dy;}};
  for(const e of doc.elements){
   if(state==='eyes_closed'&&/^(eye_white|iris|pupil|eye_glint|outer_lash)/.test(e.name))e.visibility=false;
   if(e.name.startsWith('upper_lash')){
    if(state==='eyes_closed')alter(e,.55,-.45);
    if(state==='confident')alter(e,1,0,e.name.endsWith('1')?.09:-.09);
    if(state==='serious')alter(e,1,-.12,e.name.endsWith('1')?-.08:.08);
    if(state==='surprised')alter(e,1,.15);
   }
   if(e.name.startsWith('eyebrow')){
    if(state==='confident')alter(e,1,e.name.endsWith('_1')?.15:0);
    if(state==='serious')alter(e,1,-.15,e.name.endsWith('_1')?-.11:.11);
    if(state==='surprised')alter(e,1,.24);
   }
   if(e.name==='quiet_mouth'){
    if(state==='smile'||state==='confident')alter(e,1,0,.12);
    if(state==='surprised')alter(e,3.2,-.02);
   }
  }
  fs.writeFileSync(path.join(facesDir,doc.name+'.bbmodel'),JSON.stringify(doc));faceModels.push(doc);
  const preview=render(doc,{width:256,height:256,span:9,target:[0,25.3,0]});fs.writeFileSync(path.join(facesDir,state+'.png'),png(256,256,preview.pixels));
 }
 const portrait=render(model,{width:256,height:256,span:11,target:[0,24.0,0],yaw:.08,hideStaff:true});fs.writeFileSync(path.join(ui,'amelia_portrait.png'),png(256,256,portrait.pixels));fs.writeFileSync(path.join(ui,'amelia_hero_icon.png'),png(256,256,portrait.pixels));
 // Original pixel iconography; no third-party image assets or dependencies.
 const size=128;
 for(const type of ['basic','q','e','r']){
  const pixels=new Uint8Array(size*size*4);
  const put=(x,y,c)=>{x=Math.round(x);y=Math.round(y);if(x<0||y<0||x>=size||y>=size)return;pixels.set(c,(y*size+x)*4);};
  for(let y=0;y<size;y++)for(let x=0;x<size;x++){const d=Math.hypot(x-64,y-64)/90;put(x,y,[15+20*(1-d),26+25*(1-d),44+30*(1-d),255]);}
  const line=(a,b,c,width=1)=>{const steps=Math.ceil(Math.hypot(b[0]-a[0],b[1]-a[1]));for(let k=0;k<=steps;k++)for(let dx=-width;dx<=width;dx++)for(let dy=-width;dy<=width;dy++)put(a[0]+(b[0]-a[0])*k/steps+dx,a[1]+(b[1]-a[1])*k/steps+dy,c);};
  const triangle=(a,b,c,color)=>{const area=(b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);for(let y=Math.floor(Math.min(a[1],b[1],c[1]));y<=Math.max(a[1],b[1],c[1]);y++)for(let x=Math.floor(Math.min(a[0],b[0],c[0]));x<=Math.max(a[0],b[0],c[0]);x++){const u=((b[0]-x)*(c[1]-y)-(b[1]-y)*(c[0]-x))/area,v=((c[0]-x)*(a[1]-y)-(c[1]-y)*(a[0]-x))/area;if(u>=0&&v>=0&&u+v<=1)put(x,y,color);}};
  const crystal=(x,y,tx,ty,w)=>{const dx=tx-x,dy=ty-y,len=Math.hypot(dx,dy),mx=x+dx*.3,my=y+dy*.3,left=[mx-dy/len*w,my+dx/len*w],right=[mx+dy/len*w,my-dx/len*w];triangle([x,y],left,[tx,ty],[166,222,249,255]);triangle([x,y],[tx,ty],right,[74,150,207,255]);line([x,y],[tx,ty],[213,243,255,255],0);};
  if(type==='basic'){for(let i=0;i<3;i++)line([17,77+i*9],[54,57+i*7],[72,138,182,255],1);crystal(45,82,111,24,13);}
  if(type==='q'){for(let i=0;i<7;i++){const y=19+i*14;line([18,65],[106,y],[58,105+i*8,168+i*8,255],1);crystal(70,y*.7+20,110,y,3);}crystal(15,78,42,54,7);}
  if(type==='e'){for(let i=0;i<128;i++){const a=i*Math.PI/64;for(const r of [28,42,46])put(64+Math.cos(a)*r,65+Math.sin(a)*r*.65,[120,199,241,255]);}for(let i=0;i<8;i++){const a=i*Math.PI/4;crystal(64+Math.cos(a)*30,65+Math.sin(a)*23,64+Math.cos(a)*51,65+Math.sin(a)*44,4);}line([64,44],[64,84],[213,243,255,255]);line([44,64],[84,64],[213,243,255,255]);}
  if(type==='r'){line([15,105],[115,105],[118,176,204,255]);crystal(61,103,66,15,14);crystal(34,103,20,51,12);crystal(86,103,107,36,11);}
  for(let i=6;i<122;i++){put(i,6,[95,144,179,255]);put(i,121,[95,144,179,255]);put(6,i,[95,144,179,255]);put(121,i,[95,144,179,255]);}
  fs.writeFileSync(path.join(ui,'amelia_'+type+'.png'),png(size,size,pixels));
 }
 fs.writeFileSync(path.join(facesDir,'states.json'),JSON.stringify({ids:states.map(s=>'amelia.face.'+s),runtimeSupport:false,source:'Separate editable head models; neutral is used by the main asset.'},null,2));
}
