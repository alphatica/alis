import { createEffect, createSignal, For, onCleanup, onMount, Show } from 'solid-js';
import uPlot from 'uplot';
import 'uplot/dist/uPlot.min.css';
import { numberFormat } from '../format';
import { type Equity, formatStat, type Stat } from '../state';
import { showError } from '../toasts';
import { ChartGroup } from './chartGroup';
import { openContextMenu } from './ContextMenu';
import { AMOUNT_LOG, type ChartScale, type LogAxis, logSplits, RETURN_LOG, toPositions } from './logAxis';
import { ScaleSwitch, Segmented, type SegmentedOption } from './Segmented';

/** nav: account value, jumps with deposits and withdrawals; return: cumulative return in %, without them. */
export type EquityMode = 'nav' | 'return';

// The closed positions line is drawn first, so where it is the same as the main line (nothing open), the main line covers it
const CLOSED = 1;
const MAIN = 2;

/** yyyymmdd → seconds since the epoch at midnight UTC (uPlot's time unit). */
function toSeconds(time: number) {
  return Date.UTC(Math.floor(time / 10000), (Math.floor(time / 100) % 100) - 1, time % 100) / 1000;
}

/** The value axis in log scale; undefined in linear. */
function logAxis(mode: EquityMode, scale: ChartScale): LogAxis | undefined {
  if (scale === 'linear') {
    return undefined;
  }
  return mode === 'nav' ? AMOUNT_LOG : RETURN_LOG;
}

type Plotted = { closed: (number | null)[]; main: (number | null)[] };

/** The plotted values of both lines: in log scale, their positions on the axis. */
function plotted(equity: Equity, mode: EquityMode, scale: ChartScale): Plotted {
  const log = logAxis(mode, scale);
  const positions = (values: number[]) => (log ? toPositions(log, values) : values);
  return mode === 'nav'
    ? { closed: positions(equity.closed), main: positions(equity.nav) }
    : { closed: positions(equity.closedReturnPercent), main: positions(equity.returnPercent) };
}

/** Colors come from the CSS tokens, so the chart follows the app's theme. */
function color(token: string) {
  return getComputedStyle(document.documentElement).getPropertyValue(token).trim();
}

// In the user's locale, like the rest of the portfolio screen
const formatNav = numberFormat(0);
const formatPercent = new Intl.NumberFormat(undefined, { maximumFractionDigits: 1 });
const formatCursorPercent = numberFormat(2);

/** seconds → yyyy-mm-dd */
function formatDate(seconds: number) {
  return new Date(seconds * 1000).toISOString().slice(0, 10);
}

/** Moves the cursor to the nearest session's point on the line, so the crosshair shows actual values. */
function snapToPoint(series: number) {
  return (u: uPlot, left: number, top: number): [number, number] => {
    if (left < 0) {
      return [left, top];
    }
    const idx = u.posToIdx(left);
    const y = u.data[series][idx];
    if (y == null) {
      return [left, top];
    }
    return [Math.round(u.valToPos(u.data[0][idx], 'x')), Math.round(u.valToPos(y, 'y'))];
  };
}

function cursor(group: ChartGroup, series: number): uPlot.Cursor {
  return { drag: { x: true, y: false }, move: snapToPoint(series), sync: { key: group.key, scales: ['x', null] } };
}

function zoomHooks(group: ChartGroup): uPlot.Hooks.Arrays {
  return { setScale: [(u, key) => key === 'x' && group.zoomed(u)] };
}

/**
 * Date and value of the point under the cursor, shown on the axes where the crosshair meets them, and the closed positions
 * line's value at its height, unless it would cover the main line's label.
 */
