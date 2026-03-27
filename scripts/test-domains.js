const fetch = globalThis.fetch;

const urls = [
  'https://api.reddit.com/r/all/hot.json?limit=3&raw_json=1',
  'https://www.reddit.com/r/all/hot.json?limit=3&raw_json=1',
  'https://reddit.com/r/all/hot.json?limit=3&raw_json=1',
  'https://old.reddit.com/r/all/hot.json?limit=3&raw_json=1',
];

(async () => {
  for (const url of urls) {
    try {
      const r = await fetch(url, {
        headers: {
          'User-Agent': 'minidash-test/1.0',
          Accept: 'application/json',
        },
      });
      const text = await r.text();
      console.log(`${url.split('/')[2].padEnd(20)} | status=${r.status} | json=${text.slice(0, 50).includes('{')}`);
    } catch (e) {
      console.log(`${url.split('/')[2].padEnd(20)} | ERROR: ${e.message}`);
    }
  }
})();
