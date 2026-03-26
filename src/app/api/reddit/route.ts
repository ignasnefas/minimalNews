import { NextResponse } from 'next/server';
import type { RedditPost, ApiResponse } from '@/types/api';

export async function GET(request: Request) {
  const { searchParams } = new URL(request.url);
  const subreddit = searchParams.get('subreddit') || 'all';
  const sort = searchParams.get('sort') || 'hot'; // hot, new, top, rising
  const limit = Math.min(parseInt(searchParams.get('limit') || '15', 10), 25);

  // prefer the official api.reddit.com endpoint (less likely to be blocked by security / hosting policies)
  // avoid old.reddit.com where Vercel and cloud workers are often blocked.
  const redditHosts = ['https://api.reddit.com', 'https://www.reddit.com'];
  const query = `/r/${subreddit}/${sort}.json?limit=${limit}&raw_json=1`;

  function getUserAgent() {
    return (
      process.env.REDDIT_USER_AGENT ||
      'minimal-news/1.0 (by u/minimal-news-app) - contact: https://github.com/your-user/minimalNews'
    );
  }

  async function fetchFromHost(host: string) {
    const redditUrl = `${host}${query}`;
    const response = await fetch(redditUrl, {
      headers: {
        'User-Agent': getUserAgent(),
        'Accept': 'application/json',
      },
      next: { revalidate: 300 }, // Cache for 5 minutes
    });
    return response;
  }

  try {
    let response: Response | null = null;
    let lastError: string | null = null;

    for (const host of redditHosts) {
      try {
        response = await fetchFromHost(host);
        if (response.ok) break;

        const body = await response.text().catch(() => '<unreadable body>');
        const blockedContent = /<html|<body|whoa there, pardner!/i.test(body);

        lastError = `Reddit API unavailable from ${host}: ${response.status} ${response.statusText} - ${blockedContent ? 'blocked by network policy' : body}`;

        // Retry with next host on 403/429 or HTML-blocked pages
        if (!blockedContent && response.status !== 403 && response.status !== 429) {
          break;
        }
      } catch (subError) {
        lastError = `Fetch failed for ${host}: ${(subError as Error).message}`;
      }
    }

    if (!response || !response.ok) {
      throw new Error(lastError || 'Unknown Reddit fetch failure');
    }

    const data = await response.json();

    const children = data?.data?.children ?? [];
    const posts: RedditPost[] = Array.isArray(children)
      ? children
          .filter((child: any) => child?.kind === 't3' && child?.data)
          .map((child: any) => {
            const post = child.data;
            return {
              id: post.id,
              title: post.title,
              subreddit: post.subreddit,
              score: post.score,
              numComments: post.num_comments,
              url: post.url,
              permalink: `https://reddit.com${post.permalink}`,
              author: post.author,
              createdAt: new Date(post.created_utc * 1000).toISOString(),
            };
          })
      : [];

    if (posts.length === 0) {
      throw new Error('No Reddit posts available');
    }

    const result: ApiResponse<RedditPost[]> = {
      data: posts,
      error: null,
      timestamp: new Date().toISOString(),
    };

    return NextResponse.json(result);
  } catch (error) {
    console.error('Reddit API error:', error);

    const result: ApiResponse<RedditPost[]> = {
      data: null,
      error: `Unable to fetch Reddit data: ${(error as Error).message}`,
      timestamp: new Date().toISOString(),
    };

    return NextResponse.json(result, { status: 502 });
  }
}
