'use client';

import { createContext, useContext, useState, useCallback, ReactNode, useEffect } from 'react';

export interface WidgetConfig {
  id: string;
  component: string;
  props?: Record<string, any>;
}

interface WidgetContextType {
  widgets: WidgetConfig[];
  availableWidgets: WidgetConfig[];
  updateWidgetProps: (id: string, props: Record<string, any>) => void;
  refreshAllWidgets: () => void;
  executeCommand: (command: string, args: string[]) => void;
  refreshKey: number;
  toggleWidget: (id: string) => void;
  enableWidget: (id: string) => void;
  disableWidget: (id: string) => void;
  enableAllWidgets: () => void;
  disableAllWidgets: () => void;
  resetWidgets: () => void;
  isEnabled: (id: string) => boolean;
  moveWidget: (id: string, dir: 'up' | 'down') => void;
  reorderWidgets: (orderedIds: string[]) => void;
}

const WidgetContext = createContext<WidgetContextType | undefined>(undefined);

const DEFAULT_WIDGETS: WidgetConfig[] = [
  { id: 'quote', component: 'QuoteWidget' },
  { id: 'weather', component: 'WeatherWidget' },
  { id: 'trending', component: 'TrendingWidget' },
  { id: 'hackernews', component: 'HackerNewsWidget' },
  { id: 'news', component: 'NewsWidget' },
  { id: 'reddit', component: 'RedditWidget' },
  { id: 'crypto', component: 'CryptoWidget' },
  { id: 'clocks', component: 'WorldClocksWidget' },
  { id: 'todo', component: 'TodoWidget' },
  { id: 'systeminfo', component: 'SystemInfoWidget' },
];

// All available widgets (includes new widgets like crypto and clocks)
const AVAILABLE_WIDGETS: WidgetConfig[] = [...DEFAULT_WIDGETS];

export function WidgetProvider({ children }: { children: ReactNode }) {
  // Initialize widgets from localStorage if available to preserve enabled/disabled state across refreshes.
  const [refreshKey, setRefreshKey] = useState(0);

  const getInitialWidgets = () => {
    return DEFAULT_WIDGETS;
  };

  const [widgets, setWidgets] = useState<WidgetConfig[]>(getInitialWidgets);

  useEffect(() => {
    try {
      const raw = localStorage.getItem('enabledWidgets');
      if (raw) {
        const ids: string[] = JSON.parse(raw);
        const initial = ids
          .map(id => AVAILABLE_WIDGETS.find(w => w.id === id))
          .filter(Boolean) as WidgetConfig[];
        if (initial.length) setWidgets(initial);
      }
    } catch (err) {
      // ignore
    }
  }, []);

  useEffect(() => {
    try {
      const ids = widgets.map(w => w.id);
      localStorage.setItem('enabledWidgets', JSON.stringify(ids));
    } catch (err) {
      // ignore
    }
  }, [widgets]);

  const updateWidgetProps = useCallback((id: string, props: Record<string, any>) => {
    setWidgets(prev => prev.map(widget =>
      widget.id === id ? { ...widget, props: { ...widget.props, ...props } } : widget
    ));
  }, []);

  const refreshAllWidgets = useCallback(() => {
    setRefreshKey(prev => prev + 1);
  }, []);

  // Toggle whether a widget is enabled/visible
  const toggleWidget = useCallback((id: string) => {
    setWidgets(prev => {
      const exists = prev.some(w => w.id === id);
      if (exists) {
        return prev.filter(w => w.id !== id);
      }
      const found = AVAILABLE_WIDGETS.find(w => w.id === id);
      if (found) return [...prev, found];
      return prev;
    });
  }, []);

  const enableWidget = useCallback((id: string) => {
    setWidgets(prev => {
      if (prev.some(w => w.id === id)) return prev;
      const found = AVAILABLE_WIDGETS.find(w => w.id === id);
      if (!found) return prev;
      return [...prev, found];
    });
  }, []);

  const disableWidget = useCallback((id: string) => {
    setWidgets(prev => prev.filter(w => w.id !== id));
  }, []);

  const isEnabled = useCallback((id: string) => {
    return widgets.some(w => w.id === id);
  }, [widgets]);

  // Move widget up or down within the enabled widgets list (quote is header-only, keep it anchored)
  const moveWidget = useCallback((id: string, dir: 'up' | 'down') => {
    setWidgets(prev => {
      if (id === 'quote') return prev; // no reordering needed; quote stays in header

      const quoteWidget = prev.find(w => w.id === 'quote');
      const nonQuote = prev.filter(w => w.id !== 'quote');
      const idx = nonQuote.findIndex(w => w.id === id);
      if (idx === -1) return prev;

      const newIdx = dir === 'up' ? Math.max(0, idx - 1) : Math.min(nonQuote.length - 1, idx + 1);
      if (newIdx === idx) return prev;

      const arr = [...nonQuote];
      const [item] = arr.splice(idx, 1);
      arr.splice(newIdx, 0, item);

      return quoteWidget ? [quoteWidget, ...arr] : arr;
    });
  }, []);

  const enableAllWidgets = useCallback(() => {
    setWidgets(AVAILABLE_WIDGETS);
  }, []);

  const disableAllWidgets = useCallback(() => {
    setWidgets([]);
  }, []);

  const resetWidgets = useCallback(() => {
    setWidgets(DEFAULT_WIDGETS);
  }, []);

  const executeCommand = useCallback((command: string, args: string[]) => {
    const commandMap: Record<string, (args: string[]) => void> = {
      weather: (args) => updateWidgetProps('weather', { defaultLocation: args.join(' ') || 'New York' }),
      news: (args) => updateWidgetProps('news', { category: args[0] || 'general' }),
      reddit: (args) => updateWidgetProps('reddit', { subreddit: args[0] || 'all' }),
      hackernews: () => {},
      trending: () => {},
      quote: () => {},
      enable: (args) => enableWidget(args[0]),
      disable: (args) => disableWidget(args[0]),
      toggle: (args) => toggleWidget(args[0]),
    };

    const action = commandMap[command];
    if (action) {
      action(args);
      // Trigger refresh for widgets that need it
      if (['weather', 'news', 'reddit', 'hackernews', 'trending', 'quote'].includes(command)) {
        setRefreshKey(prev => prev + 1);
      }
    }
  }, [updateWidgetProps, enableWidget, disableWidget, toggleWidget]);

  const reorderWidgets = useCallback((orderedIds: string[]) => {
    setWidgets(prev => {
      const quoteWidget = prev.find(w => w.id === 'quote');
      const nonQuote = prev.filter(w => w.id !== 'quote');
      const ordered = orderedIds
        .map(id => nonQuote.find(w => w.id === id))
        .filter((w): w is WidgetConfig => Boolean(w));
      return quoteWidget ? [quoteWidget, ...ordered] : ordered;
    });
  }, []);

  const value = {
    widgets,
    availableWidgets: AVAILABLE_WIDGETS,
    updateWidgetProps,
    refreshAllWidgets,
    executeCommand,
    refreshKey,
    toggleWidget,
    enableWidget,
    disableWidget,
    enableAllWidgets,
    disableAllWidgets,
    resetWidgets,
    isEnabled,
    moveWidget,
    reorderWidgets,
  };

  return (
    <WidgetContext.Provider value={value}>
      {children}
    </WidgetContext.Provider>
  );
}

export function useWidgets() {
  const context = useContext(WidgetContext);
  if (context === undefined) {
    throw new Error('useWidgets must be used within a WidgetProvider');
  }
  return context;
}