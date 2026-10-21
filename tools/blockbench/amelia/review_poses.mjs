import {hierarchy} from './runtime.mjs';
export function withReviewPoses(source){
 const doc=structuredClone(source),bones=hierarchy(doc).bones;
 for(const [name,angle] of [['hands_forward',65],['hands_up',145]]){
 const a={name:'amelia.review_'+name,length:1,loop:'hold',animators:{}};
 for(const side of ['left','right'])for(const [prefix,value] of [['upper_arm_',[-angle,0,side==='left'?-12:12]],['forearm_',[-15,0,0]]]){const b=bones.find(b=>b.name===prefix+side);a.animators[b.uuid]={name:b.name,type:'bone',keyframes:[{channel:'rotation',time:0,data_points:[{x:value[0],y:value[1],z:value[2]}],interpolation:'linear'}]};}
 doc.animations.push(a);
 }return doc;
}
