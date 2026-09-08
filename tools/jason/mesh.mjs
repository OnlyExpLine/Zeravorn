import { createHash } from 'node:crypto';
import { deflateSync } from 'node:zlib';

export const add = (a, b) => a.map((x, i) => x + b[i]);
export const sub = (a, b) => a.map((x, i) => x - b[i]);
export const mul = (a, s) => a.map(x => x * s);
export const dot = (a, b) => a.reduce((s, x, i) => s + x * b[i], 0);
export const cross = (a, b) => [a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]];
export const unit = a => mul(a, 1 / (Math.hypot(...a) || 1));
export const round = x => Math.round(x * 100000) / 100000;
export function uuid(name) {
  const s = createHash('sha256').update(`zeravorn:jason:v2:${name}`).digest('hex');
  return `${s.slice(0,8)}-${s.slice(8,12)}-5${s.slice(13,16)}-a${s.slice(17,20)}-${s.slice(20,32)}`;
}
export function noise(name) {
  return createHash('sha256').update(name).digest().readUInt32LE() / 0xffffffff;
}

// Palette ramp atlas: a face selects a material row and a stable shade column.
// Fine-grained original geometry carries the anatomical/material detail.
export const palette = {
  skin: [166,113,76], skinLight: [186,136,93], skinDark: [118,76,50],
  pants: [46,43,39], pantsLight: [63,58,50], belt: [137,29,31],
  beltDark: [88,18,23], bandage: [185,175,149], bandageEdge: [131,124,106],
  hair: [28,27,25], hairLight: [44,41,35], scar: [112,69,51],
  scarLight: [195,140,104], eyeWhite: [194,181,151], iris: [89,62,31],
  black: [25,19,18], lip: [108,70,53], nail: [150,112,82],
};
export const ATLAS_SIZE = 256;
export function faceColor(material, shade = 8) {
  const base = palette[material];
  return base.map(c => Math.max(0, Math.min(255, Math.round(c * (0.80 + shade * 0.025)))));
}
export function uvFor(material, shade, n) {
  const row = Object.keys(palette).indexOf(material);
  if (row < 0) throw Error(`Unknown material ${material}`);
  const x = shade * 16, y = row * 12;
  return [[x+3,y+3], [x+12,y+3], [x+12,y+9], [x+3,y+9]].slice(0,n);
}
const crcTable = Uint32Array.from({length:256}, (_, n) => {
  for (let i=0;i<8;i++) n = n&1 ? 0xedb88320^(n>>>1) : n>>>1;
  return n>>>0;
});
function chunk(type, data) {
  const header = Buffer.alloc(8); header.writeUInt32BE(data.length); header.write(type,4);
  let crc = 0xffffffff;
  for (const x of Buffer.concat([header.subarray(4),data])) crc = crcTable[(crc^x)&255]^(crc>>>8);
  const tail = Buffer.alloc(4); tail.writeUInt32BE((crc^0xffffffff)>>>0);
  return Buffer.concat([header,data,tail]);
}
export function png(width, height, rgba) {
  const scan = Buffer.alloc((width*4+1)*height), header = Buffer.alloc(13);
  for (let y=0;y<height;y++) Buffer.from(rgba.buffer,rgba.byteOffset+y*width*4,width*4).copy(scan,y*(width*4+1)+1);
  header.writeUInt32BE(width); header.writeUInt32BE(height,4); header[8]=8; header[9]=6;
  return Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',header),chunk('IDAT',deflateSync(scan)),chunk('IEND',Buffer.alloc(0))]);
}
export function atlas() {
  const p = new Uint8Array(ATLAS_SIZE*ATLAS_SIZE*4), materials = Object.keys(palette);
  for (let y=0;y<ATLAS_SIZE;y++) for (let x=0;x<ATLAS_SIZE;x++) {
    const color = faceColor(materials[Math.min(materials.length-1,Math.floor(y/12))],Math.floor(x/16));
    p.set([...color,255],(y*ATLAS_SIZE+x)*4);
  }
  return png(ATLAS_SIZE,ATLAS_SIZE,p);
}

