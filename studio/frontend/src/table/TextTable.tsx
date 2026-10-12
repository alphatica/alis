// Tables as sent by the backend (Table.java), all rows at once or in pages. Rows are formatted as text once, so the
// widest cells can be measured and scrolling does not format them again.

import { type Accessor, createMemo } from 'solid-js';
import { type ColumnSpec, formatCell, formatRow, isNumeric, type Value } from '../format';
import { type PagedSource, staticSource, type TableData } from './sources';
import { type Column, VirtualTable } from './VirtualTable';

/**
 * Columns showing cell i of a row formatted by formatRow. Recomputed only when titles or alignment change, so
 * loading another page or refreshing the data does not rebuild the table.
 */
export function textColumns(specs: Accessor<ColumnSpec[] | undefined>): Accessor<Column<string[]>[]> {
  return createMemo(
    () =>
      (specs() ?? []).map((spec, i) => ({
        title: spec.title,
        numeric: isNumeric(spec),
        cell: (row: string[]) => row[i],
      })),
    [],
    {
      equals: (a, b) =>
        a.length === b.length && a.every((column, i) => column.title === b[i].title && column.numeric === b[i].numeric),
    },
  );
}

/** Columns showing the values `cells(row)`, formatted when shown; for rows that are not plain tables. */
export function valueColumns<R>(specs: ColumnSpec[], cells: (row: R) => Value[]): Column<R>[] {
  return specs.map((spec, i) => ({
    title: spec.title,
    numeric: isNumeric(spec),
    cell: (row: R) => formatCell(spec, cells(row)[i]),
  }));
}

/** Whole table already in memory. */
export function TextTable(props: { table: TableData; class?: string }) {
  const columns = textColumns(() => props.table.columns);
  const rows = createMemo(() => props.table.rows.map((row) => formatRow(props.table.columns, row)));
  const widest = createMemo(() => widestRows(rows(), props.table.columns.length));
  return (
    <VirtualTable
      columns={columns()}
      source={staticSource(rows)}
      emptyMessage={props.table.emptyMessage}
      class={props.class}
      widest={widest()}
    />
  );
}

/** Table fetched page by page while scrolling. */
export function PagedTable(props: { source: PagedSource; class?: string }) {
  const columns = textColumns(() => props.source.data()?.columns);
  return (
    <VirtualTable
      columns={columns()}
      source={props.source}
      emptyMessage={props.source.data()?.emptyMessage}
      class={props.class}
    />
  );
}

/** For each column, the row with the longest text in it (close enough to the widest cell). */
function widestRows(rows: string[][], columnCount: number): (string[] | undefined)[] {
  const tracker = widestTracker(columnCount);
  for (const row of rows) {
    tracker.add(row);
  }
  return tracker.rows;
}

/**
 * The same as widestRows, but for rows that arrive over time: add() each new row,
 * so the whole table is not scanned again on every batch.
 */
export function widestTracker(columnCount = 0) {
  let widest: (string[] | undefined)[] = new Array(columnCount).fill(undefined);
  let lengths: number[] = new Array(columnCount).fill(-1);
  return {
    get rows() {
      return widest;
    },
    /** True when the row is the widest in at least one column. */
    add(row: string[]): boolean {
      let changed = false;
      for (let i = 0; i < Math.max(widest.length, row.length); i++) {
        const length = row[i]?.length ?? 0;
        if (length > (lengths[i] ?? -1)) {
          lengths[i] = length;
          widest[i] = row;
          changed = true;
        }
      }
      return changed;
    },
    clear() {
      widest = [];
      lengths = [];
    },
  };
}
