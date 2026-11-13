const gemstoneColours=['#8b1538','#8f5aba','#8ccbd9','#f3f5ee','#21915c','#538c91','#bc2647','#a5bc35','#254cb4','#df8bba','#e5b341','#7771b8'];
export function earringPreview(product,custom,stones){
 if(!custom)return null;
 const pair=(draw)=>[300,600].map((x,i)=>`<g transform="translate(${x} 0)">${draw(i)}</g>`).join('');
 let jewellery='';
 if(product.id==='birthstone-story-studs'){
  const index=stones.findIndex(s=>s.month===custom.birthMonth);if(index<0)return null;
  jewellery=pair(()=>`<circle cx="0" cy="440" r="79" fill="url(#gold)" stroke="#ac7a2b" stroke-width="2"/><circle cx="0" cy="440" r="66" fill="${gemstoneColours[index]}"/><path d="M0 378L54 410L54 470L0 502L-54 470L-54 410Z M0 378L-30 440L0 502L30 440Z M-54 410L30 440L-54 470 M54 410L-30 440L54 470" fill="none" stroke="#ffffff" stroke-opacity=".32" stroke-width="2"/><ellipse cx="-22" cy="408" rx="20" ry="8" fill="#ffffff" opacity=".2"/>`);
 }else if(product.id==='initial-verse-huggies'){
  if(!/^[A-Z]$/.test(custom.leftInitial||'')&&!/^[A-Z]$/.test(custom.rightInitial||''))return null;
  jewellery=pair(i=>{const value=(i?custom.rightInitial:custom.leftInitial)||'A';if(!/^[A-Z]$/.test(value))return '';return `<ellipse cx="0" cy="355" rx="59" ry="85" fill="none" stroke="#ac7a2b" stroke-width="17"/><ellipse cx="0" cy="355" rx="59" ry="85" fill="none" stroke="url(#gold)" stroke-width="14"/><path d="M-42 295L42 295" stroke="url(#gold)" stroke-width="5"/><ellipse cx="0" cy="450" rx="7" ry="13" fill="none" stroke="url(#gold)" stroke-width="5"/><text x="0" y="510" text-anchor="middle" font-family="Georgia,serif" font-weight="bold" font-size="58" fill="url(#gold)" stroke="#a8782a" stroke-width="1">${value}</text>`;});
 }else if(product.id==='pearl-halo-jackets'){
  if(!['Pearl jackets','Studs only'].includes(custom.wearMode))return null;
  jewellery=pair(()=>`<circle cx="0" cy="355" r="22" fill="url(#gold)" stroke="#b58738"/>${custom.wearMode==='Pearl jackets'?`<path d="M0 375L0 455M-82 448Q0 548 82 448" fill="none" stroke="url(#gold)" stroke-width="8"/>${[[-75,458],[-42,491],[0,504],[42,491],[75,458]].map(([x,y])=>`<circle cx="${x}" cy="${y}" r="25" fill="url(#pearl)" stroke="#cbbba0"/>`).join('')}`:''}`);
 }else return null;
 const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="900" height="900" viewBox="0 0 900 900"><defs><radialGradient id="bg"><stop stop-color="#fff9ef"/><stop offset="1" stop-color="#f0e0cb"/></radialGradient><linearGradient id="gold" x2="1" y2=".6"><stop stop-color="#a77724"/><stop offset=".3" stop-color="#ffedaf"/><stop offset=".6" stop-color="#c39a48"/><stop offset="1" stop-color="#f4d889"/></linearGradient><radialGradient id="pearl" cx=".3" cy=".25"><stop stop-color="#ffffff"/><stop offset=".55" stop-color="#f7f0e6"/><stop offset="1" stop-color="#cbbfae"/></radialGradient><filter id="shadow"><feDropShadow dx="1" dy="6" stdDeviation="5" flood-color="#6e4b20" flood-opacity=".2"/></filter></defs><rect width="900" height="900" fill="url(#bg)"/><g filter="url(#shadow)">${jewellery}</g></svg>`;
 return 'data:image/svg+xml;charset=utf-8,'+encodeURIComponent(svg);
}
