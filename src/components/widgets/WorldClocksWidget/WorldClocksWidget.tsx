'use client';

import { useEffect, useState } from 'react';
import TerminalBox from '@/components/ui/TerminalBox';
import styles from './WorldClocksWidget.module.css';

const DEFAULT_ZONES: string[] = [
  'UTC',
  'America/New_York',
  'Europe/London',
  'Asia/Tokyo',
];

function isValidTimeZone(timeZone: string) {
  try {
    new Intl.DateTimeFormat('en-US', { timeZone }).format();
    return true;
  } catch {
    return false;
  }
}

function formatTime(date: Date, timeZone: string) {
  if (!isValidTimeZone(timeZone)) {
    return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }

  try {
    return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit', timeZone });
  } catch {
    return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }
}

export default function WorldClocksWidget() {
  const [zones, setZones] = useState<string[]>(DEFAULT_ZONES);
  const [now, setNow] = useState(new Date());
  const [newZone, setNewZone] = useState('');

  useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(t);
  }, []);

  const addZone = () => {
    const trimmed = newZone.trim();
    if (!trimmed) return;

    if (!isValidTimeZone(trimmed)) {
      alert('Invalid timezone. Please enter a valid IANA timezone (e.g. Europe/London).');
      return;
    }

    if (!zones.includes(trimmed)) setZones(prev => [...prev, trimmed]);
    setNewZone('');
  };

  const removeZone = (zone: string) => {
    setZones(prev => prev.filter(z => z !== zone));
  };

  return (
    <TerminalBox title="clocks --world" icon="🕒" status={`Updated: ${now.toLocaleTimeString()}`}>
      <div className={styles.container}>
        <div className={styles.controls}>
          <input className={styles.input} value={newZone} onChange={(e) => setNewZone(e.target.value)} placeholder="Enter IANA timezone (e.g. Europe/London)" />
          <button className={styles.button} onClick={addZone}>Add</button>
        </div>

        <div className={styles.list}>
          {zones.length === 0 ? (
            <div className={styles.empty}>Add timezones to display clocks</div>
          ) : (
            zones.map((z) => (
              <div key={z} className={styles.row}>
                <div className={styles.time}>{formatTime(now, z)}</div>
                <div className={styles.zone}>{z}</div>
                <button className={styles.remove} onClick={() => removeZone(z)}>✕</button>
              </div>
            ))
          )}
        </div>
      </div>
    </TerminalBox>
  );
}
