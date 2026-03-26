import { NextResponse } from 'next/server';
import type { RedditPost, ApiResponse } from '@/types/api';

export async function GET(request: Request) {
  const { searchParams } = new URL(request.url);
  const subreddit = searchParams.get('subreddit') || 'all';
  const sort = searchParams.get('sort') || 'hot'; // hot, new, top, rising
  const limit = Math.min(parseInt(searchParams.get('limit') || '15', 10), 25);

  // Reddit API endpoint - try multiple domain variations
  // api.reddit.com may be blocked on Vercel, but other domains might work
  // Testing on Vercel showed these work:
  // - www.reddit.com - main domain
  // - old.reddit.com - legacy interface
  // - reddit.com - root domain
  const redditHosts = [
    'https://api.reddit.com',
    'https://www.reddit.com',
    'https://reddit.com',
    'https://old.reddit.com',
  ];
  const query = `/r/${subreddit}/${sort}.json?limit=${limit}&raw_json=1`;

  function getUserAgent() {
    return (
      process.env.REDDIT_USER_AGENT ||
      'minimal-news/1.0 (by u/minimal-news-app) - contact: https://github.com/your-user/minimalNews'
    );
  }

  type ValidateJsonResponseResult =
    | { ok: true; text: string }
    | { ok: false; reason: string; body: string };

  async function validateJsonResponse(resp: Response): Promise<ValidateJsonResponseResult> {
    const contentType = resp.headers.get('content-type') || '';
    const text = await resp.text().catch(() => '');
    const isHtml = /<html|<body|<!doctype html/i.test(text);

    if (!resp.ok) {
      return {
        ok: false,
        reason: `HTTP ${resp.status} ${resp.statusText}`,
        body: text,
      };
    }

    if (!contentType.includes('application/json')) {
      return {
        ok: false,
        reason: `Invalid content-type: ${contentType}`,
        body: text,
      };
    }

    if (isHtml) {
      return {
        ok: false,
        reason: 'Received HTML body (likely blocked page)',
        body: text,
      };
    }

    return {
      ok: true,
      text,
    };
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
    let redditBody: string | null = null;

    // Try direct endpoints (api.reddit.com, www.reddit.com)
    for (const host of redditHosts) {
      try {
        const redditUrl = `${host}${query}`;
        response = await fetch(redditUrl, {
          headers: {
            'User-Agent': getUserAgent(),
            'Accept': 'application/json',
          },
          next: { revalidate: 300 }, // Cache for 5 minutes
        });

        const validation = await validateJsonResponse(response);

        if (validation.ok) {
          redditBody = validation.text;
          break;
        }

        lastError = `Reddit API unavailable from ${host}: ${validation.reason}`;

        if (response.status !== 403 && response.status !== 429) {
          // If non-rate-limited error (e.g. 404), no need to keep retrying
          break;
        }
      } catch (subError) {
        lastError = `Fetch failed for ${host}: ${(subError as Error).message}`;
      }
    }

    if (!redditBody) {
      throw new Error(
        `${lastError || 'Unknown Reddit fetch failure'}. ` +
        'Reddit requires direct endpoint access - public proxies are blocked. ' +
        'If your hosting provider blocks api.reddit.com, your only option is authenticated Reddit API access via OAuth.'
      );
    }

    let data;
    try {
      data = JSON.parse(redditBody);
    } catch (parseError) {
      throw new Error(`Failed to parse Reddit JSON response: ${(parseError as Error).message}`);
    }

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
