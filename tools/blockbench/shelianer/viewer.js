const canvas=document.querySelector('#model'),gl=canvas.getContext('webgl',{antialias:true,preserveDrawingBuffer:true});
if(!gl)throw Error('Для просмотра нужен WebGL.');
const shader=(type,source)=>{const s=gl.createShader(type);gl.shaderSource(s,source);gl.compileShader(s);if(!gl.getShaderParameter(s,gl.COMPILE_STATUS))throw Error(gl.getShaderInfoLog(s));return s;};
const program=gl.createProgram();
gl.attachShader(program,shader(gl.VERTEX_SHADER,`attribute vec3 point;attribute vec2 texcoord;attribute float light;uniform mat4 view;varying vec2 uv;varying float shade;void main(){gl_Position=view*vec4(point,1.0);uv=texcoord;shade=light;}`));
gl.attachShader(program,shader(gl.FRAGMENT_SHADER,`precision mediump float;uniform sampler2D atlas;varying vec2 uv;varying float shade;void main(){vec4 c=texture2D(atlas,uv);gl_FragColor=vec4(c.rgb*shade,c.a);}`));
gl.linkProgram(program);if(!gl.getProgramParameter(program,gl.LINK_STATUS))throw Error(gl.getProgramInfoLog(program));gl.useProgram(program);
const buffer=gl.createBuffer();gl.bindBuffer(gl.ARRAY_BUFFER,buffer);
for(const [name,size,offset] of [['point',3,0],['texcoord',2,12],['light',1,20]]){const loc=gl.getAttribLocation(program,name);gl.enableVertexAttribArray(loc);gl.vertexAttribPointer(loc,size,gl.FLOAT,false,24,offset);}
const tex=gl.createTexture();gl.bindTexture(gl.TEXTURE_2D,tex);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MIN_FILTER,gl.NEAREST);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MAG_FILTER,gl.NEAREST);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_S,gl.CLAMP_TO_EDGE);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_T,gl.CLAMP_TO_EDGE);
const texImage=new Image();texImage.onload=()=>{gl.bindTexture(gl.TEXTURE_2D,tex);gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA,gl.RGBA,gl.UNSIGNED_BYTE,texImage);requestAnimationFrame(frame);};texImage.src=MODELS[0].textures[0].source;
let selected=0,yaw=.45,pitch=.04,span=34,time=0,playing=false,previous=0,dirty=true,clip='',drag=null;
const select=document.querySelector('#clip'),timeline=document.querySelector('#timeline');
function changeModel(){selected=+document.querySelector('#asset').value;const m=MODELS[selected];select.replaceChildren(new Option('Исходная поза',''),...m.animations.map(a=>new Option(a.name,a.name)));clip='';time=0;playing=false;span=selected===2?32:34;yaw=.45;pitch=.04;if(selected===1){select.value='shelianer.fp.idle';clip=select.value;yaw=0;}dirty=true;document.querySelector('#count').textContent=`${m.elements.length} элементов · ${hierarchy(m).bones.length} костей · ${m.animations.length} анимаций`;}
document.querySelector('#asset').onchange=changeModel;changeModel();
select.onchange=()=>{clip=select.value;time=0;dirty=true;timeline.max=MODELS[selected].animations.find(a=>a.name===clip)?.length||1;};
document.querySelector('#play').onclick=()=>{playing=!playing;document.querySelector('#play').textContent=playing?'Пауза':'Воспроизвести';};
timeline.oninput=()=>{time=+timeline.value;playing=false;dirty=true;};
document.querySelector('#staff').onchange=()=>dirty=true;
document.querySelectorAll('[data-yaw]').forEach(b=>b.onclick=()=>{yaw=+b.dataset.yaw;pitch=0;dirty=true;});
canvas.onpointerdown=e=>{drag=[e.clientX,e.clientY];canvas.setPointerCapture(e.pointerId);};canvas.onpointerup=()=>drag=null;
canvas.onpointermove=e=>{if(!drag)return;yaw+=(e.clientX-drag[0])*.009;pitch=Math.max(-1,Math.min(1,pitch+(e.clientY-drag[1])*.006));drag=[e.clientX,e.clientY];dirty=true;};
canvas.onwheel=e=>{e.preventDefault();span=Math.max(8,Math.min(65,span*Math.exp(e.deltaY*.001)));dirty=true;};
document.querySelector('#reference').onchange=e=>{const file=e.target.files[0];if(!file)return;const img=document.querySelector('#concept');if(img.dataset.url)URL.revokeObjectURL(img.dataset.url);img.dataset.url=URL.createObjectURL(file);img.src=img.dataset.url;img.hidden=false;};
document.querySelector('#save').onclick=()=>{const a=document.createElement('a');a.href=canvas.toDataURL('image/png');a.download='shelianer_'+(clip||'bind')+'_'+time.toFixed(2)+'.png';a.click();};
const query=new URLSearchParams(location.search);
if(query.has('asset')){document.querySelector('#asset').value=query.get('asset');changeModel();}
if(query.has('clip')){select.value=query.get('clip');clip=select.value;}
if(query.has('time'))time=+query.get('time');
function frame(now){
 const m=MODELS[selected],anim=m.animations.find(a=>a.name===clip),dt=Math.min(.05,(now-previous)/1000);previous=now;
 if(playing&&anim){time+=dt;if(time>anim.length){if(anim.loop==='loop')time%=anim.length;else{time=anim.length;playing=false;}}dirty=true;}
 const w=Math.round(canvas.clientWidth*devicePixelRatio),h=Math.round(canvas.clientHeight*devicePixelRatio);if(canvas.width!==w||canvas.height!==h){canvas.width=w;canvas.height=h;dirty=true;}
 if(dirty){
  const triangles=triangleList(evaluated(m,clip,time)),data=[];
  for(const tri of triangles){if(!document.querySelector('#staff').checked&&/^(dagger_)/.test(tri.name))continue;
   const [a,b,c]=tri.points,u=b.map((v,i)=>v-a[i]),v=c.map((n,i)=>n-a[i]),n=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]],len=Math.hypot(...n)||1,shade=.64+.32*Math.abs((-.385*n[0]+.693*n[1]-.616*n[2])/len);
   for(let i=0;i<3;i++)data.push(...tri.points[i],tri.uv[i][0]/256,tri.uv[i][1]/256,shade);
  }
  gl.bufferData(gl.ARRAY_BUFFER,new Float32Array(data),gl.DYNAMIC_DRAW);gl.viewport(0,0,w,h);gl.enable(gl.DEPTH_TEST);gl.clearColor(.055,.085,.13,1);gl.clear(gl.COLOR_BUFFER_BIT|gl.DEPTH_BUFFER_BIT);
  let view;
  if(selected===1){const f=1/Math.tan(70*Math.PI/360),aspect=w/h,near=.1,far=100;view=[f/aspect,0,0,0,0,f,0,0,0,0,(far+near)/(near-far),-1,0,0,2*far*near/(near-far),0];}
  else{const sx=2/span*h/w,sy=2/span,cz=Math.cos(yaw),sz=Math.sin(yaw),cp=Math.cos(pitch),sp=Math.sin(pitch),target=selected===2?0:15;view=[cz*sx,sz*sp*sy,-sz*cp/60,0,0,cp*sy,sp/60,0,sz*sx,-cz*sp*sy,cz*cp/60,0,0,-target*cp*sy,-target*sp/60,1];}
  gl.uniformMatrix4fv(gl.getUniformLocation(program,'view'),false,new Float32Array(view));gl.drawArrays(gl.TRIANGLES,0,data.length/6);
  document.body.dataset.renderStatus=gl.getError()===gl.NO_ERROR?'ready':'webgl_error';
  timeline.max=anim?.length||1;timeline.value=time;document.querySelector('#time').textContent=time.toFixed(2)+' с';dirty=false;
 }
 requestAnimationFrame(frame);
}
