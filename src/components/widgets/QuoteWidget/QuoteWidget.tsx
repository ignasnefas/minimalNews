'use client';

import { useState, useEffect } from 'react';
import type { QuoteOfTheDay, ApiResponse } from '@/types/api';
import styles from './QuoteWidget.module.css';

export default function QuoteWidget() {
  const [quote, setQuote] = useState<QuoteOfTheDay | null>(null);

  useEffect(() => {
    fetchQuote();
  }, []);

  async function fetchQuote() {
    try {
      const response = await fetch('/api/quote');
      const result: ApiResponse<QuoteOfTheDay> = await response.json();
      if (result.data) {
        setQuote(result.data);
      }
    } catch (err) {
      console.error('Failed to fetch quote');
    }
  }

  if (!quote) return null;

  const copyQuote = async () => {
    try {
      await navigator.clipboard.writeText(`"${quote.text}" — ${quote.author}`);
    } catch (err) {
      console.error('Copy failed', err);
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.quote}>
        <span className={styles.mark}>"</span>
        {quote.text}
        <span className={styles.mark}>"</span>
      </div>
      <div className={styles.authorRow}>
        <span className={styles.author}>— {quote.author}</span>
        <div className={styles.authorActions}>
          <button onClick={fetchQuote} className={styles.iconButton} aria-label="New Quote">
            ↻
          </button>
          <button onClick={copyQuote} className={styles.iconButton} aria-label="Copy Quote">
            📋
          </button>
        </div>
      </div>
    </div>
  );
}
