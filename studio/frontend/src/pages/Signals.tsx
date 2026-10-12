import { createEffect, createMemo, createRoot, createSignal, For, Show } from 'solid-js';
import { api } from '../api';
import { Loaded } from '../components/Loaded';
import { StableLabel } from '../components/StableLabel';
import { signalsOptions } from '../state';
import { type TableData, TextTable } from '../table';
import { showError } from '../toasts';

type SignalsResult = { signals: TableData; info: TableData };

// Kept outside the component, so the form and the last result are still there after switching screens.
const [strategy, setStrategy] = createSignal('');
const [forPortfolio, setForPortfolio] = createSignal(true);
const [startTime, setStartTime] = createSignal('');
const [running, setRunning] = createSignal(false);
const [result, setResult] = createSignal<SignalsResult>();

// Defaults: the first strategy; start date = the last day of market data, or today without data
// (updated when new data is loaded).
createRoot(() => {
  createEffect(() => {
    const options = signalsOptions.data();
    if (options && !options.strategies.includes(strategy())) {
      setStrategy(options.strategies[0] ?? '');
    }
  });
  const defaultStartTime = createMemo(() => signalsOptions.data()?.defaultStartTime ?? '');
  createEffect(() => setStartTime(defaultStartTime()));
});

async function show() {
  setResult(undefined);
  setRunning(true);
  try {
    setResult(
      await api.post<SignalsResult>('/api/signals', {
        strategy: strategy(),
        forPortfolio: forPortfolio(),
        startTime: startTime(),
      }),
    );
  } catch (e) {
    showError(e);
  } finally {
    setRunning(false);
  }
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
