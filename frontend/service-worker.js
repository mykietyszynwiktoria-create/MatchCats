const CACHE_NAME='matchcats-shell-v3';
const APP_SHELL=['./','./index.html','./styles.css','./sky-garden.css','./live.css','./i18n.js','./api.js','./safety.js','./error-pages.js','./legal.js','./plans.js','./live.js','./app.js','./assets/sky-hero.jpg','./assets/sky-portraits.jpg'];
const SHELL_PATHS=new Set(APP_SHELL.map(path=>new URL(path,location.origin+'/').pathname));
const isStaticAsset=pathname=>SHELL_PATHS.has(pathname);
self.addEventListener('install',event=>event.waitUntil(caches.open(CACHE_NAME).then(cache=>cache.addAll(APP_SHELL)).then(()=>self.skipWaiting())));
self.addEventListener('activate',event=>event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(key=>key.startsWith('matchcats-shell-')&&key!==CACHE_NAME).map(key=>caches.delete(key)))).then(()=>self.clients.claim())));
self.addEventListener('fetch',event=>{
  if(event.request.method!=='GET')return;
  const url=new URL(event.request.url);
  if(url.origin!==location.origin||url.search||!isStaticAsset(url.pathname))return;
  event.respondWith(fetch(event.request).then(response=>{
    if(response.ok&&response.type==='basic'){
      const copy=response.clone();
      event.waitUntil(caches.open(CACHE_NAME).then(cache=>cache.put(event.request,copy)));
    }
    return response;
  }).catch(()=>caches.open(CACHE_NAME).then(cache=>cache.match(event.request))));
});
