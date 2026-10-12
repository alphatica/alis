// Strategies → Optimization: settings, iteration count and Start/Stop on the left,
// the best 100 results on the right. Results arrive live from the optimization job; "View" shows a result's
// parameters as Java code.

import { createEffect, createMemo, createRoot, createSignal, For, on } from 'solid-js';
import { api, type ErrorMessage } from '../api';
import { Field } from '../components/Field';
import { Loaded } from '../components/Loaded';
import { Modal } from '../components/Modal';
import { StableLabel } from '../components/StableLabel';
import { type ColumnSpec, numberFormat, type Value } from '../format';
import { subscribe } from '../live';
import { followDefaults, optimizationOptions } from '../state';
import { type Column, staticSource, valueColumns, VirtualTable } from '../table';
import { pushToast, showError } from '../toasts';

/** One row of the results; `cells` are the values of the results columns. */
type Result = { score: number; cells: Value[]; params: string[] };

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

const [jobId, setJobId] = createSignal<string>();
const [stopping, setStopping] = createSignal(false);
const [iterations, setIterations] = createSignal<number>();
const [results, setResults] = createSignal<Result[]>([]);
const [started, setStarted] = createSignal(false);
const [shown, setShown] = createSignal<Result>();

const running = () => jobId() !== undefined;

/** Keeps the current choice while the backend still offers it, otherwise takes the first option. */
function keepOrFirst(current: () => string, set: (value: string) => void, options: string[]) {
  if (!options.includes(current())) {
    set(options[0] ?? '');
  }
}

// Defaults from the backend, like in Backtest; max permutations is reset whenever the strategy or the
// parameters selection changes.
createRoot(() => {
  createEffect(() => {
    const options = optimizationOptions.data();
    if (options) {
      keepOrFirst(strategy, setStrategy, options.strategies);
      keepOrFirst(resultVerifier, setResultVerifier, options.resultVerifiers);
      keepOrFirst(parametersSelection, setParametersSelection, options.parametersSelections);
      keepOrFirst(scorer, setScorer, options.scorers);
    }
  });
  followDefaults(
    () => optimizationOptions.data()?.defaults,
    (defaults) => {
      setStartTime(defaults.startTime);
      setEndTime(defaults.endTime);
      setCommissionRate(defaults.commissionRate);
      setInitialCapital(defaults.initialCapital);
    },
  );
  createEffect(
    on([strategy, parametersSelection], ([name, selection]) => {
      const count = optimizationOptions.data()?.maxPermutations[name]?.[selection];
      if (count !== undefined) {
        setMaxPermutations(String(count));
      }
    }),
  );
});

const isFullPermutation = () => parametersSelection() === optimizationOptions.data()?.fullPermutation;

async function start() {
  setResults([]);
  setIterations(undefined);
  setStopping(false);
  setStarted(true);
  try {
    const { jobId: id } = await api.post<{ jobId: string }>('/api/optimization', {
      strategy: strategy(),
      startTime: startTime(),
      endTime: endTime(),
      commissionRate: commissionRate(),
      initialCapital: initialCapital(),
      resultVerifier: resultVerifier(),
      parametersSelection: parametersSelection(),
      scorer: scorer(),
      maxPermutations: maxPermutations(),
    });
    setJobId(id);
    follow(id);
  } catch (e) {
    showError(e);
    setStarted(false);
  }
}

function follow(id: string) {
  const unsubscribe = subscribe(`job/${id}`, (event, data) => {
    switch (event) {
      case 'results':
        setResults(data as Result[]);
        break;
      case 'iterations':
        setIterations(data as number);
        break;
      case 'error':
        (data as ErrorMessage[]).forEach(pushToast);
        break;
      case 'done':
        setJobId(undefined);
        unsubscribe();
        break;
    }
  });
}

function stop() {
  const id = jobId();
  if (id) {
    setStopping(true);
    api.post(`/api/jobs/${id}/cancel`).catch(showError);
  }
}

export function Optimization() {
  return (
    <div class="page-fill">
      <ParamsDialog result={shown()} onClose={() => setShown(undefined)} />
      <div class="split split-narrow split-stretch">
        <div class="sidebar">
          <Loaded store={optimizationOptions}>{(options) => <SettingsForm options={options()} />}</Loaded>
        </div>
        <div class="fill-column">
          <h2>Optimization results</h2>
          <Loaded store={optimizationOptions}>{(options) => <ResultsTable columns={options().results} />}</Loaded>
        </div>
      </div>
    </div>
  );
}

