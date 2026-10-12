// Row sources for VirtualTable.

import { type Accessor, createSignal } from 'solid-js';
import { api, type ErrorMessage, toErrorMessage } from '../api';
import { type ColumnSpec, formatRow, type Value } from '../format';
import type { RowSource } from './VirtualTable';

/** Table as sent by the backend (Table.java): one value per column in each row, formatted by the GUI. */
export type TableData = {
  columns: ColumnSpec[];
  rows: Value[][];
  emptyMessage: string;
};

/** Everything except the rows; sent with every page (Table.TablePage). */
export type TableMeta = {
  columns: ColumnSpec[];
  total: number;
  emptyMessage: string;
};

/**
 * All rows already in memory. Also for tables filled live (e.g. job output over the WebSocket):
 * keep the rows in a signal and append to it.
 */
export function staticSource<R>(rows: Accessor<R[]>): RowSource<R> {
  return {
    count: () => rows().length,
    row: (index) => rows()[index],
  };
}

/** Rows are formatted as text when their page arrives. */
export type PagedSource = RowSource<string[]> & {
  /** Undefined until the first page arrives. */
  data: Accessor<TableMeta | undefined>;
  error: Accessor<ErrorMessage | undefined>;
  /** (Re)load: fetches the first page and the pages in view. Old rows stay on screen until new ones arrive. */
  load: () => void;
  /** Forgets all rows, e.g. while the data behind them is being replaced; load() fetches them again. */
  clear: () => void;
};

const PAGE_SIZE = 200;
const MAX_CACHED_PAGES = 50;

/**
 * Rows fetched from a TableRoutes.paged endpoint page by page, when they scroll into view
 * (plus half a page ahead, so scrolling rarely shows "Loading…"). Pages are cached;
 * the ones farthest from the view are dropped when the cache is full.
 */
export function pagedSource(url: string): PagedSource {
  const [meta, setMeta] = createSignal<TableMeta>();
  const [error, setError] = createSignal<ErrorMessage>();
  const [version, setVersion] = createSignal(0);
  let pages = new Map<number, string[][]>();
  // Pages from before the last load(): shown until their fresh version arrives, so a reload does not flash.
  let stale = new Map<number, string[][]>();
  let inFlight = new Set<number>();
  let generation = 0;
  let wanted = { first: 0, last: 0 };

  const fetchPage = (page: number) => {
    if (pages.has(page) || inFlight.has(page)) {
      return;
    }
    const requests = inFlight;
    const requestGeneration = generation;
    requests.add(page);
    const separator = url.includes('?') ? '&' : '?';
    api
      .get<TableMeta & { offset: number; rows: Value[][] }>(
        `${url}${separator}offset=${page * PAGE_SIZE}&limit=${PAGE_SIZE}`,
      )
      .then(
        (response) => {
          if (requestGeneration !== generation) {
            return;
          }
          pages.set(page, response.rows.map((row) => formatRow(response.columns, row)));
          stale.delete(page);
          evict();
          setMeta({ columns: response.columns, total: response.total, emptyMessage: response.emptyMessage });
          setError(undefined);
          setVersion((v) => v + 1);
        },
        (e) => {
          if (requestGeneration === generation) {
            setError(toErrorMessage(e));
          }
        },
      )
      .finally(() => requests.delete(page));
  };

  const evict = () => {
    for (const cache of [pages, stale]) {
      if (cache.size > MAX_CACHED_PAGES) {
        const distance = (page: number) => Math.max(wanted.first - page, page - wanted.last, 0);
        [...cache.keys()]
          .sort((a, b) => distance(b) - distance(a))
          .slice(0, cache.size - MAX_CACHED_PAGES)
          .forEach((page) => cache.delete(page));
      }
    }
  };

  const request = (start: number, end: number) => {
    const total = meta()?.total ?? 0;
    const lastPage = Math.max(0, Math.ceil(total / PAGE_SIZE) - 1);
    const margin = PAGE_SIZE / 2;
    wanted = {
      first: Math.floor(Math.max(0, start - margin) / PAGE_SIZE),
      last: Math.min(lastPage, Math.floor((end + margin) / PAGE_SIZE)),
    };
    for (let page = wanted.first; page <= wanted.last; page++) {
      fetchPage(page);
    }
  };

  const load = () => {
    generation++;
    stale = new Map([...stale, ...pages]);
    pages = new Map();
    inFlight = new Set();
    fetchPage(0);
    for (let page = wanted.first; page <= wanted.last; page++) {
      fetchPage(page);
    }
  };

  const clear = () => {
    generation++;
    pages = new Map();
    stale = new Map();
    inFlight = new Set();
    wanted = { first: 0, last: 0 };
    setMeta(undefined);
    setError(undefined);
    setVersion((v) => v + 1);
  };

  return {
    data: meta,
    error,
    load,
    clear,
    request,
    count: () => meta()?.total ?? 0,
    row: (index) => {
      version();
      const page = Math.floor(index / PAGE_SIZE);
      return (pages.get(page) ?? stale.get(page))?.[index - page * PAGE_SIZE];
    },
  };
}
