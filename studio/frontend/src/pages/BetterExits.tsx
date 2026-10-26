// Analysis → Find better exits: an account log file, the commission rate and
// the scorer on the left, with the log's own score and the iteration count; the best 100 alternative exits on the
// right. Results arrive live from the search job, which runs until stopped. The job and its results live on the
// backend: after reopening the GUI the last search is shown again.

import { createEffect, createMemo, createRoot, createSignal, For, on, Show } from 'solid-js';
import { api, type ErrorMessage } from '../api';
import { JobFailed } from '../components/ErrorBox';
import { Field } from '../components/Field';
import { Loaded } from '../components/Loaded';
import { StableLabel } from '../components/StableLabel';
import { type ColumnSpec, formatValue, numberFormat, type Value } from '../format';
import { createJob } from '../jobs';
import { betterExitsOptions, type BetterExitsOptions } from '../state';
import { staticSource, valueColumns, VirtualTable } from '../table';
import { showError } from '../toasts';

/** One row of the results; `cells` are the values of the results columns. */
type Result = { score: number; name: string; cells: Value[] };
type LoadedFile = NonNullable<BetterExitsOptions['file']>;
/** The form as sent to the backend (BetterExitsApi.SearchRequest). */
type SearchInput = { commissionRate: string; scorer: string };

// Kept outside the component, so the form, the running job and the results survive switching screens.
const [file, setFile] = createSignal<LoadedFile>();
const [loading, setLoading] = createSignal(false);
const [commissionRate, setCommissionRate] = createSignal('');
const [scorer, setScorer] = createSignal('');
const [originalScore, setOriginalScore] = createSignal<Value>();

const [iterations, setIterations] = createSignal<number>();
const [results, setResults] = createSignal<Result[]>([]);
const [lastError, setLastError] = createSignal<ErrorMessage>();

const job = createJob<SearchInput>('better-exits', {
  reset: () => {
    setResults([]);
    setIterations(undefined);
    setLastError(undefined);
  },
  restore: (input) => {
    setCommissionRate(input.commissionRate);
    setScorer(input.scorer);
    refreshScore();
  },
  event: (event, data) => {
    if (event === 'results') {
      setResults(data as Result[]);
    } else if (event === 'iterations') {
      setIterations(data as number);
    }
  },
  failed: setLastError,
});
const { running, stopping, started } = job;

createRoot(() => {
  createEffect(() => {
    const options = betterExitsOptions.data();
    if (options) {
      if (!options.scorers.includes(scorer())) {
        setScorer(options.scorers[0] ?? '');
      }
      if (!commissionRate()) {
        setCommissionRate(options.defaultCommissionRate);
      }
    }
  });
  // The log stays loaded on the backend, e.g. after reloading the page: show it with its score.
  createEffect(
    on(
      () => betterExitsOptions.data()?.file,
      (loaded) => {
        if (loaded && file() === undefined) {
          setFile(loaded);
          refreshScore();
        }
      },
    ),
  );
});

let scoreRequest = 0;

/** Score of the loaded log as it is; recalculated when the scorer or commission rate changes. */
async function refreshScore() {
  if (!file()) {
    return;
  }
  const request = ++scoreRequest;
  try {
    const { score } = await api.post<{ score: Value }>('/api/better-exits/score', settings());
    if (request === scoreRequest) {
      setOriginalScore(score);
    }
  } catch (e) {
    if (request === scoreRequest) {
      setOriginalScore(undefined);
      showError(e);
    }
  }
}

async function loadFile(chosen: File) {
  setLoading(true);
  try {
    const loaded = await api.upload<LoadedFile>(`/api/better-exits/file?name=${encodeURIComponent(chosen.name)}`, chosen);
    setFile(loaded);
    setOriginalScore(undefined);
    await refreshScore();
  } catch (e) {
    showError(e);
  } finally {
    setLoading(false);
  }
}

const settings = (): SearchInput => ({ commissionRate: commissionRate(), scorer: scorer() });

function start() {
  refreshScore();
  const input = settings();
  job.start(() => api.post<{ jobId: string }>('/api/better-exits', input));
}

export function BetterExits() {
  return (
    <div class="page-fill">
      <div class="split split-narrow split-stretch">
        <div class="sidebar">
          <Loaded store={betterExitsOptions}>{(options) => <SettingsForm scorers={options().scorers} />}</Loaded>
        </div>
        <div class="fill-column">
          <h2>Exit analysis results</h2>
          <Loaded store={betterExitsOptions}>{(options) => <ResultsTable columns={options().results} />}</Loaded>
        </div>
      </div>
    </div>
  );
}

function SettingsForm(props: { scorers: string[] }) {
  let fileInput!: HTMLInputElement;
  return (
    <form
      class="form-stack"
      onSubmit={(event) => {
        event.preventDefault();
        if (!running() && file()) {
          start();
        }
      }}
    >
      <div class="field">
        <label for="better-exits-file">Account log</label>
        <input
          ref={fileInput}
          id="better-exits-file"
          type="file"
          accept=".csv,.txt,text/csv,text/plain"
          hidden
          onChange={(event) => {
            const chosen = event.currentTarget.files?.[0];
            event.currentTarget.value = '';
            if (chosen) {
              loadFile(chosen);
            }
          }}
        />
        <button class="btn" type="button" disabled={running() || loading()} onClick={() => fileInput.click()}>
          <StableLabel labels={['Load file…', 'Loading…']} current={loading() ? 1 : 0} />
        </button>
      </div>
      <span class="muted">
        Loaded file:{' '}
        <Show when={file()} fallback="—">
          {(loaded) => (
            <>
              {loaded().name} ({numberFormat(0).format(loaded().actions)} actions)
            </>
          )}
        </Show>
      </span>
      <Field
        id="better-exits-commission"
        label="Commission rate"
        inputmode="decimal"
        title="Fraction of the order value, e.g. 0.01"
        disabled={running()}
        value={commissionRate()}
        onInput={setCommissionRate}
        onChange={() => refreshScore()}
      />
      <div class="field">
        <label for="better-exits-scorer">Result scorer</label>
        <select
          id="better-exits-scorer"
          disabled={running()}
          value={scorer()}
          onChange={(event) => {
            setScorer(event.currentTarget.value);
            refreshScore();
          }}
        >
          <For each={props.scorers}>{(name) => <option value={name}>{name}</option>}</For>
        </select>
      </div>
      <span class="muted">Original score: {formatValue(originalScore(), 1) || '—'}</span>
      <span class="muted">
        Iterations: {iterations() === undefined ? (running() ? '…' : '—') : numberFormat(0).format(iterations()!)}
      </span>
      <JobFailed error={lastError()} />
      <div class="sidebar-buttons">
        <button class="btn btn-primary" type="submit" disabled={running() || !file() || loading()}>
          <StableLabel labels={['Start', 'Running…']} current={running() ? 1 : 0} />
        </button>
        <button class="btn" type="button" disabled={!running() || stopping()} onClick={job.stop}>
          <StableLabel labels={['Stop', 'Stopping…']} current={stopping() && running() ? 1 : 0} />
        </button>
      </div>
    </form>
  );
}

function ResultsTable(props: { columns: ColumnSpec[] }) {
  const columns = createMemo(() => valueColumns(props.columns, (row: Result) => row.cells));
  return (
    <VirtualTable
      columns={columns()}
      source={staticSource(results)}
      emptyMessage={
        running() ? 'No results yet.' : started() ? 'No results.' : 'Load an account log and start the search to see better exits.'
      }
    />
  );
}
