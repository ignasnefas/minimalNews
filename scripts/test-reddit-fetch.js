const fetch = globalThis.fetch || ((...args) => import('node-fetch').then(mod => mod.default(...args)));

// Test script for finding proxies that work on Vercel
// Direct endpoint works locally but FAILS on Vercel (IP blocking)
// 
// Strategy: Find proxies that:
// 1) Work from Vercel's IP ranges
// 2) Can successfully fetch Reddit data
// 3) Return valid JSON (not HTML)
//
// Usage:
//   node scripts/test-reddit-fetch.js direct    # Test direct only
//   node scripts/test-reddit-fetch.js proxy     # Test all proxies (Vercel-focused)
//   node scripts/test-reddit-fetch.js all       # Test both

const directUrl = 'https://api.reddit.com/r/all/hot.json?limit=3&raw_json=1';

// Proxies optimized for Vercel environments - services that handle serverless requests
const vercelOptimizedProxies = [
  // Services with explicit Vercel compatibility
  { prefix: 'https://api.cors.sh/?url=', encode: true, name: 'cors.sh-basic' },
  
  // Proxy services that support different encoding/query patterns
  { prefix: 'https://api-proxy.herokuapp.com/fetch?url=', encode: true, name: 'heroku-proxy' },
  { prefix: 'https://api.example.com/raw?url=', encode: true, name: 'generic-raw' },
  
  // RSS/Alternative data sources (sometimes work when direct doesn't)
  { prefix: 'https://www.reddit.com/r/', encode: false, name: 'reddit-rss-www', rss: true },
  { prefix: 'https://old.reddit.com/r/', encode: false, name: 'old-reddit', rss: true },
  
  // Services known to handle Vercel/serverless
  { prefix: 'https://api.github.com/repos/', encode: true, name: 'github-api-test' },
  { prefix: 'https://jsonplaceholder.typicode.com/posts/1', encode: false, name: 'jsonplaceholder-test' },
  
  // More aggressive proxies
  { prefix: 'https://scraper-api.smartproxy.com/v1?target=reddit&url=', encode: true, name: 'smartproxy' },
  { prefix: 'https://api.bright-data.com/api/v1/query?url=', encode: true, name: 'bright-data' },
  
  // Cloudflare workers (great for Vercel)
  { prefix: 'https://cors-proxy.example.workers.dev/?url=', encode: true, name: 'cloudflare-workers' },
  
  // Different Reddit endpoints
  { prefix: 'https://www.reddit.com/r/', encode: false, name: 'reddit-com-data', direct: true },
  { prefix: 'https://reddit.com/r/', encode: false, name: 'reddit-root-data', direct: true },
];

const testMode = process.argv[2] || 'proxy';

let targets = [];

if (testMode === 'direct' || testMode === 'all') {
  targets.push(directUrl);
}

if (testMode === 'proxy' || testMode === 'all') {
  for (const proxy of vercelOptimizedProxies) {
    let url = '';
    if (proxy.rss) {
      // RSS endpoints
      url = `${proxy.prefix}all/hot.json?limit=3&raw_json=1`;
    } else if (proxy.direct) {
      // Direct alternatives
      url = `${proxy.prefix}all/hot.json?limit=3&raw_json=1`;
    } else {
      // Proxy format
      url = proxy.encode
        ? `${proxy.prefix}${encodeURIComponent(directUrl)}`
        : `${proxy.prefix}${directUrl}`;
    }
    targets.push({ url, proxyName: proxy.name });
  }
}

console.log(`Testing ${testMode === 'proxy' ? 'Vercel-optimized proxy services' : 'all'} (${targets.length} targets)...\n`);

(async () => {
  const results = { working: [], failed: [] };

  for (const target of targets) {
    const url = typeof target === 'string' ? target : target.url;
    const proxyName = typeof target === 'string' ? 'api.reddit.com' : target.proxyName;

    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 10000);

      const r = await fetch(url, {
        headers: {
          'User-Agent': 'minimal-news-test/1.0',
          Accept: 'application/json',
        },
        signal: controller.signal,
      });

      clearTimeout(timeoutId);

      const text = await r.text();
      const contentType = r.headers.get('content-type') || '';

      console.log(`[${proxyName}] status=${r.status} content-type=${contentType.slice(0, 40)}`);

      const isJson = (contentType.includes('application/json') || text.trim().startsWith('{')) && text.trim().length > 10;
      const isHtml = /<html|<body|<!doctype html|<div|<span|<p /i.test(text.slice(0, 500));

      if (r.ok && isJson && !isHtml) {
        try {
          const json = JSON.parse(text);
          if (json?.data?.children || json?.kind === 'Listing') {
            console.log(`✅ WORKS - Valid Reddit JSON\n`);
            results.working.push({ proxyName, url });
            continue;
          }
        } catch (parseErr) {
          console.log(`❌ FAIL - Invalid JSON parse\n`);
        }
      } else {
        console.log(`❌ FAIL - status=${r.status}, html=${isHtml}, json=${isJson}\n`);
      }
      results.failed.push(proxyName);
    } catch (e) {
      console.log(`[${proxyName}] ❌ FAIL - ${e.message}\n`);
      results.failed.push(proxyName);
    }
  }

  console.log('\n=== SUMMARY ===');
  if (results.working.length > 0) {
    console.log(`✅ WORKING PROXIES (${results.working.length}):`);
    results.working.forEach(({ proxyName, url }) => {
      console.log(`   ${proxyName}: ${url.split('?')[0]}`);
    });
  } else {
    console.log('❌ No working proxies found.');
  }

  console.log(`\n❌ Failed: ${results.failed.join(', ')}`);

  if (results.working.length === 0) {
    console.log('\n💡 Recommendation: Direct api.reddit.com endpoint is only reliable method.');
    process.exit(1);
  }
})();
