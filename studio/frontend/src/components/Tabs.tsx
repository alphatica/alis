import { For, type JSX, Show } from 'solid-js';

/** actions: controls shown at the right end of the tab bar while the tab is selected */
export type Tab = { label: string; content: () => JSX.Element; actions?: () => JSX.Element };

/** Tab bar with the selected tab's content below. Only the selected tab is rendered. */
export function Tabs(props: { tabs: Tab[]; selected: number; onSelect: (index: number) => void }) {
  return (
    <>
      <div class="tabs" role="tablist">
        <For each={props.tabs}>
          {(tab, i) => (
            <button
              type="button"
              role="tab"
              aria-selected={i() === props.selected}
              onMouseDown={(event) => event.button === 0 && props.onSelect(i())}
              onClick={() => props.onSelect(i())}
            >
              {tab.label}
            </button>
          )}
        </For>
        <Show when={props.tabs[props.selected]?.actions}>{(actions) => <div class="tabs-actions">{actions()()}</div>}</Show>
      </div>
      <div role="tabpanel" class="tabpanel">{props.tabs[props.selected]?.content()}</div>
    </>
  );
}
