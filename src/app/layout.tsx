import type { Metadata, Viewport } from 'next';
import { ThemeProvider } from '@/context/ThemeContext';
import { WidgetProvider } from '@/context/WidgetContext';
import '@/styles/globals.css';
import '@/styles/terminal.css';

export const metadata: Metadata = {
  title: 'MiniDash - Digital Essentials Hub',
  description: 'A minimal digital essentials hub that unites all essential stuff like weather, news, social media trending, Reddit, HackerNews in one place with a terminal aesthetic.',
  keywords: ['digital hub', 'terminal', 'news aggregator', 'weather', 'reddit', 'hackernews', 'minimal'],
  authors: [{ name: 'MiniDash' }],
  openGraph: {
    title: 'MiniDash - Digital Essentials Hub',
    description: 'Your minimal digital essentials hub with a terminal aesthetic',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'MiniDash',
    description: 'Your minimal digital essentials hub with a terminal aesthetic',
  },
};

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  themeColor: [
    { media: '(prefers-color-scheme: light)', color: '#ffffff' },
    { media: '(prefers-color-scheme: dark)', color: '#0d1117' },
  ],
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <link
          rel="preconnect"
          href="https://fonts.googleapis.com"
        />
        <link
          rel="preconnect"
          href="https://fonts.gstatic.com"
          crossOrigin="anonymous"
        />
        <link
          href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;700&display=swap"
          rel="stylesheet"
        />
      </head>
      <body>
        <ThemeProvider>
          <WidgetProvider>
            {children}
          </WidgetProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
