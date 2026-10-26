// Values from the backend shown in the user's locale (the browser's language): thousands separators and decimal mark.

const formats = new Map<string, Intl.NumberFormat>();

/** Formatter with `decimals` to `maxDecimals` fraction digits; created once per pair. */
export function numberFormat(decimals: number, maxDecimals = decimals): Intl.NumberFormat {
  const key = `${decimals}:${maxDecimals}`;
  let format = formats.get(key);
  if (!format) {
    format = new Intl.NumberFormat(undefined, { minimumFractionDigits: decimals, maximumFractionDigits: maxDecimals });
    formats.set(key, format);
  }
  return format;
}

/**
 * A table cell or a stat: a number, a text, or null when empty or unknown.
 * Numbers that are not finite arrive as text ("NaN", "Infinity").
 */
export type Value = string | number | null;

/** Column of a table as sent by the backend (Column.java). */
export type ColumnSpec = {
  title: string;
  /** DATE cells are yyyymmdd numbers. */
  kind: 'TEXT' | 'DATE' | 'NUMBER';
  /** Fraction digits of a NUMBER cell: at least `decimals`, at most `maxDecimals`. */
  decimals: number;
  maxDecimals: number;
  /** Shown after a NUMBER cell, e.g. " %". */
  suffix: string;
};

/** A number with `decimals` (up to `maxDecimals`) fraction digits and `suffix`; a text as it is; '' for null. */
export function formatValue(value: Value | undefined, decimals = 0, suffix = '', maxDecimals = decimals): string {
  return typeof value === 'number' ? numberFormat(decimals, maxDecimals).format(value) + suffix : (value ?? '');
}

export function formatCell(column: ColumnSpec, value: Value | undefined): string {
  return column.kind === 'NUMBER'
    ? formatValue(value, column.decimals, column.suffix, column.maxDecimals)
    : String(value ?? '');
}

export function formatRow(columns: ColumnSpec[], row: Value[]): string[] {
  return columns.map((column, i) => formatCell(column, row[i]));
}

/** Numbers and dates: right-aligned, monospace digits. */
export function isNumeric(column: ColumnSpec): boolean {
  return column.kind !== 'TEXT';
}