function cursorLabels(
  formatValue: (value: number) => string,
  series: number,
  closedSeries?: number,
  showDate = true,
): uPlot.Plugin {
  const xLabel = document.createElement('div');
  const yLabel = document.createElement('div');
  const closedLabel = document.createElement('div');
  xLabel.className = 'chart-cursor-label chart-cursor-x';
  yLabel.className = 'chart-cursor-label chart-cursor-y';
  closedLabel.className = 'chart-cursor-label chart-cursor-y chart-cursor-closed';
  return {
    hooks: {
      init: (u) => {
        u.over.parentElement!.append(xLabel, closedLabel, yLabel);
      },
      setCursor: (u) => {
        const { left, top, idx } = u.cursor;
        const y = idx == null ? null : u.data[series][idx];
        const visible = left != null && top != null && left >= 0 && y != null;
        xLabel.hidden = yLabel.hidden = closedLabel.hidden = !visible;
        xLabel.hidden ||= !showDate;
        if (!visible) {
          return;
        }
        const plotLeft = u.over.offsetLeft;
        const plotTop = u.over.offsetTop;
        xLabel.textContent = formatDate(u.data[0][idx!]);
        // Kept inside the chart at both ends
        const half = xLabel.offsetWidth / 2;
        const wrapWidth = u.over.parentElement!.clientWidth;
        xLabel.style.left = `${Math.min(Math.max(plotLeft + left, plotLeft + half), wrapWidth - half)}px`;
        xLabel.style.top = `${plotTop + u.over.clientHeight}px`;
        yLabel.textContent = formatValue(y);
        yLabel.style.left = `${plotLeft}px`;
        yLabel.style.top = `${plotTop + top}px`;
        const closed = closedSeries == null ? null : u.data[closedSeries][idx!];
        const closedTop = closed == null ? null : u.valToPos(closed, 'y');
        closedLabel.hidden = closedTop == null || Math.abs(closedTop - top) < yLabel.offsetHeight;
        if (!closedLabel.hidden) {
          closedLabel.textContent = formatValue(closed!);
          closedLabel.style.left = `${plotLeft}px`;
          closedLabel.style.top = `${plotTop + closedTop!}px`;
        }
      },
    },
  };
}

const measureContext = document.createElement('canvas').getContext('2d')!;

function textWidth(text: string, font: string) {
  measureContext.font = font;
  return measureContext.measureText(text).width;
}

// The value axis is at least this wide, and wider when its labels or the cursor's label need more
const VALUE_AXIS_MIN_WIDTH = 70;
// uPlot's default tick length (10) and gap between the tick and its label (5)
const TICK_SPACE = 15;
// .chart-cursor-y's offset from the axis (app.css)
const CURSOR_LABEL_GAP = 4;

/** Names and colors of the lines, drawn in the plot's top left corner, so they are also in the copied image. */
function legend(items: { label: string; color: string }[]): uPlot.Plugin {
  return {
    hooks: {
      draw: (u) => {
        const ratio = uPlot.pxRatio;
        const context = u.ctx;
        const padding = 6 * ratio;
        const swatch = 14 * ratio;
        const gap = 5 * ratio;
        const spacing = 12 * ratio;
        const height = 20 * ratio;
        context.save();
        context.font = `${11 * ratio}px ${color('--font-ui')}`;
        const widths = items.map((item) => context.measureText(item.label).width);
        const width = widths.reduce((sum, w) => sum + swatch + gap + w, 0) + spacing * (items.length - 1) + 2 * padding;
        const left = u.bbox.left + 8 * ratio;
        const top = u.bbox.top + 8 * ratio;
        context.globalAlpha = 0.85;
        context.fillStyle = color('--bg');
        context.fillRect(left, top, width, height);
        context.globalAlpha = 1;
        // uPlot leaves the alignment of its last axis labels (centered or right) in the context
        context.textAlign = 'left';
        context.textBaseline = 'middle';
        const middle = top + height / 2;
        let x = left + padding;
        items.forEach((item, i) => {
          context.strokeStyle = item.color;
          context.lineWidth = 2 * ratio;
          context.beginPath();
          context.moveTo(x, middle);
          context.lineTo(x + swatch, middle);
          context.stroke();
          x += swatch + gap;
          context.fillStyle = color('--text-muted');
          context.fillText(item.label, x, middle);
          x += widths[i] + spacing;
        });
        context.restore();
      },
    },
  };
}

