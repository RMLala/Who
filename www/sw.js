/* Cache only the public application shell. Never cache or replace Cloud/API responses. */
const C='worshipflow-v5-8-3-appforge-1';
const A=['./','./index.html','./manifest.webmanifest','./icons/icon-192.png','./icons/icon-512.png','./icons/icon-maskable-512.png'];
self.addEventListener('install',e=>e.waitUntil(caches.open(C).then(c=>c.addAll(A)).then(()=>self.skipWaiting())));
self.addEventListener('activate',e=>e.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k.startsWith('worshipflow-')&&k!==C).map(k=>caches.delete(k)))).then(()=>self.clients.claim())));
self.addEventListener('fetch',e=>{
 const url=new URL(e.request.url),allowed=A.some(path=>new URL(path,self.location.href).pathname===url.pathname);
 if(e.request.method!=='GET'||url.origin!==self.location.origin||!allowed||e.request.headers.has('Authorization'))return;
 e.respondWith(caches.match(e.request).then(cached=>cached||fetch(e.request).then(response=>{if(response.ok){const copy=response.clone();e.waitUntil(caches.open(C).then(cache=>cache.put(e.request,copy)))}return response})));
});
