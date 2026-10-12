import { type Accessor, type JSX, Match, Switch } from 'solid-js';
import type { Store } from '../state';
import { ReportButton } from '../toasts';

/** Shows the store's data; until it arrives "Loading…", or the error if loading failed. */
export function Loaded<T>(props: { store: Store<T>; children: (data: Accessor<T>) => JSX.Element }) {
  return (
    <Switch fallback={<p class="muted">Loading…</p>}>
      <Match when={props.store.data()}>{(data) => props.children(data)}</Match>
      <Match when={props.store.error()}>
        {(error) => (
          <div class="panel">
            <h3>{error().title}</h3>
            <p class="muted">{error().message}</p>
            {error().reportId && <ReportButton reportId={error().reportId!} />}
          </div>
        )}
      </Match>
    </Switch>
  );
}
