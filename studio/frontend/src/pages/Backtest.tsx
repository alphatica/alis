// Strategies → Backtest: settings and actions on the left; equity with stats under it,
// trades and orders on the right. Orders and trades arrive live from the backtest job. The jobs and their output live
// on the backend: after reopening the GUI the last backtest and the last comparison to random are shown again.

import { createEffect, createRoot, createSignal, For, Show } from 'solid-js';
import { api, download, type ErrorMessage, toErrorMessage } from '../api';
import { EquityChart, type EquityMode, EquityModeSwitch } from '../components/EquityChart';
import { ErrorBox, JobFailed } from '../components/ErrorBox';
import { Field } from '../components/Field';
import { Loaded } from '../components/Loaded';
import { Modal } from '../components/Modal';
import { StableLabel } from '../components/StableLabel';
import { StatTiles } from '../components/StatTiles';
import { Tabs } from '../components/Tabs';
import { type ColumnSpec, formatRow, type Value } from '../format';
import { createJob } from '../jobs';
import { backtestOptions, type Equity, followDefaults, type Stat } from '../state';
import { staticSource, textColumns, VirtualTable, widestTracker } from '../table';
import { showError } from '../toasts';

type Progress = { time: string; bar: number; bars: number };
type BacktestView = { stats: Stat[]; equity: Equity };
/** The form as sent to the backend (BacktestApi.BacktestRequest). */
type BacktestInput = { strategy: string; startTime: string; endTime: string; commissionRate: string; initialCapital: string };

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

const [progress, setProgress] = createSignal<Progress>();
const [result, setResult] = createSignal<BacktestView>();
const [lastError, setLastError] = createSignal<ErrorMessage>();
/** Strategy of the shown result, for the CSV file name. */
const [resultStrategy, setResultStrategy] = createSignal('');
const orders = liveRows();
const trades = liveRows();
/** Rows that came before the column formats: after reopening the GUI the job's output may come before the options. */
let waitingRows: { event: string; rows: Value[][] }[] = [];

function addRows(event: string, rows: Value[][]) {
  const options = backtestOptions.data();
  if (!options) {
    waitingRows.push({ event, rows });
  } else if (event === 'orders') {
    orders.append(rows.map((row) => formatRow(options.orders, row)));
  } else {
    trades.append(rows.map((row) => formatRow(options.trades, row)));
  }
}

const job = createJob<BacktestInput>('backtest', {
  reset: () => {
    orders.clear();
    trades.clear();
    waitingRows = [];
    setResult(undefined);
    setProgress(undefined);
    setLastError(undefined);
  },
  restore: (input) => {
    setStrategy(input.strategy);
    setStartTime(input.startTime);
    setEndTime(input.endTime);
    setCommissionRate(input.commissionRate);
    setInitialCapital(input.initialCapital);
    setResultStrategy(input.strategy);
  },
  event: (event, data) => {
    switch (event) {
      case 'orders':
      case 'trades':
        addRows(event, data as Value[][]);
        break;
      case 'progress':
        setProgress(data as Progress);
        break;
      case 'result':
        setResult(data as BacktestView);
        break;
    }
  },
  failed: setLastError,
});
const running = job.running;

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
  createEffect(() => {
    if (backtestOptions.data() && waitingRows.length > 0) {
      const waiting = waitingRows;
      waitingRows = [];
      waiting.forEach((batch) => addRows(batch.event, batch.rows));
    }
  });
  // Not over the settings of a running backtest or of one found after reopening the GUI
  followDefaults(
    () => backtestOptions.data()?.defaults,
    (defaults) => {
      if (running() || job.restored()) {
        return;
      }
      setStartTime(defaults.startTime);
      setEndTime(defaults.endTime);
      setCommissionRate(defaults.commissionRate);
      setInitialCapital(defaults.initialCapital);
    },
  );
});

function start() {
  const input: BacktestInput = {
    strategy: strategy(),
    startTime: startTime(),
    endTime: endTime(),
    commissionRate: commissionRate(),
    initialCapital: initialCapital(),
  };
  setResultStrategy(input.strategy);
  job.start(() => api.post<{ jobId: string }>('/api/backtest', input));
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
          <JobFailed error={lastError()} />
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
      <Show when={error()}>{(e) => <ErrorBox error={e()} />}</Show>
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
const [randomError, setRandomError] = createSignal<ErrorMessage>();

const randomJob = createJob<{ iterations: string }>('backtest-random', {
  reset: () => {
    setRandomLines([]);
    setRandomError(undefined);
  },
  restore: (input) => setIterations(input.iterations),
  event: (event, data) => {
    if (event === 'lines') {
      setRandomLines((lines) => [...lines, ...(data as string[])]);
    }
  },
  failed: setRandomError,
  // Toasts would be hidden under the dialog's backdrop
  toastErrors: false,
});

function startRandom() {
  const input = { iterations: iterations() };
  randomJob.start(() => api.post<{ jobId: string }>('/api/backtest/compare-random', input));
}

function stopRandom() {
  randomJob.stop();
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
          if (!randomJob.running()) {
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
        <button class="btn btn-primary" type="submit" disabled={randomJob.running()}>
          Start
        </button>
        <button class="btn" type="button" disabled={!randomJob.running() || randomJob.stopping()} onClick={stopRandom}>
          Stop
        </button>
      </form>
      <pre ref={output} class="job-output">
        {randomLines().join('\n')}
      </pre>
      <Show when={randomError()}>{(e) => <ErrorBox error={e()} />}</Show>
      <div class="actions dialog-actions">
        <button class="btn" type="button" onClick={close}>
          Close
        </button>
      </div>
    </Modal>
  );
}
