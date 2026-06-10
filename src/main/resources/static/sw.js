const CACHE_NAME = 'upimesh-v2';
const ASSETS = [
    '/',
    '/index.html',
    '/styles.css',
    '/app.js',
    '/manifest.json',
    'https://cdn-icons-png.flaticon.com/512/10149/10149458.png'
];

// Install Event
self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME).then((cache) => cache.addAll(ASSETS))
    );
    self.skipWaiting();
});

// Activate Event
self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys().then((keys) => {
            return Promise.all(
                keys.filter(key => key !== CACHE_NAME).map(key => caches.delete(key))
            );
        })
    );
});

// Fetch Event: Network-First for API, Cache-First for Assets
self.addEventListener('fetch', (event) => {
    const url = new URL(event.request.url);

    // Bypass cache for all /api/ calls to ensure heartbeat and ingestion are real network probes
    if (url.pathname.startsWith('/api/')) {
        return; // Let it go to the network directly
    }

    event.respondWith(
        caches.match(event.request).then((cachedResponse) => {
            if (cachedResponse) {
                return cachedResponse;
            }
            return fetch(event.request);
        })
    );
});
