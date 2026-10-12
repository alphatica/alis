import { createEffect, For, onCleanup, onMount } from 'solid-js';
import uPlot from 'uplot';
import 'uplot/dist/uPlot.min.css';
import { numberFormat } from '../format';
import { type Equity, formatStat, type Stat } from '../state';
import { showError } from '../toasts';
import { openContextMenu } from './ContextMenu';

/** nav: account value, jumps with deposits and withdrawals; return: cumulative return in %, without them. */
export type EquityMode = 'nav' | 'return';

/** yyyymmdd → seconds since the epoch at midnight UTC (uPlot's time unit). */
function toSeconds(time: number) {
  return Date.UTC(Math.floor(time / 10000), (Math.floor(time / 100) % 100) - 1, time % 100) / 1000;
}

function toData(equity: Equity, mode: EquityMode): uPlot.AlignedData {
  return [equity.times.map(toSeconds), mode === 'nav' ? equity.nav : equity.returnPercent];
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
function snapToPoint(u: uPlot, left: number, top: number): [number, number] {
  if (left < 0) {
    return [left, top];
  }
  const idx = u.posToIdx(left);
  const y = u.data[1][idx];
  if (y == null) {
    return [left, top];
  }
  return [Math.round(u.valToPos(u.data[0][idx], 'x')), Math.round(u.valToPos(y, 'y'))];
}

/** Date and value of the point under the cursor, shown on the axes where the crosshair meets them. */
function cursorLabels(formatValue: (value: number) => string): uPlot.Plugin {
  const xLabel = document.createElement('div');
  const yLabel = document.createElement('div');
  xLabel.className = 'chart-cursor-label chart-cursor-x';
  yLabel.className = 'chart-cursor-label chart-cursor-y';
  return {
    hooks: {
      init: (u) => {
        u.over.parentElement!.append(xLabel, yLabel);
      },
      setCursor: (u) => {
        const { left, top, idx } = u.cursor;
        const y = idx == null ? null : u.data[1][idx];
        const visible = left != null && top != null && left >= 0 && y != null;
        xLabel.hidden = yLabel.hidden = !visible;
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
function widestCursorLabel(values: number[], format: (value: number) => string) {
  if (values.length === 0) {
    return 0;
  }
  let min = Infinity;
  let max = -Infinity;
  for (const v of values) {
    min = Math.min(min, v);
    max = Math.max(max, v);
  }
  return Math.max(cursorLabelWidth(format(min)), cursorLabelWidth(format(max)));
}

function options(width: number, height: number, mode: EquityMode, values: number[]): uPlot.Options {
  const muted = color('--text-muted');
  const grid = { stroke: color('--border'), width: 1 };
  const accent = color('--accent');
  const axisFont = `11px ${color('--font-ui')}`;
  const axis = { stroke: muted, grid, ticks: grid, font: axisFont };
  const format = mode === 'nav' ? (v: number) => formatNav.format(v) : (v: number) => `${formatPercent.format(v)} %`;
  const formatCursor = mode === 'nav' ? format : (v: number) => `${formatCursorPercent.format(v)} %`;
  // The cursor's label sits left of the plot, over the axis: the axis is wide enough for any value, so it is not cut off
  const cursorLabelSpace = CURSOR_LABEL_GAP + widestCursorLabel(values, formatCursor);
  const valueAxisSize = (_: uPlot, ticks: string[] | null) =>
    Math.ceil(
      Math.max(
        VALUE_AXIS_MIN_WIDTH,
        cursorLabelSpace,
        TICK_SPACE + Math.max(0, ...(ticks ?? []).map((tick) => textWidth(tick, axisFont))),
      ),
    );
  return {
    width,
    height,
    // Sessions are dates, not instants: show them the same in every time zone.
    tzDate: (seconds) => uPlot.tzDate(new Date(seconds * 1000), 'Etc/UTC'),
    legend: { show: false },
    cursor: { drag: { x: true, y: false }, move: snapToPoint },
    plugins: [cursorLabels(formatCursor)],
    scales: { x: { time: true } },
    axes: [axis, { ...axis, size: valueAxisSize, values: (_, values) => values.map(format) }],
    series: [
      {},
      {
        label: mode === 'nav' ? 'NAV' : 'Return',
        stroke: accent,
        width: 1.5,
        // NAV is filled down to the axis; return is filled to 0 %, so gains and losses are visible.
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
 * Account value or return over time. Drag to zoom, double-click to zoom out. Right click: copy or save as PNG,
 * together with the stats (shown under the chart by StatTiles). The plot fills its container and follows its size.
 */
export function EquityChart(props: { equity: Equity; mode: EquityMode; stats: Stat[]; fileName: string }) {
  let container!: HTMLDivElement;
  let plot: uPlot | undefined;

  onMount(() => {
    // Rebuilt when the data or the mode change: axis formats and series differ between modes.
    createEffect(() => {
      const data = toData(props.equity, props.mode);
      plot?.destroy();
      plot = new uPlot(
        options(container.clientWidth, container.clientHeight, props.mode, data[1] as number[]),
        data,
        container,
      );
    });
    const resize = new ResizeObserver(() => plot?.setSize({ width: container.clientWidth, height: container.clientHeight }));
    resize.observe(container);
    onCleanup(() => {
      resize.disconnect();
      plot?.destroy();
      plot = undefined;
    });
  });

  const showMenu = (event: MouseEvent) => {
    const u = plot;
    if (!u) {
      return;
    }
    const fileName = `${props.fileName}-${props.mode === 'nav' ? 'nav' : 'return'}.png`;
    openContextMenu(event, [
      { label: 'Copy image', run: () => copyImage(u, props.stats) },
      { label: 'Save image…', run: () => saveImage(u, props.stats, fileName) },
    ]);
  };

  return <div ref={container} class="chart" onContextMenu={showMenu} />;
}

const EQUITY_MODES: { mode: EquityMode; label: string; title: string }[] = [
  { mode: 'nav', label: 'NAV', title: 'Account value; deposits and withdrawals show as jumps' },
  { mode: 'return', label: 'Return %', title: 'Cumulative return, not affected by deposits and withdrawals' },
];

/** NAV / Return % switch, e.g. in the tab bar above the chart. */
export function EquityModeSwitch(props: { mode: EquityMode; onChange: (mode: EquityMode) => void }) {
  return (
    <div class="segmented" role="group" aria-label="Chart">
      <For each={EQUITY_MODES}>
        {(item) => (
          <button
            type="button"
            aria-pressed={props.mode === item.mode}
            title={item.title}
            onClick={() => props.onChange(item.mode)}
          >
            {item.label}
          </button>
        )}
      </For>
    </div>
  );
}
