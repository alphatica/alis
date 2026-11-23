import type uPlot from 'uplot';

type Range = { min: number; max: number };

let groups = 0;

export class ChartGroup {
  readonly key = `chart-group-${++groups}`;
  private readonly plots = new Set<uPlot>();
  private readonly widths = new Map<uPlot, number>();
  private range: Range | undefined;
  private data: unknown;
  private realignQueued = false;

  use(data: unknown) {
    if (data !== this.data) {
      this.data = data;
      this.range = undefined;
    }
  }

  add(u: uPlot) {
    this.plots.add(u);
    if (this.range) {
      u.setScale('x', this.range);
    }
  }

  remove(u: uPlot) {
    this.plots.delete(u);
    const before = this.widest();
    this.widths.delete(u);
    if (this.widest() !== before) {
      this.realign();
    }
  }

  zoomed(u: uPlot) {
    const { min, max } = u.scales.x;
    if (this.plots.has(u) && min != null && max != null) {
      this.range = { min, max };
    }
  }

  valueAxisWidth(u: uPlot, width: number) {
    const before = this.widest();
    this.widths.set(u, width);
    const after = this.widest();
    if (after !== before) {
      this.realign();
    }
    return after;
  }

  private widest() {
    return Math.max(0, ...this.widths.values());
  }

  private realign() {
    if (this.realignQueued) {
      return;
    }
    this.realignQueued = true;
    queueMicrotask(() => {
      this.realignQueued = false;
      this.plots.forEach((u) => u.redraw(u.scales.x.min != null, true));
    });
  }
}
