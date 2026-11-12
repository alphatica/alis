import { createEffect, createSignal, For, Show } from 'solid-js';
import { api, type ErrorMessage, toErrorMessage } from '../api';
import type { EdgeView } from '../state';
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
              <For each={shown().timelines}>
                {(timeline) => (
                  <section class="panel edge-card">
                    <h3 class="edge-title">
                      {timeline.title}
                      <InfoButton text={timeline.help} label={timeline.title} />
                    </h3>
                    <p class="muted edge-subtitle">{timeline.subtitle}</p>
                    <TimelineChart timeline={timeline} />
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
