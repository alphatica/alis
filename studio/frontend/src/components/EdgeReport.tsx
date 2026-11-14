import { createEffect, createSignal, For, Show } from 'solid-js';
import { api, type ErrorMessage, toErrorMessage } from '../api';
import type { EdgeView } from '../state';
import { ErrorBox } from './ErrorBox';
import { InfoButton } from './InfoButton';
import type { ChartScale } from './logAxis';
import { ScaleSwitch } from './Segmented';
import { TimelineChart } from './TimelineChart';

// Shared by every report (the portfolio's tab and the dialogs), like the equity chart's switches on each screen
const [scale, setScale] = createSignal<ChartScale>('linear');

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
            <Show when={shown().timelines.some((timeline) => timeline.series.length > 0)}>
              <div class="edge-toolbar">
                <ScaleSwitch scale={scale()} onChange={setScale} />
              </div>
            </Show>
            <div class="edge-cards">
              <For each={shown().timelines}>
                {(timeline) => (
                  <section class="panel edge-card">
                    <h3 class="edge-title">
                      {timeline.title}
                      <InfoButton text={timeline.help} label={timeline.title} />
                    </h3>
                    <p class="muted edge-subtitle">{timeline.subtitle}</p>
                    <TimelineChart timeline={timeline} scale={scale()} />
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
