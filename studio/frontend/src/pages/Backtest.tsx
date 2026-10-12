// Strategies → Backtest: settings and actions on the left; equity with stats under it,
// trades and orders on the right. Orders and trades arrive live from the backtest job.

import { createEffect, createRoot, createSignal, For, Show } from 'solid-js';
import { api, download, type ErrorMessage, toErrorMessage } from '../api';
import { EquityChart, type EquityMode, EquityModeSwitch } from '../components/EquityChart';
import { Field } from '../components/Field';
import { Loaded } from '../components/Loaded';
import { Modal } from '../components/Modal';
import { StableLabel } from '../components/StableLabel';
import { StatTiles } from '../components/StatTiles';
import { Tabs } from '../components/Tabs';
import { type ColumnSpec, formatRow, type Value } from '../format';
import { subscribe } from '../live';
import { backtestOptions, type Equity, followDefaults, type Stat } from '../state';
import { staticSource, textColumns, VirtualTable, widestTracker } from '../table';
import { pushToast, ReportButton, showError } from '../toasts';

type Progress = { time: string; bar: number; bars: number };
type BacktestView = { stats: Stat[]; equity: Equity };

/** Rows appended by a job, formatted as text: kept in a plain array, `version` tells the table that more arrived. */
function liveRows() {
  let rows: string[][] = [];
  const [version, setVersion] = createSignal(0);
  // Widest row per column, kept up to date while rows arrive, so the column widths do not change while scrolling
  const tracker = widestTracker();
  const [widest, setWidest] = createSignal<(string[] | undefined)[]>([]);
  return {
    source: staticSource(() => {
      version();
      return rows;
    }),
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

// Kept outside the component, so the form, the running job and the last result survive switching screens.
const [strategy, setStrategy] = createSignal('');
const [startTime, setStartTime] = createSignal('');
const [endTime, setEndTime] = createSignal('');
const [commissionRate, setCommissionRate] = createSignal('');
const [initialCapital, setInitialCapital] = createSignal('');

const [running, setRunning] = createSignal(false);
const [progress, setProgress] = createSignal<Progress>();
const [result, setResult] = createSignal<BacktestView>();
/** Strategy of the shown result, for the CSV file name. */
const [resultStrategy, setResultStrategy] = createSignal('');
const orders = liveRows();
const trades = liveRows();

const [tab, setTab] = createSignal(0);
const [equityMode, setEquityMode] = createSignal<EquityMode>('nav');
const [dialog, setDialog] = createSignal<'drawdown' | 'random'>();

// Defaults from the backend: the first strategy, the last 10 years of data, commission and capital
// (dates and amounts are set again when new data is loaded).
createRoot(() => {
  createEffect(() => {
    const options = backtestOptions.data();
    if (options && !options.strategies.includes(strategy())) {
      setStrategy(options.strategies[0] ?? '');
    }
  });
  followDefaults(
    () => backtestOptions.data()?.defaults,
    (defaults) => {
      setStartTime(defaults.startTime);
      setEndTime(defaults.endTime);
      setCommissionRate(defaults.commissionRate);
      setInitialCapital(defaults.initialCapital);
    },
  );
});

async function start() {
  const options = backtestOptions.data();
  if (!options) {
    return;
  }
  orders.clear();
  trades.clear();
  setResult(undefined);
  setProgress(undefined);
  setRunning(true);
  try {
    const { jobId } = await api.post<{ jobId: string }>('/api/backtest', {
      strategy: strategy(),
      startTime: startTime(),
      endTime: endTime(),
      commissionRate: commissionRate(),
      initialCapital: initialCapital(),
    });
    setResultStrategy(strategy());
    follow(jobId, options.orders, options.trades);
  } catch (e) {
    showError(e);
    setRunning(false);
  }
}

function follow(jobId: string, orderColumns: ColumnSpec[], tradeColumns: ColumnSpec[]) {
  const unsubscribe = subscribe(`job/${jobId}`, (event, data) => {
    switch (event) {
      case 'orders':
        orders.append((data as Value[][]).map((row) => formatRow(orderColumns, row)));
        break;
      case 'trades':
        trades.append((data as Value[][]).map((row) => formatRow(tradeColumns, row)));
        break;
      case 'progress':
        setProgress(data as Progress);
        break;
      case 'result':
        setResult(data as BacktestView);
        break;
      case 'error':
        (data as ErrorMessage[]).forEach(pushToast);
        break;
      case 'done':
        setRunning(false);
        unsubscribe();
        break;
    }
  });
}

async function exportTrades() {
  try {
    await download('/api/backtest/trades.csv', `${resultStrategy() || 'backtest'}-trades.csv`);
  } catch (e) {
    showError(e);
  }
}

export function Backtest() {
  const canAnalyze = () => !running() && result() !== undefined;
  return (
    <div class="page-fill">
      <DrawdownDialog open={dialog() === 'drawdown'} onClose={() => setDialog(undefined)} />
      <RandomDialog open={dialog() === 'random'} onClose={() => setDialog(undefined)} />
      <div class="split split-narrow split-stretch">
        <div class="sidebar">
          <Loaded store={backtestOptions}>{(options) => <SettingsForm strategies={options().strategies} />}</Loaded>
          <Show when={running()}>
            <BacktestProgress />
          </Show>
          <div class="sidebar-buttons sidebar-buttons-stack">
            <button class="btn" type="button" disabled={!canAnalyze()} onClick={exportTrades}>
              Export trades to CSV
            </button>
            <button class="btn" type="button" disabled={!canAnalyze()} onClick={() => setDialog('drawdown')}>
              Drawdown check…
            </button>
            <button class="btn" type="button" disabled={!canAnalyze()} onClick={() => setDialog('random')}>
              Compare to random…
            </button>
          </div>
        </div>
        <div class="fill-column">
          <Loaded store={backtestOptions}>
            {(options) => (
              <Tabs
                selected={tab()}
                onSelect={setTab}
                tabs={[
                  {
                    label: 'Equity',
                    content: () => <BacktestEquity />,
                    actions: () =>
                      result() ? <EquityModeSwitch mode={equityMode()} onChange={setEquityMode} /> : undefined,
                  },
                  {
                    label: 'Trades',
                    content: () => <LiveTable columns={options().trades} rows={trades} empty="No trades." />,
                  },
                  {
                    label: 'Orders',
                    content: () => <LiveTable columns={options().orders} rows={orders} empty="No orders." />,
                  },
                ]}
              />
            )}
          </Loaded>
        </div>
      </div>
    </div>
  );
}

function SettingsForm(props: { strategies: string[] }) {
  return (
    <form
      class="form-stack"
      onSubmit={(event) => {
        event.preventDefault();
        if (!running()) {
          start();
        }
      }}
    >
      <div class="field">
        <label for="backtest-strategy">Strategy</label>
        <select id="backtest-strategy" value={strategy()} onChange={(event) => setStrategy(event.currentTarget.value)}>
          <For each={props.strategies}>{(name) => <option value={name}>{name}</option>}</For>
        </select>
      </div>
      <Field id="backtest-start" label="Start time" placeholder="yyyymmdd" inputmode="numeric" value={startTime()} onInput={setStartTime} />
      <Field id="backtest-end" label="End time" placeholder="yyyymmdd" inputmode="numeric" value={endTime()} onInput={setEndTime} />
      <Field
        id="backtest-commission"
        label="Commission rate"
        inputmode="decimal"
        title="Fraction of the order value, e.g. 0.01"
        value={commissionRate()}
        onInput={setCommissionRate}
      />
      <Field id="backtest-capital" label="Initial capital" inputmode="decimal" value={initialCapital()} onInput={setInitialCapital} />
      <button class="btn btn-primary" type="submit" disabled={running() || !strategy()}>
        <StableLabel labels={['Start', 'Running…']} current={running() ? 1 : 0} />
      </button>
    </form>
  );
}

function BacktestProgress() {
  const percent = () => {
    const p = progress();
    return p && p.bars > 0 ? (100 * p.bar) / p.bars : 0;
  };
  return (
    <div class="backtest-progress">
      <div class="progress" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(percent())}>
        <span style={{ width: `${percent()}%` }} />
      </div>
      <span class="muted">{progress() ? `${progress()!.time} · ${Math.floor(percent())} %` : 'Starting…'}</span>
    </div>
  );
}

function BacktestEquity() {
  return (
    <Show
      when={result()}
      fallback={<p class="muted">{running() ? 'The equity is shown when the backtest ends.' : 'Run a backtest to see its equity.'}</p>}
    >
      {(view) => (
        <>
          <Show when={view().equity.times.length > 0} fallback={<p class="muted">No sessions in the time range.</p>}>
            <EquityChart equity={view().equity} mode={equityMode()} stats={view().stats} fileName={resultStrategy() || 'backtest'} />
          </Show>
          <StatTiles stats={view().stats} />
        </>
      )}
    </Show>
  );
}

function LiveTable(props: { columns: ColumnSpec[]; rows: ReturnType<typeof liveRows>; empty: string }) {
  const columns = textColumns(() => props.columns);
  return (
    <VirtualTable
      columns={columns()}
      source={props.rows.source}
      widest={props.rows.widest()}
      emptyMessage={running() ? 'Running…' : result() ? props.empty : 'Run a backtest to see its results.'}
    />
  );
}

// ---------- Dialogs ----------

/** Error inside a dialog: toasts would be hidden under its backdrop. */
function DialogError(props: { error: ErrorMessage }) {
  return (
    <div class="dialog-error" role="alert">
      <div class="toast-title">{props.error.title}</div>
      <div class="toast-message">{props.error.message}</div>
      <Show when={props.error.reportId}>
        {(reportId) => (
          <div class="toast-actions">
            <ReportButton reportId={reportId()} />
          </div>
        )}
      </Show>
    </div>
  );
}

function DrawdownDialog(props: { open: boolean; onClose: () => void }) {
  const [lines, setLines] = createSignal<string[]>();
  const [error, setError] = createSignal<ErrorMessage>();
  createEffect(() => {
    if (props.open) {
      setLines(undefined);
      setError(undefined);
      api.post<{ lines: string[] }>('/api/backtest/drawdown').then(
        (response) => setLines(response.lines),
        (e) => setError(toErrorMessage(e)),
      );
    }
  });
  return (
    <Modal open={props.open} onClose={props.onClose} title="Drawdown check">
      <pre class="job-output">{lines()?.join('\n') ?? (error() ? '' : 'Checking…')}</pre>
      <Show when={error()}>{(e) => <DialogError error={e()} />}</Show>
      <div class="actions dialog-actions">
        <button class="btn" type="button" onClick={() => props.onClose()}>
          Close
        </button>
      </div>
    </Modal>
  );
}

// Kept outside the dialog: the output stays after closing it, the comparison runs until Stop or closing.
const [iterations, setIterations] = createSignal('1000');
const [randomLines, setRandomLines] = createSignal<string[]>([]);
const [randomJob, setRandomJob] = createSignal<string>();
const [randomError, setRandomError] = createSignal<ErrorMessage>();

async function startRandom() {
  setRandomLines([]);
  setRandomError(undefined);
  try {
    const { jobId } = await api.post<{ jobId: string }>('/api/backtest/compare-random', { iterations: iterations() });
    setRandomJob(jobId);
    const unsubscribe = subscribe(`job/${jobId}`, (event, data) => {
      if (event === 'lines') {
        setRandomLines((lines) => [...lines, ...(data as string[])]);
      } else if (event === 'error') {
        setRandomError((data as ErrorMessage[])[0]);
      } else if (event === 'done') {
        setRandomJob(undefined);
        unsubscribe();
      }
    });
  } catch (e) {
    setRandomError(toErrorMessage(e));
  }
}

function stopRandom() {
  const jobId = randomJob();
  if (jobId) {
    api.post(`/api/jobs/${jobId}/cancel`).catch(showError);
  }
}

function RandomDialog(props: { open: boolean; onClose: () => void }) {
  let output: HTMLPreElement | undefined;
  // Follows the newest line
  createEffect(() => {
    randomLines();
    output?.scrollTo({ top: output.scrollHeight });
  });
  const close = () => {
    stopRandom();
    props.onClose();
  };
  return (
    <Modal open={props.open} onClose={close} title="Compare to random">
      <form
        class="toolbar"
        onSubmit={(event) => {
          event.preventDefault();
          if (!randomJob()) {
            startRandom();
          }
        }}
      >
        <label for="random-iterations">Iterations</label>
        <input
          id="random-iterations"
          type="text"
          inputmode="numeric"
          size={8}
          value={iterations()}
          onInput={(event) => setIterations(event.currentTarget.value)}
        />
        <button class="btn btn-primary" type="submit" disabled={randomJob() !== undefined}>
          Start
        </button>
        <button class="btn" type="button" disabled={randomJob() === undefined} onClick={stopRandom}>
          Stop
        </button>
      </form>
      <pre ref={output} class="job-output">
        {randomLines().join('\n')}
      </pre>
      <Show when={randomError()}>{(e) => <DialogError error={e()} />}</Show>
      <div class="actions dialog-actions">
        <button class="btn" type="button" onClick={close}>
          Close
        </button>
      </div>
    </Modal>
  );
}
