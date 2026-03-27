# MiniDash - Android

Native Android port of the MiniDash web app with full home screen widget support.

## Setup

### Option A: Android Studio (Recommended)
1. Open Android Studio
2. **File → Open** → select this `android` folder
3. Android Studio will prompt to download/configure Gradle — accept all defaults
4. Wait for sync to complete
5. Click **Run** (▶) or **Build → Build APK**

### Option B: Command Line
1. Run `setup.bat` to bootstrap the Gradle wrapper
2. Run `gradlew.bat assembleDebug` to build the debug APK
3. APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Features

### In-App Widgets
- **Clock** — Digital clock with date
- **Quote** — Random inspirational quotes
- **Weather** — Current weather + 5-day forecast (OpenMeteo, no API key)
- **News** — RSS headlines from BBC, NPR, Guardian, CNN
- **Hacker News** — Top/New/Best stories from HN
- **Reddit** — Posts from any subreddit
- **Crypto** — Live prices from CoinGecko
- **World Clocks** — Multiple timezone display
- **Todo** — Task list (persisted with Room DB)
- **Trending** — GitHub trending repos
- **System Info** — Device information

### Home Screen Widgets
All major widgets can be placed on your Android home screen:
- Clock, Weather, News, Todo, Quote, Crypto, Hacker News
- Auto-refresh via WorkManager (30 min intervals)
- Manual refresh button on each widget
- Tap to open full app

### Theme Support
8 terminal-style themes: Dark, Light, Retro Green, Amber, Matrix, Blue, Solarized Dark

### Architecture
- **Kotlin** + **Jetpack Compose** (in-app UI)
- **RemoteViews** + **AppWidgetProvider** (home screen widgets)
- **Room** database (todos)
- **OkHttp** + **Gson** (networking, RSS parsing)
- **WorkManager** (periodic widget updates)
- All APIs are free/keyless (OpenMeteo, CoinGecko, HN Firebase, Reddit JSON, RSS feeds)
