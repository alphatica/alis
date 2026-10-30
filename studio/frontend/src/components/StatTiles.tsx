import { For } from 'solid-js';
import { formatStat, type Stat } from '../state';

/**
 * Stat tiles in as many columns as fit, e.g. under the equity chart. Wide: a few tiles stretched over the whole
 * width, each one line high (name left, value right). Results: the stats of a backtest in a fixed number of columns, so
 * they lay out the same on the Backtest and Walk-forward screens.
 */
export function StatTiles(props: { stats: Stat[]; wide?: boolean; results?: boolean }) {
  return (
    <div class="stats stats-below" classList={{ 'stats-wide': props.wide, 'stats-results': props.results }}>
      <For each={props.stats}>
        {(stat) => (
          <div class="stat">
            <div class="stat-name">{stat.name}</div>
            <div class="stat-value">{formatStat(stat)}</div>
          </div>
        )}
      </For>
    </div>
  );
}
