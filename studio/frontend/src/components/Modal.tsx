import { createEffect, type JSX, Show } from 'solid-js';

/** Native modal dialog: Escape and the Cancel buttons call onClose. The content is created anew on every opening. */
export function Modal(props: { open: boolean; onClose: () => void; title: string; children: JSX.Element }) {
  let dialog!: HTMLDialogElement;
  createEffect(() => {
    if (props.open && !dialog.open) {
      dialog.showModal();
    } else if (!props.open && dialog.open) {
      dialog.close();
    }
  });
  return (
    <dialog ref={dialog} onClose={() => props.onClose()}>
      <h2 class="dialog-title">{props.title}</h2>
      <Show when={props.open}>{props.children}</Show>
    </dialog>
  );
}
