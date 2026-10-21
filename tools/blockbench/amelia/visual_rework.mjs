// Amelia concept-sheet rework. Original rig/API retained, replacement geometry.
function remove(m,predicate){const ids=new Set(m.elements.filter(predicate).map(e=>e.uuid));m.elements=m.elements.filter(e=>!ids.has(e.uuid));for(const b of Object.values(m.bones))b.children=b.children.filter(c=>!ids.has(c));}
function line(m,n,b,p,mat='gold',r=.045){for(let i=1;i<p.length;i++)m.rod(n+'_'+i,b,p[i-1],p[i],r,mat,5);}
function gem(m,n,b,x,y,z,h=.7,w=h*.6){m.crystal(n,b,[x,y-h/2,z],[x,y+h/2,z],w,w*.6);}
function plate(m,n,b,pts,mat='white',depth=.12){const c=pts.reduce((a,p)=>a.map((v,i)=>v+p[i]/pts.length),[0,0,0]),v=[...pts,[c[0],c[1],c[2]-depth]],f=pts.map((_,i)=>[pts.length,i,(i+1)%pts.length]);return m.mesh(n,b,v,f,mat);}
function panel(m,n,b,rows,mat='blue',back=false){
 const v=[],f=[],count=rows.length*5;
 for(const [x,y,z,w] of rows)for(let i=0;i<5;i++)v.push([x+(i-2)*w/2,y,z+(i%2?-.1:.05)*(back?-1:1)]);
 v.push(...v.map(p=>[p[0],p[1],p[2]+.1]));for(let j=0;j<rows.length-1;j++)for(let i=0;i<4;i++){const a=j*5+i;f.push([a,a+1,a+6,a+5],[a+count+5,a+count+6,a+count+1,a+count]);}
 for(let j=0;j<rows.length-1;j++)for(const i of [0,4]){const a=j*5+i;f.push([a,a+5,a+count+5,a+count]);}
 for(const j of [0,rows.length-1])for(let i=0;i<4;i++){const a=j*5+i;f.push([a,a+count,a+count+1,a+1]);}
 const e=m.mesh(n,b,v,f,mat);line(m,n+'_trim_left',b,rows.map(r=>[r[0]-r[3],r[1],r[2]-.1]),'gold',.055);line(m,n+'_trim_right',b,rows.map(r=>[r[0]+r[3],r[1],r[2]-.1]),'gold',.055);return e;
}
function snow(m,n,b,x,y,z,r=.7){for(let i=0;i<3;i++){const a=i*Math.PI/3,dx=Math.cos(a)*r,dy=Math.sin(a)*r;line(m,n+i,b,[[x-dx,y-dy,z],[x+dx,y+dy,z]],'whiteLight',.05);for(const s of [-1,1])for(const k of [-1,1])line(m,n+i+s+k,b,[[x+s*dx*.65,y+s*dy*.65,z],[x+s*dx*.38-k*dy*.2,y+s*dy*.38+k*dx*.2,z]],'silver',.035);}}

