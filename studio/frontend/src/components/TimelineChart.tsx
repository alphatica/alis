import { createEffect, onCleanup, onMount, Show } from 'solid-js';
import uPlot from 'uplot';
import 'uplot/dist/uPlot.min.css';
import { numberFormat } from '../format';
import type { EdgeSeries, EdgeTimeline } from '../state';
import { type ChartScale, logSplits, RETURN_LOG, toPositions } from './logAxis';

const MAIN_HEIGHT = 260;
const DIFFERENCE_HEIGHT = 150;
const VALUE_AXIS_WIDTH = 72;
const SERIES_COLORS = ['--text-muted', '--accent'];
const DIFFERENCE_COLOR = '--series-2';
const percent = numberFormat(0, 1);
const cursorPercent = numberFormat(2);
let charts = 0;

type ValueScale = {
  axis: (value: number) => string;
  cursor: (value: number) => string;
  splits?: (u: uPlot, axisIdx: number, min: number, max: number) => number[];
};

const signed = (format: Intl.NumberFormat, change: number) => `${change > 0 ? '+' : ''}${format.format(change)} %`;

const CUMULATIVE: ValueScale = {
  axis: (value) => `${percent.format(value)} %`,
  cursor: (value) => `${cursorPercent.format(value)} %`,
};

/** Cumulative return in log scale: the values are positions on the axis (RETURN_LOG). */
const CUMULATIVE_LOG: ValueScale = {
  axis: (value) => CUMULATIVE.axis(RETURN_LOG.fromLog(value)),
  cursor: (value) => CUMULATIVE.cursor(RETURN_LOG.fromLog(value)),
  splits: logSplits(RETURN_LOG),
};

/** The values are already 100 × ln of the ratio, so this axis is always in log scale. */
const RELATIVE: ValueScale = {
  axis: (value) => signed(percent, RETURN_LOG.fromLog(value)),
  cursor: (value) => signed(cursorPercent, RETURN_LOG.fromLog(value)),
  splits: logSplits(RETURN_LOG),
};

function color(token: string) {
  return getComputedStyle(document.documentElement).getPropertyValue(token).trim();
}

function seconds(day: number) {
  return Date.UTC(Math.floor(day / 10000), (Math.floor(day / 100) % 100) - 1, day % 100) / 1000;
}

function formatDate(value: number) {
  return new Date(value * 1000).toISOString().slice(0, 10);
}

function lastValue(u: uPlot, seriesIdx: number): number | null {
  const values = u.data?.[seriesIdx] ?? [];
  for (let i = values.length - 1; i >= 0; i--) {
    const value = values[i];
    if (value != null) {
      return value;
    }
  }
  return null;
}

function shown(u: uPlot, value: number | null, seriesIdx: number, idx: number | null) {
  return idx == null ? lastValue(u, seriesIdx) : value;
}

function options(
  width: number,
  height: number,
  sync: string,
  series: { name: string; color: string }[],
  scale: ValueScale,
): uPlot.Options {
  const grid = { stroke: color('--border'), width: 1 };
  const axis = { stroke: color('--text-muted'), grid, ticks: grid, font: `11px ${color('--font-ui')}` };
  return {
    width,
    height,
    tzDate: (value) => uPlot.tzDate(new Date(value * 1000), 'Etc/UTC'),
    cursor: { drag: { x: false, y: false }, sync: { key: sync } },
    scales: { x: { time: true } },
    axes: [
      axis,
      { ...axis, size: VALUE_AXIS_WIDTH, splits: scale.splits, values: (_, values) => values.map((value) => scale.axis(value)) },
    ],
    series: [
      {
        label: 'Time',
        value: (u, value, seriesIdx, idx) => {
          const time = shown(u, value, seriesIdx, idx);
          return time == null ? '—' : formatDate(time);
        },
      },
      ...series.map((line) => ({
        label: line.name,
        stroke: line.color,
        width: 1.5,
        value: (u: uPlot, value: number | null, seriesIdx: number, idx: number | null) => {
          const point = shown(u, value, seriesIdx, idx);
          return point == null ? '—' : scale.cursor(point);
        },
      })),
    ],
  };
}

function lines(series: EdgeSeries[], colors: string[]) {
  return series.map((line, i) => ({ name: line.name, color: color(colors[i % colors.length]) }));
}

/** Cumulative returns of the series above (in the scale), and the difference between them below (always log). */
export function TimelineChart(props: { timeline: EdgeTimeline; scale: ChartScale }) {
  let main!: HTMLDivElement;
  let difference!: HTMLDivElement;
  let mainPlot: uPlot | undefined;
  const sync = `edge-timeline-${++charts}`;
  const hasSeries = () => props.timeline.series.length > 0;
  onMount(() => {
    const times = props.timeline.times.map(seconds);
    const differenceHeight = hasSeries() ? DIFFERENCE_HEIGHT : MAIN_HEIGHT + DIFFERENCE_HEIGHT;
    // Rebuilt when the scale changes; it stays in the cursor sync with the difference below
    createEffect(() => {
      if (!hasSeries()) {
        return;
      }
      const log = props.scale === 'log';
      mainPlot?.destroy();
      mainPlot = new uPlot(
        options(main.clientWidth, MAIN_HEIGHT, sync, lines(props.timeline.series, SERIES_COLORS), log ? CUMULATIVE_LOG : CUMULATIVE),
        [times, ...props.timeline.series.map((line) => (log ? toPositions(RETURN_LOG, line.values) : line.values))],
        main,
      );
    });
    const differencePlot = new uPlot(
      options(difference.clientWidth, differenceHeight, sync, lines([props.timeline.difference], [DIFFERENCE_COLOR]), RELATIVE),
      [times, props.timeline.difference.values],
      difference,
    );
    const resize = new ResizeObserver(() => {
      mainPlot?.setSize({ width: main.clientWidth, height: MAIN_HEIGHT });
      differencePlot.setSize({ width: difference.clientWidth, height: differenceHeight });
    });
    resize.observe(difference);
    onCleanup(() => {
      resize.disconnect();
      mainPlot?.destroy();
      mainPlot = undefined;
      differencePlot.destroy();
    });
  });
  return (
    <div class="timeline-chart">
      <Show when={hasSeries()}>
        <div ref={main} class="timeline-pane" />
      </Show>
      <div ref={difference} class="timeline-pane" />
    </div>
  );
}