/** Width of a cursor label with the text, measured on the page, so it is exactly what the browser draws. */
function cursorLabelWidth(text: string) {
  const label = document.createElement('div');
  label.className = 'chart-cursor-label';
  label.style.visibility = 'hidden';
  label.textContent = text;
  document.body.append(label);
  const width = label.getBoundingClientRect().width;
  label.remove();
  return width;
}

/** Width of the widest cursor label for the values; the widest is the one farthest from 0, so the min or the max. */
function widestCursorLabel(lines: (number | null)[][], format: (value: number) => string) {
  let min = Infinity;
  let max = -Infinity;
  for (const v of lines.flat()) {
    if (v != null) {
      min = Math.min(min, v);
      max = Math.max(max, v);
    }
  }
  if (min > max) {
    return 0;
  }
  return Math.max(cursorLabelWidth(format(min)), cursorLabelWidth(format(max)));
}

function valueAxisSize(group: ChartGroup, cursorLabelSpace: number, font: string) {
  return (u: uPlot, ticks: string[] | null) =>
    group.valueAxisWidth(
      u,
      Math.ceil(
        Math.max(
          VALUE_AXIS_MIN_WIDTH,
          cursorLabelSpace,
          TICK_SPACE + Math.max(0, ...(ticks ?? []).map((tick) => textWidth(tick, font))),
        ),
      ),
    );
}

function options(
  width: number,
  height: number,
  mode: EquityMode,
  scale: ChartScale,
  values: Plotted,
  group: ChartGroup,
): uPlot.Options {
  const muted = color('--text-muted');
  const grid = { stroke: color('--border'), width: 1 };
  const accent = color('--accent');
  const closedColor = color('--series-2');
  const mainLabel = mode === 'nav' ? 'NAV' : 'Return';
  const axisFont = `11px ${color('--font-ui')}`;
  const axis = { stroke: muted, grid, ticks: grid, font: axisFont };
  const log = logAxis(mode, scale);
  // In log scale the plotted values are positions on the axis: labels show the values back
  const value = log ? log.fromLog : (v: number) => v;
  const format =
    mode === 'nav' ? (v: number) => formatNav.format(value(v)) : (v: number) => `${formatPercent.format(value(v))} %`;
  const formatCursor = mode === 'nav' ? format : (v: number) => `${formatCursorPercent.format(value(v))} %`;
  // The cursor's label sits left of the plot, over the axis: the axis is wide enough for any value, so it is not cut off
  const cursorLabelSpace = CURSOR_LABEL_GAP + widestCursorLabel([values.main, values.closed], formatCursor);
  return {
    width,
    height,
    // Sessions are dates, not instants: show them the same in every time zone.
    tzDate: (seconds) => uPlot.tzDate(new Date(seconds * 1000), 'Etc/UTC'),
    legend: { show: false },
    cursor: cursor(group, MAIN),
    hooks: zoomHooks(group),
    plugins: [
      cursorLabels(formatCursor, MAIN, CLOSED),
      legend([
        { label: mainLabel, color: accent },
        { label: 'Closed positions', color: closedColor },
      ]),
    ],
    scales: { x: { time: true } },
    axes: [
      axis,
      {
        ...axis,
        size: valueAxisSize(group, cursorLabelSpace, axisFont),
        splits: log && logSplits(log),
        values: (_, values) => values.map(format),
      },
    ],
    series: [
      {},
      { label: 'Closed positions', stroke: closedColor, width: 1.25 },
      {
        label: mainLabel,
        stroke: accent,
        width: 1.5,
        // NAV is filled down to the axis; return is filled to 0 % (also 0 in log scale), so gains and losses are visible.
        fill: `${accent}1f`,
        fillTo: mode === 'nav' ? undefined : 0,
      },
    ],
  };
}

