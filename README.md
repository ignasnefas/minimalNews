# MiniDash - Digital Essentials Hub

A minimal digital essentials hub that unites all essential stuff like weather, news, social media trending, Reddit, HackerNews/Slashdot in one place, styled with a terminal/ASCII aesthetic like [wttr.in](https://wttr.in/).

## Features

- 🌤️ **Weather Widget** - Real-time weather with ASCII art (powered by wttr.in)
- 📰 **News Widget** - Latest headlines across categories
- 🔥 **Reddit Widget** - Top posts from various subreddits
- 🔶 **HackerNews Widget** - Top/new/best stories from HN
- 📈 **Trending Widget** - Twitter/X trends and GitHub trending repos
- 💬 **Quote of the Day** - Daily inspirational quotes
- 🕐 **Live Clock** - Real-time clock display
- 🌙 **Light/Dark Themes** - Toggle between themes
- 📱 **Mobile Friendly** - Fully responsive design

## Tech Stack

- **Framework**: Next.js 14 with App Router
- **Language**: TypeScript
- **Styling**: CSS Modules with CSS Variables
- **Font**: JetBrains Mono (monospace)

## Getting Started

### Prerequisites

- Node.js 18+ 
- npm or yarn

### Installation

```bash
# Install dependencies
npm install

# Copy environment variables
cp .env.example .env.local
```

Edit `.env.local` and add your API keys (see Environment Variables section below).

```bash
# Run development server
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) to view the app.

### Environment Variables

- `REDDIT_USER_AGENT` (optional, strongly recommended on Vercel/hosted environments)
  - default: `minidash/1.0 (by u/minidash) - contact: https://github.com/minidash`
  - set to a unique descriptive value like `minidash/1.0 (by u/youruser)` to avoid Reddit rate-limits/blocks.

No other environment variables are required for this application. All data is fetched from public sources without API keys.

### Build for Production

```bash
npm run build
npm start
```

## Project Structure

```
src/
├── app/
│   ├── api/               # API routes
│   │   ├── weather/       # Weather data endpoint
│   │   ├── hackernews/    # HackerNews endpoint
│   │   ├── reddit/        # Reddit endpoint
│   │   ├── news/          # News endpoint
│   │   ├── trending/      # Social trending endpoint
│   │   └── quote/         # Quote of the day endpoint
│   ├── layout.tsx         # Root layout
│   ├── page.tsx           # Home page
│   └── page.module.css    # Home page styles
├── components/
│   ├── layout/            # Layout components
│   │   ├── Header/
│   │   └── Footer/
│   ├── ui/                # UI components
│   │   ├── TerminalBox/
│   │   ├── TerminalList/
│   │   ├── ThemeToggle/
│   │   └── AsciiArt/
│   └── widgets/           # Feature widgets
│       ├── WeatherWidget/
│       ├── NewsWidget/
│       ├── RedditWidget/
│       ├── HackerNewsWidget/
│       ├── TrendingWidget/
│       ├── QuoteWidget/
│       └── ClockWidget/
├── context/
│   └── ThemeContext.tsx   # Theme state management
├── styles/
│   ├── globals.css        # Global styles
│   ├── terminal.css       # Terminal styling
│   └── themes.ts          # Theme definitions
└── types/
    └── api.ts             # TypeScript type definitions
```

## Deployment

### Vercel (Recommended)

1. Push your code to a Git repository (GitHub, GitLab, BitBucket)
2. Import the project on [Vercel](https://vercel.com)
3. Vercel will automatically detect Next.js and configure the build
4. Deploy!

```bash
# Or use Vercel CLI
npm i -g vercel
vercel
```

## API Endpoints

| Endpoint | Description | Parameters |
|----------|-------------|------------|
| `/api/weather` | Weather data | `location` |
| `/api/hackernews` | HN stories | `type`, `limit` |
| `/api/reddit` | Reddit posts | `subreddit`, `sort`, `limit` |
| `/api/news` | News headlines | `category`, `limit` |
| `/api/trending` | Social trends | - |
| `/api/quote` | Quote of the day | - |

## Customization

### Themes

Edit `src/styles/themes.ts` and `src/styles/globals.css` to customize colors.

### Adding Widgets

1. Create a new folder in `src/components/widgets/`
2. Add your widget component and styles
3. Import and add to `src/app/page.tsx`

## License

MIT
