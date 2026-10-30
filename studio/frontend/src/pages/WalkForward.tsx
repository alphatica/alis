// Research → Walk-forward: optimizes a strategy on every training window, as on the Optimization screen, and trades each
// test window after it with the best parameters, all test windows in one backtest. At the start of a test window the
// strategy switches to its parameters and keeps its open positions. Settings, progress and Start/Stop on the left;
// equity and stats, the windows, trades and orders on the right. The job lives on the backend (WalkForwardApi.java):
// after reopening the GUI the last walk-forward is shown again, with its settings in the form.

import { createEffect, createRoot, createSignal, on, onCleanup, Show } from 'solid-js';
import { api, type ErrorMessage } from '../api';
import { EquityChart, type EquityMode, EquityModeSwitch } from '../components/EquityChart';
import { JobFailed } from '../components/ErrorBox';
import { Field } from '../components/Field';
import { Loaded } from '../components/Loaded';
import { ParamsDialog } from '../components/ParamsDialog';
import { Select } from '../components/Select';
import { StableLabel } from '../components/StableLabel';
import { StatTiles } from '../components/StatTiles';
import { Tabs } from '../components/Tabs';
import { type ColumnSpec, formatRow, numberFormat, type Value } from '../format';
import { createJob } from '../jobs';
import { type Equity, followDefaults, type Stat, walkForwardOptions, type WalkForwardOptions } from '../state';
import { type Column, type LiveRows, liveRows, staticSource, textColumns, VirtualTable } from '../table';

/** The form as sent to the backend (WalkForwardApi.StartRequest). */
type WalkForwardInput = {
  strategy: string;
  startTime: string;
  endTime: string;
  commissionRate: string;
  initialCapital: string;
  resultVerifier: string;
  parametersSelection: string;
  scorer: string;
  maxPermutations: string;
  trainingMonths: string;
  testMonths: string;
};

/** WalkForwardApi.WindowsPreview; days as yyyymmdd. */
type WindowsPreview = { windows: number; firstTrainingStart: number | null; lastTestEnd: number | null; error: string | null };
/** WalkForwardApi.WindowRow; days as yyyymmdd; the test results are null until the backtest ends. */
type WindowRow = {
  number: number;
  trainingStart: number;
  trainingEnd: number;
  testStart: number;
  testEnd: number;
  params: string[];
  trainingScore: number;
  returnPercent: number | null;
  transactions: number | null;
};
type WindowProgress = { number: number; windows: number };
type Progress = { time: string; bar: number; bars: number };
type BacktestView = { stats: Stat[]; equity: Equity };

// Kept outside the component, so the form, the running job and the results survive switching screens.
const [strategy, setStrategy] = createSignal('');
const [startTime, setStartTime] = createSignal('');
const [endTime, setEndTime] = createSignal('');
const [commissionRate, setCommissionRate] = createSignal('');
const [initialCapital, setInitialCapital] = createSignal('');
const [resultVerifier, setResultVerifier] = createSignal('');
const [parametersSelection, setParametersSelection] = createSignal('');
const [scorer, setScorer] = createSignal('');
const [maxPermutations, setMaxPermutations] = createSignal('');
const [trainingMonths, setTrainingMonths] = createSignal('');
const [testMonths, setTestMonths] = createSignal('');
const [preview, setPreview] = createSignal<WindowsPreview>();
let previewRequest = 0;

const [windowProgress, setWindowProgress] = createSignal<WindowProgress>();
const [iterations, setIterations] = createSignal<number>();
const [windows, setWindows] = createSignal<WindowRow[]>([]);
const [progress, setProgress] = createSignal<Progress>();
const [result, setResult] = createSignal<BacktestView>();
const [lastError, setLastError] = createSignal<ErrorMessage>();
const [resultStrategy, setResultStrategy] = createSignal('');
const [shown, setShown] = createSignal<{ params: string[]; title: string }>();
const [tab, setTab] = createSignal(0);
const [equityMode, setEquityMode] = createSignal<EquityMode>('nav');

const orders = liveRows();
const trades = liveRows();
/** Rows that came before the column formats: after reopening the GUI the job's output may come before the options. */
let waitingRows: { event: string; rows: Value[][] }[] = [];

