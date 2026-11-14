import { For } from 'solid-js';
import type { ChartScale } from './logAxis';

export type SegmentedOption<T> = { value: T; label: string; title: string };

/** Buttons of which one is on, e.g. in the tab bar above a chart. */
export function Segmented<T>(props: { label: string; options: SegmentedOption<T>[]; value: T; onChange: (value: T) => void }) {
  return (
    <div class="segmented" role="group" aria-label={props.label}>
      <For each={props.options}>
        {(option) => (
          <button
            type="button"
            aria-pressed={props.value === option.value}
            title={option.title}
            onClick={() => props.onChange(option.value)}
          >
            {option.label}
          </button>
        )}
      </For>
    </div>
  );
}

const SCALES: SegmentedOption<ChartScale>[] = [
  { value: 'linear', label: 'Linear', title: 'Arithmetic scale: equal distances are equal changes in value' },
  { value: 'log', label: 'Log', title: 'Logarithmic scale: equal distances are equal changes in percent' },
];

/** Linear / Log switch of a chart's value axis. */
export function ScaleSwitch(props: { scale: ChartScale; onChange: (scale: ChartScale) => void }) {
  return <Segmented label="Scale" options={SCALES} value={props.scale} onChange={props.onChange} />;
}
