import {createHash} from 'node:crypto';
import {deflateSync} from 'node:zlib';

export const uuid = s => {const h=createHash('sha256').update('zeravorn.lane:'+s).digest('hex');return `${h.slice(0,8)}-${h.slice(8,12)}-4${h.slice(13,16)}-a${h.slice(17,20)}-${h.slice(20,32)}`;};
export const sub=(a,b)=>a.map((v,i)=>v-b[i]);
export const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
export const norm=a=>a.map(v=>v/(Math.hypot(...a)||1));
export const dot=(a,b)=>a.reduce((s,v,i)=>s+v*b[i],0);
export const identity=()=>[1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1];
export function multiply(a,b){const c=Array(16).fill(0);for(let r=0;r<4;r++)for(let k=0;k<4;k++)for(let j=0;j<4;j++)c[r*4+j]+=a[r*4+k]*b[k*4+j];return c;}
export const transform=(m,v)=>[0,1,2].map(r=>m[r*4]*v[0]+m[r*4+1]*v[1]+m[r*4+2]*v[2]+m[r*4+3]);
export function rotation(r){const [x,y,z]=r.map(v=>v*Math.PI/180),cx=Math.cos(x),sx=Math.sin(x),cy=Math.cos(y),sy=Math.sin(y),cz=Math.cos(z),sz=Math.sin(z);return [cz*cy,cz*sy*sx-sz*cx,cz*sy*cx+sz*sx,0,sz*cy,sz*sy*sx+cz*cx,sz*sy*cx-cz*sx,0,-sy,cy*sx,cy*cx,0,0,0,0,1];}
const colors={team:'#284e9d',teamLight:'#507fc5',teamDark:'#16243e',steel:'#8896ae',edge:'#c1c9d5',dark:'#303644',gold:'#bd9460',goldDark:'#795d3f',skin:'#a399a4',skinDark:'#706475',leather:'#55413b',wood:'#776049',crystal:'#42b8ff',glow:'#b5f0ff',black:'#151a25',cloth:'#314975'};
export const materials=Object.keys(colors);
export function atlas(team='blue',emissive=false){
 const palette={...colors,...(team==='red'?{team:'#963347',teamLight:'#d04c60',teamDark:'#331c2d',crystal:'#ff345e',glow:'#ffb4c5',cloth:'#6f263c'}:{})};
 const p=new Uint8Array(128*128*4);
 for(let i=0;i<16;i++)for(let y=0;y<32;y++)for(let x=0;x<32;x++){
  const key=materials[i],c=[1,3,5].map(j=>parseInt(palette[key].slice(j,j+2),16));
  const grain=((Math.floor(x/2)*17+Math.floor(y/2)*31+i*13)%11-5)*.008;
  const edge=(x<2||y<2)?.12:(x>29||y>29)?-.16:0;
  const scratch=['steel','team','gold'].includes(key)&&((x*3+y*7+i)%79<2)? .14:0;
  let shade=.91+grain+edge+scratch;
  if(key==='wood')shade+=x%7<2?-.14:.02;
  if(key==='crystal')shade+=x<16-y/3?.18:-.12;
  if(key==='cloth')shade+=(x%4===0||y%4===0)?-.06:0;
  const idx=((Math.floor(i/4)*32+y)*128+i%4*32+x)*4,glow=['crystal','glow'].includes(key);
  for(let j=0;j<3;j++)p[idx+j]=emissive?(glow?c[j]:0):Math.max(0,Math.min(255,Math.round(c[j]*shade)));
  p[idx+3]=emissive&&!glow?0:255;
 }
 return p;
}
const crcTable=Array.from({length:256},(_,n)=>{for(let i=0;i<8;i++)n=n&1?0xedb88320^(n>>>1):n>>>1;return n>>>0;});
export function png(w,h,p){const crc=b=>{let c=0xffffffff;for(const v of b)c=crcTable[(c^v)&255]^(c>>>8);return(c^0xffffffff)>>>0;};const chunk=(t,d)=>{const b=Buffer.alloc(d.length+12),s=Buffer.from(t);b.writeUInt32BE(d.length);s.copy(b,4);d.copy(b,8);b.writeUInt32BE(crc(Buffer.concat([s,d])),d.length+8);return b;};const hd=Buffer.alloc(13);hd.writeUInt32BE(w);hd.writeUInt32BE(h,4);hd[8]=8;hd[9]=6;const raw=Buffer.alloc(h*(w*4+1));for(let y=0;y<h;y++)Buffer.from(p.buffer,p.byteOffset+y*w*4,w*4).copy(raw,y*(w*4+1)+1);return Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',hd),chunk('IDAT',deflateSync(raw)),chunk('IEND',Buffer.alloc(0))]);}

export class Model {
 constructor(name){this.name=name;this.bones={};this.outliner=[];this.elements=[];this.animations=[];}
 bone(name,parent,origin=[0,0,0]){if(this.bones[name])throw Error(name);const b={name,uuid:uuid(this.name+name),origin,rotation:[0,0,0],export:true,isOpen:true,children:[]};this.bones[name]=b;(parent?this.bones[parent].children:this.outliner).push(b);return b;}
 box(name,bone,c,size,mat,rot=[0,0,0]){
  const i=materials.indexOf(mat);if(i<0)throw Error(mat);const x=i%4*32,y=Math.floor(i/4)*32;
  const faces={};for(const f of ['north','east','south','west','up','down'])faces[f]={uv:[x+.5,y+.5,x+31.5,y+31.5],texture:0};
  const e={name,uuid:uuid(this.name+':'+name),type:'cube',from:c.map((v,i)=>v-size[i]/2),to:c.map((v,i)=>v+size[i]/2),origin:c,rotation:rot,box_uv:false,faces,visibility:true,export:true,zeravorn_material:mat};
  this.elements.push(e);this.bones[bone].children.push(e.uuid);return e;
 }
 rod(name,bone,a,b,width,depth,mat){const d=sub(b,a),length=Math.hypot(...d),rot=[Math.atan2(d[2],Math.hypot(d[0],d[1]))*180/Math.PI,0,-Math.atan2(d[0],d[1])*180/Math.PI];return this.box(name,bone,a.map((v,i)=>(v+b[i])/2),[width,length,depth],mat,rot);}
 save(){return {meta:{format_version:'4.10',model_format:'free',box_uv:false},name:this.name,model_identifier:'zeravorn:'+this.name,resolution:{width:128,height:128},elements:this.elements,outliner:this.outliner,textures:['blue','red'].map((team,i)=>({name:this.name+'_'+team+'.png',id:String(i),uuid:uuid(this.name+team),mode:'bitmap',source:'data:image/png;base64,'+png(128,128,atlas(team)).toString('base64'),internal:true,saved:true,width:128,height:128,uv_width:128,uv_height:128})),animations:this.animations,zeravorn:{units_per_block:16,front:'-Z',team_variant:'Apply blue or red texture to all faces; shared UVs and geometry',authority:'presentation_only'}};}
}
export function hierarchy(model){const bones=[],parents={},owners={};const visit=(nodes,parent=null)=>{for(const n of nodes)if(typeof n==='string')owners[n]=parent;else {bones.push(n);parents[n.uuid]=parent;visit(n.children,n);}};visit(model.outliner);return {bones,parents,owners};}
export function sample(a,c,t){const keys=(a?.keyframes||[]).filter(k=>k.channel===c).sort((a,b)=>a.time-b.time),vec=k=>['x','y','z'].map(n=>Number(k.data_points[0][n]));if(!keys.length)return [0,0,0];if(t<=keys[0].time)return vec(keys[0]);for(let i=1;i<keys.length;i++)if(t<=keys[i].time){const u=(t-keys[i-1].time)/(keys[i].time-keys[i-1].time);return vec(keys[i-1]).map((v,j)=>v+(vec(keys[i])[j]-v)*u);}return vec(keys.at(-1));}
export function boneMatrices(model,clipName='',time=0){const {bones,parents}=hierarchy(model),clip=model.animations.find(a=>a.name===clipName),m={};for(const b of bones){const parent=parents[b.uuid],a=clip?.animators[b.uuid],r=rotation(sample(a,'rotation',time).map((v,i)=>v*(i<2?-1:1))),p=sample(a,'position',time);for(let i=0;i<3;i++)r[i*4+3]=b.origin[i]-(parent?.origin[i]||0)+p[i]*(i===0?-1:1);m[b.uuid]=parent?multiply(m[parent.uuid],r):r;}return m;}
export const faceIndices={north:[1,0,3,2],east:[5,1,2,6],south:[4,5,6,7],west:[0,4,7,3],up:[3,7,6,2],down:[0,1,5,4]};
export function cubeVertices(e){const [x,y,z]=e.from,[X,Y,Z]=e.to,r=rotation(e.rotation);return [[x,y,z],[X,y,z],[X,Y,z],[x,Y,z],[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]].map(v=>transform(r,sub(v,e.origin)).map((v,i)=>v+e.origin[i]));}
export function triangles(model,clip='',time=0){const {owners}=hierarchy(model),m=boneMatrices(model,clip,time),result=[];for(const e of model.elements){const b=owners[e.uuid],verts=cubeVertices(e).map(v=>transform(m[b.uuid],sub(v,b.origin)));for(const [face,ids] of Object.entries(faceIndices)){const [u,v,U,V]=e.faces[face].uv,uv=[[u,v],[U,v],[U,V],[u,V]];for(const ix of [[0,1,2],[0,2,3]])result.push({points:ix.map(i=>verts[ids[i]]),uv:ix.map(i=>uv[i]),name:e.name,mat:e.zeravorn_material});}}return result;}
