'use client';

import ThemeToggle from '@/components/ui/ThemeToggle';
import ClockWidget from '@/components/widgets/ClockWidget';
import QuoteWidget from '@/components/widgets/QuoteWidget';
import WidgetsManager from '@/components/layout/WidgetsManager/WidgetsManager';
import { useWidgets } from '@/context/WidgetContext';
import styles from './Header.module.css';

interface HeaderProps {
  onOpenCli?: () => void;
}

const ASCII_LOGO = `
════════════ MINI DASH ════════════
digital essentials`;

export default function Header({ onOpenCli }: HeaderProps) {
  const { widgets } = useWidgets();
  const showQuote = widgets.some(w => w.id === 'quote');
  const activeFlags = widgets.length > 0 ? widgets.map(w => `--${w.id}`).join(' ') : '--none';

  return (
    <header className={styles.header}>
      <div className={styles.logoContainer}>
        <pre className={styles.logo} aria-label="MiniDash">{ASCII_LOGO}</pre>
      </div>
      <div className={styles.topControls}>
        <ThemeToggle />
        <WidgetsManager />
        {onOpenCli && (
          <button onClick={onOpenCli} className={styles.cliButton} aria-label="Open CLI">
            ⌘
          </button>
        )}
      </div>
      <div className={styles.headerContent}>
        <ClockWidget />
        {showQuote && <QuoteWidget />}
      </div>
      <div className={styles.tagline}>
        <span className={styles.prompt}>$</span>
        <span className={styles.command}>fetch</span>
        <span className={styles.args}>{activeFlags}</span>
      </div>
    </header>
  );
}
