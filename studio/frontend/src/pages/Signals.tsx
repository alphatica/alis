// Strategies → Signals: strategy and start date on the left, the signals and the strategy's info on the right.
// Computed by a job on the backend: after reopening the GUI the last result is shown again, with its settings.

import { createEffect, createMemo, createRoot, createSignal, For, Show, untrack } from 'solid-js';
import { api, type ErrorMessage } from '../api';
import { JobFailed } from '../components/ErrorBox';
import { Loaded } from '../components/Loaded';
import { StableLabel } from '../components/StableLabel';
import { createJob } from '../jobs';
import { signalsOptions } from '../state';
import { type TableData, TextTable } from '../table';

type SignalsResult = { signals: TableData; info: TableData };
/** The form as sent to the backend (SignalsApi.SignalsRequest). */
type SignalsInput = { strategy: string; forPortfolio: boolean; startTime: string };

// Kept outside the component, so the form and the last result are still there after switching screens.
const [strategy, setStrategy] = createSignal('');
const [forPortfolio, setForPortfolio] = createSignal(true);
const [startTime, setStartTime] = createSignal('');
const [result, setResult] = createSignal<SignalsResult>();
const [lastError, setLastError] = createSignal<ErrorMessage>();

const job = createJob<SignalsInput>('signals', {
  reset: () => {
    setResult(undefined);
    setLastError(undefined);
  },
  restore: (input) => {
    setStrategy(input.strategy);
    setForPortfolio(input.forPortfolio);
    setStartTime(input.startTime);
  },
  event: (event, data) => {
    if (event === 'result') {
      setResult(data as SignalsResult);
    }
  },
  failed: setLastError,
});
const running = job.running;

// Defaults: the first strategy; start date = the last day of market data, or today without data
// (updated when new data is loaded).
createRoot(() => {
  createEffect(() => {
    const options = signalsOptions.data();
    if (options && !options.strategies.includes(strategy())) {
      setStrategy(options.strategies[0] ?? '');
    }
  });
  // Not over the start date of a result found after reopening the GUI
  const defaultStartTime = createMemo(() => signalsOptions.data()?.defaultStartTime ?? '');
  createEffect(() => {
    const value = defaultStartTime();
    if (!untrack(job.restored)) {
      setStartTime(value);
    }
  });
});

function show() {
  const input: SignalsInput = { strategy: strategy(), forPortfolio: forPortfolio(), startTime: startTime() };
  job.start(() => api.post<{ jobId: string }>('/api/signals', input));
}

export function Signals() {
  return (
    <div class="split split-narrow split-fill">
      <Loaded store={signalsOptions}>
        {(options) => (
          <form
            class="form-stack"
            onSubmit={(event) => {
              event.preventDefault();
              show();
            }}
          >
            <div class="field">
              <label for="signals-strategy">Select strategy:</label>
              <select
                id="signals-strategy"
                value={strategy()}
                onChange={(event) => setStrategy(event.currentTarget.value)}
              >
                <For each={options().strategies}>{(name) => <option value={name}>{name}</option>}</For>
              </select>
            </div>
            <label class="checkbox">
              <input
                type="checkbox"
                checked={forPortfolio()}
                onChange={(event) => setForPortfolio(event.currentTarget.checked)}
              />
              For portfolio
            </label>
            <div class="field">
              <label for="signals-start">Start date</label>
              <input
                id="signals-start"
                type="text"
                inputmode="numeric"
                placeholder="yyyymmdd"
                maxLength={10}
                value={startTime()}
                disabled={forPortfolio()}
                title={forPortfolio() ? 'Signals for the portfolio start at its first action' : undefined}
                onInput={(event) => setStartTime(event.currentTarget.value)}
              />
            </div>
            <button class="btn btn-primary" type="submit" disabled={running() || !strategy()}>
              <StableLabel labels={['Show signals', 'Running…']} current={running() ? 1 : 0} />
            </button>
            <JobFailed error={lastError()} />
          </form>
        )}
      </Loaded>
      <div class="fill-column">
        <Show when={result()}>
          {(data) => {
            const hasInfo = () => data().info.rows.length > 0;
            return (
              <>
                <TextTable table={data().signals} />
                <Show when={hasInfo()}>
                  <h2>Strategy info</h2>
                  <TextTable table={data().info} />
                </Show>
              </>
            );
          }}
        </Show>
      </div>
    </div>
  );
}
