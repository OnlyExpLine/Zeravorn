// Authoring primitives adapted from the project Amelia pipeline; Shelianer geometry is original.
import {createHash} from 'node:crypto';
import {deflateSync} from 'node:zlib';
export const uuid = name => { const h=createHash('sha256').update('zeravorn.shelianer.v1:'+name).digest('hex'); return `${h.slice(0,8)}-${h.slice(8,12)}-4${h.slice(13,16)}-a${h.slice(17,20)}-${h.slice(20,32)}`; };
export const add=(a,b)=>a.map((v,i)=>v+b[i]), sub=(a,b)=>a.map((v,i)=>v-b[i]), mul=(a,s)=>a.map(v=>v*s);
export const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
export const dot=(a,b)=>a.reduce((s,v,i)=>s+v*b[i],0), norm=a=>mul(a,1/(Math.hypot(...a)||1));
export const mix=(a,b,t)=>a.map((v,i)=>v+(b[i]-v)*t);
export const palette={skin:'#b99ba9',skinShade:'#806878',hair:'#201b2c',hairLight:'#393044',hairShade:'#17131f',white:'#292632',whiteLight:'#494151',blue:'#653097',blueLight:'#9954ce',navy:'#181520',cloth:'#39204f',gold:'#92735c',silver:'#a29bab',ice:'#a341fa',iceLight:'#ecc0ff',iceDark:'#6520ae',eye:'#c770ff',lash:'#100e18',lip:'#655363',wood:'#463039',fur:'#73616b'};
export const materials=Object.keys(palette);
export const rgb=hex=>[1,3,5].map(i=>parseInt(hex.slice(i,i+2),16));
export function atlas(emissive=false){
 const pixels=new Uint8Array(256*256*4);
 for(let m=0;m<materials.length;m++)for(let variant=0;variant<8;variant++){
  const idx=m*8+variant, ox=idx%16*16, oy=Math.floor(idx/16)*16, key=materials[m], color=rgb(palette[key]);
  for(let y=0;y<16;y++)for(let x=0;x<16;x++){
   const hash=((x*71+y*137+variant*29+m*11)%17-8)/240;
   let shade=.90+variant*.025+hash;
   if(key.startsWith('hair'))shade+=((x+variant*3)%5===0?.075:0)-y*.002;
   if(['blue','cloth','navy','white'].includes(key))shade+=((x%4===0||y%4===0)?-.035:0);
   if(key.startsWith('ice'))shade+=x<8-y*.25?.13:-.09;
   const glow=key.startsWith('ice')?1:key==='eye'?.15:0, p=((oy+y)*256+ox+x)*4;
   for(let c=0;c<3;c++)pixels[p+c]=emissive?Math.round(color[c]*glow):Math.max(0,Math.min(255,Math.round(color[c]*shade)));
   pixels[p+3]=emissive?(glow?255:0):255;
  }
 }
 return pixels;
}
const crcTable=Array.from({length:256},(_,n)=>{for(let k=0;k<8;k++)n=n&1?0xedb88320^(n>>>1):n>>>1;return n>>>0;});
const crc=b=>{let c=0xffffffff;for(const v of b)c=crcTable[(c^v)&255]^(c>>>8);return(c^0xffffffff)>>>0;};
export function png(w,h,pixels){
 const chunk=(type,data)=>{let t=Buffer.from(type),b=Buffer.alloc(12+data.length);b.writeUInt32BE(data.length);t.copy(b,4);data.copy(b,8);b.writeUInt32BE(crc(Buffer.concat([t,data])),8+data.length);return b;};
 const head=Buffer.alloc(13);head.writeUInt32BE(w);head.writeUInt32BE(h,4);head[8]=8;head[9]=6;
 const raw=Buffer.alloc(h*(w*4+1));for(let y=0;y<h;y++)Buffer.from(pixels.buffer,pixels.byteOffset+y*w*4,w*4).copy(raw,y*(w*4+1)+1);
 return Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',head),chunk('IDAT',deflateSync(raw)),chunk('IEND',Buffer.alloc(0))]);
}
export class Model {
 constructor(name){this.name=name;this.elements=[];this.bones={};this.outliner=[];this.animations=[];}
 bone(name,parent,origin=[0,0,0]){if(this.bones[name])throw Error('Duplicate bone '+name);const b={name,uuid:uuid(this.name+'.bone.'+name),origin,rotation:[0,0,0],export:true,isOpen:true,children:[]};this.bones[name]=b;(parent?this.bones[parent].children:this.outliner).push(b);return b;}
 mesh(name,bone,vertices,polygons,material='white'){
  const verts=Object.fromEntries(vertices.map((v,i)=>['v'+i,v.map(n=>+n.toFixed(5))]));
  const faces={};polygons.forEach((poly,i)=>{const mat=Array.isArray(material)?material[i%material.length]:material;const idx=materials.indexOf(mat)*8+(i*3+name.length)%8; if(idx<0)throw Error(mat);const x=idx%16*16,y=Math.floor(idx/16)*16;const uv=[[x+.5,y+.5],[x+15.5,y+.5],[x+15.5,y+15.5],[x+.5,y+15.5]];faces['f'+i]={vertices:poly.map(j=>'v'+j),uv:Object.fromEntries(poly.map((j,k)=>['v'+j,uv[k%4]])),texture:0};});
  const el={name,uuid:uuid(this.name+'.mesh.'+name),type:'mesh',origin:[0,0,0],rotation:[0,0,0],vertices:verts,faces,visibility:true,export:true};this.elements.push(el);this.bones[bone].children.push(el.uuid);return el;
 }
 // Rows are [centerX,Y,centerZ,halfWidth,halfDepth]; all surfaces are closed.
 loft(name,bone,rows,mat,segments=8,power=1){
  const verts=[];for(const [cx,y,cz,rx,rz] of rows)for(let i=0;i<segments;i++){let a=2*Math.PI*i/segments;let x=Math.sin(a),z=-Math.cos(a);verts.push([cx+rx*Math.sign(x)*Math.abs(x)**power,y,cz+rz*Math.sign(z)*Math.abs(z)**power]);}
  const faces=[];for(let r=0;r<rows.length-1;r++)for(let i=0;i<segments;i++){const j=(i+1)%segments;faces.push([r*segments+i,r*segments+j,(r+1)*segments+j,(r+1)*segments+i]);}
  // Fan caps use explicit centers for portable triangulation.
  for(const [r,flip] of [[0,true],[rows.length-1,false]]){const c=verts.length;verts.push(rows[r].slice(0,3));for(let i=0;i<segments;i++){const a=r*segments+i,b=r*segments+(i+1)%segments;faces.push(flip?[c,b,a]:[c,a,b]);}}
  return this.mesh(name,bone,verts,faces.map(f=>f.reverse()),mat);
 }
 box(name,bone,c,size,mat){const [x,y,z]=c,[w,h,d]=size;return this.loft(name,bone,[[x,y-h/2,z,w/2,d/2],[x,y+h/2,z,w/2,d/2]],mat,8,.20);}
 ribbon(name,bone,path,width,depth,mat){return this.loft(name,bone,path.map((p,i)=>[...p,(Array.isArray(width)?width[i]:width)/2,(Array.isArray(depth)?depth[i]:depth)/2]),mat,8,.35);}
 rod(name,bone,a,b,r,mat,segments=6){const axis=norm(sub(b,a)),u=norm(cross(axis,Math.abs(axis[1])>.9?[1,0,0]:[0,1,0])),v=cross(axis,u);const verts=[];for(const p of [a,b])for(let i=0;i<segments;i++)verts.push(add(p,add(mul(u,r*Math.cos(i*2*Math.PI/segments)),mul(v,r*Math.sin(i*2*Math.PI/segments)))));const f=[];for(let i=0;i<segments;i++)f.push([i,(i+1)%segments,(i+1)%segments+segments,i+segments]);for(const [p,rev] of [[0,true],[segments,false]])for(let i=1;i<segments-1;i++)f.push(rev?[p,p+i+1,p+i]:[p,p+i,p+i+1]);return this.mesh(name,bone,verts,f,mat);}
 crystal(name,bone,base,tip,width,depth=width*.7){const mid=mix(base,tip,.42),ax=norm(sub(tip,base)),u=norm(cross(ax,[0,0,1])),v=cross(ax,u);const verts=[base,tip,add(mid,mul(u,width/2)),add(mid,mul(v,depth/2)),add(mid,mul(u,-width/2)),add(mid,mul(v,-depth/2))];const f=[];for(let i=0;i<4;i++){f.push([0,2+(i+1)%4,2+i]);f.push([1,2+i,2+(i+1)%4]);}return this.mesh(name,bone,verts,f,['ice','iceLight','iceDark','ice','iceLight','ice']);}
 save(texture){return {meta:{format_version:'4.10',model_format:'free',box_uv:false},name:this.name,model_identifier:'zeravorn:'+this.name,resolution:{width:256,height:256},elements:this.elements,outliner:this.outliner,textures:[{name:this.name+'.png',id:'0',uuid:uuid(this.name+'.texture'),mode:'bitmap',source:'data:image/png;base64,'+texture.toString('base64'),internal:true,saved:true,width:256,height:256,uv_width:256,uv_height:256,render_mode:'default'}],animations:this.animations};}
}
