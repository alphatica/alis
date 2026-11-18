import { createEffect, createSignal, For, Show } from 'solid-js';
import { api, type ErrorMessage, toErrorMessage } from '../api';
import { formatCell, isNumeric } from '../format';
import { type EdgeTable, type EdgeView, formatStat } from '../state';
import { BridgeChart } from './BridgeChart';
import { ErrorBox } from './ErrorBox';
import { InfoButton } from './InfoButton';
import { TimelineChart } from './TimelineChart';

export function EdgeReport(props: { url: string }) {
  const [view, setView] = createSignal<EdgeView>();
  const [error, setError] = createSignal<ErrorMessage>();
  let latest = 0;
  createEffect(() => {
    const request = ++latest;
    setView(undefined);
    setError(undefined);
    api.post<EdgeView>(props.url).then(
      (response) => request === latest && setView(response),
      (e) => request === latest && setError(toErrorMessage(e)),
    );
  });
  return (
    <>
      <Show when={view()} fallback={<Show when={!error()}><p class="muted">Analyzing…</p></Show>}>
        {(shown) => (
          <div class="edge-report">
            <div class="edge-cards">
              <For each={shown().cards}>
                {(card) => (
                  <section class="panel edge-card">
                    <h3 class="edge-title">
                      {card.title}
                      <InfoButton text={card.help} label={card.title} />
                    </h3>
                    <p class="muted edge-subtitle">{card.subtitle}</p>
                    <Show when={card.table}>{(table) => <Table table={table()} />}</Show>
                    <For each={card.bridges}>{(bridge) => <BridgeChart bridge={bridge} />}</For>
                    <Show when={card.facts.length > 0}>
                      <div class="edge-facts">
                        <For each={card.facts}>
                          {(fact) => (
                            <span>
                              <span class="muted">{fact.name}</span> <span class="mono">{formatStat(fact)}</span>
                            </span>
                          )}
                        </For>
                      </div>
                    </Show>
                    <Show when={card.chart}>{(chart) => <TimelineChart chart={chart()} />}</Show>
                  </section>
                )}
              </For>
            </div>
          </div>
        )}
      </Show>
      <Show when={error()}>{(e) => <ErrorBox error={e()} />}</Show>
    </>
  );
}

function Table(props: { table: EdgeTable }) {
  return (
    <table class="edge-table">
      <thead>
        <tr>
          <For each={props.table.columns}>{(column) => <th classList={{ numeric: isNumeric(column) }}>{column.title}</th>}</For>
        </tr>
      </thead>
      <tbody>
        <For each={props.table.rows}>
          {(row) => (
            <tr>
              <For each={props.table.columns}>
                {(column, i) => (
                  <td classList={{ numeric: isNumeric(column) }}>{formatCell(column, row[i()]) || '—'}</td>
                )}
              </For>
            </tr>
          )}
        </For>
      </tbody>
    </table>
  );
}
