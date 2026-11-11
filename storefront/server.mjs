import { createServer } from 'node:http';
import { readFile, stat, mkdir } from 'node:fs/promises';
import { readFileSync } from 'node:fs';
import { resolve, dirname, extname, sep } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createHash, randomBytes } from 'node:crypto';
import { DatabaseSync } from 'node:sqlite';
import { expandBag, selectionSnapshot } from './dist/catalogue-model.js';

const directory = dirname(fileURLToPath(import.meta.url));
const catalogue = JSON.parse(readFileSync(resolve(directory, 'dist/catalogue.json'), 'utf8'));
const variants = new Set(catalogue.flatMap(p => p.options.map(o => `${p.sku}-${o}`)));
const provinces = new Set(['Eastern Cape','Free State','Gauteng','KwaZulu-Natal','Limpopo','Mpumalanga','North West','Northern Cape','Western Cape']);
const mime = { '.html':'text/html; charset=utf-8', '.css':'text/css; charset=utf-8', '.js':'text/javascript; charset=utf-8', '.json':'application/json; charset=utf-8', '.svg':'image/svg+xml', '.webp':'image/webp', '.png':'image/png' };
const defaults = { product: 'http://127.0.0.1:8080', order: 'http://127.0.0.1:8081', inventory: 'http://127.0.0.1:8082', payment: 'http://127.0.0.1:8083', delivery: 'http://127.0.0.1:8084' };

