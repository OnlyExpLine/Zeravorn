import {atlas,png,triangles,norm,cross,sub,dot} from './core.mjs';
export function render(model,{width=500,height=600,yaw=0,pitch=.12,span=30,target=[0,12,0],team='blue',clip='',time=0,silhouette=false}={}){
 const pixels=new Uint8Array(width*height*4),depth=new Float64Array(width*height).fill(Infinity),tex=atlas(model.zeravorn.kind),scale=height/span;
 for(let y=0;y<height;y++)for(let x=0;x<width;x++){const i=(y*width+x)*4;pixels.set(silhouette?[232,235,240,255]:[15+y/height*8,23+y/height*9,36+y/height*10,255],i);}
 const cy=Math.cos(yaw),sy=Math.sin(yaw),cp=Math.cos(pitch),sp=Math.sin(pitch),light=norm([-.6,.9,-1]);
 const project=v=>{const [x,y,z]=sub(v,target),a=cy*x+sy*z,b=-sy*x+cy*z;return [width/2+a*scale,height/2-(cp*y+sp*b)*scale,cp*b-sp*y];};
 for(const tr of triangles(model,clip,time)){
  const p=tr.points.map(project),n=norm(cross(sub(tr.points[1],tr.points[0]),sub(tr.points[2],tr.points[0]))),shade=['glow','crystal'].includes(tr.mat)?1:.54+.4*Math.max(0,dot(n,light));
  const area=(p[1][0]-p[0][0])*(p[2][1]-p[0][1])-(p[1][1]-p[0][1])*(p[2][0]-p[0][0]);if(Math.abs(area)<1e-8)continue;
  const minx=Math.max(0,Math.floor(Math.min(...p.map(v=>v[0])))),maxx=Math.min(width-1,Math.ceil(Math.max(...p.map(v=>v[0])))),miny=Math.max(0,Math.floor(Math.min(...p.map(v=>v[1])))),maxy=Math.min(height-1,Math.ceil(Math.max(...p.map(v=>v[1]))));
  for(let y=miny;y<=maxy;y++)for(let x=minx;x<=maxx;x++){
   const a=((p[1][0]-x-.5)*(p[2][1]-y-.5)-(p[1][1]-y-.5)*(p[2][0]-x-.5))/area,b=((p[2][0]-x-.5)*(p[0][1]-y-.5)-(p[2][1]-y-.5)*(p[0][0]-x-.5))/area,c=1-a-b;if(a<0||b<0||c<0)continue;
   const d=a*p[0][2]+b*p[1][2]+c*p[2][2],idx=y*width+x;if(d>=depth[idx])continue;depth[idx]=d;
   const u=Math.max(0,Math.min(127,Math.floor(a*tr.uv[0][0]+b*tr.uv[1][0]+c*tr.uv[2][0]))),v=Math.max(0,Math.min(127,Math.floor(a*tr.uv[0][1]+b*tr.uv[1][1]+c*tr.uv[2][1])));
   for(let j=0;j<3;j++)pixels[idx*4+j]=silhouette?0:Math.round(tex[(v*128+u)*4+j]*shade);
  }
 }
 return {width,height,pixels};
}
export function join(panels,columns=panels.length){const w=panels[0].width,h=panels[0].height,width=w*columns,height=h*Math.ceil(panels.length/columns),pixels=new Uint8Array(width*height*4);panels.forEach((p,i)=>{for(let y=0;y<h;y++)pixels.set(p.pixels.subarray(y*w*4,(y+1)*w*4),(((Math.floor(i/columns)*h+y)*width)+(i%columns)*w)*4);});return png(width,height,pixels);}
