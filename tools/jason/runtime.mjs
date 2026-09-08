// Shared dependency-free evaluator for source inspection and GLTF export.
// Matches Blockbench BoneAnimator signs and ZYX Euler order (4.12.6).
export function identity() { return [1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1]; }
export function matrixMultiply(a,b) {
  const c=Array(16).fill(0);
  for(let col=0;col<4;col++) for(let row=0;row<4;row++) for(let k=0;k<4;k++) c[col*4+row]+=a[k*4+row]*b[col*4+k];
  return c;
}
export function transformPoint(m,p) {return [0,1,2].map(i=>m[i]*p[0]+m[4+i]*p[1]+m[8+i]*p[2]+m[12+i]);}
export function quaternion(rotation) {
  const [x,y,z]=rotation.map(v=>v*Math.PI/360),c1=Math.cos(x),c2=Math.cos(y),c3=Math.cos(z),s1=Math.sin(x),s2=Math.sin(y),s3=Math.sin(z);
  return [s1*c2*c3-c1*s2*s3,c1*s2*c3+s1*c2*s3,c1*c2*s3-s1*s2*c3,c1*c2*c3+s1*s2*s3];
}
export function compose(position,rotation=[0,0,0],scale=[1,1,1]) {
  const [x,y,z,w]=quaternion(rotation),xx=x*x,yy=y*y,zz=z*z,xy=x*y,xz=x*z,yz=y*z,wx=w*x,wy=w*y,wz=w*z;
  return [(1-2*(yy+zz))*scale[0],2*(xy+wz)*scale[0],2*(xz-wy)*scale[0],0,
    2*(xy-wz)*scale[1],(1-2*(xx+zz))*scale[1],2*(yz+wx)*scale[1],0,
    2*(xz+wy)*scale[2],2*(yz-wx)*scale[2],(1-2*(xx+yy))*scale[2],0,...position,1];
}
export function sample(animator,channel,time) {
  const keys=(animator?.keyframes||[]).filter(k=>k.channel===channel).sort((a,b)=>a.time-b.time);
  if(!keys.length) return channel==='scale'?[1,1,1]:[0,0,0];
  let a=keys[0],b=keys[keys.length-1];
  for(const k of keys) {if(k.time<=time)a=k;if(k.time>=time){b=k;break;}}
  const t=b.time===a.time?0:Math.max(0,Math.min(1,(time-a.time)/(b.time-a.time)));
  return ['x','y','z'].map(axis=>Number(a.data_points[0][axis])*(1-t)+Number(b.data_points[0][axis])*t);
}
export function boneTransform(bone,parentOrigin,animator,time) {
  const p=sample(animator,'position',time),r=sample(animator,'rotation',time),s=sample(animator,'scale',time);
  return compose(bone.origin.map((v,i)=>v-parentOrigin[i]+p[i]*(i===0?-1:1)),r.map((v,i)=>v*(i===2?1:-1)),s);
}
export function evaluate(model,animationName,time=0) {
  const anim=model.animations.find(a=>a.name===animationName),matrices=new Map(),parents=new Map();
  if(anim)time=anim.loop==='loop'?time%anim.length:Math.min(time,anim.length);
  function visit(b,parentMatrix,parentOrigin) {
    const m=matrixMultiply(parentMatrix,boneTransform(b,parentOrigin,anim?.animators[b.uuid],time));
    matrices.set(b.uuid,m);
    for(const c of b.children) if(typeof c==='string')parents.set(c,{matrix:m,origin:b.origin});else visit(c,m,b.origin);
  }
  model.outliner.forEach(b=>visit(b,identity(),[0,0,0]));
  const triangles=[];
  for(const e of model.elements) {
    const parent=parents.get(e.uuid),local=compose(e.origin.map((v,i)=>v-parent.origin[i]),e.rotation),m=matrixMultiply(parent.matrix,local);
    const vertices=Object.fromEntries(Object.entries(e.vertices).map(([k,p])=>[k,transformPoint(m,p)]));
    for(const f of Object.values(e.faces)) for(let j=1;j<f.vertices.length-1;j++) {
      const keys=[f.vertices[0],f.vertices[j],f.vertices[j+1]];
      triangles.push({points:keys.map(k=>vertices[k]),uv:keys.map(k=>f.uv[k]),mesh:e.name});
    }
  }
  return triangles;
}
