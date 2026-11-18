import { createMemo, For, Show } from 'solid-js';
import { numberFormat } from '../format';
import type { EdgeBridge, EdgeStep } from '../state';

type Bar = { from: number; to: number };

/** Where every step's bar starts and ends: a total from 0, a change from the level so far; null if one is unknown. */
function bars(steps: EdgeStep[]): Bar[] | null {
  let level = 0;
  const result: Bar[] = [];
  for (const step of steps) {
    if (step.value == null) {
      return null;
    }
    const from = step.total ? 0 : level;
    level = step.total ? step.value : level + step.value;
    result.push({ from, to: level });
  }
  return result;
}

function kind(step: EdgeStep, last: boolean) {
  if (step.total) {
    return last ? 'result' : 'total';
  }
  return (step.value ?? 0) < 0 ? 'down' : 'up';
}

/** Bridge (waterfall) chart: one row per step, with its name, its bar and its value. */
export function BridgeChart(props: { bridge: EdgeBridge }) {
  const format = () => numberFormat(props.bridge.decimals);
  const text = (step: EdgeStep) => {
    if (step.value == null) {
      return '—';
    }
    const sign = !step.total && step.value > 0 ? '+' : '';
    return `${sign}${format().format(step.value)}${props.bridge.suffix}`;
  };
  const layout = createMemo(() => {
    const shown = bars(props.bridge.steps);
    if (shown == null) {
      return null;
    }
    const ends = shown.flatMap((bar) => [bar.from, bar.to]);
    const low = Math.min(0, ...ends);
    const high = Math.max(0, ...ends);
    const span = high > low ? high - low : 1;
    const at = (value: number) => ((value - low) / span) * 100;
    return { bars: shown, at };
  });
  return (
    <>
      <Show when={props.bridge.title}>{(title) => <h4 class="edge-bridge-title">{title()}</h4>}</Show>
      <div class="edge-bridge">
        <For each={props.bridge.steps}>
          {(step, i) => (
            <div class="edge-bridge-row" classList={{ 'edge-bridge-total': step.total }}>
              <span class="edge-bridge-name">{step.name}</span>
              <span class="edge-bridge-track" aria-hidden="true">
                <Show when={layout()}>
                  {(shown) => {
                    const bar = () => shown().bars[i()];
                    return (
                      <>
                        <span class="edge-bridge-zero" style={{ left: `${shown().at(0)}%` }} />
                        <span
                          class={`edge-bar edge-bar-${kind(step, i() === props.bridge.steps.length - 1)}`}
                          style={{
                            left: `${shown().at(Math.min(bar().from, bar().to))}%`,
                            width: `${shown().at(Math.max(bar().from, bar().to)) - shown().at(Math.min(bar().from, bar().to))}%`,
                          }}
                        />
                      </>
                    );
                  }}
                </Show>
              </span>
              <span class="edge-bridge-value">{text(step)}</span>
            </div>
          )}
        </For>
      </div>
    </>
  );
}