function face(m){
 const ids=new Set();function visit(b){for(const c of b.children)typeof c==='object'?visit(c):ids.add(c);}visit(m.bones.face);remove(m,e=>ids.has(e.uuid)||e.name.startsWith('earring_'));
 m.loft('face_sculpt','face',[[0,22.93,.06,.63,.86],[0,23.15,.02,.98,1.03],[0,23.65,0,1.43,1.18],[0,24.75,.03,1.83,1.32],[0,25.9,.09,1.86,1.39],[0,27.15,.13,1.63,1.31]],'skin',12,.58);
 for(const s of [-1,1]){
 const x=s*.81;m.box('ear_'+s,'face',[s*1.89,24.63,.02],[.27,.79,.42],'skin');gem(m,'earring_'+s,'head',s*1.91,23.96,-.12,.43);
 m.box('eye_white_'+s,'face',[x,24.89,-1.35],[1.05,.78,.05],'whiteLight');m.box('iris_'+s,'face',[x-s*.07,24.86,-1.39],[.52,.72,.045],'eye');m.box('iris_dark_'+s,'face',[x-s*.07,25.08,-1.42],[.52,.26,.03],'blue');m.box('pupil_'+s,'face',[x-s*.07,24.89,-1.445],[.18,.45,.025],'navy');m.box('eye_glint_'+s,'face',[x-.12,25.08,-1.47],[.16,.17,.024],'whiteLight');
 line(m,'upper_lash_'+s,'face',[[x-s*.53,25.28,-1.39],[x,25.33,-1.43],[x+s*.59,25.36,-1.35]],'lash',.073);
 line(m,'lower_lash_'+s,'face',[[x-s*.42,24.51,-1.4],[x+s*.43,24.52,-1.37]],'skinShade',.028);
 line(m,'eyebrow_'+s,'face',[[x-s*.48,25.72,-1.36],[x,25.79,-1.4],[x+s*.47,25.71,-1.3]],'hairShade',.065);
 }
 m.loft('small_nose','face',[[0,24.05,-1.24,.12,.07],[0,24.36,-1.31,.08,.09],[0,24.56,-1.31,.05,.025]],'skin',6);
 line(m,'quiet_mouth','face',[[-.23,23.65,-1.24],[0,23.61,-1.26],[.23,23.65,-1.24]],'lip',.033);
}
function crown(m){
 const ids=new Set();function walk(b){for(const c of b.children)typeof c==='object'?walk(c):ids.add(c);}walk(m.bones.crown);remove(m,e=>ids.has(e.uuid));
 for(let i=0;i<12;i++){const a=i*Math.PI/6,b=a+Math.PI/6;line(m,'diadem_band'+i,'crown',[[Math.sin(a)*2.12,27.18,-Math.cos(a)*1.65],[Math.sin(b)*2.12,27.18,-Math.cos(b)*1.65]],'silver',.125);}
 for(const s of [-1,1]){
 plate(m,'crown_swept_base'+s,'crown',[[0,27.12,-1.9],[s*.63,27.82,-1.74],[s*1.65,27.41,-1.49],[s*2.08,27.74,-.82],[s*1.88,26.97,-1.19],[s*.78,26.94,-1.83]],'silver',.14);
 line(m,'diadem_arch'+s,'crown',[[0,27.13,-2.05],[s*.67,27.8,-1.89],[s*1.64,27.39,-1.66],[s*2.1,27.73,-.99]],'whiteLight',.055);
 for(let j=0;j<2;j++){const b='crown_'+(s===1?'left':'right')+'_0'+(j+1)+'_crystal';m.crystal('diadem_fan_'+s+j,b,[s*(.81+j*.68),27.2,-1.74+j*.43],[s*(1.22+j*.76),28.48-j*.18,-1.63+j*.5],.6-j*.1,.36);}
 gem(m,'crown_temple_'+s,'crown',s*2.08,27.45,-.31,.75,.46);
 }
 m.crystal('diadem_center','crown_center_crystal',[0,26.95,-1.96],[0,29.02,-1.85],.95,.59);gem(m,'forehead_jewel','crown',0,26.72,-1.87,.68,.49);
 m.crystal('diadem_back','crown_back_crystal',[0,27,1.83],[0,27.88,1.83],.5,.3);
}
function hair(m){
 const ids=new Set();function walk(b){for(const c of b.children)typeof c==='object'?walk(c):ids.add(c);}walk(m.bones.hair_root);remove(m,e=>ids.has(e.uuid));
 m.loft('silver_hair_cap','hair_root',[[0,25.9,.23,2.05,1.61],[0,27.02,.17,2.17,1.69],[0,27.52,.21,1.99,1.58],[0,27.78,.25,1.45,1.26]],['hair','hairLight','hair'],12,.7);
 for(const s of [-1,1]){
 const side=s===1?'left':'right';
 for(let i=0;i<3;i++)m.ribbon('fringe_swept_'+side+i,'hair_front_'+side,[[s*(.13+i*.4),27.43,-1.41],[s*(.56+i*.43),26.74,-1.65],[s*(1.08+i*.38),25.98+i*.14,-1.72],[s*(1.79+i*.24),25.81+i*.08,-1.61]],[.57,.7,.54,.04],[.29,.35,.27,.035],i%2?'hair':'hairLight');
 m.ribbon('fringe_center_'+side,'hair_front_'+side,[[s*.19,27.4,-1.51],[s*.25,26.43,-1.61],[s*.36,25.79,-1.56]],[.44,.39,.065],.23,'hair');
 for(let i=0;i<3;i++){
 const b='hair_side_'+side,x=s*(1.76+i*.25),z=-.89+i*.55;
 m.ribbon('side_curl_'+side+i,b,[[x,26.69,z],[x+s*.1,24.98,z-.36],[x+s*.16,23.23,z-.4],[x+s*.57,22.2,z-.38],[x+s*.27,20.98,z-.38],[x+s*.64,20.3,z-.22]],[.56,.63,.65,.59,.43,.035],[.46,.43,.43,.41,.32,.035],i%2?'hair':'hairLight');
 }
 for(let i=0;i<2;i++){
 const n='hair_side_'+side+'_lower_'+i;m.bone(n,'hair_strands_'+side,[s*(2.1+i*.33),21.5,1]);
 m.ribbon('loose_lock_'+side+i,n,[[s*(2.15+i*.4),22.1,.9+i*.22],[s*(2.6+i*.48),18.7,1.38],[s*(3.05+i*.34),15.8,1.74],[s*(2.93+i*.49),13.9,1.73],[s*(3.38+i*.4),14.1,1.43]],[.52,.66,.58,.36,.035],[.42,.45,.39,.27,.035],i?'hairShade':'hair');
 }
 }
 // Seven major masses split further only at the tips, with an uneven U hem.
 for(let i=0;i<7;i++){
 const x=(i-3)*.54,z=1.43+.27*(1-Math.abs(i-3)/4),bottom=11.7+Math.abs(i-3)*.65+(i%2)*.3;
 const bn='hair_back_lower_lock_'+i;m.bone(bn,'hair_back_lower',[x*1.25,15.5,z+.35]);
 m.ribbon('back_upper_mass_'+i,'hair_back_upper',[[x,27.05,z],[x*1.07,24,z+.18],[x*1.13,21,z+.22]],[.7,.79,.8],[.61,.63,.61],i%3===0?'hairLight':'hair');
 m.ribbon('back_middle_mass_'+i,'hair_back_mid',[[x*1.13,21,z+.22],[x*1.24+Math.sin(i)*.16,18,z+.44],[x*1.3,15.5,z+.5]],[.8,.94,.96],[.61,.67,.58],i%3===1?'hairShade':'hair');
 m.ribbon('back_lower_curl_'+i,bn,[[x*1.3,15.5,z+.5],[x*1.43,14.1,z+.63],[x*1.45+Math.sin(i*2)*.23,bottom,z+.62],[x*1.49+Math.sin(i*2)*.28,bottom+.2,z+.32]],[.96,.86,.48,.035],[.58,.5,.29,.035],i%3===0?'hairLight':'hair');
 }
}
function torso(m){
 remove(m,e=>/^(exposed_bust_|open_lower_corset_|open_cup_|cup_seam_|bodice_gold_|corset_boning_|waist_chevron_|sternum_ice|waist_ice|corset_tailored_waist)/.test(e.name));
 m.loft('corset_tailored_waist','waist',[[0,15.15,0,2.43,1.37],[0,16.25,0,1.66,1.09],[0,17.4,-.08,1.88,1.26],[0,18.44,-.12,2.25,1.57]],['blue','cloth','blueLight','blue'],12,.8);
 for(const s of [-1,1]){
 const cx=s*1.16;
 m.loft('chest_integrated_'+s,'chest',[[cx*.75,18.1,-.48,.64,.59],[cx*.96,18.56,-.65,1.05,.98],[cx,19.28,-.68,1.4,1.42],[cx*.97,19.9,-.56,1.32,1.32],[cx*.81,20.47,-.31,1.05,.94],[cx*.62,20.85,-.04,.5,.44]],'skin',12);
 // Cups share the bust contour, with a sloped neckline and real thickness.
 const vs=[],fs=[],rows=5,segments=12;
 for(let j=0;j<=rows;j++)for(let i=0;i<=segments;i++){
 const phi=-Math.PI/2+Math.PI*i/segments,top=.06+.43*(s*Math.sin(phi)),theta=-1.42+(top+1.42)*j/rows;
 vs.push([cx+Math.sin(phi)*Math.cos(theta)*1.43,19.27+Math.sin(theta)*1.22,-.68-Math.cos(phi)*Math.cos(theta)*1.47]);
 }
 for(let j=0;j<rows;j++)for(let i=0;i<segments;i++){const a=j*(segments+1)+i;fs.push([a,a+1,a+segments+2,a+segments+1]);}
 m.mesh('corset_cup_'+s,'chest',vs,fs,'white');line(m,'cup_top_trim_'+s,'chest',vs.slice(rows*(segments+1)),'gold',.047);
 for(const k of [4,8])line(m,'cup_seam_'+s+k,'chest',Array.from({length:rows+1},(_,j)=>vs[j*(segments+1)+k].map((n,i)=>i===2?n-.025:n)),'silver',.035);
 plate(m,'corset_side_'+s,'waist',[[s*.35,18.45,-1.62],[s*1.68,18.36,-1.5],[s*1.21,16.8,-1.16],[s*1.99,15.4,-1.37],[s*.58,15.95,-1.28]],'white',.06);
 line(m,'corset_boning_'+s,'waist',[[s*1.78,18.27,-1.49],[s*1.16,17.22,-1.28],[s*.94,16.41,-1.2],[s*2.19,15.35,-1.39]],'gold',.062);
 line(m,'waist_chevron_'+s,'waist',[[s*2.45,15.43,-1.21],[s*1.32,14.98,-1.58],[0,14.24,-1.78]],'gold',.085);
 line(m,'waist_chevron_inner_'+s,'waist',[[s*1.86,15.24,-1.48],[0,14.63,-1.82]],'whiteLight',.065);
 }
 plate(m,'corset_center','waist',[[-.29,18.45,-1.74],[.29,18.45,-1.74],[.52,17.36,-1.46],[.18,16.1,-1.28],[0,15.7,-1.36],[-.18,16.1,-1.28],[-.52,17.36,-1.46]],'blueLight',.065);
 gem(m,'sternum_ice','chest',0,18.46,-1.86,.65);gem(m,'waist_ice','waist',0,16.47,-1.4,.72);
}
function arms(m){
 remove(m,e=>/^(cuff_tuft_|arm_diamond_|sleeve_diamond_)/.test(e.name));
 for(const s of [-1,1]){
 const side=s===1?'left':'right',upper='upper_arm_'+side,fore='forearm_'+side;
 m.bone('cuff_'+side+'_root',fore,[s*4.37,16.55,0]);const cuff='cuff_'+side+'_root';
 m.loft('cuff_snow_base_'+side,cuff,[[s*4.4,16.18,0,.95,.93],[s*4.37,16.55,0,1.03,1],[s*4.32,16.93,0,.91,.9]],'fur',10);
 for(let i=0;i<9;i++){const a=i*Math.PI*2/9,x=s*4.37+Math.cos(a)*.87,z=Math.sin(a)*.84;m.loft('cuff_soft_tuft_'+side+i,cuff,[[x,16.09+(i%2)*.1,z,.19,.2],[x,16.46,z,.29,.3],[x,16.96-(i%2)*.1,z,.19,.21]],i%3?'fur':'whiteLight',6);}
 m.bone('shoulder_decoration_'+side,'shoulder_'+side,[s*3.65,20.65,0]);const b='shoulder_decoration_'+side;
 for(let j=0;j<3;j++){const x=s*(3.1+j*.43);plate(m,'shoulder_petal_'+side+j,b,[[x-s*.32,20.25,-.65],[x,20.83+(j===1?.25:0),-.7],[x+s*.4,20.15,-.6],[x+s*.13,19.88,-.63]],'white',.15);gem(m,'shoulder_crystal_'+side+j,b,x,20.36,-.91,.52,.29);}
 line(m,'sleeve_top_band_'+side,upper,[[s*3.51,18.69,-.58],[s*4.01,18.48,-.75],[s*4.53,18.55,-.53]],'gold',.07);
 plate(m,'sleeve_front_inlay_'+side,fore,[[s*4.1,16,-.84],[s*4.7,15.7,-.94],[s*5.12,14.32,-.95],[s*4.72,14,-1.05],[s*4.23,14.47,-.92]],'blueLight',.05);
 gem(m,'sleeve_diamond_'+side,fore,s*4.67,14.82,-1.06,.82,.44);line(m,'sleeve_v_trim_'+side,fore,[[s*4.07,15.37,-.88],[s*4.66,14.49,-1.14],[s*5.14,15.28,-.89]],'gold',.055);
 }
}
function dress(m){
 remove(m,e=>/^(skirt_|white_inner_robe_|inner_robe_gold_|large_back_snowflake)/.test(e.name));
 panel(m,'skirt_front_light','skirt_front_center',[[0,15,-1.63,.59],[0,11.6,-1.9,.67],[0,7.4,-2.01,.62],[0,5,-2.1,.25]],'whiteLight');
 for(const s of [-1,1]){
 const side=s===1?'left':'right',fb='skirt_front_'+side;
 panel(m,'dress_front_white_'+side,fb,[[s*.94,15.03,-1.51,.45],[s*1.55,12,-1.64,.59],[s*2.05,9,-1.7,.6],[s*2.5,6.6,-1.8,.26]],'white');
 const outer='outer_panel_'+side,inner='inner_panel_'+side;m.bone(outer,'skirt_side_'+side,[s*2.5,15,.05]);m.bone(inner,fb,[s*1.45,15,-1.4]);
 panel(m,'outer_panel_blue_'+side,outer,[[s*2.22,15,-.91,.65],[s*3.12,11.3,-1.28,.78],[s*4.04,7.2,-1.62,.94],[s*4.91,2.2,-1.82,.66]],'blue');
 panel(m,'inner_panel_silver_'+side,inner,[[s*1.46,14.9,-1.62,.24],[s*2.2,11,-1.79,.35],[s*3.05,7,-1.92,.43],[s*3.72,4,-2.07,.18]],'whiteLight');
 for(let i=0;i<4;i++){const t=i/3,x=s*(2.65+1.7*t),y=12.4-8*t;gem(m,'panel_crystal_'+side+i,outer,x,y,-1.36-.45*t,.51);}
 snow(m,'panel_snow_'+side,outer,s*4.25,5.13,-1.98,.65);
 panel(m,'back_side_blue_'+side,'skirt_back_'+side,[[s*1.89,15,1.03,.7],[s*2.76,10.8,1.94,.9],[s*3.64,6.2,2.72,1.1],[s*4.05,2.5,3.05,.88]],'cloth',true);
 panel(m,'back_side_white_'+side,'skirt_back_'+side,[[s*1.22,14.8,1.23,.36],[s*1.85,10.3,2.29,.55],[s*2.53,5.5,3.02,.75],[s*2.91,3.4,3.22,.38]],'white',true);
 for(let j=0;j<4;j++)gem(m,'back_frost_'+side+j,'skirt_back_'+side,s*(2+j*.4),8.9-j*1.63,2.53+j*.18,.45);
 }
 panel(m,'skirt_back_center_layer','skirt_back_center',[[0,15,1.53,1.07],[0,10.3,2.5,1.57],[0,6,3.17,1.91],[0,1.6,3.49,1.45]],'blue',true);
 snow(m,'back_snowflake','skirt_back_center',0,4.7,3.56,1.54);
 for(let i=0;i<5;i++)gem(m,'front_ice_inlay_'+i,'skirt_front_center',Math.sin(i*1.7)*.22,12.5-i*1.46,-2.14,.52);
}
function boots(m){
 remove(m,e=>/^(boot_toe_|boot_heel_|boot_sole_|instep_armor_)/.test(e.name));
 for(const s of [-1,1]){const side=s===1?'left':'right',x=s*1.3,b='foot_'+side;
 m.loft('boot_sole_'+side,b,[[x,0,-.46,.7,1.18],[x,.25,-.46,.77,1.25]],'silver',10);
 m.loft('boot_toe_'+side,b,[[x,.23,-.59,.72,1.1],[x,.73,-.66,.73,1.09],[x,1.21,-.36,.6,.75],[x,1.93,-.12,.56,.6]],'white',10);
 m.loft('boot_heel_'+side,b,[[x,.2,.56,.47,.43],[x,1.2,.51,.5,.44]],'navy',8);
 plate(m,'boot_front_armor_'+side,b,[[x-.58,.7,-1.51],[x,1.34,-1.25],[x+.58,.7,-1.51],[x+.48,.37,-1.61],[x-.48,.37,-1.61]],'whiteLight',.04);
 gem(m,'boot_ankle_gem_'+side,b,x,1.62,-.76,.56,.35);
 }
}
function finish(m){
 const headIds=new Set();function walk(b){for(const c of b.children)typeof c==='object'?walk(c):headIds.add(c);}walk(m.bones.head);
 for(const e of m.elements){
 if(e.name.startsWith('outer_panel_blue_'))for(const v of Object.values(e.vertices))v[2]-=.57;
 if(e.name.startsWith('loose_lock_'))for(const v of Object.values(e.vertices))v[2]+=.55;
 if(e.name.startsWith('back_lower_curl_'))for(const v of Object.values(e.vertices))v[2]+=.45;
 if(headIds.has(e.uuid))for(const v of Object.values(e.vertices)){
 if(e.name==='face_sculpt'||/^(ear_|eye_|iris|pupil|upper_lash|lower_lash|eyebrow|small_nose|quiet_mouth)/.test(e.name)){if(v[1]<24.8)v[1]=24.8+(v[1]-24.8)*.72;}
 v[1]-=.45;
 }
 if(e.name==='neck')for(const v of Object.values(e.vertices))if(v[1]>22)v[1]+=.44;
 if(e.name.startsWith('back_lower_curl_')){const i=Number(e.name.split('_').at(-1));for(const v of Object.values(e.vertices)){const cx=(i-3)*.54*1.4;v[0]=cx+(v[0]-cx)*.73;if(v[1]<14){v[0]+=(i-3)*.095;v[2]+=.15*Math.sin(i);}}}
 }
 // Depth-bearing mantle side panels connect front and rear layers.
 for(const s of [-1,1]){
 const side=s===1?'left':'right',b='outer_panel_'+side,v=[],f=[];
 const rows=[[2.43,14.9,1.09],[3.23,10.8,1.78],[4.18,6.4,2.39],[4.78,2.1,2.78]];
 for(const [x,y,r] of rows)for(let i=0;i<5;i++)v.push([s*(x+.22*Math.sin(i*Math.PI/4)),y+(i===2?.1:0),-r+2*r*i/4]);
 for(let j=0;j<3;j++)for(let i=0;i<4;i++){const a=j*5+i;f.push([a,a+1,a+6,a+5]);}
 m.mesh('mantle_side_volume_'+side,b,v,f,['blue','cloth','blue','blue']);
 line(m,'mantle_hem_'+side,b,v.slice(15),'gold',.07);
 for(let i=0;i<5;i++){const p=v[15+i];m.box('mantle_hem_step_'+side+i,b,[p[0],p[1]+(i%2)*.18,p[2]],[.19,.37,.44],'gold');}
 // Snow applique follows the YZ-facing outer mantle surface.
 const x=s*4.49,y=5.5;for(let i=0;i<3;i++){const a=i*Math.PI/3,dy=Math.cos(a)*.83,dz=Math.sin(a)*.83;line(m,'side_snow_'+side+i,b,[[x,y-dy,-dz],[x,y+dy,dz]],'whiteLight',.065);}
 for(let j=0;j<3;j++){
 const n='hair_tip_overlay_'+side+j,bn='hair_side_'+side+'_lower_'+(j%2);
 m.ribbon(n,bn,[[s*(1.94+j*.34),19.6,1.95],[s*(2.37+j*.39),16.7,2.23],[s*(2.48+j*.47),14.4+j*.45,2.26],[s*(2.17+j*.43),13.9+j*.45,2.07]],[.49,.57,.39,.035],[.3,.35,.24,.035],j%2?'hair':'hairLight');
 }
 // Small cloth/crystal applique clusters rather than uniform empty hems.
 for(let j=0;j<6;j++){const x=s*(3.7+(j%3)*.32),y=3+(j%2)*.75+Math.floor(j/3)*.85;plate(m,'dress_frost_patch_'+side+j,b,[[x-.12,y-.19,-1.98],[x-.19,y+.09,-1.98],[x,y+.27,-1.98],[x+.21,y+.02,-1.98],[x+.11,y-.2,-1.98]],j%2?'white':'blueLight',.025);}
 }
}
export function rework(m){face(m);crown(m);hair(m);torso(m);arms(m);dress(m);boots(m);finish(m);}
export function reworkArms(m){arms(m);}
