import { onCleanup, onMount } from 'solid-js';
import uPlot from 'uplot';
import 'uplot/dist/uPlot.min.css';
import { numberFormat } from '../format';
import type { EdgeChart } from '../state';
import { logSplits, RETURN_LOG } from './logAxis';

const HEIGHT = 300;
const VALUE_AXIS_WIDTH = 72;
const LINE_COLORS = ['--series-2', '--accent', '--text', '--text-muted'];
const AXIS_DECIMALS = 1;
const MAX_AXIS_DECIMALS = 4;
const cursorPercent = numberFormat(2);
let charts = 0;

type ValueScale = {
  /** Labels of the axis values, e.g. with the same fraction digits */
  axis: (values: number[]) => string[];
  cursor: (value: number) => string;
  splits?: (u: uPlot, axisIdx: number, min: number, max: number) => number[];
};

/** A change in percent with its sign; a change that shows as 0 has none (not "+0 %" or "-0 %"). */
function signed(format: Intl.NumberFormat, change: number) {
  const text = format.format(change);
  if (!/[1-9]/.test(text)) {
    return `${format.format(0)} %`;
  }
  return `${change > 0 ? '+' : ''}${text} %`;
}

/** Enough fraction digits to show every label as it is (2 for 0.02 %), so labels of a small range differ. */
function axisFormat(values: number[]) {
  let decimals = AXIS_DECIMALS;
  for (const value of values) {
    const tolerance = 1e-9 * Math.max(1, Math.abs(value));
    while (decimals < MAX_AXIS_DECIMALS && Math.abs(Math.round(value * 10 ** decimals) / 10 ** decimals - value) > tolerance) {
      decimals++;
    }
  }
  return numberFormat(0, decimals);
}

const SCALES: Record<EdgeChart['scale'], ValueScale> = {
  /** The values are already 100 × ln of the ratio, so this axis is always in log scale. */
  RELATIVE: {
    axis: (values) => {
      const changes = values.map((value) => RETURN_LOG.fromLog(value));
      const format = axisFormat(changes);
      return changes.map((change) => signed(format, change));
    },
    cursor: (value) => signed(cursorPercent, RETURN_LOG.fromLog(value)),
    splits: logSplits(RETURN_LOG),
  },
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

/** The value under the cursor, or the last one when the cursor is not on the chart. */
function shown(u: uPlot, value: number | null, seriesIdx: number, idx: number | null) {
  return idx == null ? lastValue(u, seriesIdx) : value;
}

function options(width: number, chart: EdgeChart): uPlot.Options {
  const scale = SCALES[chart.scale];
  const grid = { stroke: color('--border'), width: 1 };
  const axis = { stroke: color('--text-muted'), grid, ticks: grid, font: `11px ${color('--font-ui')}` };
  return {
    width,
    height: HEIGHT,
    tzDate: (value) => uPlot.tzDate(new Date(value * 1000), 'Etc/UTC'),
    cursor: { drag: { x: false, y: false }, sync: { key: `edge-chart-${++charts}` } },
    scales: { x: { time: true } },
    axes: [
      axis,
      { ...axis, size: VALUE_AXIS_WIDTH, splits: scale.splits, values: (_, values) => scale.axis(values) },
    ],
    series: [
      {
        label: 'Time',
        value: (u, value, seriesIdx, idx) => {
          const time = shown(u, value, seriesIdx, idx);
          return time == null ? '—' : formatDate(time);
        },
      },
      ...chart.lines.map((line, i) => ({
        label: line.name,
        stroke: color(LINE_COLORS[i % LINE_COLORS.length]),
        width: 1.5,
        value: (u: uPlot, value: number | null, seriesIdx: number, idx: number | null) => {
          const point = shown(u, value, seriesIdx, idx);
          return point == null ? '—' : scale.cursor(point);
        },
      })),
    ],
  };
}

/** Lines of an edge card over the sessions, in the chart's scale. */
export function TimelineChart(props: { chart: EdgeChart }) {
  let pane!: HTMLDivElement;
  onMount(() => {
    const plot = new uPlot(
      options(pane.clientWidth, props.chart),
      [props.chart.times.map(seconds), ...props.chart.lines.map((line) => line.values)],
      pane,
    );
    const resize = new ResizeObserver(() => plot.setSize({ width: pane.clientWidth, height: HEIGHT }));
    resize.observe(pane);
    onCleanup(() => {
      resize.disconnect();
      plot.destroy();
    });
  });
  return <div ref={pane} class="timeline-pane" />;
}
