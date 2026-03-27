(async () => {
  try {
    const url = 'https://nominatim.openstreetmap.org/search?q=Vilnius&format=json&limit=1';

    let r = await fetch(url);
    console.log('NO-UA status', r.status);
    let t = await r.text();
    console.log('NO-UA body preview:', t.slice(0, 500));

    r = await fetch(url, { headers: { 'User-Agent': 'minidash/1.0 (dev@example.com)' } });
    console.log('WITH-UA status', r.status);
    t = await r.text();
    console.log('WITH-UA body preview:', t.slice(0, 500));

    let data;
    try { data = JSON.parse(t); } catch (e) { console.log('parse error', e); }

    if (data && data[0]) {
      const lat = data[0].lat, lon = data[0].lon;
      const weatherUrl = `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m,visibility,surface_pressure&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto`;
      const wr = await fetch(weatherUrl);
      console.log('Open-Meteo status', wr.status);
      const wt = await wr.text();
      console.log('Open-Meteo body preview:', wt.slice(0, 500));
    } else {
      console.log('No geocode data');
    }
  } catch (e) {
    console.error('ERR', e);
  }
})();
