'use client';

import { useState, useCallback, useEffect } from 'react';
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  DragEndEvent,
} from '@dnd-kit/core';
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';

import Header from '@/components/layout/Header';
import Footer from '@/components/layout/Footer';
import WeatherWidget from '@/components/widgets/WeatherWidget';
import NewsWidget from '@/components/widgets/NewsWidget';
import RedditWidget from '@/components/widgets/RedditWidget';
import HackerNewsWidget from '@/components/widgets/HackerNewsWidget';
import TrendingWidget from '@/components/widgets/TrendingWidget';
import QuoteWidget from '@/components/widgets/QuoteWidget';
import CryptoWidget from '@/components/widgets/CryptoWidget/CryptoWidget';
import WorldClocksWidget from '@/components/widgets/WorldClocksWidget/WorldClocksWidget';
import TodoWidget from '@/components/widgets/TodoWidget';
import SystemInfoWidget from '@/components/widgets/SystemInfoWidget';
import DraggableWidget from '@/components/ui/DraggableWidget';
import CommandLine from '@/components/ui/CommandLine';
import { useWidgets } from '@/context/WidgetContext';
import { useKeyboardShortcuts } from '@/hooks/useKeyboardShortcuts';
import styles from './page.module.css';

const WIDGET_COMPONENTS = {
  WeatherWidget,
  NewsWidget,
  RedditWidget,
  HackerNewsWidget,
  TrendingWidget,
  QuoteWidget,
  CryptoWidget,
  WorldClocksWidget,
  TodoWidget,
  SystemInfoWidget,
};

export default function Home() {
  const { widgets, executeCommand, refreshKey, reorderWidgets } = useWidgets();
  const [isCliOpen, setIsCliOpen] = useState(false);
  const [isFocusMode, setIsFocusMode] = useState(false);
  const [widgetOrder, setWidgetOrder] = useState(() => widgets.filter(w => w.id !== 'quote').map(w => w.id));

  // Keep widget order in sync when enabled widgets change (exclude quote widget from main grid, it is rendered in Header)
  useEffect(() => {
    setWidgetOrder(widgets.filter(w => w.id !== 'quote').map(w => w.id));
  }, [widgets]);


  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 10,
      },
    }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;

    if (over && active.id !== over.id) {
      setWidgetOrder((items) => {
        const oldIndex = items.indexOf(active.id as string);
        const newIndex = items.indexOf(over.id as string);

        const nextOrder = arrayMove(items, oldIndex, newIndex);
        reorderWidgets(nextOrder);
        return nextOrder;
      });
    }
  };

  const handleCommand = useCallback((command: string, args: string[]) => {
    executeCommand(command, args);
  }, [executeCommand]);

  const toggleFocusMode = useCallback(() => {
    setIsFocusMode(prev => !prev);
  }, []);

  useKeyboardShortcuts({
    onOpenCli: () => setIsCliOpen(true),
    onToggleFocus: toggleFocusMode,
  });

  const renderWidget = (widgetId: string) => {
    if (widgetId === 'quote') {
      return null;
    }

    const widget = widgets.find(w => w.id === widgetId);
    if (!widget) return null;

    const Component = WIDGET_COMPONENTS[widget.component as keyof typeof WIDGET_COMPONENTS];
    if (!Component) return null;

    return (
      <DraggableWidget key={`${widget.id}-${refreshKey}`} id={widget.id}>
        <Component key={`${widget.id}-${refreshKey}-content`} {...widget.props} />
      </DraggableWidget>
    );
  };

  return (
    <div className={`${styles.container} ${isFocusMode ? styles.focusMode : ''}`}>
      <Header onOpenCli={() => setIsCliOpen(true)} />

      <main className={styles.main}>
        <DndContext
          sensors={sensors}
          collisionDetection={closestCenter}
          onDragEnd={handleDragEnd}
        >
          <SortableContext items={widgetOrder} strategy={verticalListSortingStrategy}>
            <div className={styles.grid}>
              {widgetOrder.map(renderWidget)}
            </div>
          </SortableContext>
        </DndContext>
      </main>

      <Footer />

      <CommandLine
        isOpen={isCliOpen}
        onClose={() => setIsCliOpen(false)}
        onCommand={handleCommand}
      />
    </div>
  );
}
