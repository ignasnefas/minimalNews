'use client';

import { useState } from 'react';
import { useWidgets } from '@/context/WidgetContext';
import styles from './WidgetsManager.module.css';

export default function WidgetsManager() {
  const { widgets, availableWidgets, toggleWidget, moveWidget, enableAllWidgets, disableAllWidgets, resetWidgets } = useWidgets();
  const [open, setOpen] = useState(false);

  const enabled = widgets;
  const disabled = availableWidgets.filter(w => !widgets.some(x => x.id === w.id));

  return (
    <div className={styles.wrapper}>
      <button className={styles.iconBtn} onClick={() => setOpen(true)}>⚙</button>

      {open && (
        <div className={styles.overlay} onClick={() => setOpen(false)}>
          <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
            <div className={styles.header}>Widgets</div>
            <div className={styles.controlRow}>
              <button className={styles.smallBtn} onClick={() => enableAllWidgets()}>Enable all</button>
              <button className={styles.smallBtn} onClick={() => disableAllWidgets()}>Disable all</button>
              <button className={styles.smallBtn} onClick={() => resetWidgets()}>Reset defaults</button>
            </div>
            <div className={styles.list}>
              {enabled.map((w, index) => (
                <label key={w.id} className={styles.item}>
                  <input type="checkbox" checked onChange={() => toggleWidget(w.id)} />
                  <span className={styles.name}>{w.id}</span>
                  <span className={styles.comp}>{w.component}</span>
                  <div className={styles.reorder}>
                    <button
                      className={styles.smallBtn}
                      onClick={() => moveWidget(w.id, 'up')}
                      disabled={index === 0}
                      aria-label={`Move ${w.id} up`}
                    >
                      ▲
                    </button>
                    <button
                      className={styles.smallBtn}
                      onClick={() => moveWidget(w.id, 'down')}
                      disabled={index === enabled.length - 1}
                      aria-label={`Move ${w.id} down`}
                    >
                      ▼
                    </button>
                  </div>
                </label>
              ))}

              {disabled.length > 0 && (
                <div className={styles.disabledSection}>
                  <span className={styles.disabledHeader}>Available</span>
                  {disabled.map(w => (
                    <label key={w.id} className={styles.item}>
                      <input type="checkbox" checked={false} onChange={() => toggleWidget(w.id)} />
                      <span className={styles.name}>{w.id}</span>
                      <span className={styles.comp}>{w.component}</span>
                    </label>
                  ))}
                </div>
              )}
            </div>
            <div className={styles.footer}>
              <button className={styles.close} onClick={() => setOpen(false)}>Done</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
