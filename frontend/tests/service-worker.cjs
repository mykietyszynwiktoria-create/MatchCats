const assert=require('assert/strict'),fs=require('fs'),path=require('path'),vm=require('vm');
(async()=>{
 const handlers={},entries=new Map(),puts=[];
 const origin='https://matchcats.test';let online=true,version='fresh',ok=true;
 const cache={addAll:async()=>{},match:async request=>entries.get(request.url||request),put:async(request,response)=>{puts.push(request.url);entries.set(request.url,response);}};
 const caches={open:async()=>cache,match:cache.match,keys:async()=>['matchcats-shell-v1'],delete:async name=>{assert.equal(name,'matchcats-shell-v1');return true;}};
 vm.runInNewContext(fs.readFileSync(path.resolve(__dirname,'../service-worker.js'),'utf8'),{URL,location:{origin},self:{addEventListener:(name,handler)=>handlers[name]=handler,skipWaiting:async()=>{},clients:{claim:async()=>{}}},caches,fetch:async()=>{if(!online)throw Error('offline');return {ok,type:'basic',version,clone(){return this;}};}});
 async function dispatch(url,method='GET'){
  let response;const tasks=[];
  handlers.fetch({request:{url:origin+url,method},respondWith:value=>response=Promise.resolve(value),waitUntil:value=>tasks.push(value)});
  const result=response?await response:undefined;await Promise.all(tasks);return result;
 }
 for(const url of ['/users/me','/notifications?limit=50','/cats/1/photo','/auth/csrf','/billing/me','/unknown','/private.jpg','/unknown.js'])assert.equal(await dispatch(url),undefined,'Private or unknown requests must bypass cache: '+url);
 assert.deepEqual(puts,[]);
 const asset=origin+'/live.js';entries.set(asset,{version:'stale'});
 assert.equal((await dispatch('/live.js')).version,'fresh','Online requests must refresh the shell');
 ok=false;version='failed';
 assert.equal((await dispatch('/live.js')).version,'failed');
 online=false;assert.equal((await dispatch('/live.js')).version,'fresh','Offline requests use only cached shell assets');
 assert.equal(await dispatch('/live.js','POST'),undefined);
 assert.equal(await dispatch('/live.js?private=1'),undefined);
 let activation;handlers.activate({waitUntil:task=>activation=task});await activation;
 console.log('PASS: worker bypasses private requests, refreshes shell online, supports offline assets and removes old caches.');
})().catch(error=>{console.error(error);process.exitCode=1;});
