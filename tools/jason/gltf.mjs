import { quaternion, sample } from './runtime.mjs';
import { cross, sub, unit } from './mesh.mjs';

// Standard glTF 2.0: independent rigid mesh nodes follow the bone hierarchy.
// No renderer dependency is imposed on the Fabric project by this source export.
export function gltf(model) {
  const buffers=[],views=[],accessors=[],nodes=[],meshes=[],boneNodes=new Map();let offset=0;
  function data(values,type,components,bounds=false) {
    const bytes=Buffer.alloc(values.length*4);values.forEach((v,i)=>bytes.writeFloatLE(v,i*4));
    const bufferView=views.length;views.push({buffer:0,byteOffset:offset,byteLength:bytes.length});buffers.push(bytes);offset+=bytes.length;
    const accessor={bufferView,componentType:5126,count:values.length/components,type};
    if(bounds){accessor.min=Array.from({length:components},(_,i)=>Math.min(...values.filter((_,j)=>j%components===i)));accessor.max=Array.from({length:components},(_,i)=>Math.max(...values.filter((_,j)=>j%components===i)));}
    accessors.push(accessor);return accessors.length-1;
  }
  const byId=new Map(model.elements.map(e=>[e.uuid,e]));
  nodes.push({name:'zeravorn_block_units_to_metres',scale:[1/16,1/16,1/16],children:[]});
  function visit(b,parent,index) {
    const n=nodes.length;nodes.push({name:b.name,translation:sub(b.origin,parent),children:[]});nodes[index].children.push(n);boneNodes.set(b.uuid,n);
    for(const child of b.children) {
      if(typeof child==='object'){visit(child,b.origin,n);continue;}
      const e=byId.get(child),positions=[],normals=[],uv=[];
      for(const face of Object.values(e.faces))for(let i=1;i<face.vertices.length-1;i++){
        const keys=[face.vertices[0],face.vertices[i],face.vertices[i+1]],points=keys.map(k=>e.vertices[k]),normal=unit(cross(sub(points[1],points[0]),sub(points[2],points[0])));
        keys.forEach((k,j)=>{positions.push(...points[j]);normals.push(...normal);uv.push(face.uv[k][0]/model.resolution.width,face.uv[k][1]/model.resolution.height);});
      }
      const mesh=meshes.length;meshes.push({name:e.name,primitives:[{attributes:{POSITION:data(positions,'VEC3',3,true),NORMAL:data(normals,'VEC3',3),TEXCOORD_0:data(uv,'VEC2',2)},material:0}]});
      nodes[n].children.push(nodes.length);nodes.push({name:e.name,mesh,translation:sub(e.origin,b.origin)});
    }
  }
  model.outliner.forEach(b=>visit(b,[0,0,0],0));
  const animations=model.animations.map(a=>{
    const channels=[],samplers=[];
    for(const [id,animator]of Object.entries(a.animators))for(const channel of new Set(animator.keyframes.map(k=>k.channel))){
      const keys=animator.keyframes.filter(k=>k.channel===channel);
      // Include authored event/pose times and dense samples to preserve Euler motion.
      const times=[...new Set([...keys.map(k=>k.time),...Array.from({length:Math.ceil(a.length*30)+1},(_,i)=>Math.min(a.length,i/30))])].sort((a,b)=>a-b);
      const values=times.flatMap(t=>{
        const v=sample(animator,channel,t);
        if(channel==='rotation')return quaternion([-v[0],-v[1],v[2]]);
        if(channel==='position'){const rest=nodes[boneNodes.get(id)].translation;return [rest[0]-v[0],rest[1]+v[1],rest[2]+v[2]];}
        return v;
      });
      channels.push({sampler:samplers.length,target:{node:boneNodes.get(id),path:channel==='position'?'translation':channel}});
      samplers.push({input:data(times,'SCALAR',1,true),output:data(values,channel==='rotation'?'VEC4':'VEC3',channel==='rotation'?4:3),interpolation:'LINEAR'});
    }
    return {name:a.name,channels,samplers,extras:{loop:a.loop,events:a.zeravorn_event_markers}};
  });
  return {asset:{version:'2.0',generator:'Zeravorn Jason source pipeline v2'},scene:0,scenes:[{nodes:[0]}],nodes,meshes,animations,
    materials:[{name:'jason_atlas',doubleSided:true,pbrMetallicRoughness:{baseColorTexture:{index:0},metallicFactor:0,roughnessFactor:1}}],
    textures:[{sampler:0,source:0}],samplers:[{magFilter:9728,minFilter:9728,wrapS:33071,wrapT:33071}],images:[{uri:model.textures[0].source}],
    buffers:[{byteLength:offset,uri:`data:application/octet-stream;base64,${Buffer.concat(buffers).toString('base64')}`}],bufferViews:views,accessors,
    extras:{authority:'cosmetic_only',note:'Rigid hierarchical animation; loop flags and server gates are metadata for a future client adapter.'}};
}
