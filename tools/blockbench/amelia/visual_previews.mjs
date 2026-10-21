import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {sheet,render} from './preview.mjs';
import {png} from './core.mjs';
import {withReviewPoses} from './review_poses.mjs';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../../..'),folder=root+'/assets/source/blockbench/heroes/amelia',out=folder+'/previews';
const doc=JSON.parse(fs.readFileSync(folder+'/amelia.bbmodel','utf8')),old=JSON.parse(fs.readFileSync(folder+'/amelia_before_visual_rework.bbmodel','utf8'));
const views=[['front',0],['front_3_4',.68],['side',Math.PI/2],['back',Math.PI]];
for(const [name,yaw] of views){sheet(doc,[{yaw,width:850,height:1050,span:35}],out+'/amelia_'+name+'.png');sheet(doc,[{yaw,width:850,height:1050,span:35,silhouette:true}],out+'/amelia_silhouette_'+name+'.png');}
sheet(doc,[{yaw:.5,width:1100,height:1100,span:37,clip:'amelia.idle',time:0}],out+'/amelia_staff_pose.png');
sheet(doc,views.map(([,yaw])=>({yaw,width:470,height:850,span:35,silhouette:true})),out+'/amelia_silhouette.png');
sheet(doc,[['idle',0],['walk',.3],['run',.2],['basic',.25],['q_loop',.5],['e',.35],['r',.55],['death',1.8]].map(([clip,time])=>({yaw:.7,width:400,height:680,span:37,clip:'amelia.'+clip,time})),out+'/amelia_clipping_poses.png');
// Honest comparison: old and new only; attached concept has no local bitmap path.
const oldRow=views.map(([,yaw])=>render(old,{yaw,width:470,height:800,span:35,hideStaff:true})),newRow=views.map(([,yaw])=>render(doc,{yaw,width:470,height:800,span:35,hideStaff:true})),pixels=new Uint8Array(1880*1600*4);
for(const [row,panels] of [oldRow,newRow].entries())for(const [col,p] of panels.entries())for(let y=0;y<800;y++)pixels.set(p.pixels.subarray(y*470*4,(y+1)*470*4),((row*800+y)*1880+col*470)*4);
fs.writeFileSync(out+'/amelia_comparison_old_new.png',png(1880,1600,pixels));
sheet(withReviewPoses(doc),['hands_forward','hands_up'].flatMap(n=>[0,.8].map(yaw=>({clip:'amelia.review_'+n,time:0,yaw,width:500,height:850,span:47}))),out+'/amelia_joint_test.png');