// The stat tiles in the image, drawn like StatTiles (.stats, .stat, .stat-name, .stat-value in app.css)
const TILE_MIN_WIDTH = 180;
const TILE_PADDING_X = 16;
const TILE_PADDING_Y = 12;
const NAME_LINE_HEIGHT = 16.5;
const VALUE_TOP_MARGIN = 4;
const VALUE_LINE_HEIGHT = 27;
const TILES_GAP = 16;
// Copyright line under everything, right-aligned
const FOOTER_HEIGHT = 22;

const nameFont = () => `600 11px ${color('--font-ui')}`;
const NAME_LETTER_SPACING = '0.66px';

/** Height of a tile whose name takes the given number of lines. */
function tileHeight(nameLines: number) {
  return 2 * TILE_PADDING_Y + nameLines * NAME_LINE_HEIGHT + VALUE_TOP_MARGIN + VALUE_LINE_HEIGHT;
}

/**
 * The chart as drawn now (with the current zoom) and the stat tiles under it, as PNG on the page background
 * (the chart's canvas itself is transparent). Laid out like on screen: as many tile columns as fit, long names
 * wrap, and every tile in a row is as tall as the tallest one.
 */
function toPng(u: uPlot, stats: Stat[]): Promise<Blob> {
  const source = u.ctx.canvas;
  // The canvas is in device pixels; everything else is drawn in CSS pixels and scaled to match
  const scale = source.width / u.width;
  const width = u.width;
  // Tiles and footer line up with the plot area (between the value axis and the right end of the line), not the whole image
  const plotLeft = u.bbox.left / scale;
  const plotWidth = u.bbox.width / scale;
  const columns = Math.max(1, Math.floor(plotWidth / TILE_MIN_WIDTH));
  const tileWidth = (plotWidth - 1) / columns;

  const image = document.createElement('canvas');
  const context = image.getContext('2d')!;
  // Names are wrapped first: the row heights, and so the image height, depend on them
  context.font = nameFont();
  context.letterSpacing = NAME_LETTER_SPACING;
  const names = stats.map((stat) => wrapText(context, stat.name.toUpperCase(), tileWidth - 2 * TILE_PADDING_X));
  const rowHeights: number[] = [];
  names.forEach((lines, i) => {
    const row = Math.floor(i / columns);
    rowHeights[row] = Math.max(rowHeights[row] ?? 0, tileHeight(lines.length));
  });
  const tilesTop = u.height + TILES_GAP;
  const contentHeight = stats.length > 0 ? tilesTop + rowHeights.reduce((sum, h) => sum + h, 0) + 1 : u.height;
  const height = contentHeight + FOOTER_HEIGHT;

  // Resizing resets the context, so the scale and fonts are set after it
  image.width = Math.round(width * scale);
  image.height = Math.round(height * scale);
  context.scale(scale, scale);
  context.fillStyle = color('--bg');
  context.fillRect(0, 0, width, height);
  context.drawImage(source, 0, 0, u.width, u.height);

  let rowTop = tilesTop;
  stats.forEach((stat, i) => {
    const column = i % columns;
    const row = Math.floor(i / columns);
    if (column === 0 && row > 0) {
      rowTop += rowHeights[row - 1];
    }
    drawTile(context, names[i], formatStat(stat), plotLeft + column * tileWidth, rowTop, tileWidth, rowHeights[row]);
  });
  drawFooter(context, plotLeft + plotWidth, contentHeight);

  return new Promise((resolve, reject) =>
    image.toBlob((blob) => (blob ? resolve(blob) : reject(new Error('Unable to create the image'))), 'image/png'),
  );
}

