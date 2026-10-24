import fs from 'node:fs';
import path from 'node:path';
const root=process.cwd(),tolerance=1e-9;
let fields=0,maxDifference=0;
function compare(a,b,at){
  if(typeof a==='number'&&typeof b==='number'){
    const difference=Math.abs(a-b);maxDifference=Math.max(maxDifference,difference);fields++;
    if(difference>tolerance)throw new Error(`${at}: ${a} differs from ${b}`);
  }else if(Array.isArray(a)&&Array.isArray(b)){
    if(a.length!==b.length)throw new Error(`${at}: array lengths differ`);a.forEach((value,i)=>compare(value,b[i],`${at}[${i}]`));
  }else if(a&&b&&typeof a==='object'&&typeof b==='object'){
    if(Object.keys(a).sort().join('|')!==Object.keys(b).sort().join('|'))throw new Error(`${at}: fields differ`);
    for(const key of Object.keys(a))compare(a[key],b[key],`${at}.${key}`);
  }else if(a!==b)throw new Error(`${at}: ${a} differs from ${b}`);
}
for(const time of [0,1500,3000,9000,15500,21000,29000]){
  const filename=`frame-${time}.json`;
  const a=JSON.parse(fs.readFileSync(path.join(root,'test-results/ingame-1.21.11',filename),'utf8'));
  const b=JSON.parse(fs.readFileSync(path.join(root,'test-results/ingame-26.2',filename),'utf8'));
  compare(a,b,filename);
}
const result={timestamps:[0,1500,3000,9000,15500,21000,29000],numericFieldsCompared:fields,maxDifference,tolerance,status:'pass'};
fs.writeFileSync(path.join(root,'test-results/cross-version-frames.json'),JSON.stringify(result,null,2));
console.log(JSON.stringify(result));
