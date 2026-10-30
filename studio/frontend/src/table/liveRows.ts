import { createSignal } from 'solid-js';
import type { RowSource } from './VirtualTable';
import { widestTracker } from './TextTable';

/**
 * Rows appended by a job, formatted as text: kept in a plain array, `version` tells the table that more arrived.
 * Shown newest first: the table reads the array from its end, so appending does not copy it.
 */
export function liveRows() {
  let rows: string[][] = [];
  const [version, setVersion] = createSignal(0);
  // Widest row per column, kept up to date while rows arrive, so the column widths do not change while scrolling
  const tracker = widestTracker();
  const [widest, setWidest] = createSignal<(string[] | undefined)[]>([]);
  const source: RowSource<string[]> = {
    count: () => {
      version();
      return rows.length;
    },
    row: (index) => {
      version();
      return rows[rows.length - 1 - index];
    },
  };
  return {
    source,
    widest,
    append: (items: string[][]) => {
      let widestChanged = false;
      // A loop, not push(...items): a batch may hold more rows than a call may take arguments
      for (const item of items) {
        rows.push(item);
        widestChanged = tracker.add(item) || widestChanged;
      }
      setVersion((v) => v + 1);
      if (widestChanged) {
        setWidest([...tracker.rows]);
      }
    },
    clear: () => {
      rows = [];
      tracker.clear();
      setVersion((v) => v + 1);
      setWidest([]);
    },
  };
}

export type LiveRows = ReturnType<typeof liveRows>;