function drawTile(
  context: CanvasRenderingContext2D,
  nameLines: string[],
  text: string,
  x: number,
  y: number,
  width: number,
  height: number,
) {
  context.fillStyle = color('--surface');
  context.fillRect(x, y, width, height);
  context.strokeStyle = color('--border');
  context.lineWidth = 1;
  context.strokeRect(x + 0.5, y + 0.5, width, height);

  const textLeft = x + TILE_PADDING_X;
  context.textBaseline = 'middle';
  context.font = nameFont();
  context.letterSpacing = NAME_LETTER_SPACING;
  context.fillStyle = color('--text-muted');
  nameLines.forEach((line, i) =>
    context.fillText(line, textLeft, y + TILE_PADDING_Y + (i + 0.5) * NAME_LINE_HEIGHT),
  );
  const valueTop = y + TILE_PADDING_Y + nameLines.length * NAME_LINE_HEIGHT + VALUE_TOP_MARGIN;
  context.font = `18px ${color('--font-mono')}`;
  context.letterSpacing = '0px';
  context.fillStyle = color('--text');
  context.fillText(fitText(context, text, width - 2 * TILE_PADDING_X), textLeft, valueTop + VALUE_LINE_HEIGHT / 2);
}

/** "© Alphatica <year>", small and muted, so it is readable but does not draw attention. */
function drawFooter(context: CanvasRenderingContext2D, right: number, top: number) {
  context.font = `10px ${color('--font-ui')}`;
  context.letterSpacing = '0.3px';
  context.fillStyle = color('--text-muted');
  context.globalAlpha = 0.7;
  context.textAlign = 'right';
  context.textBaseline = 'bottom';
  context.fillText(`© Alphatica ${new Date().getFullYear()}`, right, top + FOOTER_HEIGHT - 4);
  context.globalAlpha = 1;
  context.textAlign = 'left';
}

/** Splits the text into lines at spaces, like the tiles on screen; a word wider than a line is shortened with "…". */
function wrapText(context: CanvasRenderingContext2D, text: string, maxWidth: number): string[] {
  const lines: string[] = [];
  let line = '';
  for (const word of text.split(' ')) {
    const candidate = line ? `${line} ${word}` : word;
    if (!line || context.measureText(candidate).width <= maxWidth) {
      line = candidate;
    } else {
      lines.push(line);
      line = word;
    }
  }
  lines.push(line);
  return lines.map((l) => fitText(context, l, maxWidth));
}

/** The text, shortened with "…" when it is wider than maxWidth. */
function fitText(context: CanvasRenderingContext2D, text: string, maxWidth: number) {
  if (context.measureText(text).width <= maxWidth) {
    return text;
  }
  let end = text.length;
  while (end > 0 && context.measureText(`${text.slice(0, end)}…`).width > maxWidth) {
    end--;
  }
  return `${text.slice(0, end)}…`;
}

async function copyImage(u: uPlot, stats: Stat[]) {
  try {
    // A promise of the blob, not the blob: Safari allows writing only within the click
    await navigator.clipboard.write([new ClipboardItem({ 'image/png': toPng(u, stats) })]);
  } catch (e) {
    showError(e);
  }
}

async function saveImage(u: uPlot, stats: Stat[], fileName: string) {
  try {
    const link = document.createElement('a');
    link.href = URL.createObjectURL(await toPng(u, stats));
    link.download = fileName;
    link.click();
    setTimeout(() => URL.revokeObjectURL(link.href), 0);
  } catch (e) {
    showError(e);
  }
}

/**
 * Account value or return over time, with the effect of closed positions (open ones at entry value). Drag to zoom, double-click to zoom out. Right click: copy or save as PNG,
 * together with the stats (shown under the chart by StatTiles). The plot fills its container and follows its size.
 */
