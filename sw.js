/* GyanSetu service worker — offline-first cache.
   Strategy: cache app shell on install; serve from cache, fall back to network,
   update cache opportunistically. The CDN React/Babel bundles are cached on
   first successful load so subsequent launches work fully offline. */

const CACHE = 'gyansetu-v3-vendored';
const SHELL = [
  './',
  './index.html',
  './manifest.webmanifest',
  './icon.svg',
  './vendor/react.production.min.js',
  './vendor/react-dom.production.min.js',
  './vendor/babel.min.js',
  './vendor/baloo-bhai-2.css',
  './vendor/fonts/sZlDdRSL-z1VEWZ4YNA7Y5I3dNTgiHw.woff2',
];

self.addEventListener('install', (e) => {
  e.waitUntil((async () => {
    const cache = await caches.open(CACHE);
    // Best-effort: don't fail install if a CDN URL hiccups.
    await Promise.allSettled(SHELL.map(u => cache.add(u).catch(() => {})));
    self.skipWaiting();
  })());
});

self.addEventListener('activate', (e) => {
  e.waitUntil((async () => {
    const keys = await caches.keys();
    await Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k)));
    self.clients.claim();
  })());
});

self.addEventListener('fetch', (e) => {
  const req = e.request;
  if (req.method !== 'GET') return;

  e.respondWith((async () => {
    const cache = await caches.open(CACHE);
    const cached = await cache.match(req, { ignoreVary: true });
    if (cached) {
      // Refresh in background.
      fetch(req).then(r => { if (r && r.ok) cache.put(req, r.clone()); }).catch(() => {});
      return cached;
    }
    try {
      const fresh = await fetch(req);
      if (fresh && fresh.ok && (req.url.startsWith(self.location.origin) ||
          req.url.includes('unpkg.com') || req.url.includes('fonts.g'))) {
        cache.put(req, fresh.clone());
      }
      return fresh;
    } catch {
      // Last resort: serve index.html for navigation requests.
      if (req.mode === 'navigate') {
        const idx = await cache.match('./index.html');
        if (idx) return idx;
      }
      return new Response('Offline', { status: 503, statusText: 'Offline' });
    }
  })());
});
