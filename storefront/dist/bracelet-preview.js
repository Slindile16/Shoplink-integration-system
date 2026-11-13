const esc=s=>String(s||'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&apos;'}[c]));
const colors=['#8b1538','#8f5aba','#8ccbd9','#f3f5ee','#21915c','#538c91','#bc2647','#a5bc35','#254cb4','#df8bba','#e5b341','#7771b8'];
export function braceletPreview(product,custom,stones){
 if(!custom||!['initial-verse-bracelet','birthstone-story-bracelet','written-promise-bracelet','chapter-charm-bracelet'].includes(product.id))return null;
 const kind=product.id==='chapter-charm-bracelet'?custom.charm:product.id.startsWith('initial')?'Initial':product.id.startsWith('birthstone')?'Birthstone':'Engraving';
 if(!kind||kind==='Initial'&&!/^[A-Z]$/.test(custom.initial||'')||kind==='Birthstone'&&!stones.some(s=>s.month===custom.birthMonth)||kind==='Engraving'&&!custom.engraving)return null;
 const stoneIndex=stones.findIndex(s=>s.month===custom.birthMonth);
 let charm='';
 if(kind==='Initial')charm=`<text x="450" y="722" text-anchor="middle" font-family="Georgia,serif" font-size="85" font-weight="bold" fill="url(#gold)" stroke="#a37328">${esc(custom.initial)}</text>`;
 if(kind==='Birthstone')charm=`<circle cx="450" cy="680" r="35" fill="${colors[stoneIndex]}" stroke="url(#gold)" stroke-width="9"/><path d="M425 665L450 650L475 665L465 697L435 697Z" fill="none" stroke="#ffffff" opacity=".35"/>`;
 if(kind==='Heart')charm='<path d="M450 720C352 661 411 616 450 650C489 616 548 661 450 720Z" fill="url(#gold)" stroke="#a37328"/>';
 if(kind==='Star')charm='<path d="M450 640L462 667L492 670L470 691L476 721L450 706L424 721L430 691L408 670L438 667Z" fill="url(#gold)" stroke="#a37328"/>';
 if(kind==='Moon')charm='<path d="M469 642A40 40 0 1 0 479 707A35 35 0 0 1 469 642Z" fill="url(#gold)" stroke="#a37328"/>';
 if(kind==='Engraving')charm=`<rect x="305" y="602" width="290" height="62" rx="22" fill="url(#gold)" stroke="#b48a3d"/><text x="450" y="640" text-anchor="middle" font-family="Georgia,serif" font-size="${custom.engraving.length>16?15:21}" fill="#725022">${esc(custom.engraving.slice(0,24))}</text>`;
 const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="900" height="900" viewBox="0 0 900 900"><defs><radialGradient id="bg"><stop stop-color="#fff9ef"/><stop offset="1" stop-color="#efdfca"/></radialGradient><linearGradient id="gold" x2="1" y2=".8"><stop stop-color="#a77724"/><stop offset=".3" stop-color="#ffedaf"/><stop offset=".6" stop-color="#c39a48"/><stop offset="1" stop-color="#f4d889"/></linearGradient><filter id="shadow"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-opacity=".18"/></filter></defs><rect width="900" height="900" fill="url(#bg)"/><g filter="url(#shadow)"><ellipse cx="450" cy="450" rx="290" ry="185" fill="none" stroke="#a87930" stroke-width="7"/><ellipse cx="450" cy="450" rx="290" ry="185" fill="none" stroke="url(#gold)" stroke-width="9" stroke-dasharray="12 5"/><ellipse cx="450" cy="450" rx="290" ry="185" fill="none" stroke="#fff1bc" stroke-width="2" stroke-dasharray="9 8"/><rect x="433" y="257" width="34" height="16" rx="8" fill="url(#gold)" stroke="#ad8031"/>${kind!=='Engraving'?'<ellipse cx="450" cy="640" rx="6" ry="10" fill="none" stroke="url(#gold)" stroke-width="5"/>':''}${charm}</g></svg>`;
 return 'data:image/svg+xml;charset=utf-8,'+encodeURIComponent(svg);
}