export function EquityChart(props: {
  equity: Equity;
  mode: EquityMode;
  scale: ChartScale;
  stats: Stat[];
  fileName: string;
}) {
  let container!: HTMLDivElement;
  let plot: uPlot | undefined;
  const group = new ChartGroup();

  onMount(() => {
    // Rebuilt when the data, the mode or the scale change: axis formats and series differ between them.
    createEffect(() => {
      const values = plotted(props.equity, props.mode, props.scale);
      group.use(props.equity);
      destroyPlot();
      plot = new uPlot(
        options(container.clientWidth, container.clientHeight, props.mode, props.scale, values, group),
        [props.equity.times.map(toSeconds), values.closed, values.main],
        container,
      );
      group.add(plot);
    });
    const resize = new ResizeObserver(() => plot?.setSize({ width: container.clientWidth, height: container.clientHeight }));
    resize.observe(container);
    onCleanup(() => {
      resize.disconnect();
      destroyPlot();
    });
  });

  const destroyPlot = () => {
    if (plot) {
      group.remove(plot);
      plot.destroy();
      plot = undefined;
    }
  };

  const showMenu = (event: MouseEvent) => {
    const u = plot;
    if (!u) {
      return;
    }
    const fileName = `${props.fileName}-${props.mode === 'nav' ? 'nav' : 'return'}${props.scale === 'log' ? '-log' : ''}.png`;
    openContextMenu(event, [
      { label: 'Copy image', run: () => copyImage(u, props.stats) },
      { label: 'Save image…', run: () => saveImage(u, props.stats, fileName) },
    ]);
  };

  return (
    <div class="equity-chart">
      <div ref={container} class="chart" onContextMenu={showMenu} />
      <For each={PANELS}>
        {(panel) => (
          <Show when={isOpen(panel)}>
            <PanelChart panel={panel} equity={props.equity} group={group} fileName={props.fileName} />
          </Show>
        )}
      </For>
      <div class="chart-panel-toggles" role="group" aria-label="More charts">
        <For each={PANELS}>
          {(panel) => (
            <button
              type="button"
              aria-expanded={isOpen(panel)}
              title={panel.title}
              onClick={() => toggle(panel)}
            >
              <span class="chart-panel-arrow" aria-hidden="true">
                ▸
              </span>
              {panel.label}
            </button>
          )}
        </For>
      </div>
    </div>
  );
}

type Panel = {
  id: string;
  label: string;
  title: string;
  color: string;
  values: (equity: Equity) => number[];
  range: (min: number, max: number) => [number, number];
  format: (value: number) => string;
  formatCursor: (value: number) => string;
  incrs?: number[];
  stepped?: boolean;
};

const percent = {
  format: (v: number) => `${formatPercent.format(v)} %`,
  formatCursor: (v: number) => `${formatCursorPercent.format(v)} %`,
};

const formatCount = numberFormat(0);

const drawdownRange = (min: number): [number, number] => [Math.min(-1, 1.05 * min), 0];

const PANELS: Panel[] = [
  {
    id: 'exposure',
    label: 'Exposure',
    title: 'Positions at the session\'s prices as % of NAV',
    color: '--accent',
    values: (equity) => equity.exposurePercent,
    range: (_, max) => [0, 1.05 * Math.max(100, max)],
    ...percent,
  },
  {
    id: 'drawdown',
    label: 'Drawdown',
    title: 'Decline from the highest value, not affected by deposits and withdrawals',
    color: '--loss',
    values: (equity) => equity.drawdownPercent,
    range: drawdownRange,
    ...percent,
  },
  {
    id: 'positions',
    label: 'Positions',
    title: 'Number of markets held, whatever their value',
    color: '--series-2',
    values: (equity) => equity.positionCount,
    range: (_, max) => [0, Math.max(1, Math.ceil(1.05 * max))],
    format: (v) => formatCount.format(v),
    formatCursor: (v) => formatCount.format(v),
    incrs: [1, 2, 5, 10, 20, 50, 100, 200, 500, 1000],
    stepped: true,
  },
  {
    id: 'downside-drawdown',
    label: 'Downside drawdown',
    title: 'NAV below the highest closed equity: giving back unrealized profit is not a drawdown',
    color: '--loss',
    values: (equity) => equity.downsideDrawdownPercent,
    range: drawdownRange,
    ...percent,
  },
];

const [openPanels, setOpenPanels] = createSignal<string[]>([]);

const isOpen = (panel: Panel) => openPanels().includes(panel.id);

const toggle = (panel: Panel) =>
  setOpenPanels((open) => (open.includes(panel.id) ? open.filter((id) => id !== panel.id) : [...open, panel.id]));

