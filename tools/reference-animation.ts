import Spring from '../upstream/beautiful-lyrics/Universal/Modules/LegacySpring.ts';
import Spline from 'npm:cubic-spline@3.0.3';
const scale = new Spline([0,.7,1],[.95,1.025,1]);
const lift = new Spline([0,.9,1],[.01,-1/60,0]);
const glow = new Spline([0,.15,.6,1],[0,1,1,0]);
const s = new Spring(.95,.6,.7), y = new Spring(.01,.4,1.25), g = new Spring(0,.5,1);
const samples = [];
for (let tick=0;tick<=120;tick++) {
  const time = Math.min(1,tick/60);
  s.Final=scale.at(time); y.Final=lift.at(time); g.Final=glow.at(time);
  const result={tick,scale:s.Update(1/60),lift:y.Update(1/60),glow:g.Update(1/60)};
  if(tick%10===0)samples.push(result);
}
await Deno.writeTextFile('test-fixtures/expected-animation.json',JSON.stringify({
  source:'Unmodified Beautiful Lyrics LegacySpring.ts + cubic-spline 3.0.3',
  revision:'61ac582da092311e893423269ca7f09003108705',samples,
},null,2));
