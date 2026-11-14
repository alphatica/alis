import type uPlot from 'uplot';

/** linear: equal distances are equal changes in value; log: equal distances are equal changes in percent. */
export type ChartScale = 'linear' | 'log';

/**
 * A value axis in log scale. uPlot draws the positions (toLog of the values) on its linear scale, and the axis and the
 * cursor show the values back (fromLog), so the labels stay in the chart's own units.
 */
export type LogAxis = { toLog: (value: number) => number; fromLog: (position: number) => number };

/** Return in %: drawn at 100 × ln(1 + return), so −50 % is as far below 0 as +100 % is above it. */
export const RETURN_LOG: LogAxis = {
  toLog: (change) => 100 * Math.log1p(change / 100),
  fromLog: (position) => Math.expm1(position / 100) * 100,
};

/** Amounts above 0, e.g. the account value. */
export const AMOUNT_LOG: LogAxis = { toLog: Math.log, fromLog: Math.exp };

/** The positions of the values; values the axis cannot show (e.g. an amount of 0, a return of −100 %) are gaps. */
export function toPositions(axis: LogAxis, values: (number | null)[]): (number | null)[] {
  return values.map((value) => {
    if (value == null) {
      return null;
    }
    const position = axis.toLog(value);
    return Number.isFinite(position) ? position : null;
  });
}

// Labels are at least this far apart, in CSS pixels
const TICK_SPACING = 30;
const MAX_CANDIDATES = 2000;

type Candidate = { position: number; roundness: number };

/**
 * How round multiple × 10^exponent is: 0 most, then by the power of 10 it is a multiple of, and 5 and even last
 * digits before odd ones (100, 500, 200, 300 …).
 */
function roundness(multiple: number, exponent: number) {
  if (multiple === 0) {
    return Infinity;
  }
  let digits = Math.abs(multiple);
  let zeros = 0;
  while (digits % 10 === 0) {
    digits /= 10;
    zeros++;
  }
  const last = digits % 10;
  const bonus = (last === 5 ? 0.6 : last % 2 === 0 ? 0.3 : 0) + (digits < 10 ? 0.1 : 0);
  return exponent + zeros + bonus;
}

/** The smallest of 1, 2 and 5 × 10^exponent that is at least raw, as factor × 10^exponent. */
function niceStep(raw: number) {
  const exponent = Math.floor(Math.log10(raw));
  const factor = [1, 2, 5, 10].find((f) => f * 10 ** exponent >= raw * (1 - 1e-9)) ?? 10;
  return factor === 10 ? { factor: 1, exponent: exponent + 1 } : { factor, exponent };
}

/** units × 10^exponent; dividing by a power of 10 instead of multiplying by its inverse keeps decimals exact (3 / 10 is 0.3). */
function decimal(units: number, exponent: number) {
  return exponent >= 0 ? units * 10 ** exponent : units / 10 ** -exponent;
}

/**
 * Round values between the positions min and max. Going up from min, each next one is the next multiple of the nice
 * step (1, 2 or 5 × 10^n) over the value change that TICK_SPACING pixels make there: in log scale that change grows
 * with the value, so the step grows along the axis.
 */
function candidates(axis: LogAxis, min: number, max: number, gap: number): Candidate[] {
  const result: Candidate[] = [];
  const high = axis.fromLog(max);
  let value = axis.fromLog(min);
  let first = true;
  while (result.length < MAX_CANDIDATES) {
    const raw = axis.fromLog(axis.toLog(value) + gap) - value;
    if (!(raw > 0) || !Number.isFinite(raw)) {
      break;
    }
    const { factor, exponent } = niceStep(raw);
    // In units of 10^exponent; the epsilon keeps a value that is a multiple from rounding to just off it
    const units = exponent >= 0 ? value / 10 ** exponent : value * 10 ** -exponent;
    // The first value may be min itself, every next one is above the previous one
    const multiple = first ? Math.ceil(units / factor - 1e-9) : Math.floor(units / factor + 1e-9) + 1;
    first = false;
    // || 0: Math.ceil gives −0 for values between −1 and 0, which would be labeled "-0 %"
    value = decimal(multiple * factor || 0, exponent);
    if (value > high * (1 + Math.sign(high) * 1e-12)) {
      break;
    }
    result.push({ position: axis.toLog(value), roundness: roundness(multiple * factor, exponent) });
  }
  return result;
}

/** uPlot's splits for a log axis: the roundest values that keep the labels TICK_SPACING apart. */
export function logSplits(axis: LogAxis) {
  return (u: uPlot, _axisIdx: number, min: number, max: number): number[] => {
    if (!(max > min)) {
      return [min];
    }
    const height = u.bbox.height / devicePixelRatio || u.height;
    const gap = ((max - min) * TICK_SPACING) / height;
    const kept: number[] = [];
    for (const candidate of candidates(axis, min, max, gap).sort((a, b) => b.roundness - a.roundness)) {
      if (kept.every((position) => Math.abs(position - candidate.position) >= gap)) {
        kept.push(candidate.position);
      }
    }
    return kept.sort((a, b) => a - b);
  };
}