function panelOptions(width: number, height: number, panel: Panel, values: number[], group: ChartGroup): uPlot.Options {
  const muted = color('--text-muted');
  const grid = { stroke: color('--border'), width: 1 };
  const stroke = color(panel.color);
  const axisFont = `11px ${color('--font-ui')}`;
  const axis = { stroke: muted, grid, ticks: grid, font: axisFont };
  const { format, formatCursor } = panel;
  const cursorLabelSpace = CURSOR_LABEL_GAP + widestCursorLabel([values], formatCursor);
  return {
    width,
    height,
    tzDate: (seconds) => uPlot.tzDate(new Date(seconds * 1000), 'Etc/UTC'),
    legend: { show: false },
    cursor: cursor(group, 1),
    hooks: zoomHooks(group),
    plugins: [cursorLabels(formatCursor, 1, undefined, false), legend([{ label: panel.label, color: stroke }])],
    scales: { x: { time: true }, y: { range: (_, min, max) => panel.range(min, max) } },
    axes: [
      { ...axis, ticks: { show: false }, size: 4, values: (_, ticks) => ticks.map(() => '') },
      {
        ...axis,
        size: valueAxisSize(group, cursorLabelSpace, axisFont),
        incrs: panel.incrs,
        values: (_, ticks) => ticks.map(format),
      },
    ],
    series: [
      {},
      {
        label: panel.label,
        stroke,
        width: 1.25,
        fill: `${stroke}1f`,
        fillTo: 0,
        paths: panel.stepped ? uPlot.paths.stepped!({ align: 1 }) : undefined,
      },
    ],
  };
}

function PanelChart(props: { panel: Panel; equity: Equity; group: ChartGroup; fileName: string }) {
  let container!: HTMLDivElement;
  let plot: uPlot | undefined;

  const destroyPlot = () => {
    if (plot) {
      props.group.remove(plot);
      plot.destroy();
      plot = undefined;
    }
  };

  onMount(() => {
    createEffect(() => {
      const values = props.panel.values(props.equity);
      props.group.use(props.equity);
      destroyPlot();
      plot = new uPlot(
        panelOptions(container.clientWidth, container.clientHeight, props.panel, values, props.group),
        [props.equity.times.map(toSeconds), values],
        container,
      );
      props.group.add(plot);
    });
    const resize = new ResizeObserver(() => plot?.setSize({ width: container.clientWidth, height: container.clientHeight }));
    resize.observe(container);
    onCleanup(() => {
      resize.disconnect();
      destroyPlot();
    });
  });

  const showMenu = (event: MouseEvent) => {
    const u = plot;
    if (!u) {
      return;
    }
    const fileName = `${props.fileName}-${props.panel.id}.png`;
    openContextMenu(event, [
      { label: 'Copy image', run: () => copyImage(u, []) },
      { label: 'Save image…', run: () => saveImage(u, [], fileName) },
    ]);
  };

  return <div ref={container} class="chart-panel" onContextMenu={showMenu} />;
}

const EQUITY_MODES: SegmentedOption<EquityMode>[] = [
  {
    value: 'nav',
    label: 'NAV',
    title: 'Account value; deposits and withdrawals show as jumps. Closed positions line: open positions at entry value',
  },
  {
    value: 'return',
    label: 'Return %',
    title: 'Cumulative return, not affected by deposits and withdrawals. Closed positions line: open positions at entry value',
  },
];

/** NAV / Return % switch. */
function EquityModeSwitch(props: { mode: EquityMode; onChange: (mode: EquityMode) => void }) {
  return <Segmented label="Chart" options={EQUITY_MODES} value={props.mode} onChange={props.onChange} />;
}

/** NAV / Return % and Linear / Log switches, e.g. in the tab bar above the chart. */
export function EquityChartSwitches(props: {
  mode: EquityMode;
  onModeChange: (mode: EquityMode) => void;
  scale: ChartScale;
  onScaleChange: (scale: ChartScale) => void;
}) {
  return (
    <>
      <EquityModeSwitch mode={props.mode} onChange={props.onModeChange} />
      <ScaleSwitch scale={props.scale} onChange={props.onScaleChange} />
    </>
  );
}
