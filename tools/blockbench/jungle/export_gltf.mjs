import fs from 'node:fs';
import {hierarchy,sample} from './core.mjs';
import {norm,cross,sub} from './core.mjs';
function quaternion(r){const [x,y,z]=r.map(n=>n*Math.PI/360),cx=Math.cos(x),sx=Math.sin(x),cy=Math.cos(y),sy=Math.sin(y),cz=Math.cos(z),sz=Math.sin(z);return [sx*cy*cz-cx*sy*sz,cx*sy*cz+sx*cy*sz,cx*cy*sz-sx*sy*cz,cx*cy*cz+sx*sy*sz];}
export function exportGltf(model,file){
 const {bones,parents,owners}=hierarchy(model),blocks=[],views=[],accessors=[];let offset=0;
 const accessor=(values,type,size,minmax=false)=>{
  const arr=new Float32Array(values),buf=Buffer.from(arr.buffer),view=views.length;views.push({buffer:0,byteOffset:offset,byteLength:buf.length});blocks.push(buf);offset+=buf.length;
  const a={bufferView:view,componentType:5126,count:values.length/size,type};if(minmax){a.min=Array(size).fill(Infinity);a.max=Array(size).fill(-Infinity);for(let i=0;i<values.length;i++){a.min[i%size]=Math.min(a.min[i%size],values[i]);a.max[i%size]=Math.max(a.max[i%size],values[i]);}}accessors.push(a);return accessors.length-1;
 };
 const nodes=[{name:'units_to_blocks',scale:[1/16,1/16,1/16],children:[]}],indices={},meshes=[];
 for(const b of bones){indices[b.uuid]=nodes.length;nodes.push({name:b.name,translation:b.origin.map((v,i)=>v-(parents[b.uuid]?.origin[i]||0)),children:[]});}
 for(const b of bones)nodes[parents[b.uuid]?indices[parents[b.uuid].uuid]:0].children.push(indices[b.uuid]);
 const grouped=new Map();
 for(const element of model.elements){const owner=owners[element.uuid],group=grouped.get(owner.uuid)||{name:owner.name,owner,elements:[]};group.elements.push(element);grouped.set(owner.uuid,group);}
 for(const group of grouped.values()){const positions=[],normals=[],uvs=[],owner=group.owner;for(const e of group.elements)for(const face of Object.values(e.faces))for(let j=1;j<face.vertices.length-1;j++){
  const ids=[face.vertices[0],face.vertices[j],face.vertices[j+1]],points=ids.map(id=>e.vertices[id]),normal=norm(cross(sub(points[1],points[0]),sub(points[2],points[0])));
  for(let k=0;k<3;k++){positions.push(...points[k].map((v,i)=>v-owner.origin[i]));normals.push(...normal);uvs.push(...face.uv[ids[k]].map(v=>v/128));}
 }
 const mesh=meshes.length;meshes.push({name:group.name,primitives:[{attributes:{POSITION:accessor(positions,'VEC3',3,true),NORMAL:accessor(normals,'VEC3',3),TEXCOORD_0:accessor(uvs,'VEC2',2)},material:0,mode:4}],extras:{sourceElements:group.elements.map(e=>e.name)}});const node=nodes.length;nodes.push({name:group.name+'_mesh',mesh});nodes[indices[owner.uuid]].children.push(node);
 }
 const animations=[];
 for(const clip of model.animations){const channels=[],samplers=[];for(const [id,a] of Object.entries(clip.animators)){
  for(const channel of ['rotation','position']){
   const keys=a.keyframes.filter(k=>k.channel===channel);if(!keys.length)continue;
   const times=[...new Set(keys.map(k=>k.time))].sort((a,b)=>a-b),values=[];let previous=null;
   const bone=bones.find(b=>b.uuid===id);
   for(const t of times){let v=sample(a,channel,t);if(channel==='rotation'){v=quaternion(v.map((n,i)=>n*(i<2?-1:1)));if(previous&&v.reduce((s,n,i)=>s+n*previous[i],0)<0)v=v.map(n=>-n);previous=v;}else v=v.map((n,i)=>(i===0?-n:n)+bone.origin[i]-(parents[id]?.origin[i]||0));values.push(...v);}
   channels.push({sampler:samplers.length,target:{node:indices[id],path:channel==='position'?'translation':'rotation'}});samplers.push({input:accessor(times,'SCALAR',1,true),output:accessor(values,channel==='position'?'VEC3':'VEC4',channel==='position'?3:4),interpolation:'LINEAR'});
  }
 }animations.push({name:clip.name,channels,samplers,extras:{loop:clip.loop,layer:clip.zeravorn_layer,events:clip.zeravorn_event_markers}});}
 const doc={asset:{version:'2.0',generator:'Zeravorn jungle cuboid pipeline'},scene:0,scenes:[{nodes:[0]}],nodes,meshes,materials:[{name:'Jungle_pixel_atlas',doubleSided:true,pbrMetallicRoughness:{baseColorTexture:{index:0},metallicFactor:0,roughnessFactor:1}}],textures:[{sampler:0,source:0}],samplers:[{magFilter:9728,minFilter:9728,wrapS:33071,wrapT:33071}],images:[{uri:model.textures[0].source}],animations,bufferViews:views,accessors,buffers:[{byteLength:offset,uri:'data:application/octet-stream;base64,'+Buffer.concat(blocks).toString('base64')}],extras:{authority:'presentation_only',front:'-Z',source:model.name+'.bbmodel'}};
 fs.writeFileSync(file,JSON.stringify(doc));
}
