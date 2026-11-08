import {
  type Accessor,
  createEffect,
  createMemo,
  createSignal,
  For,
  type JSX,
  on,
  onCleanup,
  onMount,
  Show,
  untrack,
} from 'solid-js';

/** How to show one column. `cell` may return text or any element (colored value, button…). */
export type Column<R> = {
  title: string;
  /** Right-aligned, monospace digits. */
  numeric?: boolean;
  cell: (row: R) => JSX.Element;
  sortKey?: (row: R) => number | null;
};

export type Sort = { column: number; descending: boolean };

/**
 * Where the rows come from: all in memory (staticSource), pages fetched while scrolling (pagedSource),
 * or anything else that knows the row count and can return a row by index.
 */
export type RowSource<R> = {
  count: Accessor<number>;
  /** Reactive; undefined while the row is not loaded yet (shown as "Loading…"). */
  row: (index: number) => R | undefined;
  /** Rows start..end are on screen (plus a margin); a source may fetch them now. */
  request?: (start: number, end: number) => void;
  sort?: { current: Accessor<Sort | undefined>; set: (sort: Sort | undefined) => void };
};

const ESTIMATED_ROW_HEIGHT = 32;
const OVERSCAN = 10;
// Until the table is laid out its real height is unknown; start small, the ResizeObserver corrects it.
const INITIAL_VIEW_HEIGHT = 400;

/**
 * Table with a sticky header. Only the rows in view (plus a margin) are in the DOM, so tables with
 * thousands of rows scroll and update as fast as small ones. All rows have the same height (no wrapping).
 */
