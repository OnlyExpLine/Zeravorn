import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {evaluated,triangleList,hierarchy} from './runtime.mjs';
import {sub,cross,dot} from './core.mjs';
import {withReviewPoses} from './review_poses.mjs';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../../..'),folder=root+'/assets/source/blockbench/heroes/amelia';
const doc=withReviewPoses(JSON.parse(fs.readFileSync(folder+'/amelia.bbmodel','utf8'))),old=JSON.parse(fs.readFileSync(folder+'/amelia_before_visual_rework.bbmodel','utf8'));
const box=e=>[0,1,2].map(i=>[Math.min(...Object.values(e.vertices).map(v=>v[i])),Math.max(...Object.values(e.vertices).map(v=>v[i]))]);
function hit(p,q,tri){const [a,b,c]=tri,d=sub(q,p),e1=sub(b,a),e2=sub(c,a),h=cross(d,e2),det=dot(e1,h);if(Math.abs(det)<1e-8)return false;const s=sub(p,a),u=dot(s,h)/det;if(u<.005||u>.995)return false;const r=cross(s,e1),v=dot(d,r)/det;if(v<.005||u+v>.995)return false;const t=dot(e2,r)/det;return t>.005&&t<.995;}
const groups=[['arm_chest',/^(bare_upper_arm_|detached_sleeve_|gloved_palm_)/,/^(chest_integrated_|corset_cup_)/],['arm_hair',/^(bare_upper_arm_|detached_sleeve_)/,/^(side_curl_|back_middle_mass_|loose_lock_)/],['staff_body',/^staff_dark_shaft$/,/^(upper_body_skin|chest_integrated_|corset_tailored_waist)/],['staff_hair',/^staff_dark_shaft$/,/^(side_curl_|back_upper_mass_|back_middle_mass_|back_lower_curl_)/],['leg_skirt',/^(thigh_|boot_calf_)/,/^(dress_front_white_|outer_panel_blue_|mantle_side_volume_|skirt_front_light)/],['hair_back_dress',/^(back_lower_curl_|loose_lock_)/,/^(back_side_|skirt_back_center_layer)/]];
let pairs=0;const findings=[];
for(const [name,time] of [['idle',0],['walk',.3],['run',.2],['basic',.25],['q_loop',.5],['e',.35],['r',.55],['death',1.8],['review_hands_forward',0],['review_hands_up',0]]){
 const es=evaluated(doc,'amelia.'+name,time);
 for(const [category,ar,br] of groups)for(const a of es.filter(e=>ar.test(e.name)))for(const b of es.filter(e=>br.test(e.name))){pairs++;const ab=box(a),bb=box(b);if(!ab.every((r,i)=>r[0]<bb[i][1]-.02&&r[1]>bb[i][0]+.02))continue;const ta=triangleList([a]),tb=triangleList([b]);let found=false;for(const x of ta){if(found)break;for(const y of tb)if([0,1,2].some(i=>hit(x.points[i],x.points[(i+1)%3],y.points)||hit(y.points[i],y.points[(i+1)%3],x.points))){found=true;break;}}if(found)findings.push({clip:name,time,category,a:a.name,b:b.name});}
}
const oldBones=hierarchy(old).bones,newBones=hierarchy(doc).bones;
const report={scope:'8 retained-clip poses; sampled triangle crossings, excluding intentional adjacent layers. Not exhaustive.',testedPairs:pairs,findings,stableBonesPreserved:oldBones.map(b=>b.name),addedBones:newBones.filter(b=>!oldBones.some(o=>o.name===b.name)).map(b=>b.name),meshGeometry:true,concept:'User-attached Amelia sheet, 2026-10-04',visualApproval:'PENDING',animationPolish:'NOT STARTED'};
fs.writeFileSync(folder+'/visual_audit.json',JSON.stringify(report,null,2));console.log(JSON.stringify({testedPairs:pairs,findingCount:findings.length,byClip:Object.fromEntries([...new Set(findings.map(f=>f.clip))].map(c=>[c,findings.filter(f=>f.clip===c).length])),addedBones:report.addedBones},null,2));
