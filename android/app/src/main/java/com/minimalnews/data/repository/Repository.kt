package com.minimalnews.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.*
import com.minimalnews.data.models.*
import kotlinx.coroutines.*
import okhttp3.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.*
import java.net.URLEncoder

class Repository(context: Context) {
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "MiniDash/1.0 Android")
                    .build()
            )
        }
        .build()

    private val gson = Gson()
    val prefs: SharedPreferences =
        context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)

    private fun fetch(url: String): String {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        return response.body?.string() ?: throw Exception("Empty response")
    }

    // ── Weather ──────────────────────────────────────────────────────────────

    suspend fun fetchWeather(location: String): WeatherData = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(location, "UTF-8")
        val geoJson = JsonParser.parseString(
            fetch("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=en")
        ).asJsonObject

        val results = geoJson.getAsJsonArray("results")
            ?: throw Exception("Location not found")
        val first = results[0].asJsonObject
        val lat = first.get("latitude").asDouble
        val lon = first.get("longitude").asDouble
        val locationName = first.get("name").asString

        val weatherUrl = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature," +
                "weather_code,wind_speed_10m,wind_direction_10m,surface_pressure" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum" +
                "&timezone=auto&forecast_days=5"

        val wJson = JsonParser.parseString(fetch(weatherUrl)).asJsonObject
        val cur = wJson.getAsJsonObject("current")
        val daily = wJson.getAsJsonObject("daily")

        val forecast = mutableListOf<ForecastDay>()
        val dates = daily.getAsJsonArray("time")
        val highs = daily.getAsJsonArray("temperature_2m_max")
        val lows = daily.getAsJsonArray("temperature_2m_min")
        val codes = daily.getAsJsonArray("weather_code")
        val precip = daily.getAsJsonArray("precipitation_sum")

        for (i in 0 until dates.size()) {
            forecast.add(
                ForecastDay(
                    date = dates[i].asString,
                    high = highs[i].asDouble,
                    low = lows[i].asDouble,
                    condition = weatherCodeToCondition(codes[i].asInt),
                    icon = weatherCodeToIcon(codes[i].asInt),
                    precipitation = precip[i].asDouble
                )
            )
        }

        WeatherData(
            location = locationName,
            current = CurrentWeather(
                temp = cur.get("temperature_2m").asDouble,
                feelsLike = cur.get("apparent_temperature").asDouble,
                humidity = cur.get("relative_humidity_2m").asInt,
                windSpeed = cur.get("wind_speed_10m").asDouble,
                windDirection = degreeToDirection(cur.get("wind_direction_10m").asInt),
                condition = weatherCodeToCondition(cur.get("weather_code").asInt),
                icon = weatherCodeToIcon(cur.get("weather_code").asInt),
                visibility = 10.0,
                pressure = cur.get("surface_pressure").asDouble
            ),
            forecast = forecast
        )
    }

    // ── News (RSS) ───────────────────────────────────────────────────────────

    suspend fun fetchNews(category: String = "all"): List<NewsItem> =
        withContext(Dispatchers.IO) {
            val feeds = mapOf(
                "https://feeds.bbci.co.uk/news/rss.xml" to "BBC",
                "https://feeds.npr.org/1001/rss.xml" to "NPR",
                "https://www.theguardian.com/world/rss" to "Guardian",
                "http://rss.cnn.com/rss/edition.rss" to "CNN"
            )

            val allItems = mutableListOf<NewsItem>()
            feeds.forEach { (url, source) ->
                try {
                    val xml = fetch(url)
                    allItems.addAll(parseRss(xml, source))
                } catch (_: Exception) {
                }
            }
            allItems.sortedByDescending { it.publishedAt }.take(20)
        }

    private fun parseRss(xml: String, source: String): List<NewsItem> {
        val items = mutableListOf<NewsItem>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var inItem = false
        var title = ""
        var link = ""
        var pubDate = ""
        var currentTag = ""

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name ?: ""
                    if (currentTag == "item" || currentTag == "entry") {
                        inItem = true
                        title = ""; link = ""; pubDate = ""
                    }
                    if (inItem && currentTag == "link" && parser.attributeCount > 0) {
                        for (i in 0 until parser.attributeCount) {
                            if (parser.getAttributeName(i) == "href") {
                                link = parser.getAttributeValue(i)
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inItem) {
                        val text = parser.text?.trim() ?: ""
                        when (currentTag) {
                            "title" -> title += text
                            "link" -> if (link.isEmpty()) link += text
                            "pubDate", "published", "updated" -> pubDate += text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if ((parser.name == "item" || parser.name == "entry") && inItem) {
                        if (title.isNotBlank()) {
                            items.add(
                                NewsItem(
                                    id = link.hashCode().toString(),
                                    title = title.trim(),
                                    source = source,
                                    url = link.trim(),
                                    publishedAt = pubDate.trim()
                                )
                            )
                        }
                        inItem = false
                    }
                    currentTag = ""
                }
            }
            parser.next()
        }
        return items
    }

    // ── HackerNews ───────────────────────────────────────────────────────────

    suspend fun fetchHackerNews(type: String = "top", limit: Int = 10): List<HackerNewsItem> =
        withContext(Dispatchers.IO) {
            val ids = gson.fromJson(
                fetch("https://hacker-news.firebaseio.com/v0/${type}stories.json"),
                LongArray::class.java
            )

            coroutineScope {
                ids.take(limit).map { id ->
                    async {
                        try {
                            gson.fromJson(
                                fetch("https://hacker-news.firebaseio.com/v0/item/$id.json"),
                                HackerNewsItem::class.java
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }
        }

    // ── Reddit ───────────────────────────────────────────────────────────────

    suspend fun fetchReddit(
        subreddit: String = "all",
        sort: String = "hot",
        limit: Int = 15
    ): List<RedditPost> = withContext(Dispatchers.IO) {
        val body = fetch("https://www.reddit.com/r/$subreddit/$sort.json?limit=$limit&raw_json=1")
        val json = JsonParser.parseString(body).asJsonObject
        val children = json.getAsJsonObject("data").getAsJsonArray("children")

        children.mapNotNull { child ->
            try {
                val d = child.asJsonObject.getAsJsonObject("data")
                RedditPost(
                    id = d.get("id").asString,
                    title = d.get("title").asString,
                    subreddit = d.get("subreddit").asString,
                    score = d.get("score").asInt,
                    numComments = d.get("num_comments").asInt,
                    url = d.get("url").asString,
                    permalink = d.get("permalink").asString,
                    author = d.get("author").asString,
                    createdAt = d.get("created_utc").asLong
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    // ── Crypto ───────────────────────────────────────────────────────────────

    suspend fun fetchCrypto(
        ids: List<String> = listOf("bitcoin", "ethereum", "solana", "dogecoin")
    ): List<CryptoPrice> = withContext(Dispatchers.IO) {
        val idsParam = ids.joinToString(",")
        val url =
            "https://api.coingecko.com/api/v3/simple/price?ids=$idsParam&vs_currencies=usd&include_24hr_change=true"
        val json = JsonParser.parseString(fetch(url)).asJsonObject

        json.entrySet().map { (id, value) ->
            val obj = value.asJsonObject
            CryptoPrice(
                id = id,
                symbol = id.take(4).uppercase(),
                name = id.replaceFirstChar { it.uppercase() },
                price = obj.get("usd")?.asDouble ?: 0.0,
                change24h = obj.get("usd_24h_change")?.asDouble ?: 0.0
            )
        }
    }

    // ── Quote ────────────────────────────────────────────────────────────────

    suspend fun fetchQuote(): Quote = withContext(Dispatchers.IO) {
        val quotes = gson.fromJson(fetch("https://type.fit/api/quotes"), Array<Quote>::class.java)
        quotes.random()
    }

    // ── Trending (GitHub) ────────────────────────────────────────────────────

    suspend fun fetchTrending(): List<GitHubTrending> = withContext(Dispatchers.IO) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        val url =
            "https://api.github.com/search/repositories?q=created:>$dateStr&sort=stars&order=desc&per_page=10"
        val json = JsonParser.parseString(fetch(url)).asJsonObject
        val items = json.getAsJsonArray("items")

        items.mapNotNull { item ->
            try {
                val obj = item.asJsonObject
                GitHubTrending(
                    name = obj.get("full_name").asString,
                    description = obj.get("description")
                        ?.takeIf { !it.isJsonNull }?.asString ?: "",
                    language = obj.get("language")
                        ?.takeIf { !it.isJsonNull }?.asString ?: "Unknown",
                    stars = obj.get("stargazers_count").asInt,
                    url = obj.get("html_url").asString
                )
            } catch (_: Exception) { null }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    fun saveWidgetData(key: String, data: String) {
        prefs.edit().putString(key, data).apply()
    }

    fun getWidgetData(key: String): String? = prefs.getString(key, null)

    private fun weatherCodeToCondition(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2, 3 -> "Partly Cloudy"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rain"
        71, 73, 75 -> "Snow"
        77 -> "Snow Grains"
        80, 81, 82 -> "Rain Showers"
        85, 86 -> "Snow Showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm + Hail"
        else -> "Unknown"
    }

    private fun weatherCodeToIcon(code: Int): String = when (code) {
        0 -> "☀"
        1, 2, 3 -> "⛅"
        45, 48 -> "🌫"
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> "🌧"
        71, 73, 75, 77, 85, 86 -> "❄"
        95, 96, 99 -> "⛈"
        else -> "🌤"
    }

    private fun degreeToDirection(degrees: Int): String {
        val dirs = listOf(
            "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
        )
        return dirs[(degrees / 22.5).toInt() % 16]
    }
}
