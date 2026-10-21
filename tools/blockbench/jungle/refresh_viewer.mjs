import fs from 'node:fs';
import {writeViewer} from './viewer.mjs';
const root='assets/source/blockbench/jungle',file=root+'/previews/jungle_review.html';
const models=['red_buff','blue_buff','green_1','green_2'].map(k=>JSON.parse(fs.readFileSync(root+'/'+k+'/jungle_'+k+'.bbmodel')));
if(fs.existsSync(file)){const backup='assets/backups/jungle-viewer/'+new Date().toISOString().replace(/[:.]/g,'-');fs.mkdirSync(backup,{recursive:true});fs.copyFileSync(file,backup+'/jungle_review.html');}
writeViewer(models,file);
console.log('Rebuilt viewer from existing .bbmodel sources; geometry and exports unchanged.');
