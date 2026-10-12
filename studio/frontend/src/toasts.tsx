import { createSignal, For, Show } from 'solid-js';
import { api, type ErrorMessage, toErrorMessage } from './api';

type Toast = ErrorMessage & { id: number };

const [toasts, setToasts] = createSignal<Toast[]>([]);
let nextId = 0;

export function pushToast(error: ErrorMessage) {
  setToasts((list) => [...list, { ...error, id: nextId++ }]);
}

export function showError(e: unknown) {
  pushToast(toErrorMessage(e));
}

function remove(id: number) {
  setToasts((list) => list.filter((toast) => toast.id !== id));
}

/** Errors caused by the user hide themselves after a while; unexpected ones stay and can be reported. */
export function Toasts() {
  return (
    <div id="toasts">
      <For each={toasts()}>{(toast) => <ToastView toast={toast} />}</For>
    </div>
  );
}

function ToastView(props: { toast: Toast }) {
  return (
    <div
      class="toast"
      classList={{ 'toast-persistent': props.toast.reportId !== null }}
      role="alert"
      onClick={(event) => {
        if (!(event.target as HTMLElement).closest('button')) {
          remove(props.toast.id);
        }
      }}
      onAnimationEnd={() => remove(props.toast.id)}
    >
      <div class="toast-title">{props.toast.title}</div>
      <div class="toast-message">{props.toast.message}</div>
      <Show when={props.toast.reportId}>
        {(reportId) => (
          <div class="toast-actions">
            <ReportButton reportId={reportId()} />
          </div>
        )}
      </Show>
    </div>
  );
}

/** Sends the error report only when the user asks for it. */
export function ReportButton(props: { reportId: string }) {
  const [reported, setReported] = createSignal(false);
  const report = () =>
    api
      .post(`/api/errors/${props.reportId}/report`)
      .then(() => setReported(true))
      .catch(showError);
  return (
    <Show when={!reported()} fallback={<span class="muted">Reported, thank you.</span>}>
      <button class="btn" onClick={report}>
        Report
      </button>
    </Show>
  );
}