export class Rig {
  constructor(name) { this.name=name; this.elements=[]; this.bones=new Map(); this.outliner=[]; }
  bone(name, origin, parent) {
    if (this.bones.has(name)) throw Error(`Duplicate bone ${name}`);
    const b = {name,uuid:uuid(`${this.name}/bone/${name}`),origin,rotation:[0,0,0],export:true,visibility:true,children:[]};
    this.bones.set(name,b);
    (parent ? this.bones.get(parent).children : this.outliner).push(b);
    return b;
  }
  mesh(name, bone, positions, polygons, material, options={}) {
    const origin = this.bones.get(bone).origin;
    const vertices = Object.fromEntries(positions.map((p,i)=>[`v${i}`,sub(p,origin).map(round)]));
    const faces = {};
    polygons.forEach((poly,i) => {
      const indices=poly.slice(), points=indices.map(j=>positions[j]);
      const normal=cross(sub(points[1],points[0]),sub(points[2],points[0]));
      if (Math.hypot(...normal)<0.0000001) return;
      const shade=Math.round(name==='torso_sculpt'||name==='face_planes'?7+noise(`${name}/${i}`)*2:6+noise(`${name}/${i}`)*4);
      const color=typeof material==='function'?material(i,points):material;
      const keys=indices.map(j=>`v${j}`), uv=uvFor(color,shade,keys.length);
      faces[`f${i}`]={vertices:keys,uv:Object.fromEntries(keys.map((k,j)=>[k,uv[j]])),texture:0};
    });
    const mesh={name,uuid:uuid(`${this.name}/mesh/${name}`),type:'mesh',origin:origin.slice(),rotation:[0,0,0],vertices,faces,export:true,visibility:true,locked:false,...options};
    this.elements.push(mesh); this.bones.get(bone).children.push(mesh.uuid);
    return mesh;
  }
  // Cross sections about Y; consistently outward triangulated faces.
  loft(name,bone,rings,material,segments=12,phase=0,ragged=0) {
    const vertices=[];
    rings.forEach(([cx,y,cz,rx,rz],r)=>{
      for(let i=0;i<segments;i++) {
        const a=2*Math.PI*i/segments+phase;
        const fold=material==='pants'||material==='pantsLight' ? 1+0.09*Math.cos(i*2.5+r*.65) : 1;
        vertices.push([cx+Math.cos(a)*rx*fold,y+(r===0?ragged*(i%2):0),cz+Math.sin(a)*rz*fold]);
      }
    });
    const faces=[];
    for(let r=0;r<rings.length-1;r++) for(let i=0;i<segments;i++) {
      const a=r*segments+i,b=r*segments+(i+1)%segments,c=b+segments,d=a+segments;
      if ((r+i)%2) faces.push([a,d,b],[b,d,c]); else faces.push([a,d,c],[a,c,b]);
    }
    const cap=(r,top)=>{
      const center=vertices.length; vertices.push(rings[r].slice(0,3));
      for(let i=0;i<segments;i++) faces.push(top?[center,r*segments+(i+1)%segments,r*segments+i]:[center,r*segments+i,r*segments+(i+1)%segments]);
    };
    cap(0,false);cap(rings.length-1,true);
    return this.mesh(name,bone,vertices,faces,material);
  }
  ellipsoid(name,bone,center,radii,material,segments=10,latitudes=5,tilt=0) {
    const vertices=[],faces=[];
    // Offset intermediate longitudes gives less mechanical, sculpted triangulation.
    for(let r=1;r<latitudes;r++) {
      const phi=Math.PI*r/latitudes;
      for(let i=0;i<segments;i++) {
        const a=2*Math.PI*(i+(r%2)*.18)/segments;
        let x=Math.cos(a)*Math.sin(phi)*radii[0], y=Math.cos(phi)*radii[1];
        const z=Math.sin(a)*Math.sin(phi)*radii[2];
        const t=tilt*Math.PI/180; [x,y]=[x*Math.cos(t)-y*Math.sin(t),x*Math.sin(t)+y*Math.cos(t)];
        vertices.push(add(center,[x,y,z]));
      }
    }
    for(let r=0;r<latitudes-2;r++) for(let i=0;i<segments;i++) {
      const a=r*segments+i,b=r*segments+(i+1)%segments,c=b+segments,d=a+segments;
      faces.push([a,b,c],[a,c,d]);
    }
    const t=tilt*Math.PI/180,top=vertices.length;
    vertices.push(add(center,[-Math.sin(t)*radii[1],Math.cos(t)*radii[1],0]));
    const bottom=vertices.length;vertices.push(add(center,[Math.sin(t)*radii[1],-Math.cos(t)*radii[1],0]));
    for(let i=0;i<segments;i++) {faces.push([top,(i+1)%segments,i]);const a=(latitudes-2)*segments;faces.push([bottom,a+i,a+(i+1)%segments]);}
    return this.mesh(name,bone,vertices,faces,material);
  }
  ribbon(name,bone,path,width,material) {
    const v=[],f=[];
    path.forEach((p,i)=>{
      const direction=sub(path[Math.min(i+1,path.length-1)],path[Math.max(0,i-1)]);
      const side=mul(unit([-direction[1],direction[0],0]),width/2);
      v.push(add(p,side),sub(p,side));
      if(i) f.push([2*i-2,2*i,2*i+1],[2*i-2,2*i+1,2*i-1]);
    });
    return this.mesh(name,bone,v,f,material);
  }
}
