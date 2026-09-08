// Bundled with the actual projects by preview_jason_blockbench.mjs. No CDN/server.
const canvas=document.querySelector('canvas'),gl=canvas.getContext('webgl',{antialias:true});
const rigSelect=document.querySelector('#rig'),clipSelect=document.querySelector('#clip'),scrub=document.querySelector('#scrub'),play=document.querySelector('#play'),readout=document.querySelector('#readout');
let current=MODELS[0],time=0,playing=false,angle=25,elevation=7,span=41,last=0,drag=null;
if(!gl)throw Error('WebGL is required for this local inspector.');
function shader(type,source){const s=gl.createShader(type);gl.shaderSource(s,source);gl.compileShader(s);if(!gl.getShaderParameter(s,gl.COMPILE_STATUS))throw Error(gl.getShaderInfoLog(s));return s;}
const program=gl.createProgram();
gl.attachShader(program,shader(gl.VERTEX_SHADER,`attribute vec3 position;attribute vec3 normal;attribute vec2 uv;uniform mat4 viewProjection;varying vec3 n;varying vec2 tex;void main(){gl_Position=viewProjection*vec4(position,1.);n=normal;tex=uv;}`));
gl.attachShader(program,shader(gl.FRAGMENT_SHADER,`precision mediump float;uniform sampler2D atlas;varying vec3 n;varying vec2 tex;void main(){vec3 nn=normalize(n);float light=.57+.43*max(0.,dot(nn,normalize(vec3(-.6,.9,-.8))))+.13*max(0.,dot(nn,normalize(vec3(.8,.2,.5))));gl_FragColor=vec4(texture2D(atlas,tex).rgb*light,1.);}`));
gl.linkProgram(program);if(!gl.getProgramParameter(program,gl.LINK_STATUS))throw Error(gl.getProgramInfoLog(program));gl.useProgram(program);
const buffer=gl.createBuffer();gl.bindBuffer(gl.ARRAY_BUFFER,buffer);
for(const [name,size,offset]of [['position',3,0],['normal',3,12],['uv',2,24]]){const attr=gl.getAttribLocation(program,name);gl.enableVertexAttribArray(attr);gl.vertexAttribPointer(attr,size,gl.FLOAT,false,32,offset);}
const texture=gl.createTexture();gl.bindTexture(gl.TEXTURE_2D,texture);
const image=new Image();image.onload=()=>{gl.bindTexture(gl.TEXTURE_2D,texture);gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA,gl.RGBA,gl.UNSIGNED_BYTE,image);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MIN_FILTER,gl.NEAREST);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MAG_FILTER,gl.NEAREST);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_S,gl.CLAMP_TO_EDGE);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_T,gl.CLAMP_TO_EDGE);draw();};image.src=current.textures[0].source;
gl.enable(gl.DEPTH_TEST);gl.clearColor(.085,.097,.116,1);
function selected(){return current.animations.find(a=>a.name===clipSelect.value);}
function resetClips(){clipSelect.replaceChildren(new Option('Bind pose / модель',''));for(const a of current.animations)clipSelect.add(new Option(a.name,a.name));time=0;playing=false;play.textContent='▶';scrub.max=1;scrub.value=0;draw();}
rigSelect.onchange=()=>{current=MODELS[Number(rigSelect.value)];image.src=current.textures[0].source;resetClips();};
clipSelect.onchange=()=>{time=0;scrub.max=selected()?.length||1;draw();};
play.onclick=()=>{if(!selected()){clipSelect.value=current.animations[0].name;scrub.max=selected().length;}playing=!playing;play.textContent=playing?'❚❚':'▶';};
scrub.oninput=()=>{time=Number(scrub.value);playing=false;play.textContent='▶';draw();};
canvas.onpointerdown=e=>{drag=[e.clientX,e.clientY];canvas.setPointerCapture(e.pointerId);};canvas.onpointerup=()=>drag=null;
canvas.onpointermove=e=>{if(!drag||current.zeravorn.first_person)return;angle+=(e.clientX-drag[0])*.45;elevation=Math.max(-45,Math.min(65,elevation+(e.clientY-drag[1])*.3));drag=[e.clientX,e.clientY];draw();};
canvas.onwheel=e=>{e.preventDefault();span=Math.max(10,Math.min(80,span+e.deltaY*.025));draw();};
document.querySelector('#front').onclick=()=>{angle=0;elevation=0;span=41;draw();};document.querySelector('#quarter').onclick=()=>{angle=25;elevation=7;span=41;draw();};document.querySelector('#back').onclick=()=>{angle=180;elevation=7;span=41;draw();};
function draw(){
  const rect=canvas.getBoundingClientRect(),w=Math.round(rect.width*devicePixelRatio),h=Math.round(rect.height*devicePixelRatio);if(canvas.width!==w||canvas.height!==h){canvas.width=w;canvas.height=h;}gl.viewport(0,0,w,h);gl.clear(gl.COLOR_BUFFER_BIT|gl.DEPTH_BUFFER_BIT);
  const first=current.zeravorn.first_person,triangles=evaluate(current,clipSelect.value,time),data=[];
  for(const t of triangles){const [a,b,c]=t.points,u=b.map((v,i)=>v-a[i]),v=c.map((x,i)=>x-a[i]),n=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]],length=Math.hypot(...n)||1;for(let i=0;i<3;i++)data.push(...t.points[i],...n.map(x=>x/length),t.uv[i][0]/256,t.uv[i][1]/256);}
  gl.bufferData(gl.ARRAY_BUFFER,new Float32Array(data),gl.DYNAMIC_DRAW);
  let matrix;
  if(first){const f=1/Math.tan(70*Math.PI/360),near=.1,far=100;matrix=[f/(w/h),0,0,0,0,f,0,0,0,0,-(far+near)/(far-near),-1,0,0,-2*far*near/(far-near),0];}
  else{const a=angle*Math.PI/180,e=elevation*Math.PI/180,r=[Math.cos(a),0,Math.sin(a)],u=[-Math.sin(a)*Math.sin(e),Math.cos(e),Math.cos(a)*Math.sin(e)],d=[Math.sin(a)*Math.cos(e),Math.sin(e),-Math.cos(a)*Math.cos(e)],sx=2/(span*w/h),sy=2/span;
    matrix=[r[0]*sx,u[0]*sy,-d[0]/100,0,r[1]*sx,u[1]*sy,-d[1]/100,0,r[2]*sx,u[2]*sy,-d[2]/100,0,0,-18*u[1]*sy,18*d[1]/100,1];}
  gl.uniformMatrix4fv(gl.getUniformLocation(program,'viewProjection'),false,new Float32Array(matrix));gl.drawArrays(gl.TRIANGLES,0,data.length/8);
  const animation=selected();scrub.value=time;readout.textContent=`${time.toFixed(2)} / ${(animation?.length||0).toFixed(2)} s · ${triangles.length.toLocaleString()} triangles`;
  document.querySelector('#events').textContent=(animation?.zeravorn_event_markers||[]).map(e=>`${e.time.toFixed(2)} s  ${e.id}  [${e.gate}]`).join('   •   ')||'Cosmetic animation · damage and movement remain server-authoritative';
  document.querySelector('#crosshair').hidden=!first;
}
window.addEventListener('resize',draw);
function tick(now){if(playing&&now-last>32){const a=selected();time+=(now-last)/1000;if(time>=a.length){if(a.loop==='hold'){time=a.length;playing=false;play.textContent='▶';}else time%=a.length;}draw();}if(now-last>32)last=now;requestAnimationFrame(tick);}resetClips();requestAnimationFrame(tick);
