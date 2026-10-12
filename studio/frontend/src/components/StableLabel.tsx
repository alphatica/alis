import { For } from 'solid-js';

/**
 * Text that switches between labels (e.g. "Show signals" / "Running…") without changing width:
 * all labels are laid out on top of each other and only the current one is visible.
 */
export function StableLabel(props: { labels: string[]; current: number }) {
  return (
    <span class="stable-label">
      <For each={props.labels}>{(label, i) => <span aria-hidden={i() !== props.current}>{label}</span>}</For>
    </span>
  );
}