class HttpError extends Error { constructor(status, message, code) { super(message); this.status=status;this.code=code; } }
function json(response,status,data) { response.writeHead(status,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store'});response.end(JSON.stringify(data)); }
async function body(request) {
  const chunks=[];let length=0;
  for await(const chunk of request){length+=chunk.length;if(length>16384)throw new HttpError(413,'The checkout request is too large.');chunks.push(chunk);}
  try{return JSON.parse(Buffer.concat(chunks).toString('utf8'));}catch{throw new HttpError(400,'Send a valid JSON checkout request.');}
}
export function validateCheckout(input) {
  const customer=input?.customer;
  if(!customer||typeof customer!=='object')throw new HttpError(400,'Enter the demo customer details.');
  const clean={};
  for(const [field,max] of Object.entries({firstName:60,lastName:60,email:120,address:200,city:80,postalCode:4,province:30})){
    if(typeof customer[field]!=='string'||!customer[field].trim()||customer[field].trim().length>max)throw new HttpError(400,`Enter a valid ${field.replace(/([A-Z])/g,' $1').toLowerCase()}.`);
    clean[field]=customer[field].trim();
  }
  if(!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(clean.email)||!/^\d{4}$/.test(clean.postalCode)||!provinces.has(clean.province))throw new HttpError(400,'Check the email, postal code and province.');
  if(!['on',true].includes(customer.acknowledged))throw new HttpError(400,'Please acknowledge the simulated checkout.');
  if(input.selections!==undefined){
    try{return {customer:clean,items:expandBag(catalogue,input.selections),selections:selectionSnapshot(catalogue,input.selections)};}
    catch(error){throw new HttpError(400,error.message);}
  }
  if(!Array.isArray(input.items)||input.items.length<1||input.items.length>variants.size)throw new HttpError(400,'Your bag must contain valid product options.');
  const seen=new Set();
  const items=input.items.map(item=>{
    if(!item||!variants.has(item.productId)||!Number.isInteger(item.quantity)||item.quantity<1||item.quantity>10||seen.has(item.productId))throw new HttpError(400,'Check the product options and quantities in your bag.');
    seen.add(item.productId);return {productId:item.productId,quantity:item.quantity};
  }).sort((a,b)=>a.productId.localeCompare(b.productId));
  return {customer:clean,items};
}
export async function createStorefront(options={}) {
  const root=resolve(options.staticDirectory||resolve(directory,'dist'));
  const dataDirectory=options.dataDirectory||process.env.DATA_DIRECTORY||resolve(directory,'data');
  await mkdir(dataDirectory,{recursive:true});
  const db=new DatabaseSync(resolve(dataDirectory,'checkout.sqlite'));
  db.exec(`PRAGMA journal_mode=WAL; PRAGMA busy_timeout=5000;
    CREATE TABLE IF NOT EXISTS checkouts (session TEXT NOT NULL, request_key TEXT NOT NULL, payload_hash TEXT NOT NULL, state TEXT NOT NULL, order_id TEXT, customer TEXT NOT NULL, created_at TEXT NOT NULL, PRIMARY KEY(session,request_key));
    CREATE INDEX IF NOT EXISTS checkout_order ON checkouts(session,order_id);`);
  if(!db.prepare('PRAGMA table_info(checkouts)').all().some(column=>column.name==='selection_snapshot'))db.exec('ALTER TABLE checkouts ADD COLUMN selection_snapshot TEXT');
  const services=Object.fromEntries(Object.entries(defaults).map(([key,value])=>[key,options.services?.[key]||process.env[`${key.toUpperCase()}_SERVICE_URL`]||value]));
  const upstream=async(service,path,init={})=>{
    let response;
    try{response=await fetch(new URL(path,services[service]),{...init,headers:{'Content-Type':'application/json',...init.headers},signal:AbortSignal.timeout(8000)});}catch(error){throw new HttpError(503,'The integrated services are unavailable. Please start ShopLink and try again.',error.name==='TimeoutError'?'UPSTREAM_TIMEOUT':'UPSTREAM_UNAVAILABLE');}
    const data=await response.json().catch(()=>null);
    if(!response.ok)throw new HttpError(response.status,data?.detail||data?.message||data?.error||(response.status===409?'An item no longer has enough stock. Please adjust your bag.':'The service could not complete this request.'),'UPSTREAM_REJECTED');
    return data;
  };
  const server=createServer(async(request,response)=>{
    response.setHeader('X-Content-Type-Options','nosniff');response.setHeader('Referrer-Policy','same-origin');response.setHeader('X-Frame-Options','SAMEORIGIN');
    response.setHeader('Content-Security-Policy',"default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; script-src 'self'; connect-src 'self'; base-uri 'self'; form-action 'self'; frame-ancestors 'self'");
    try {
      const url=new URL(request.url,'http://localhost');
      if(url.pathname.startsWith('/api/')) {
        const host=request.headers.host;
        if(!host)throw new HttpError(400,'A host header is required.');
        if(request.headers.origin&&new URL(request.headers.origin).host!==host)throw new HttpError(403,'Cross-origin checkout requests are not allowed.');
        if(request.headers['sec-fetch-site']==='cross-site')throw new HttpError(403,'Cross-site requests are not allowed.');
        let session=request.headers.cookie?.match(/(?:^|;\s*)hv_session=([a-f0-9]{64})(?:;|$)/)?.[1];
        if(!session){session=randomBytes(32).toString('hex');response.setHeader('Set-Cookie',`hv_session=${session}; HttpOnly; SameSite=Strict; Path=/; Max-Age=604800`);}
        if(request.method==='GET'&&url.pathname==='/api/health'){
          await Promise.all(Object.keys(services).map(service=>upstream(service,'/actuator/health')));
          return json(response,200,{status:'UP',mode:'integrated'});
        }
        if(request.method==='GET'&&url.pathname.startsWith('/api/inventory/')){
          const id=decodeURIComponent(url.pathname.slice('/api/inventory/'.length));
          if(!variants.has(id))throw new HttpError(404,'That product option does not exist.');
          return json(response,200,await upstream('inventory',`/inventory/${encodeURIComponent(id)}`));
        }
        if(request.method==='POST'&&url.pathname==='/api/orders'){
          const key=request.headers['idempotency-key'];
          if(typeof key!=='string'||!/^[a-zA-Z0-9-]{16,100}$/.test(key))throw new HttpError(400,'A valid checkout request reference is required.');
          const input=validateCheckout(await body(request));
          const hash=createHash('sha256').update(JSON.stringify(input)).digest('hex');
          const existing=db.prepare('SELECT * FROM checkouts WHERE session=? AND request_key=?').get(session,key);
          if(existing){
            if(existing.payload_hash!==hash)throw new HttpError(409,'This request reference belongs to different checkout details. Restore the original details to recover it.','KEY_REUSED');
            if(existing.order_id)return json(response,200,{order:await upstream('order',`/orders/${encodeURIComponent(existing.order_id)}`)});
            throw new HttpError(409,existing.state==='pending'?'Your order is still being submitted. Please wait before checking again.':'The order service did not confirm the outcome. To prevent a duplicate order, this request will not be sent again. Check the local checkout journal before placing another order.','OUTCOME_UNKNOWN');
          }
          // Probe dependencies before recording a potentially ambiguous order request.
          await Promise.all(['order','inventory','product','payment','delivery'].map(service=>upstream(service,'/actuator/health')));
          db.prepare('INSERT INTO checkouts(session,request_key,payload_hash,state,order_id,customer,created_at,selection_snapshot) VALUES(?,?,?,?,?,?,?,?)').run(session,key,hash,'pending',null,JSON.stringify(input.customer),new Date().toISOString(),input.selections?JSON.stringify(input.selections):null);
          try {
            const order=await upstream('order','/orders',{method:'POST',body:JSON.stringify({customerId:`WEB-${session.slice(0,24)}`,items:input.items})});
            db.prepare("UPDATE checkouts SET state='accepted',order_id=? WHERE session=? AND request_key=?").run(order.id,session,key);
            return json(response,201,{order});
          }catch(error){
            if(error.code==='UPSTREAM_REJECTED'&&error.status>=400&&error.status<500){db.prepare('DELETE FROM checkouts WHERE session=? AND request_key=?').run(session,key);throw error;}
            db.prepare("UPDATE checkouts SET state='unknown' WHERE session=? AND request_key=?").run(session,key);
            throw new HttpError(503,'We could not confirm whether the order was accepted. Keep this bag unchanged and retry to recover the request; it will not be submitted twice.','OUTCOME_UNKNOWN');
          }
        }
        if(request.method==='GET'&&/^\/api\/orders\/[a-zA-Z0-9-]{1,100}$/.test(url.pathname)){
          const id=url.pathname.slice('/api/orders/'.length);
          const receipt=db.prepare('SELECT customer,selection_snapshot FROM checkouts WHERE session=? AND order_id=?').get(session,id);
          if(!receipt)throw new HttpError(404,'No order with this reference belongs to this browser session.');
          let order;
          try{order=await upstream('order',`/orders/${encodeURIComponent(id)}`);}catch(error){if(error.status===404)throw new HttpError(404,'This order is no longer available. The demo services may have been reset.');throw error;}
          let delivery=null;
          if(order.status==='PAID'){try{delivery=await upstream('delivery',`/deliveries/orders/${encodeURIComponent(id)}`);}catch(error){if(error.status!==404)throw error;}}
          const customer=JSON.parse(receipt.customer);
          return json(response,200,{order,delivery,receipt:{city:customer.city,province:customer.province,selections:receipt.selection_snapshot?JSON.parse(receipt.selection_snapshot):null}});
        }
        throw new HttpError(404,'That API route does not exist.');
      }
      if(!['GET','HEAD'].includes(request.method))throw new HttpError(405,'This page only supports GET and HEAD.');
      const pathname=decodeURIComponent(url.pathname);
      if(pathname.includes('\0')||pathname.split(/[\\/]/).some(part=>part.startsWith('.')&&part!==''))throw new HttpError(404,'Page not found.');
      const path=resolve(root,pathname==='/'?'index.html':`.${pathname}`);
      if(!path.startsWith(root+sep))throw new HttpError(404,'Page not found.');
      const info=await stat(path).catch(()=>null);
      if(!info?.isFile())throw new HttpError(404,'Page not found.');
      response.writeHead(200,{'Content-Type':mime[extname(path)]||'application/octet-stream','Cache-Control':path.includes(`${sep}assets${sep}`)?'public, max-age=3600':'no-cache'});
      response.end(request.method==='HEAD'?undefined:await readFile(path));
    }catch(error){if(!response.headersSent)json(response,error.status||500,{message:error.status?error.message:'Something went wrong. Please try again.',code:error.code});else response.end();}
  });
  server.on('close',()=>db.close());
  server.requestTimeout=20000;server.headersTimeout=10000;
  return server;
}
if(process.argv[1]&&resolve(process.argv[1])===fileURLToPath(import.meta.url)){
  const server=await createStorefront();
  const host=process.env.HOST||'127.0.0.1',port=Number(process.env.PORT||3000);
  server.listen(port,host,()=>console.log(`The Heirloom Verse is ready at http://${host==='0.0.0.0'?'localhost':host}:${port}`));
  for(const signal of ['SIGINT','SIGTERM'])process.on(signal,()=>server.close(()=>process.exit(0)));
}