function SettingsForm(props: { options: { strategies: string[]; resultVerifiers: string[]; parametersSelections: string[]; scorers: string[] } }) {
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
      <Select id="optimization-strategy" label="Strategy" value={strategy()} onChange={setStrategy} options={props.options.strategies} />
      <Field id="optimization-start" label="Start time" placeholder="yyyymmdd" inputmode="numeric" disabled={running()} value={startTime()} onInput={setStartTime} />
      <Field id="optimization-end" label="End time" placeholder="yyyymmdd" inputmode="numeric" disabled={running()} value={endTime()} onInput={setEndTime} />
      <Field
        id="optimization-commission"
        label="Commission rate"
        inputmode="decimal"
        title="Fraction of the order value, e.g. 0.01"
        disabled={running()}
        value={commissionRate()}
        onInput={setCommissionRate}
      />
      <Field id="optimization-capital" label="Initial capital" inputmode="decimal" disabled={running()} value={initialCapital()} onInput={setInitialCapital} />
      <Select id="optimization-verifier" label="Result verifier" value={resultVerifier()} onChange={setResultVerifier} options={props.options.resultVerifiers} />
      <Select
        id="optimization-selection"
        label="Parameters selection"
        value={parametersSelection()}
        onChange={setParametersSelection}
        options={props.options.parametersSelections}
      />
      <Select id="optimization-scorer" label="Result scorer" value={scorer()} onChange={setScorer} options={props.options.scorers} />
      <Field
        id="optimization-max-permutations"
        label="Max permutations"
        inputmode="numeric"
        title={isFullPermutation() ? 'Full permutation tries every combination of parameters' : undefined}
        disabled={running() || isFullPermutation()}
        value={maxPermutations()}
        onInput={setMaxPermutations}
      />
      <span class="muted">
        Iterations: {iterations() === undefined ? (running() ? '…' : '—') : numberFormat(0).format(iterations()!)}
      </span>
      <div class="sidebar-buttons">
        <button class="btn btn-primary" type="submit" disabled={running() || !strategy()}>
          <StableLabel labels={['Start', 'Running…']} current={running() ? 1 : 0} />
        </button>
        <button class="btn" type="button" disabled={!running() || stopping()} onClick={stop}>
          <StableLabel labels={['Stop', 'Stopping…']} current={stopping() && running() ? 1 : 0} />
        </button>
      </div>
    </form>
  );
}

function Select(props: { id: string; label: string; value: string; onChange: (value: string) => void; options: string[] }) {
  return (
    <div class="field">
      <label for={props.id}>{props.label}</label>
      <select id={props.id} disabled={running()} value={props.value} onChange={(event) => props.onChange(event.currentTarget.value)}>
        <For each={props.options}>{(name) => <option value={name}>{name}</option>}</For>
      </select>
    </div>
  );
}

function ResultsTable(props: { columns: ColumnSpec[] }) {
  const columns = createMemo<Column<Result>[]>(() => [
    ...valueColumns(props.columns, (row: Result) => row.cells),
    {
      title: 'Params',
      cell: (row: Result) => (
        <button class="link-button" type="button" onClick={() => setShown(row)}>
          View
        </button>
      ),
    },
  ]);
  return (
    <VirtualTable
      columns={columns()}
      source={staticSource(results)}
      emptyMessage={running() ? 'No results yet.' : started() ? 'No results.' : 'Start an optimization to see its results.'}
    />
  );
}

function ParamsDialog(props: { result: Result | undefined; onClose: () => void }) {
  const text = () => props.result?.params.join('\n') ?? '';
  const copy = () => navigator.clipboard.writeText(text()).catch(showError);
  return (
    <Modal
      open={props.result !== undefined}
      onClose={props.onClose}
      title={props.result ? `Score ${numberFormat(0).format(props.result.score)}` : ''}
    >
      <pre class="job-output">{text()}</pre>
      <div class="actions dialog-actions">
        <button class="btn" type="button" onClick={copy}>
          Copy
        </button>
        <button class="btn" type="button" onClick={() => props.onClose()}>
          Close
        </button>
      </div>
    </Modal>
  );
}
