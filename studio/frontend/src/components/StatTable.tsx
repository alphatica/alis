import { For } from 'solid-js';
import { formatStat, type Stat } from '../state';

/** Stats as a name / value table, e.g. next to the equity chart. */
export function StatTable(props: { stats: Stat[] }) {
  return (
    <div class="table-wrap stat-table">
      <table class="data">
        <tbody>
          <For each={props.stats}>
            {(stat) => (
              <tr>
                <td>{stat.name}</td>
                <td class="num">{formatStat(stat)}</td>
              </tr>
            )}
          </For>
        </tbody>
      </table>
    </div>
  );
}
