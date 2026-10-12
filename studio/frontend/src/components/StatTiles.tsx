import { For } from 'solid-js';
import { formatStat, type Stat } from '../state';

/** Stat tiles in as many columns as fit, e.g. under the equity chart. */
export function StatTiles(props: { stats: Stat[] }) {
  return (
    <div class="stats stats-below">
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