export function VirtualTable<R>(props: {
  columns: Column<R>[];
  source: RowSource<R>;
  emptyMessage?: string;
  class?: string;
  /**
   * For each column, the row with its widest cell. Rendered as a collapsed row, so the column widths
   * fit all rows, not only the ones in view, and do not change while scrolling.
   */
  widest?: (R | undefined)[];
}) {
  let wrap!: HTMLDivElement;
  let body: HTMLTableSectionElement | undefined;
  const [scrollTop, setScrollTop] = createSignal(0);
  const [viewHeight, setViewHeight] = createSignal(INITIAL_VIEW_HEIGHT);
  const [rowHeight, setRowHeight] = createSignal(ESTIMATED_ROW_HEIGHT);
  const [localSort, setLocalSort] = createSignal<Sort>();
  const sort = () => (props.source.sort ? props.source.sort.current() : localSort());
  const setSort = (next: Sort | undefined) => (props.source.sort ? props.source.sort.set(next) : setLocalSort(next));
  createEffect(
    on(
      () => props.columns,
      (columns, previous) => {
        const current = localSort();
        if (current && (!columns[current.column]?.sortKey || columns[current.column].title !== previous?.[current.column]?.title)) {
          setLocalSort(undefined);
        }
      },
      { defer: true },
    ),
  );
  const toggleSort = (column: number) => {
    wrap.scrollTop = 0;
    const current = sort();
    if (current?.column !== column) {
      setSort({ column, descending: true });
    } else {
      setSort(current.descending ? { column, descending: false } : undefined);
    }
  };

  const order = createMemo(() => {
    const current = localSort();
    const key = current && props.columns[current.column]?.sortKey;
    if (!key || props.source.sort) {
      return undefined;
    }
    const total = props.source.count();
    const keys: (number | null)[] = [];
    for (let i = 0; i < total; i++) {
      const row = props.source.row(i);
      const value = row === undefined ? null : key(row);
      keys.push(value === null || Number.isNaN(value) ? null : value);
    }
    return sortedIndices(keys, current.descending);
  });
  const rowAt = (index: number) => {
    const indices = order();
    return props.source.row(indices ? indices[index] : index);
  };

  // Sizes are read in the ResizeObserver callback, after layout, so reading them does not force an extra layout.
  onMount(() => {
    const observer = new ResizeObserver(() => {
      setViewHeight(wrap.clientHeight);
      const row = body?.querySelector('tr:not(.spacer):not(.sizer)');
      const height = row?.getBoundingClientRect().height ?? 0;
      if (height > 0 && Math.abs(height - rowHeight()) > 0.5) {
        setRowHeight(height);
      }
    });
    observer.observe(wrap);
    onCleanup(() => observer.disconnect());
  });

  const range = createMemo(
    () => {
      const total = props.source.count();
      const start = Math.max(0, Math.floor(scrollTop() / rowHeight()) - OVERSCAN);
      const end = Math.min(total, Math.ceil((scrollTop() + viewHeight()) / rowHeight()) + OVERSCAN);
      return { start, end, total };
    },
    undefined,
    { equals: (a, b) => a.start === b.start && a.end === b.end && a.total === b.total },
  );

  const indices = createMemo(() => {
    const { start, end } = range();
    return Array.from({ length: end - start }, (_, i) => start + i);
  });

  createEffect(() => {
    const { start, end } = range();
    untrack(() => props.source.request?.(start, end));
  });

  return (
    <div class={`table-wrap ${props.class ?? ''}`} ref={wrap} onScroll={() => setScrollTop(wrap.scrollTop)}>
      <Show when={range().total > 0} fallback={<p class="muted">{props.emptyMessage ?? 'No data.'}</p>}>
        <table class="data">
          <thead>
            <tr>
              <For each={props.columns}>
                {(column, i) => {
                  const direction = () => (sort()?.column === i() ? (sort()!.descending ? 'descending' : 'ascending') : undefined);
                  return (
                    <th classList={{ num: column.numeric, sorted: !!direction() }} aria-sort={direction()}>
                      <Show when={column.sortKey} fallback={column.title}>
                        <SortButton
                          title={column.title}
                          direction={direction()}
                          markFirst={!!column.numeric}
                          onClick={() => toggleSort(i())}
                        />
                      </Show>
                    </th>
                  );
                }}
              </For>
            </tr>
          </thead>
          <tbody ref={body}>
            <Show when={props.widest}>
              {(widest) => (
                <tr class="sizer" aria-hidden="true">
                  <For each={props.columns}>
                    {(column, i) => {
                      const row = () => widest()[i()];
                      return (
                        <td classList={{ num: column.numeric }}>
                          <Show when={row()}>{(r) => column.cell(r())}</Show>
                        </td>
                      );
                    }}
                  </For>
                </tr>
              )}
            </Show>
            <Spacer height={range().start * rowHeight()} columns={props.columns.length} />
            <For each={indices()}>
              {(index) => (
                <Show
                  when={rowAt(index)}
                  fallback={
                    <tr class="loading">
                      <td class="muted" colSpan={props.columns.length}>
                        Loading…
                      </td>
                    </tr>
                  }
                >
                  {(row) => (
                    <tr>
                      <For each={props.columns}>
                        {(column) => <td classList={{ num: column.numeric }}>{column.cell(row())}</td>}
                      </For>
                    </tr>
                  )}
                </Show>
              )}
            </For>
            <Spacer height={(range().total - range().end) * rowHeight()} columns={props.columns.length} />
          </tbody>
        </table>
      </Show>
    </div>
  );
}

function SortButton(props: {
  title: string;
  direction: 'ascending' | 'descending' | undefined;
  markFirst: boolean;
  onClick: () => void;
}) {
  const mark = () => (
    <span class="sort-mark" aria-hidden="true">
      {props.direction === 'descending' ? '▼' : props.direction === 'ascending' ? '▲' : ''}
    </span>
  );
  return (
    <button type="button" class="sort-button" onClick={() => props.onClick()}>
      {props.markFirst && mark()}
      {props.title}
      {!props.markFirst && mark()}
    </button>
  );
}

function sortedIndices(keys: (number | null)[], descending: boolean): number[] {
  const sign = descending ? -1 : 1;
  return keys
    .map((_, i) => i)
    .sort((a, b) => {
      const x = keys[a];
      const y = keys[b];
      if (x === null || y === null) {
        return x === y ? 0 : x === null ? 1 : -1;
      }
      return x === y ? 0 : x < y ? -sign : sign;
    });
}

function Spacer(props: { height: number; columns: number }) {
  return (
    <tr class="spacer" aria-hidden="true">
      <td colSpan={props.columns} style={{ height: `${props.height}px` }} />
    </tr>
  );
}
