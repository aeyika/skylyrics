import { CurveInterpolator } from 'npm:curve-interpolator@3.3.1';
const lift=new CurveInterpolator([[0,.01],[.9,-1/60],[1,0]]);
const samples=[];
for(const duration of [2,5,10,30]){
  const scale=[[0,0],[.2/duration,1.05]];
  for(let i=1;i<=Math.floor((duration-.075-.2)/2.25);i++)scale.push([(.2+i*2.25)/duration,i%2===0?1.05:.95]);
  scale.push([(duration-.075)/duration,1.15],[1,0]);
  const s=new CurveInterpolator(scale),a=new CurveInterpolator([[0,0],[.5/duration,1],[(duration-.075)/duration,1],[1,0]]);
  for(const t of [0,.01,.1,.25,.5,.7,.9,.99,1]){
    const last=(curve:CurveInterpolator)=>{const points=curve.getIntersects(t) as number[][];return points.length?points.at(-1)![1]:1;};
    samples.push({duration,t,scale:last(s),opacity:last(a),lift:lift.getPointAt(t)[1]});
  }
}
await Deno.writeTextFile('test-fixtures/expected-interlude.json',JSON.stringify({source:'curve-interpolator 3.3.1 + original Interlude.ts control points',samples},null,2));
