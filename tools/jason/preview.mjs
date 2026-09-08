import { evaluate } from './runtime.mjs';
import { dot, sub, cross, unit, palette, faceColor, png } from './mesh.mjs';

export function render(model,{width=800,height=1000,angle=25,elevation=7,animation,time=0,center=[0,18,0],span=41,firstPerson=false}={}) {
  const rgba=new Uint8Array(width*height*4),zBuffer=new Float32Array(width*height).fill(-Infinity);
  for(let y=0;y<height;y++) for(let x=0;x<width;x++) {
    const v=1-Math.hypot((x-width*.5)/width,(y-height*.4)/height);
    rgba.set([Math.round(22+v*11),Math.round(25+v*12),Math.round(29+v*15),255],(y*width+x)*4);
  }
  const a=angle*Math.PI/180,e=elevation*Math.PI/180;
  const right=[Math.cos(a),0,Math.sin(a)],up=[-Math.sin(a)*Math.sin(e),Math.cos(e),Math.cos(a)*Math.sin(e)],toward=[Math.sin(a)*Math.cos(e),Math.sin(e),-Math.cos(a)*Math.cos(e)];
  const light=unit([-.6,.9,-.8]),fill=unit([.8,.2,.5]),scale=height/span;
  const project=p=>{
    if(firstPerson){const d=-p[2],f=height/(2*Math.tan(70*Math.PI/360));return [width/2+p[0]*f/d,height/2-p[1]*f/d,1/d];}
    const q=sub(p,center);return [width/2+dot(q,right)*scale,height/2-dot(q,up)*scale,dot(q,toward)];
  };
  function triangle(points,color) {
    const [a,b,c]=points.map(project),area=(b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);
    if(Math.abs(area)<.0001)return;
    const minX=Math.max(0,Math.floor(Math.min(a[0],b[0],c[0]))),maxX=Math.min(width-1,Math.ceil(Math.max(a[0],b[0],c[0])));
    const minY=Math.max(0,Math.floor(Math.min(a[1],b[1],c[1]))),maxY=Math.min(height-1,Math.ceil(Math.max(a[1],b[1],c[1])));
    for(let y=minY;y<=maxY;y++)for(let x=minX;x<=maxX;x++){
      const v=((x+.5-a[0])*(c[1]-a[1])-(y+.5-a[1])*(c[0]-a[0]))/area;
      const w=((b[0]-a[0])*(y+.5-a[1])-(b[1]-a[1])*(x+.5-a[0]))/area,u=1-v-w;
      if(u<0||v<0||w<0)continue;
      const z=u*a[2]+v*b[2]+w*c[2],i=y*width+x;
      if(z<=zBuffer[i])continue;
      zBuffer[i]=z;rgba.set([...color,255],i*4);
    }
  }
  if(!firstPerson) {
    // Ground disk renders behind the character; no fake anatomical detail is added.
    for(let i=0;i<48;i++) {
      const u=i/48*Math.PI*2,v=(i+1)/48*Math.PI*2;
      triangle([[0,-.055,0],[Math.cos(u)*11,-.055,Math.sin(u)*5],[Math.cos(v)*11,-.055,Math.sin(v)*5]],[24,27,31]);
    }
  }
  for(const t of evaluate(model,animation,time)) {
    if(firstPerson&&t.points.some(p=>p[2]>-.1))continue;
    const n=unit(cross(sub(t.points[1],t.points[0]),sub(t.points[2],t.points[0])));
    const material=Object.keys(palette)[Math.floor(t.uv[0][1]/12)],shade=Math.floor(t.uv[0][0]/16);
    // Two-sided thin cloth, but outward lighting on closed anatomy.
    const k=.57+.43*Math.max(0,dot(n,light))+.13*Math.max(0,dot(n,fill));
    const color=faceColor(material,shade).map(v=>Math.min(255,Math.round(v*k)));
    triangle(t.points,color);
  }
  if(firstPerson) for(let i=-5;i<=5;i++) {
    rgba.set([212,212,205,255],((height>>1)*width+(width>>1)+i)*4);
    rgba.set([212,212,205,255],(((height>>1)+i)*width+(width>>1))*4);
  }
  return {rgba,width,height,buffer:png(width,height,rgba)};
}

export function sheet(model,entries,{columns=4,width=360,height=460,...options}={}) {
  const rows=Math.ceil(entries.length/columns),pixels=new Uint8Array(width*columns*height*rows*4);
  entries.forEach((entry,i)=>{
    const tile=render(model,{width,height,...options,...entry});
    for(let y=0;y<height;y++) pixels.set(tile.rgba.subarray(y*width*4,(y+1)*width*4),(((Math.floor(i/columns)*height+y)*width*columns)+(i%columns)*width)*4);
  });
  return png(width*columns,height*rows,pixels);
}
