import { earringPreview } from './earring-preview.js';
import { braceletPreview } from './bracelet-preview.js';
/** Shared jewellery-set sizing and stock rules for the browser and gateway. */
export const isSet = product => Array.isArray(product.components) && product.components.length > 0;
export const pieceName = product => ({Rings:'ring',Bracelets:'bracelet',Necklaces:'necklace',Earrings:'earring'}[product.category] || 'piece');
export function componentProducts(catalogue, product) {
  return isSet(product) ? product.components.map(component => {
    const piece = catalogue.find(p => p.id === component.productId);
    if (!piece || isSet(piece)) throw new Error('A set must contain existing individual designs.');
    return { ...component, product: piece };
  }) : [{ label: product.optionLabel, product }];
}
export function defaultOption(catalogue, product) {
  return componentProducts(catalogue, product).map(c => c.product.options[0]).join('|');
}
export function validOption(catalogue, product, option) {
  if (typeof option !== 'string') return false;
  const choices=option.split('|'),components=componentProducts(catalogue,product);
  return choices.length===components.length && components.every((c,i)=>c.product.options.includes(choices[i]));
}
export function describeOption(catalogue,product,option) {
  if(!isSet(product))return option;
  const choices=option.split('|');
  return componentProducts(catalogue,product).map((c,i)=>`${c.label}: ${choices[i]}`).join(' · ');
}
// One accepted birthstone per month, chosen for this round faceted collection.
export const birthstones = Object.freeze([
  {month:'January',stone:'Garnet',colour:'Deep red'},
  {month:'February',stone:'Amethyst',colour:'Purple'},
  {month:'March',stone:'Aquamarine',colour:'Pale aqua blue'},
  {month:'April',stone:'Diamond',colour:'Colourless'},
  {month:'May',stone:'Emerald',colour:'Green'},
  {month:'June',stone:'Alexandrite',colour:'Teal green with violet flashes'},
  {month:'July',stone:'Ruby',colour:'Crimson red'},
  {month:'August',stone:'Peridot',colour:'Yellow-green'},
  {month:'September',stone:'Sapphire',colour:'Royal blue'},
  {month:'October',stone:'Pink tourmaline',colour:'Rose pink'},
  {month:'November',stone:'Citrine',colour:'Golden yellow'},
  {month:'December',stone:'Tanzanite',colour:'Violet blue'}
].map(b=>Object.freeze({...b,image:`birthstone-story-${b.month.toLowerCase()}`})));
export function selectedBirthstone(product,custom) {
  return product.id==='birthstone-story-necklace' ? birthstones.find(b=>b.month===custom?.birthMonth) : undefined;
}
export function personalizedImage(product,custom) {
  const earPreview=earringPreview(product,custom,birthstones);
  if(earPreview)return earPreview;
  const preview=braceletPreview(product,custom,birthstones);
  if(preview)return preview;
  if(product.id==='initial-verse-necklace'&&/^[A-Z]$/.test(custom?.initial||''))return `/assets/initial-verse-${custom.initial.toLowerCase()}.svg`;
  const birthstone=selectedBirthstone(product,custom);
  return birthstone?`/assets/${birthstone.image}.webp`:(product.image?`/assets/${product.image}.${product.imageExtension||'webp'}`:null);
}
export function personalizationFields(product) {
  if(!['Necklaces','Bracelets','Earrings'].includes(product.category))return [];
  if(product.id==='initial-verse-huggies')return [{key:'leftInitial',label:'Left earring initial',choices:'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')},{key:'rightInitial',label:'Right earring initial',choices:'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')}];
  const options={
    'birthstone-story-studs':{key:'birthMonth',label:'Birth month',choices:birthstones.map(b=>b.month),choiceLabels:Object.fromEntries(birthstones.map(b=>[b.month,`${b.month} — ${b.stone}`]))},
    'pearl-halo-jackets':{key:'wearMode',label:'Wear it your way',choices:['Pearl jackets','Studs only']},
    'initial-verse-bracelet':{key:'initial',label:'Your initial',choices:'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')},
    'birthstone-story-bracelet':{key:'birthMonth',label:'Birth month',choices:birthstones.map(b=>b.month)},
    'written-promise-bracelet':{key:'engraving',label:'Your inscription',max:24},
    'chapter-charm-bracelet':{key:'charm',label:'Your charm',choices:['Initial','Heart','Star','Moon','Birthstone']},
    'initial-verse-necklace':{key:'initial',label:'Your initial',choices:'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')},
    'birthstone-story-necklace':{key:'birthMonth',label:'Birth month',choices:birthstones.map(b=>b.month),choiceLabels:Object.fromEntries(birthstones.map(b=>[b.month,`${b.month} — ${b.stone}`]))},
    'written-promise-bar':{key:'engraving',label:'Engraving',max:24},
    'forever-disc-necklace':{key:'engraving',label:'Engraving',max:24}
  };
  const fields=options[product.id]?[options[product.id]]:[];
  if(product.id==='chapter-charm-bracelet')return [...fields,{key:'initial',label:'Initial charm letter',choices:'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split(''),when:{key:'charm',value:'Initial'}},{key:'birthMonth',label:'Birthstone charm month',choices:birthstones.map(b=>b.month),when:{key:'charm',value:'Birthstone'}}];
  return fields;
}
export function normalizedPersonalization(product,custom) {
  const fields=personalizationFields(product);
  if(!fields.length)return {};
  if(!custom||typeof custom!=='object'||Array.isArray(custom))throw new Error('Choose your personalisation before adding to bag.');
  const result={};
  for(const f of fields){
    if(f.when&&custom[f.when.key]!==f.when.value)continue;
    if(typeof custom[f.key]!=='string')throw new Error('Choose your personalisation before adding to bag.');
    const value=custom[f.key].trim().replace(/\s+/g,' ');
    if(f.choices?!f.choices.includes(value):(!value||value.length>f.max||!/^[a-zA-Z0-9 &.'/:-]+$/.test(value)))throw new Error('Check your initial, birth month or engraving (maximum 24 characters).');
    result[f.key]=value;
  }
  return result;
}
export function personalizationDescription(product,custom) {
  const fields=personalizationFields(product);
  if(!fields.length)return '';
  const data=normalizedPersonalization(product,custom);
  return fields.filter(f=>!f.when||data[f.when.key]===f.when.value).map(f=>`${f.label}: ${f.choiceLabels?.[data[f.key]]||data[f.key]}`).join(' · ');
}
export function selectionKey(product,item) {
  return `${item.id}:${item.option}:${JSON.stringify(normalizedPersonalization(product,item.personalization))}`;
}
export function expandBag(catalogue,selections) {
  if(!Array.isArray(selections)||selections.length<1||selections.length>80)throw new Error('Choose between 1 and 80 designs or sets.');
  const items=new Map(),seen=new Set();
  for(const selection of selections){
    const product=catalogue.find(p=>p.id===selection?.id);
    if(!product||product.hidden||!validOption(catalogue,product,selection.option)||!Number.isInteger(selection.quantity)||selection.quantity<1||selection.quantity>10)throw new Error('Check the product options and quantities in your bag.');
    const key=selectionKey(product,selection);
    if(seen.has(key))throw new Error('Combine duplicate selections in your bag.');
    seen.add(key);
    normalizedPersonalization(product,selection.personalization);
    const choices=selection.option.split('|');
    componentProducts(catalogue,product).forEach((component,i)=>{
      const productId=`${component.product.sku}-${choices[i]}`;
      const quantity=(items.get(productId)||0)+selection.quantity;
      if(quantity>100)throw new Error('The combined quantity of a product option is too large.');
      items.set(productId,quantity);
    });
  }
  return [...items].map(([productId,quantity])=>({productId,quantity})).sort((a,b)=>a.productId.localeCompare(b.productId));
}
export function selectionSnapshot(catalogue,selections) {
  return selections.map(selection=>{
    const product=catalogue.find(p=>p.id===selection.id);
    return {name:product.name,option:[describeOption(catalogue,product,selection.option),personalizationDescription(product,selection.personalization)].filter(Boolean).join(' · '),quantity:selection.quantity,price:product.price,setSize:isSet(product)?product.components.length:(product.pieceCount||1),setUnit:pieceName(product)};
  });
}
