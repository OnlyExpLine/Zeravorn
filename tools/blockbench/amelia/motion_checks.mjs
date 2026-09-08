import {evaluated,triangleList} from './runtime.mjs';
// Clip a projected triangle against the protected crosshair rectangle.
function intersectsRect(points){
 let poly=points;
 for(const [axis,bound,sign] of [[0,-.12,1],[0,.12,-1],[1,-.10,1],[1,.10,-1]]){
  const next=[];for(let i=0;i<poly.length;i++){const a=poly[i],b=poly[(i+1)%poly.length],ina=(a[axis]-bound)*sign>=0,inb=(b[axis]-bound)*sign>=0;if(ina)next.push(a);if(ina!==inb){const t=(bound-a[axis])/(b[axis]-a[axis]);next.push(a.map((v,j)=>v+(b[j]-v)*t));}}poly=next;if(!poly.length)return false;
 }return poly.length>0;
}
export function motionChecks(third,fp){
 const feet={samples:0,maxContactError:0,minGeometryY:Infinity},reticle={samples:0,failures:[],verticalFov:70,aspect:16/9,protectedNdc:[.12,.10]};
 for(const name of ['walk','run']){
  const clip=third.animations.find(a=>a.name==='amelia.'+name);
  for(let i=0;i<=60;i++){
   const time=i/60*clip.length,elements=evaluated(third,clip.name,time);
   for(const [side,off] of [['left',0],['right',.5]]){
    const u=(time/clip.length+off)%1;if(u>=.60)continue;
    const points=elements.filter(e=>e.name==='boot_sole_'+side).flatMap(e=>Object.values(e.vertices));
    const min=Math.min(...points.map(v=>v[1]));feet.samples++;feet.maxContactError=Math.max(feet.maxContactError,Math.abs(min));
   }
   feet.minGeometryY=Math.min(feet.minGeometryY,...elements.filter(e=>!e.name.startsWith('staff_')).flatMap(e=>Object.values(e.vertices).map(v=>v[1])));
  }
 }
 const f=1/Math.tan(70*Math.PI/360);
 for(const clip of fp.animations)for(let i=0;i<=20;i++){
  const time=clip.length*i/20,triangles=triangleList(evaluated(fp,clip.name,time));reticle.samples++;
  for(const tri of triangles){
   if(tri.points.some(p=>p[2]>=-.1))continue;
   const p=tri.points.map(v=>[v[0]*f/(16/9)/-v[2],v[1]*f/-v[2]]);
   if(intersectsRect(p)){reticle.failures.push({clip:clip.name,time:+time.toFixed(3),element:tri.name});break;}
  }
 }
 return {feet,reticle};
}
