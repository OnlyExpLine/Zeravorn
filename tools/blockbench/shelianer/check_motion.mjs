import assert from 'node:assert/strict';
import {rotationMatrix,transform,sample} from './runtime.mjs';
export function checkMotion(doc){
 let frames=0,maxGroundError=0;const rad=Math.PI/180;
 // Actual authoring-space directions: front = -Z. Elbow flexion goes forward,
 // knee flexion takes the ankle backwards. This catches a channel sign reversal.
 const forward=transform(rotationMatrix([35,0,0]),[0,-3,0]);assert(forward[2]<0);
 const backward=transform(rotationMatrix([-35,0,0]),[0,-6,0]);assert(backward[2]>0);
 for(const clip of doc.animations)for(const track of Object.values(clip.animators))if(track.name.startsWith('forearm_'))for(const k of track.keyframes)if(k.channel==='rotation'){const physical=-k.data_points[0].x;assert(physical>=0&&physical<=145,clip.name+' elbow hyperextension');frames++;}
 for(const name of ['walk','run']){const clip=doc.animations.find(a=>a.name==='shelianer.'+name);for(const side of ['left','right']){const track=n=>Object.values(clip.animators).find(a=>a.name===n);for(let i=0;i<=120;i++){const t=i/120*clip.length,phase=(i/120+(side==='right'?.5:0))%1,hip=-sample(track('leg_'+side),'rotation',t)[0]*rad,knee=-sample(track('shin_'+side),'rotation',t)[0]*rad;assert(knee<=.00001&&knee>=-Math.PI*.8);if(phase<.59){const bob=sample(track('body'),'position',t)[1],height=15+bob-7*Math.cos(hip)-6*Math.cos(hip+knee)-2;maxGroundError=Math.max(maxGroundError,Math.abs(height));}}}}
 assert(maxGroundError<.035,'Foot contact '+maxGroundError);return {elbowKeysChecked:frames,elbowDirection:'forward',kneeDirection:'backward',maxStanceFootHeightError:maxGroundError};
}
