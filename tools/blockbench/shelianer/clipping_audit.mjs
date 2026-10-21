import {evaluated,triangleList} from './runtime.mjs';
import {sub,cross,dot} from './core.mjs';
const box=e=>{const v=Object.values(e.vertices);return [0,1,2].map(i=>[Math.min(...v.map(p=>p[i])),Math.max(...v.map(p=>p[i]))]);};
const overlaps=(a,b)=>a.every((r,i)=>r[0]<b[i][1]-.015&&r[1]>b[i][0]+.015);
function intersects(segment,tri){const [p,q]=segment,[a,b,c]=tri,d=sub(q,p),e1=sub(b,a),e2=sub(c,a),h=cross(d,e2),det=dot(e1,h);if(Math.abs(det)<1e-8)return false;const inv=1/det,s=sub(p,a),u=inv*dot(s,h);if(u<.001||u>.999)return false;const r=cross(s,e1),v=inv*dot(d,r);if(v<.001||u+v>.999)return false;const t=inv*dot(e2,r);return t>.001&&t<.999;}
export function clippingAudit(doc){
 const findings=[];let pairs=0;
 for(const name of ['idle','walk','run','basic','q','e','f','r','review_combat_pose']){
 const clip=doc.animations.find(a=>a.name==='shelianer.'+name);
 for(const fraction of (name==='review_combat_pose'?[0]:[0,.25,.5,.75])){
 const elements=evaluated(doc,clip.name,clip.length*fraction);
 const moving=elements.filter(e=>/^(bracer_main_|glove_palm_|dagger_.*blade_body)/.test(e.name));
 const obstacles=elements.filter(e=>/^(torso_base|chest_armor_|hood_front_|hood_side_|hood_top|male_face)/.test(e.name));
 for(const a of moving)for(const b of obstacles){pairs++;if(!overlaps(box(a),box(b)))continue;const ta=triangleList([a]),tb=triangleList([b]);let hit=false;for(const x of ta){if(hit)break;for(const y of tb){if([0,1,2].some(i=>intersects([x.points[i],x.points[(i+1)%3]],y.points)||intersects([y.points[i],y.points[(i+1)%3]],x.points))){hit=true;break;}}}if(hit)findings.push({clip:name,time:+(clip.length*fraction).toFixed(3),moving:a.name,obstacle:b.name});}
 }
 }
 return {scope:'Non-adjacent bracer/palm/blade against torso/chest/hood/face; 33 sampled poses. Cloth/body and adjacent layered armor excluded.',testedPairs:pairs,intersectionFindings:findings,exhaustive:false};
}