function addRows(event: string, rows: Value[][]) {
  const options = walkForwardOptions.data();
  if (!options) {
    waitingRows.push({ event, rows });
  } else if (event === 'orders') {
    orders.append(rows.map((row) => formatRow(options.orders, row)));
  } else {
    trades.append(rows.map((row) => formatRow(options.trades, row)));
  }
}

const job = createJob<WalkForwardInput>('walk-forward', {
  reset: () => {
    orders.clear();
    trades.clear();
    waitingRows = [];
    setWindows([]);
    setWindowProgress(undefined);
    setIterations(undefined);
    setProgress(undefined);
    setResult(undefined);
    setLastError(undefined);
  },
  // Max permutations last: changing the strategy or the parameters selection resets it (effect below)
  restore: (input) => {
    setStrategy(input.strategy);
    setStartTime(input.startTime);
    setEndTime(input.endTime);
    setCommissionRate(input.commissionRate);
    setInitialCapital(input.initialCapital);
    setResultVerifier(input.resultVerifier);
    setParametersSelection(input.parametersSelection);
    setScorer(input.scorer);
    setTrainingMonths(input.trainingMonths);
    setTestMonths(input.testMonths);
    setMaxPermutations(input.maxPermutations);
    setResultStrategy(input.strategy);
  },
  event: (event, data) => {
    switch (event) {
      case 'window':
        setWindowProgress(data as WindowProgress);
        break;
      case 'iterations':
        setIterations(data as number);
        break;
      case 'windows':
        setWindows(data as WindowRow[]);
        break;
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
const { running, stopping, started } = job;

/** Keeps the current choice while the backend still offers it, otherwise takes the first option. */
function keepOrFirst(current: () => string, set: (value: string) => void, options: string[]) {
  if (!options.includes(current())) {
    set(options[0] ?? '');
  }
}

createRoot(() => {
  createEffect(() => {
    const options = walkForwardOptions.data();
    if (options) {
      keepOrFirst(strategy, setStrategy, options.strategies);
      keepOrFirst(resultVerifier, setResultVerifier, options.resultVerifiers);
      keepOrFirst(parametersSelection, setParametersSelection, options.parametersSelections);
      keepOrFirst(scorer, setScorer, options.scorers);
      if (!trainingMonths()) {
        setTrainingMonths(String(options.trainingMonths));
      }
      if (!testMonths()) {
        setTestMonths(String(options.testMonths));
      }
    }
  });
  createEffect(() => {
    if (walkForwardOptions.data() && waitingRows.length > 0) {
      const waiting = waitingRows;
      waitingRows = [];
      waiting.forEach((batch) => addRows(batch.event, batch.rows));
    }
  });
  // Not over the settings of a running walk-forward or of one found after reopening the GUI
  followDefaults(
    () => walkForwardOptions.data()?.defaults,
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
  createEffect(
    on([strategy, parametersSelection], ([name, selection]) => {
      const count = walkForwardOptions.data()?.maxPermutations[name]?.[selection];
      if (count !== undefined) {
        setMaxPermutations(String(count));
      }
    }),
  );
  // Windows of the dates, asked a moment after the last change; only the answer to the latest request is shown
  createEffect(
    on([startTime, endTime, trainingMonths, testMonths], ([start, end, training, test]) => {
      const request = ++previewRequest;
      if (!start || !end || !training || !test) {
        setPreview(undefined);
        return;
      }
      const timer = setTimeout(() => {
        const query = new URLSearchParams({ start, end, trainingMonths: training, testMonths: test });
        const show = (value: WindowsPreview | undefined) => request === previewRequest && setPreview(value);
        api.get<WindowsPreview>(`/api/walk-forward/windows?${query}`).then(show, () => show(undefined));
      }, 300);
      onCleanup(() => clearTimeout(timer));
    }),
  );
});

const isFullPermutation = () => parametersSelection() === walkForwardOptions.data()?.fullPermutation;

function start() {
  const input: WalkForwardInput = {
    strategy: strategy(),
    startTime: startTime(),
    endTime: endTime(),
    commissionRate: commissionRate(),
    initialCapital: initialCapital(),
    resultVerifier: resultVerifier(),
    parametersSelection: parametersSelection(),
    scorer: scorer(),
    maxPermutations: maxPermutations(),
    trainingMonths: trainingMonths(),
    testMonths: testMonths(),
  };
  setResultStrategy(input.strategy);
  job.start(() => api.post<{ jobId: string }>('/api/walk-forward', input));
}

const count = numberFormat(0);
const score = numberFormat(0, 2);
const percent = numberFormat(2);

/** yyyymmdd → yyyy-mm-dd */
function formatDay(day: number) {
  const text = String(day);
  return `${text.slice(0, 4)}-${text.slice(4, 6)}-${text.slice(6, 8)}`;
}

export function WalkForward() {
  return (
    <div class="page-fill">
      <ParamsDialog params={shown()?.params} title={shown()?.title ?? ''} onClose={() => setShown(undefined)} />
      <div class="split split-medium split-stretch">
        <div class="sidebar">
          <Loaded store={walkForwardOptions}>{(options) => <SettingsForm options={options()} />}</Loaded>
        </div>
        <div class="fill-column">
          <Loaded store={walkForwardOptions}>
            {(options) => (
              <Tabs
                selected={tab()}
                onSelect={setTab}
                tabs={[
                  {
                    label: 'Equity',
                    content: () => <WalkForwardEquity />,
                    actions: () => (result() ? <EquityModeSwitch mode={equityMode()} onChange={setEquityMode} /> : undefined),
                  },
                  { label: 'Windows', content: () => <WindowsTable /> },
                  { label: 'Trades', content: () => <LiveTable columns={options().trades} rows={trades} empty="No trades." /> },
                  { label: 'Orders', content: () => <LiveTable columns={options().orders} rows={orders} empty="No orders." /> },
                ]}
              />
            )}
          </Loaded>
        </div>
      </div>
    </div>
  );
}

function SettingsForm(props: { options: WalkForwardOptions }) {
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
      <Select id="walk-forward-strategy" label="Strategy" value={strategy()} onChange={setStrategy} options={props.options.strategies} disabled={running()} />
      <Field
        id="walk-forward-start"
        label="Start time"
        placeholder="yyyymmdd"
        inputmode="numeric"
        title="First day of the first test window; the training reaches before it"
        disabled={running()}
        value={startTime()}
        onInput={setStartTime}
      />
      <Field id="walk-forward-end" label="End time" placeholder="yyyymmdd" inputmode="numeric" disabled={running()} value={endTime()} onInput={setEndTime} />
      <Field
        id="walk-forward-training"
        label="Training window (months)"
        inputmode="numeric"
        title="Months before every test window the parameters are optimized on"
        disabled={running()}
        value={trainingMonths()}
        onInput={setTrainingMonths}
      />
      <Field
        id="walk-forward-test"
        label="Test window (months)"
        inputmode="numeric"
        title="Months traded with the parameters of one optimization; the strategy is optimized again after each"
        disabled={running()}
        value={testMonths()}
        onInput={setTestMonths}
      />
      <WindowsLine />
      <Field
        id="walk-forward-commission"
        label="Commission rate"
        inputmode="decimal"
        title="Fraction of the order value, e.g. 0.01"
        disabled={running()}
        value={commissionRate()}
        onInput={setCommissionRate}
      />
      <Field id="walk-forward-capital" label="Initial capital" inputmode="decimal" disabled={running()} value={initialCapital()} onInput={setInitialCapital} />
      <Select
        id="walk-forward-verifier"
        label="Result verifier"
        value={resultVerifier()}
        onChange={setResultVerifier}
        options={props.options.resultVerifiers}
        disabled={running()}
      />
      <Select
        id="walk-forward-selection"
        label="Parameters selection"
        value={parametersSelection()}
        onChange={setParametersSelection}
        options={props.options.parametersSelections}
        disabled={running()}
      />
      <Select id="walk-forward-scorer" label="Result scorer" value={scorer()} onChange={setScorer} options={props.options.scorers} disabled={running()} />
      <Field
        id="walk-forward-max-permutations"
        label="Max permutations"
        inputmode="numeric"
        title={isFullPermutation() ? 'Full permutation tries every combination of parameters' : 'Per training window'}
        disabled={running() || isFullPermutation()}
        value={maxPermutations()}
        onInput={setMaxPermutations}
      />
      <Show when={windowProgress()}>
        {(window) => (
          <span class="muted">
            Window {window().number} / {window().windows} · iterations{' '}
            {iterations() === undefined ? '…' : count.format(iterations()!)}
          </span>
        )}
      </Show>
      <Show when={progress()}>
        {(p) => (
          <span class="muted">
            Backtest: {p().time} · {Math.floor(p().bars > 0 ? (100 * p().bar) / p().bars : 0)} %
          </span>
        )}
      </Show>
      <JobFailed error={lastError()} />
      <div class="sidebar-buttons">
        <button class="btn btn-primary" type="submit" disabled={running() || !strategy() || !!preview()?.error}>
          <StableLabel labels={['Start', 'Running…']} current={running() ? 1 : 0} />
        </button>
        <button
          class="btn"
          type="button"
          title="Ends the optimization of the current window; the windows optimized before it are backtested"
          disabled={!running() || stopping()}
          onClick={job.stop}
        >
          <StableLabel labels={['Stop', 'Stopping…']} current={stopping() && running() ? 1 : 0} />
        </button>
      </div>
    </form>
  );
}

/** Windows of the dates in the form. */
function WindowsLine() {
  return (
    <Show when={preview()}>
      {(p) => (
        <Show when={!p().error} fallback={<span class="loss">{p().error}</span>}>
          <span class="muted">
            {p().windows} test windows to {formatDay(p().lastTestEnd!)}, training from {formatDay(p().firstTrainingStart!)}
          </span>
        </Show>
      )}
    </Show>
  );
}

function WalkForwardEquity() {
  return (
    <Show
      when={result()}
      fallback={
        <p class="muted">
          {running()
            ? 'The equity is shown when the test windows are backtested.'
            : started()
              ? 'No window was optimized.'
              : 'Start a walk-forward to see the equity of its test windows.'}
        </p>
      }
    >
      {(view) => (
        <>
          <Show when={view().equity.times.length > 0} fallback={<p class="muted">No sessions in the test windows.</p>}>
            <EquityChart
              equity={view().equity}
              mode={equityMode()}
              stats={view().stats}
              fileName={`${resultStrategy() || 'strategy'}-walk-forward`}
            />
          </Show>
          <StatTiles stats={view().stats} results />
        </>
      )}
    </Show>
  );
}

function WindowsTable() {
  const columns: Column<WindowRow>[] = [
    { title: 'Window', numeric: true, cell: (row) => String(row.number) },
    { title: 'Training', cell: (row) => `${formatDay(row.trainingStart)} – ${formatDay(row.trainingEnd)}` },
    { title: 'Test', cell: (row) => `${formatDay(row.testStart)} – ${formatDay(row.testEnd)}` },
    { title: 'Training score', numeric: true, cell: (row) => score.format(row.trainingScore) },
    {
      title: 'Test return',
      numeric: true,
      cell: (row) =>
        row.returnPercent === null ? (
          '—'
        ) : (
          <span classList={{ profit: row.returnPercent > 0, loss: row.returnPercent < 0 }}>{percent.format(row.returnPercent)} %</span>
        ),
    },
    { title: 'Transactions', numeric: true, cell: (row) => (row.transactions === null ? '—' : count.format(row.transactions)) },
    {
      title: 'Params',
      cell: (row) => (
        <button class="link-button" type="button" onClick={() => setShown({ params: row.params, title: `Window ${row.number}` })}>
          View
        </button>
      ),
    },
  ];
  return (
    <VirtualTable
      columns={columns}
      source={staticSource(windows)}
      emptyMessage={running() ? 'The windows are listed as they are optimized.' : 'Start a walk-forward to see its windows.'}
    />
  );
}

function LiveTable(props: { columns: ColumnSpec[]; rows: LiveRows; empty: string }) {
  const columns = textColumns(() => props.columns);
  return (
    <VirtualTable
      columns={columns()}
      source={props.rows.source}
      widest={props.rows.widest()}
      emptyMessage={running() ? 'Running…' : result() ? props.empty : 'Start a walk-forward to see the trades of its test windows.'}
    />
  );
}
