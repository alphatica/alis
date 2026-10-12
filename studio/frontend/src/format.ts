// Values from the backend shown in the user's locale (the browser's language): thousands separators and decimal mark.

const formats = new Map<number, Intl.NumberFormat>();

/** Formatter with exactly `decimals` fraction digits; created once per count. */
export function numberFormat(decimals: number): Intl.NumberFormat {
  let format = formats.get(decimals);
  if (!format) {
    format = new Intl.NumberFormat(undefined, { minimumFractionDigits: decimals, maximumFractionDigits: decimals });
    formats.set(decimals, format);
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
  /** Fraction digits of a NUMBER cell. */
  decimals: number;
  /** Shown after a NUMBER cell, e.g. " %". */
  suffix: string;
};

/** A number with `decimals` fraction digits and `suffix`; a text as it is; '' for null. */
export function formatValue(value: Value | undefined, decimals = 0, suffix = ''): string {
  return typeof value === 'number' ? numberFormat(decimals).format(value) + suffix : (value ?? '');
}

export function formatCell(column: ColumnSpec, value: Value | undefined): string {
  return column.kind === 'NUMBER' ? formatValue(value, column.decimals, column.suffix) : String(value ?? '');
}

export function formatRow(columns: ColumnSpec[], row: Value[]): string[] {
  return columns.map((column, i) => formatCell(column, row[i]));
}

/** Numbers and dates: right-aligned, monospace digits. */
export function isNumeric(column: ColumnSpec): boolean {
  return column.kind !== 'TEXT';
}
