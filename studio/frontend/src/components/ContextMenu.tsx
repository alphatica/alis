import { createSignal, For, onCleanup, onMount, Show } from 'solid-js';

export type ContextMenuItem = { label: string; run: () => void };

const [menu, setMenu] = createSignal<{ x: number; y: number; items: ContextMenuItem[] }>();

/**
 * Shows the app's menu instead of the browser's at the pointer. Shift + right click keeps the browser's menu
 * (e.g. for Inspect).
 */
export function openContextMenu(event: MouseEvent, items: ContextMenuItem[]) {
  if (event.shiftKey) {
    return;
  }
  event.preventDefault();
  setMenu({ x: event.clientX, y: event.clientY, items });
}

const close = () => setMenu(undefined);

/** Rendered once for the whole app; closes on a click elsewhere, Escape, scrolling and resizing. */
export function ContextMenu() {
  onMount(() => {
    const onKey = (event: KeyboardEvent) => event.key === 'Escape' && close();
    window.addEventListener('keydown', onKey);
    window.addEventListener('blur', close);
    window.addEventListener('resize', close);
    window.addEventListener('scroll', close, true);
    onCleanup(() => {
      window.removeEventListener('keydown', onKey);
      window.removeEventListener('blur', close);
      window.removeEventListener('resize', close);
      window.removeEventListener('scroll', close, true);
    });
  });
  return (
    <Show when={menu()}>
      {(current) => (
        <div class="context-menu-backdrop" onMouseDown={close} onContextMenu={(event) => (event.preventDefault(), close())}>
          <div
            class="context-menu"
            role="menu"
            ref={(element) => queueMicrotask(() => placeInView(element, current().x, current().y))}
            onMouseDown={(event) => event.stopPropagation()}
          >
            <For each={current().items}>
              {(item, i) => (
                <button
                  type="button"
                  role="menuitem"
                  ref={(button) => i() === 0 && queueMicrotask(() => button.focus())}
                  onClick={() => {
                    close();
                    item.run();
                  }}
                >
                  {item.label}
                </button>
              )}
            </For>
          </div>
        </div>
      )}
    </Show>
  );
}

/** At the pointer, moved left or up when it would not fit in the window. */
function placeInView(element: HTMLElement, x: number, y: number) {
  const { width, height } = element.getBoundingClientRect();
  element.style.left = `${Math.max(0, Math.min(x, window.innerWidth - width))}px`;
  element.style.top = `${Math.max(0, Math.min(y, window.innerHeight - height))}px`;
}
