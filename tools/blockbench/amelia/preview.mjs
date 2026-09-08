import fs from 'node:fs';
import {png,atlas,cross,sub,norm,dot} from './core.mjs';
import {evaluated,triangleList} from './runtime.mjs';

export function render(model,{width=650,height=900,yaw=0,pitch=0,target=[0,15,0],span=34,clip='',time=0,hideStaff=false}={}){
 const pixels=new Uint8Array(width*height*4),zbuf=new Float64Array(width*height).fill(Infinity),texture=atlas();
 for(let y=0;y<height;y++)for(let x=0;x<width;x++){const p=(y*width+x)*4;pixels[p]=16+y/height*8;pixels[p+1]=25+y/height*9;pixels[p+2]=39+y/height*11;pixels[p+3]=255;}
 const cy=Math.cos(yaw),sy=Math.sin(yaw),cp=Math.cos(pitch),sp=Math.sin(pitch),scale=height/span;
 const camera=v=>{const [x,y,z]=v.map((n,i)=>n-target[i]),a=cy*x+sy*z,b=-sy*x+cy*z;return [width/2+a*scale,height/2-(cp*y+sp*b)*scale,cp*b-sp*y];};
 const light=norm([-.5,.9,-.8]);
 const tri=triangleList(evaluated(model,clip,time)).filter(t=>!hideStaff||!t.name.startsWith('staff_')&&!t.name.startsWith('crystal_'));
 for(const {points,uv} of tri){
  const n=norm(cross(sub(points[1],points[0]),sub(points[2],points[0]))),shade=.64+.32*Math.abs(dot(n,light)),p=points.map(camera);
  const area=(p[1][0]-p[0][0])*(p[2][1]-p[0][1])-(p[1][1]-p[0][1])*(p[2][0]-p[0][0]);if(Math.abs(area)<1e-7)continue;
  const minx=Math.max(0,Math.floor(Math.min(...p.map(q=>q[0])))),maxx=Math.min(width-1,Math.ceil(Math.max(...p.map(q=>q[0])))),miny=Math.max(0,Math.floor(Math.min(...p.map(q=>q[1])))),maxy=Math.min(height-1,Math.ceil(Math.max(...p.map(q=>q[1]))));
  for(let y=miny;y<=maxy;y++)for(let x=minx;x<=maxx;x++){
   const a=((p[1][0]-x-.5)*(p[2][1]-y-.5)-(p[1][1]-y-.5)*(p[2][0]-x-.5))/area,b=((p[2][0]-x-.5)*(p[0][1]-y-.5)-(p[2][1]-y-.5)*(p[0][0]-x-.5))/area,c=1-a-b;if(a<0||b<0||c<0)continue;
   const z=a*p[0][2]+b*p[1][2]+c*p[2][2],idx=y*width+x;if(z>=zbuf[idx])continue;zbuf[idx]=z;
   const u=Math.max(0,Math.min(255,Math.floor(a*uv[0][0]+b*uv[1][0]+c*uv[2][0]))),v=Math.max(0,Math.min(255,Math.floor(a*uv[0][1]+b*uv[1][1]+c*uv[2][1]))),tex=(v*256+u)*4;
   for(let j=0;j<3;j++)pixels[idx*4+j]=Math.round(texture[tex+j]*shade);
  }
 }
 return {pixels,width,height};
}
export function sheet(model,views,out){const panels=views.map(v=>render(model,v)),width=panels.reduce((s,p)=>s+p.width,0),height=Math.max(...panels.map(p=>p.height)),pixels=new Uint8Array(width*height*4);let ox=0;for(const p of panels){for(let y=0;y<p.height;y++)pixels.set(p.pixels.subarray(y*p.width*4,(y+1)*p.width*4),(y*width+ox)*4);ox+=p.width;}fs.writeFileSync(out,png(width,height,pixels));}
