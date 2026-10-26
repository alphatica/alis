// Data shown by the screens, kept in memory so switching screens is instant.
// Loaded once at start and reloaded in the background when the backend reports a change;
// the old value stays on screen until the new one arrives.

import { type Accessor, createEffect, createMemo, createSignal, untrack } from 'solid-js';
import { api, type ErrorMessage, toErrorMessage } from './api';
import { type ColumnSpec, formatValue, type Value } from './format';
import { restoreJobs, watchJob } from './jobs';
import { onAppEvent, onReconnect } from './live';
import { pagedSource, type TableData } from './table';
import { pushToast } from './toasts';

/** Value of a stats panel (Stat.java): a number shown with `decimals` and `suffix`, a text, or null when unknown. */
export type Stat = { name: string; value: Value; decimals: number; suffix: string };

export function formatStat(stat: Stat): string {
  return formatValue(stat.value, stat.decimals, stat.suffix) || '—';
}

/** Account value at every market session since the first action; empty without market data. */
export type Equity = {
  /** yyyymmdd */
  times: number[];
  nav: number[];
  /** Cumulative time-weighted return: deposits and withdrawals do not change it. */
  returnPercent: number[];
};

export type PortfolioData = {
  stats: Stat[];
  actions: TableData;
  history: TableData;
  equity: Equity;
};

export type Store<T> = {
  data: Accessor<T | undefined>;
  error: Accessor<ErrorMessage | undefined>;
  load: () => void;
};

function store<T>(url: string, reloadOn: string[]): Store<T> {
  const [data, setData] = createSignal<T>();
  const [error, setError] = createSignal<ErrorMessage>();
  let latest = 0;
  const load = () => {
    const request = ++latest;
    api.get<T>(url).then(
      (value) => {
        if (request === latest) {
          setData(() => value);
          setError(undefined);
        }
      },
      (e) => {
        if (request === latest) {
          setError(toErrorMessage(e));
        }
      },
    );
  };
  reloadOn.forEach((event) => onAppEvent(event, load));
  onReconnect(load);
  return { data, error, load };
}

export const portfolio = store<PortfolioData>('/api/portfolio', ['portfolio-changed', 'data-loaded']);
export type SignalsOptions = { strategies: string[]; defaultStartTime: string };

export const signalsOptions = store<SignalsOptions>('/api/signals/options', ['data-loaded']);

/** Defaults of the backtest and optimization forms (RunDefaults.java). */
export type RunDefaults = {
  /** yyyymmdd; empty without market data */
  startTime: string;
  endTime: string;
  commissionRate: string;
  initialCapital: string;
};

/**
 * Calls `apply` with the form defaults at start and whenever they change, e.g. after new data is loaded.
 * Signals read by `apply` do not call it again.
 */
export function followDefaults(defaults: Accessor<RunDefaults | undefined>, apply: (values: RunDefaults) => void) {
  const latest = createMemo(defaults, undefined, { equals: (a, b) => JSON.stringify(a) === JSON.stringify(b) });
  createEffect(() => {
    const values = latest();
    if (values) {
      untrack(() => apply(values));
    }
  });
}

export type BacktestOptions = {
  strategies: string[];
  defaults: RunDefaults;
  /** Columns of the tables filled live by the backtest job. */
  orders: ColumnSpec[];
  trades: ColumnSpec[];
};

export const backtestOptions = store<BacktestOptions>('/api/backtest/options', ['data-loaded']);
export type OptimizationOptions = {
  strategies: string[];
  resultVerifiers: string[];
  parametersSelections: string[];
  scorers: string[];
  /** The parameters selection trying every permutation: max permutations cannot be changed. */
  fullPermutation: string;
  /** Default max permutations by strategy, then by parameters selection. */
  maxPermutations: Record<string, Record<string, number>>;
  defaults: RunDefaults;
  results: ColumnSpec[];
};

export const optimizationOptions = store<OptimizationOptions>('/api/optimization/options', ['data-loaded']);
export type BetterExitsOptions = {
  scorers: string[];
  defaultCommissionRate: string;
  /** The account log loaded on the backend; null until one is loaded. */
  file: { name: string; actions: number } | null;
  results: ColumnSpec[];
};

export const betterExitsOptions = store<BetterExitsOptions>('/api/better-exits/options', []);
export const markets = pagedSource('/api/data/markets');
onAppEvent('data-loaded', markets.load);
onReconnect(markets.load);

const [dataStatus, setDataStatus] = createSignal('');
export { dataStatus, setDataStatus };

/** Full application name with the build, shown in Help. */
export const [studioName, setStudioName] = createSignal('Alis Studio');
onAppEvent('data-status', (status) => setDataStatus(String(status ?? '')));
onReconnect(() =>
  api
    .get<{ dataStatus: string }>('/api/app')
    .then((status) => setDataStatus(status.dataStatus ?? ''))
    .catch(() => {
      // The next reconnect tries again
    }),
);

export function loadAll() {
  portfolio.load();
  signalsOptions.load();
  backtestOptions.load();
  optimizationOptions.load();
  betterExitsOptions.load();
  markets.load();
  restoreJobs();
}
onReconnect(restoreJobs);

// ---------- Jobs ----------

export type JobState = 'running' | 'finished';

/**
 * Follows a backend job: its errors become toasts, the returned state turns to 'finished' at the end
 * (then onDone is called, after errors or not).
 */
export function trackJob(id: string, onDone?: () => void): Accessor<JobState> {
  const [state, setState] = createSignal<JobState>('running');
  watchJob(id, false, {
    errors: (_all, fresh) => fresh.forEach(pushToast),
    done: () => {
      setState('finished');
      onDone?.();
    },
  });
  return state;
}
