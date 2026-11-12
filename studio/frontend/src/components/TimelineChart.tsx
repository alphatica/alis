import { onCleanup, onMount, Show } from 'solid-js';
import uPlot from 'uplot';
import 'uplot/dist/uPlot.min.css';
import { numberFormat } from '../format';
import type { EdgeSeries, EdgeTimeline } from '../state';

const MAIN_HEIGHT = 260;
const DIFFERENCE_HEIGHT = 150;
const VALUE_AXIS_WIDTH = 72;
const SERIES_COLORS = ['--text-muted', '--accent'];
const DIFFERENCE_COLOR = '--series-2';
const TICK_SPACING = 30;
const NICE_CHANGES = [-99, -95, -90, -80, -70, -50, -30, -20, -10, -5, -2, -1, 1, 2, 5, 10, 20, 30, 50, 100, 200, 300, 500, 1000, 2000, 5000, 10000];
const percent = numberFormat(0, 1);
const cursorPercent = numberFormat(2);
let charts = 0;

type ValueScale = {
  axis: (value: number) => string;
  cursor: (value: number) => string;
  splits?: (u: uPlot, axisIdx: number, min: number, max: number) => number[];
};

const fromLog = (value: number) => Math.expm1(value / 100) * 100;
const toLog = (change: number) => 100 * Math.log1p(change / 100);
const signed = (format: Intl.NumberFormat, change: number) => `${change > 0 ? '+' : ''}${format.format(change)} %`;

const CUMULATIVE: ValueScale = {
  axis: (value) => `${percent.format(value)} %`,
  cursor: (value) => `${cursorPercent.format(value)} %`,
};

const RELATIVE: ValueScale = {
  axis: (value) => signed(percent, fromLog(value)),
  cursor: (value) => signed(cursorPercent, fromLog(value)),
  splits: relativeSplits,
};

function niceStep(raw: number) {
  const power = 10 ** Math.floor(Math.log10(raw));
  return [1, 2, 5, 10].map((factor) => factor * power).find((step) => step >= raw) ?? 10 * power;
}

function relativeSplits(u: uPlot, _axisIdx: number, min: number, max: number) {
  const height = u.bbox.height / devicePixelRatio || u.height;
  const minGap = ((max - min) * TICK_SPACING) / height;
  const low = fromLog(min);
  const high = fromLog(max);
  const step = niceStep((high - low) / Math.max(2, height / (2 * TICK_SPACING)));
  const candidates = new Set(NICE_CHANGES);
  for (let i = Math.ceil(low / step); i * step <= high; i++) {
    candidates.add(i * step);
  }
  const positions = [...candidates].map(toLog).filter((value) => value >= min && value <= max);
  const anchor = min <= 0 && max >= 0 ? 0 : positions.reduce((best, value) => (Math.abs(value) < Math.abs(best) ? value : best), max);
  const kept = [anchor];
  let last = anchor;
  for (const value of positions.filter((value) => value > anchor).sort((a, b) => a - b)) {
    if (value - last >= minGap) {
      kept.push(value);
      last = value;
    }
  }
  last = anchor;
  for (const value of positions.filter((value) => value < anchor).sort((a, b) => b - a)) {
    if (last - value >= minGap) {
      kept.push(value);
      last = value;
    }
  }
  return kept.sort((a, b) => a - b);
}

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

export function TimelineChart(props: { timeline: EdgeTimeline }) {
  let main!: HTMLDivElement;
  let difference!: HTMLDivElement;
  const plots: uPlot[] = [];
  const sync = `edge-timeline-${++charts}`;
  const hasSeries = () => props.timeline.series.length > 0;
  onMount(() => {
    const times = props.timeline.times.map(seconds);
    const differenceHeight = hasSeries() ? DIFFERENCE_HEIGHT : MAIN_HEIGHT + DIFFERENCE_HEIGHT;
    if (hasSeries()) {
      plots.push(
        new uPlot(
          options(main.clientWidth, MAIN_HEIGHT, sync, lines(props.timeline.series, SERIES_COLORS), CUMULATIVE),
          [times, ...props.timeline.series.map((line) => line.values)],
          main,
        ),
      );
    }
    const differencePlot = new uPlot(
      options(difference.clientWidth, differenceHeight, sync, lines([props.timeline.difference], [DIFFERENCE_COLOR]), RELATIVE),
      [times, props.timeline.difference.values],
      difference,
    );
    plots.push(differencePlot);
    const resize = new ResizeObserver(() => {
      if (hasSeries()) {
        plots[0].setSize({ width: main.clientWidth, height: MAIN_HEIGHT });
      }
      differencePlot.setSize({ width: difference.clientWidth, height: differenceHeight });
    });
    resize.observe(difference);
    onCleanup(() => {
      resize.disconnect();
      plots.forEach((plot) => plot.destroy());
      plots.length = 0;
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
