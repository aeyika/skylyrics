import wawoff2 from 'npm:wawoff2@2.0.1';
const base='presentation/src/main/resources/assets/skylyrics/fonts';
for(const weight of ['Regular','Medium','Semibold','Bold']){
  const source=await Deno.readFile(`${base}/Lyrics${weight}.woff2`);
  const font=await wawoff2.decompress(source);
  await Deno.writeFile(`${base}/beautiful-lyrics-${weight.toLowerCase()}.ttf`,font);
}
console.log('Prepared preserved Beautiful Lyrics fonts from the pinned mirror');
