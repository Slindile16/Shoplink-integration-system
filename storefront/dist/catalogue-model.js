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
export function expandBag(catalogue,selections) {
  if(!Array.isArray(selections)||selections.length<1||selections.length>80)throw new Error('Choose between 1 and 80 designs or sets.');
  const items=new Map(),seen=new Set();
  for(const selection of selections){
    const product=catalogue.find(p=>p.id===selection?.id);
    if(!product||!validOption(catalogue,product,selection.option)||!Number.isInteger(selection.quantity)||selection.quantity<1||selection.quantity>10)throw new Error('Check the product options and quantities in your bag.');
    const key=`${selection.id}:${selection.option}`;
    if(seen.has(key))throw new Error('Combine duplicate selections in your bag.');
    seen.add(key);
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
    return {name:product.name,option:describeOption(catalogue,product,selection.option),quantity:selection.quantity,price:product.price,setSize:isSet(product)?product.components.length:1,setUnit:pieceName(product)};
  });
}
