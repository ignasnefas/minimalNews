'use client';

import { useTheme } from '@/context/ThemeContext';
import styles from './ThemeToggle.module.css';

export default function ThemeToggle() {
  const { theme, toggleTheme } = useTheme();

  return (
    <button
      onClick={toggleTheme}
      className={styles.toggle}
      aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`}
      title={`Current: ${theme} theme`}
    >
      <span className={styles.icon}>
        {theme === 'dark' ? '☀' : '☾'}
      </span>
    </button>
  );
}
