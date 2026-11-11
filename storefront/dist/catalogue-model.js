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
export function personalizationFields(product) {
  if(product.category!=='Necklaces')return [];
  const options={
    'initial-verse-necklace':{key:'initial',label:'Your initial',choices:'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')},
    'birthstone-story-necklace':{key:'birthMonth',label:'Birth month',choices:['January','February','March','April','May','June','July','August','September','October','November','December']},
    'written-promise-bar':{key:'engraving',label:'Engraving',max:24},
    'forever-disc-necklace':{key:'engraving',label:'Engraving',max:24}
  };
  return options[product.id]?[options[product.id]]:[];
}
export function normalizedPersonalization(product,custom) {
  const fields=personalizationFields(product);
  if(!fields.length)return {};
  if(!custom||typeof custom!=='object'||Array.isArray(custom))throw new Error('Choose your personalisation before adding to bag.');
  const result={};
  for(const f of fields){
    if(typeof custom[f.key]!=='string')throw new Error('Choose your personalisation before adding to bag.');
    const value=custom[f.key].trim().replace(/\s+/g,' ');
    if(f.choices?!f.choices.includes(value):(!value||value.length>f.max||!/^[a-zA-Z0-9 &.'-]+$/.test(value)))throw new Error('Check your initial, birth month or engraving (maximum 24 characters).');
    result[f.key]=value;
  }
  return result;
}
export function personalizationDescription(product,custom) {
  const fields=personalizationFields(product);
  if(!fields.length)return '';
  const data=normalizedPersonalization(product,custom);
  return fields.map(f=>`${f.label}: ${data[f.key]}`).join(' · ');
}
export function selectionKey(product,item) {
  return `${item.id}:${item.option}:${JSON.stringify(normalizedPersonalization(product,item.personalization))}`;
}
export function expandBag(catalogue,selections) {
  if(!Array.isArray(selections)||selections.length<1||selections.length>80)throw new Error('Choose between 1 and 80 designs or sets.');
  const items=new Map(),seen=new Set();
  for(const selection of selections){
    const product=catalogue.find(p=>p.id===selection?.id);
    if(!product||!validOption(catalogue,product,selection.option)||!Number.isInteger(selection.quantity)||selection.quantity<1||selection.quantity>10)throw new Error('Check the product options and quantities in your bag.');
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
    return {name:product.name,option:[describeOption(catalogue,product,selection.option),personalizationDescription(product,selection.personalization)].filter(Boolean).join(' · '),quantity:selection.quantity,price:product.price,setSize:isSet(product)?product.components.length:1,setUnit:pieceName(product)};
  });
}
